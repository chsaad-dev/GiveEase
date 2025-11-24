package com.example.giveease.ngo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.databinding.FragmentManageCampaignsBinding
import com.google.firebase.database.*

class ManageCampaignsFragment : Fragment() {

    private var _binding: FragmentManageCampaignsBinding? = null
    private val binding get() = _binding!!

    private lateinit var campaignAdapter: ManageCampaignAdapter
    private val campaignsList = mutableListOf<CampaignData>()

    private lateinit var database: DatabaseReference

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentManageCampaignsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        database = FirebaseDatabase.getInstance().getReference("campaigns")

        setupClickListeners()
        setupRecyclerView()
        loadCampaigns()
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnCreateNewCampaign.setOnClickListener {
            // Navigate to create campaign
            // findNavController().navigate(R.id.action_manageCampaigns_to_createCampaign)
        }
    }

    private fun setupRecyclerView() {
        campaignAdapter = ManageCampaignAdapter(
            onEditClick = { campaign ->
                editCampaign(campaign)
            },
            onCampaignClick = { campaign ->
                viewCampaignDetails(campaign)
            }
        )

        // If you have RecyclerView in layout, setup here
        // binding.recyclerViewCampaigns.apply {
        //     layoutManager = LinearLayoutManager(requireContext())
        //     adapter = campaignAdapter
        // }
    }

    private fun loadCampaigns() {
        val ngoId = getCurrentNgoId()

        database.orderByChild("ngoId").equalTo(ngoId)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    campaignsList.clear()

                    for (campaignSnapshot in snapshot.children) {
                        val campaign = campaignSnapshot.getValue(CampaignData::class.java)
                        campaign?.let { campaignsList.add(it) }
                    }

                    campaignAdapter.submitList(campaignsList.toList())
                    updateStats()
                }

                override fun onCancelled(error: DatabaseError) {
                    // Handle error
                }
            })
    }

    private fun updateStats() {
        val totalRaised = campaignsList.sumOf { it.currentQuantity }
        val activeCampaigns = campaignsList.count { it.status == "Active" }

        // Update UI with stats
    }

    private fun editCampaign(campaign: CampaignData) {
        // Navigate to edit screen
    }

    private fun viewCampaignDetails(campaign: CampaignData) {
        // Navigate to details screen
    }

    private fun getCurrentNgoId(): String {
        return "ngo_${System.currentTimeMillis()}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
