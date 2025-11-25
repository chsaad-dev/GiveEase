package com.example.giveease.donor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.databinding.FragmentDonorFeedBinding
import com.example.giveease.donor.adapter.CampaignAdapter
import com.example.giveease.ngo.CampaignData
import com.google.firebase.firestore.FirebaseFirestore

class DonorFeedFragment : Fragment() {

    private var _binding: FragmentDonorFeedBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var campaignAdapter: CampaignAdapter
    private val campaignList = mutableListOf<CampaignData>()
    private var allCampaigns = listOf<CampaignData>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDonorFeedBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupFilters()
        loadCampaigns()

        return binding.root
    }

    private fun setupRecyclerView() {
        campaignAdapter = CampaignAdapter(campaignList) { campaign ->
            onCampaignClick(campaign)
        }

        binding.recyclerViewCampaigns.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = campaignAdapter
        }
    }

    private fun setupFilters() {
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) filterCampaigns("All")
        }

        binding.chipHealth.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) filterCampaigns("Medical & Healthcare")
        }

        binding.chipFood.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) filterCampaigns("Food & Nutrition")
        }

        binding.chipEducation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) filterCampaigns("Education")
        }
    }

    private fun loadCampaigns() {
        firestore.collection("campaigns")
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { documents ->
                allCampaigns = documents.mapNotNull { doc ->
                    try {
                        CampaignData(
                            id = doc.id,
                            ngoId = doc.getString("ngoId") ?: "",
                            ngoName = doc.getString("ngoName") ?: "",
                            category = doc.getString("category") ?: "",
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            targetQuantity = doc.getLong("targetQuantity")?.toInt() ?: 0,
                            currentQuantity = doc.getLong("currentQuantity")?.toInt() ?: 0,
                            unit = doc.getString("unit") ?: "",
                            endDate = doc.getLong("endDate") ?: 0,
                            urgencyLevel = doc.getString("urgencyLevel") ?: "",
                            itemCondition = doc.getString("itemCondition") ?: "",
                            specificRequirements = doc.getString("specificRequirements") ?: "",
                            autoClose = doc.getBoolean("autoClose") ?: false,
                            imageUrls = (doc.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            createdAt = doc.getLong("createdAt") ?: 0,
                            status = doc.getString("status") ?: "Active",
                            donorCount = doc.getLong("donorCount")?.toInt() ?: 0,
                            shareCount = doc.getLong("shareCount")?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                campaignList.clear()
                campaignList.addAll(allCampaigns)
                campaignAdapter.notifyDataSetChanged()

                if (campaignList.isEmpty()) {
                    Toast.makeText(requireContext(), "No campaigns available", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error loading campaigns: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun filterCampaigns(category: String) {
        campaignList.clear()

        if (category == "All") {
            campaignList.addAll(allCampaigns)
        } else {
            campaignList.addAll(allCampaigns.filter { it.category == category })
        }

        campaignAdapter.notifyDataSetChanged()
    }

    private fun onCampaignClick(campaign: CampaignData) {
        Toast.makeText(requireContext(), "Opening ${campaign.title}", Toast.LENGTH_SHORT).show()
        // TODO: Navigate to campaign details
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}