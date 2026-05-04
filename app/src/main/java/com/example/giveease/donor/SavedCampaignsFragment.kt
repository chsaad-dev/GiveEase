package com.example.giveease.donor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.databinding.FragmentSavedCampaignsBinding
import com.example.giveease.donor.adapter.CampaignAdapter
import com.example.giveease.ngo.CampaignData
import com.example.giveease.utils.UserManager
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore

class SavedCampaignsFragment : Fragment() {

    private var _binding: FragmentSavedCampaignsBinding? = null
    private val binding get() = _binding!!
    private lateinit var campaignAdapter: CampaignAdapter
    private val firestore = FirebaseFirestore.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSavedCampaignsBinding.inflate(inflater, container, false)

        setupRecyclerView()
        binding.btnBack.setOnClickListener { parentFragmentManager.popBackStack() }

        loadSavedCampaigns()

        return binding.root
    }

    private fun setupRecyclerView() {
        campaignAdapter = CampaignAdapter { campaign ->
            val detailsFragment = CampaignDetailsFragment.newInstance(campaign)
            parentFragmentManager.beginTransaction()
                .hide(this)
                .add((requireView().parent as ViewGroup).id, detailsFragment)
                .addToBackStack(null)
                .commit()
        }
        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = campaignAdapter
        }
    }

    private fun loadSavedCampaigns() {
        val userId = UserManager.getUserId(requireContext())
        if (userId.isEmpty()) {
            showEmptyState()
            return
        }

        binding.shimmerLayout.visibility = View.VISIBLE
        binding.shimmerLayout.startShimmer()
        binding.recyclerView.visibility = View.GONE
        binding.layoutEmpty.visibility = View.GONE

        firestore.collection("users").document(userId)
            .collection("favorites")
            .get()
            .addOnSuccessListener { favDocs ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                
                if (favDocs.isEmpty) {
                    showEmptyState()
                    return@addOnSuccessListener
                }

                val campaignIds = favDocs.documents.mapNotNull { it.getString("campaignId") }
                if (campaignIds.isEmpty()) {
                    showEmptyState()
                    return@addOnSuccessListener
                }

                // Chunking to handle 'in' query limit of 10
                val campaignsList = mutableListOf<CampaignData>()
                val chunks = campaignIds.chunked(10)
                var completedChunks = 0

                for (chunk in chunks) {
                    firestore.collection("campaigns")
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .addOnSuccessListener { campaignDocs ->
                            if (!isAdded || _binding == null) return@addOnSuccessListener
                            
                            for (doc in campaignDocs) {
                                val campaign = doc.toObject(CampaignData::class.java).copy(id = doc.id)
                                campaignsList.add(campaign)
                            }

                            completedChunks++
                            if (completedChunks == chunks.size) {
                                showCampaigns(campaignList = campaignsList)
                            }
                        }
                        .addOnFailureListener {
                            completedChunks++
                            if (completedChunks == chunks.size) {
                                showCampaigns(campaignsList)
                            }
                        }
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                showEmptyState()
                Toast.makeText(requireContext(), "Failed to load saved campaigns", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showCampaigns(campaignList: List<CampaignData>) {
        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.visibility = View.GONE

        if (campaignList.isEmpty()) {
            showEmptyState()
        } else {
            binding.recyclerView.visibility = View.VISIBLE
            binding.layoutEmpty.visibility = View.GONE
            campaignAdapter.submitList(campaignList)
        }
    }

    private fun showEmptyState() {
        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.visibility = View.GONE
        binding.recyclerView.visibility = View.GONE
        binding.layoutEmpty.visibility = View.VISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
