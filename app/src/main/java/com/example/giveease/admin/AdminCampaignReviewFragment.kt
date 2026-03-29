package com.example.giveease.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.databinding.FragmentAdminCampaignReviewBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore

class AdminCampaignReviewFragment : Fragment() {

    private var _binding: FragmentAdminCampaignReviewBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: AdminCampaignAdapter
    private var allCampaigns = mutableListOf<AdminCampaign>()
    private var currentFilter = "All"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminCampaignReviewBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupChipFilters()
        setupClickListeners()
        loadCampaigns()
    }

    private fun setupRecyclerView() {
        adapter = AdminCampaignAdapter { campaign ->
            showCampaignActionDialog(campaign)
        }
        binding.recyclerViewCampaigns.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewCampaigns.adapter = adapter
    }

    private fun setupChipFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when {
                checkedIds.contains(binding.chipAll.id) -> "All"
                checkedIds.contains(binding.chipActive.id) -> "Active"
                checkedIds.contains(binding.chipCompleted.id) -> "Completed"
                checkedIds.contains(binding.chipDeactivated.id) -> "Deactivated"
                else -> "All"
            }
            applyFilter()
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadCampaigns() {
        if (!isAdded || _binding == null) return

        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        firestore.collection("campaigns")
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                binding.progressBar.visibility = View.GONE
                allCampaigns.clear()

                for (doc in documents) {
                    val imageUrls = doc.get("imageUrls") as? List<String>
                    val campaign = AdminCampaign(
                        id = doc.id,
                        title = doc.getString("title") ?: "",
                        ngoName = doc.getString("ngoName") ?: "",
                        status = doc.getString("status") ?: "Active",
                        category = doc.getString("category") ?: "",
                        urgencyLevel = doc.getString("urgencyLevel") ?: "",
                        imageUrl = imageUrls?.firstOrNull() ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0,
                        targetQuantity = doc.getLong("targetQuantity") ?: 0,
                        currentQuantity = doc.getLong("currentQuantity") ?: 0
                    )
                    allCampaigns.add(campaign)
                }

                allCampaigns.sortByDescending { it.createdAt }
                applyFilter()
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener

                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyFilter() {
        val filtered = if (currentFilter == "All") {
            allCampaigns
        } else {
            allCampaigns.filter { it.status == currentFilter }
        }

        binding.tvCampaignCount.text = "${filtered.size} campaigns"

        if (filtered.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerViewCampaigns.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerViewCampaigns.visibility = View.VISIBLE
            adapter.submitList(filtered)
        }
    }

    private fun showCampaignActionDialog(campaign: AdminCampaign) {
        if (!isAdded) return

        val progress = if (campaign.targetQuantity > 0) {
            ((campaign.currentQuantity.toFloat() / campaign.targetQuantity) * 100).toInt()
        } else 0

        val details = """
            Title: ${campaign.title}
            NGO: ${campaign.ngoName}
            Category: ${campaign.category}
            Urgency: ${campaign.urgencyLevel}
            Progress: ${campaign.currentQuantity}/${campaign.targetQuantity} ($progress%)
            Status: ${campaign.status}
        """.trimIndent()

        val actionText = if (campaign.status == "Active") "Deactivate Campaign" else "Reactivate Campaign"

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Campaign Details")
            .setMessage(details)
            .setPositiveButton(actionText) { _, _ ->
                if (campaign.status == "Active") {
                    showDeactivateConfirmation(campaign)
                } else {
                    reactivateCampaign(campaign)
                }
            }
            .setNegativeButton("Close", null)
            .show()
    }

    private fun showDeactivateConfirmation(campaign: AdminCampaign) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Deactivate Campaign?")
            .setMessage("This will hide \"${campaign.title}\" from all donors. The NGO will be notified.\n\nAre you sure?")
            .setPositiveButton("Deactivate") { _, _ ->
                updateCampaignStatus(campaign, "Deactivated")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun reactivateCampaign(campaign: AdminCampaign) {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Reactivate Campaign?")
            .setMessage("This will make \"${campaign.title}\" visible to donors again.")
            .setPositiveButton("Reactivate") { _, _ ->
                updateCampaignStatus(campaign, "Active")
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun updateCampaignStatus(campaign: AdminCampaign, newStatus: String) {
        val updates = hashMapOf<String, Any>(
            "status" to newStatus,
            "updatedAt" to System.currentTimeMillis(),
            "adminAction" to if (newStatus == "Deactivated") "deactivated" else "reactivated",
            "adminActionAt" to System.currentTimeMillis()
        )

        firestore.collection("campaigns").document(campaign.id)
            .update(updates)
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener

                val actionText = if (newStatus == "Deactivated") "deactivated" else "reactivated"
                Toast.makeText(
                    requireContext(),
                    "Campaign $actionText successfully",
                    Toast.LENGTH_SHORT
                ).show()

                val actionType = if (newStatus == "Deactivated") "deactivate_campaign" else "reactivate_campaign"
                AdminLogger.logAction(actionType, "Update Campaign Status", "Admin $actionText campaign \"${campaign.title}\" by ${campaign.ngoName}")
                
                loadCampaigns()
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener

                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
