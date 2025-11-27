package com.example.giveease.ngo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.R
import com.example.giveease.databinding.FragmentMyCampaignsBinding
import com.example.giveease.ngo.CampaignData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class MyCampaignsFragment : Fragment() {

    private var _binding: FragmentMyCampaignsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: CampaignManagementAdapter
    private val campaignsList = mutableListOf<CampaignData>()
    private var currentFilter = "All"
    private var campaignsListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyCampaignsBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        setupFilterChips()
        setupClickListeners()
        loadCampaigns()
    }

    private fun setupRecyclerView() {
        adapter = CampaignManagementAdapter(
            onEditClick = { campaign ->
                navigateToEditCampaign(campaign)
            },
            onStatusChangeClick = { campaign ->
                toggleCampaignStatus(campaign)
            },
            onDeleteClick = { campaign ->
                showDeleteConfirmation(campaign)
            },
            onCampaignClick = { campaign ->
                Toast.makeText(requireContext(), "Campaign details coming soon", Toast.LENGTH_SHORT).show()
            }
        )

        binding.recyclerViewCampaigns.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MyCampaignsFragment.adapter
        }
    }

    private fun setupFilterChips() {
        binding.chipAll.setOnClickListener {
            currentFilter = "All"
            updateFilterUI("All")
            filterCampaigns()
        }

        binding.chipActive.setOnClickListener {
            currentFilter = "Active"
            updateFilterUI("Active")
            filterCampaigns()
        }

        binding.chipPaused.setOnClickListener {
            currentFilter = "Paused"
            updateFilterUI("Paused")
            filterCampaigns()
        }

        binding.chipCompleted.setOnClickListener {
            currentFilter = "Completed"
            updateFilterUI("Completed")
            filterCampaigns()
        }
    }

    private fun updateFilterUI(selected: String) {
        binding.chipAll.isChecked = selected == "All"
        binding.chipActive.isChecked = selected == "Active"
        binding.chipPaused.isChecked = selected == "Paused"
        binding.chipCompleted.isChecked = selected == "Completed"
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnCreateNewCampaign.setOnClickListener {
            navigateToCreateCampaign()
        }
    }

    private fun loadCampaigns() {
        val userId = auth.currentUser?.uid ?: return
        if (!isAdded || _binding == null) return

        binding.progressBar.visibility = View.VISIBLE

        // Remove old listener if exists
        campaignsListener?.remove()

        // Set up real-time listener
        campaignsListener = firestore.collection("campaigns")
            .whereEqualTo("ngoId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, error ->
                // Check if fragment is still alive
                if (!isAdded || _binding == null) {
                    campaignsListener?.remove()
                    return@addSnapshotListener
                }

                binding.progressBar.visibility = View.GONE

                if (error != null) {
                    return@addSnapshotListener
                }

                campaignsList.clear()
                snapshots?.documents?.forEach { doc ->
                    val campaign = doc.toObject(CampaignData::class.java)
                    campaign?.let { campaignsList.add(it.copy(id = doc.id)) }
                }

                updateStats()
                filterCampaigns()
            }
    }

    private fun filterCampaigns() {
        if (!isAdded || _binding == null) return

        val filteredList = when (currentFilter) {
            "Active" -> campaignsList.filter { it.status == "Active" }
            "Paused" -> campaignsList.filter { it.status == "Paused" }
            "Completed" -> campaignsList.filter { it.status == "Completed" }
            else -> campaignsList
        }

        adapter.submitList(filteredList)

        // Show/hide empty state
        if (filteredList.isEmpty()) {
            binding.recyclerViewCampaigns.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE

            val emptyMessage = when (currentFilter) {
                "Active" -> "No active campaigns"
                "Paused" -> "No paused campaigns"
                "Completed" -> "No completed campaigns"
                else -> "No campaigns yet.\nCreate your first campaign!"
            }
            binding.tvEmptyMessage.text = emptyMessage
        } else {
            binding.recyclerViewCampaigns.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }
    }

    private fun updateStats() {
        if (!isAdded || _binding == null) return

        val totalCampaigns = campaignsList.size
        val activeCampaigns = campaignsList.count { it.status == "Active" }
        val totalItems = campaignsList.sumOf { it.currentQuantity }

        binding.tvTotalCampaigns.text = totalCampaigns.toString()
        binding.tvActiveCampaigns.text = activeCampaigns.toString()
        binding.tvTotalItemsCollected.text = "$totalItems Items"
    }

    private fun toggleCampaignStatus(campaign: CampaignData) {
        if (!isAdded) return

        if (campaign.status == "Completed") {
            Toast.makeText(requireContext(), "Cannot change status of completed campaigns", Toast.LENGTH_SHORT).show()
            return
        }

        val newStatus = if (campaign.status == "Active") "Paused" else "Active"
        val statusText = if (newStatus == "Active") "activated" else "paused"

        firestore.collection("campaigns").document(campaign.id)
            .update("status", newStatus)
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Campaign $statusText successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Failed to update status: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDeleteConfirmation(campaign: CampaignData) {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Delete Campaign")
            .setMessage("Are you sure you want to delete \"${campaign.title}\"? This action cannot be undone.")
            .setPositiveButton("Delete") { dialog, _ ->
                deleteCampaign(campaign)
                dialog.dismiss()
            }
            .setNegativeButton("Cancel") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun deleteCampaign(campaign: CampaignData) {
        if (!isAdded) return

        firestore.collection("campaigns").document(campaign.id)
            .delete()
            .addOnSuccessListener {
                if (!isAdded) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Campaign deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                if (!isAdded) return@addOnFailureListener
                Toast.makeText(requireContext(), "Failed to delete campaign: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToEditCampaign(campaign: CampaignData) {
        if (!isAdded) return

        val fragment = EditCampaignFragment().apply {
            arguments = Bundle().apply {
                putSerializable("campaign", campaign)
            }
        }

        val containerId = (view?.parent as? View)?.id ?: return

        parentFragmentManager.beginTransaction()
            .replace(containerId, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToCreateCampaign() {
        if (!isAdded) return

        val containerId = (view?.parent as? View)?.id ?: return

        parentFragmentManager.beginTransaction()
            .replace(containerId, CreateCampaignFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        campaignsListener?.remove()
        campaignsListener = null
        _binding = null
    }
}