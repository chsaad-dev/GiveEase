package com.example.giveease.donor

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.R
import com.example.giveease.databinding.FragmentDonorCampaignsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class DonorCampaignsFragment : Fragment() {
    private lateinit var binding: FragmentDonorCampaignsBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var campaignAdapter: DonorCampaignAdapter
    private val campaignsList = mutableListOf<DonorCampaign>()
    private var currentFilter = "active"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDonorCampaignsBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupClickListeners()
        loadCampaigns()

        return binding.root
    }

    private fun setupRecyclerView() {
        campaignAdapter = DonorCampaignAdapter(
            campaigns = campaignsList,
            onViewContacts = { campaign -> viewCampaignContacts(campaign) },
            onEditCampaign = { campaign -> editCampaign(campaign) },
            onItemClick = { campaign -> showCampaignDetails(campaign) }
        )

        binding.recyclerViewCampaigns.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = campaignAdapter
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            btnAddCampaign.setOnClickListener {
                navigateToCreateCampaign()
            }

            btnCreateFirstCampaign.setOnClickListener {
                navigateToCreateCampaign()
            }

            btnFilterActive.setOnClickListener {
                updateFilter("active")
            }

            btnFilterCompleted.setOnClickListener {
                updateFilter("completed")
            }

            btnFilterCancelled.setOnClickListener {
                updateFilter("cancelled")
            }
        }
    }

    private fun updateFilter(filter: String) {
        currentFilter = filter

        binding.apply {
            btnFilterActive.backgroundTintList = if (filter == "active")
                ContextCompat.getColorStateList(requireContext(), R.color.secondary)
            else ContextCompat.getColorStateList(requireContext(), android.R.color.transparent)

            btnFilterCompleted.backgroundTintList = if (filter == "completed")
                ContextCompat.getColorStateList(requireContext(), R.color.secondary)
            else ContextCompat.getColorStateList(requireContext(), android.R.color.transparent)

            btnFilterCancelled.backgroundTintList = if (filter == "cancelled")
                ContextCompat.getColorStateList(requireContext(), R.color.secondary)
            else ContextCompat.getColorStateList(requireContext(), android.R.color.transparent)
        }

        loadCampaigns()
    }

    private fun loadCampaigns() {
        val userId = auth.currentUser?.uid ?: return

        var query = firestore.collection("donor_campaigns")
            .whereEqualTo("donorId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        if (currentFilter != "all") {
            query = query.whereEqualTo("status", currentFilter)
        }

        query.get()
            .addOnSuccessListener { documents ->
                val campaigns = documents.mapNotNull { doc ->
                    try {
                        DonorCampaign(
                            id = doc.id,
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            amount = doc.getDouble("amount") ?: 0.0,
                            category = doc.getString("category") ?: "",
                            status = doc.getString("status") ?: "active",
                            contactCount = doc.getLong("contactCount")?.toInt() ?: 0,
                            viewCount = doc.getLong("viewCount")?.toInt() ?: 0,
                            hasProof = doc.getBoolean("hasProof") ?: false,
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            interestedNGOs = doc.get("interestedNGOs") as? List<String> ?: emptyList()
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                updateUI(campaigns)
                updateSummary(campaigns)
            }
            .addOnFailureListener {
                showDummyData()
            }
    }

    private fun updateUI(campaigns: List<DonorCampaign>) {
        campaignsList.clear()
        campaignsList.addAll(campaigns)
        campaignAdapter.notifyDataSetChanged()

        if (campaigns.isEmpty()) {
            binding.recyclerViewCampaigns.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.recyclerViewCampaigns.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    private fun updateSummary(campaigns: List<DonorCampaign>) {
        val activeCampaigns = campaigns.count { it.status == "active" }
        val totalContacts = campaigns.sumOf { it.contactCount }

        binding.tvTotalCampaigns.text = activeCampaigns.toString()
        binding.tvTotalContacts.text = totalContacts.toString()
    }

    private fun showDummyData() {
        val dummyCampaigns = listOf(
            DonorCampaign(
                id = "1",
                title = "Help Flood Victims",
                description = "I want to donate Rs 10,000 for flood relief efforts. Looking for verified NGOs working in affected areas.",
                amount = 10000.0,
                category = "Disaster Relief",
                status = "active",
                contactCount = 3,
                viewCount = 24,
                hasProof = true,
                createdAt = System.currentTimeMillis() - 172800000,
                interestedNGOs = listOf("ngo1", "ngo2", "ngo3")
            ),
            DonorCampaign(
                id = "2",
                title = "Education Support for Children",
                description = "Offering Rs 5,000 for educational supplies and books for underprivileged children.",
                amount = 5000.0,
                category = "Education",
                status = "active",
                contactCount = 2,
                viewCount = 15,
                hasProof = false,
                createdAt = System.currentTimeMillis() - 604800000,
                interestedNGOs = listOf("ngo4", "ngo5")
            ),
            DonorCampaign(
                id = "3",
                title = "Medical Emergency Fund",
                description = "Completed donation of Rs 8,000 for cancer patient treatment through Shaukat Khanum.",
                amount = 8000.0,
                category = "Healthcare",
                status = "completed",
                contactCount = 1,
                viewCount = 32,
                hasProof = true,
                createdAt = System.currentTimeMillis() - 1209600000,
                interestedNGOs = listOf("ngo6")
            )
        )

        updateUI(dummyCampaigns)
        updateSummary(dummyCampaigns)
    }

    private fun viewCampaignContacts(campaign: DonorCampaign) {
        Toast.makeText(requireContext(), "Viewing contacts for: ${campaign.title}", Toast.LENGTH_SHORT).show()
        // TODO: Navigate to contacts screen showing interested NGOs
    }

    private fun editCampaign(campaign: DonorCampaign) {
        Toast.makeText(requireContext(), "Edit campaign: ${campaign.title}", Toast.LENGTH_SHORT).show()
        // TODO: Navigate to edit campaign screen
    }

    private fun showCampaignDetails(campaign: DonorCampaign) {
        Toast.makeText(requireContext(), "Campaign details: ${campaign.title}", Toast.LENGTH_SHORT).show()
        // TODO: Navigate to campaign detail screen
    }

    private fun navigateToCreateCampaign() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_donor, CreateCampaignFragment())
            .addToBackStack(null)
            .commit()
    }

    data class DonorCampaign(
        val id: String,
        val title: String,
        val description: String,
        val amount: Double,
        val category: String,
        val status: String,
        val contactCount: Int,
        val viewCount: Int,
        val hasProof: Boolean,
        val createdAt: Long,
        val interestedNGOs: List<String>
    )
}