package com.example.giveease.ngo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.giveease.ngo.model.Campaign
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.R
import com.example.giveease.databinding.FragmentNgoHomeBinding

class NgoHomeFragment : Fragment() {

    private var _binding: FragmentNgoHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var campaignAdapter: RecentCampaignAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNgoHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViews()
        setupClickListeners()
        loadData()
    }

    private fun setupViews() {
        // Setup NGO info
        binding.tvNgoName.text = "Edhi Foundation"

        // Setup stats
        updateStats()

        // Setup recent campaigns recycler (if you want multiple campaigns)
        // setupCampaignsRecycler()
    }

    private fun setupClickListeners() {
        binding.ivNotifications.setOnClickListener {
            // Navigate to notifications
            // findNavController().navigate(R.id.action_ngoHome_to_notifications)
        }

        binding.btnCreateNewCampaign.setOnClickListener {
            // Navigate to create campaign
            // findNavController().navigate(R.id.action_ngoHome_to_createCampaign)
        }

        binding.tvViewAllCampaigns.setOnClickListener {
            // Navigate to all campaigns
            // findNavController().navigate(R.id.action_ngoHome_to_allCampaigns)
        }
    }

    private fun updateStats() {
        // These would come from API in real implementation
        val activeCampaigns = 12
        val totalDonations = "₨ 1.85M"

        // Update UI (if you add IDs to the TextViews in XML)
        // binding.tvActiveCampaigns.text = activeCampaigns.toString()
        // binding.tvTotalDonations.text = totalDonations
    }

    private fun loadData() {
        // Simulate loading data
        // In real app, fetch from API
        val campaigns = getSampleCampaigns()

        // Update UI with campaign data
        // campaignAdapter.submitList(campaigns)
    }

    private fun getSampleCampaigns(): List<Campaign> {
        return listOf(
            Campaign(
                id = "1",
                title = "Flood Relief Emergency",
                description = "Urgent aid for flood victims",
                targetAmount = 150000.0,
                raisedAmount = 85000.0,
                status = "Active",
                daysLeft = 15
            ),
            Campaign(
                id = "2",
                title = "Education for All",
                description = "Building schools in rural areas",
                targetAmount = 200000.0,
                raisedAmount = 120000.0,
                status = "Active",
                daysLeft = 30
            )
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
