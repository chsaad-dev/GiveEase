package com.example.giveease.donor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.databinding.FragmentDonorFeedBinding
import com.example.giveease.donor.adapter.CampaignAdapter

class DonorFeedFragment : Fragment() {

    private lateinit var binding: FragmentDonorFeedBinding
    private lateinit var viewModel: DonorViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDonorFeedBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(requireActivity())[DonorViewModel::class.java]

        binding.recyclerViewCampaigns.layoutManager = LinearLayoutManager(requireContext())

        viewModel.campaigns.observe(viewLifecycleOwner) { campaignList ->
            binding.recyclerViewCampaigns.adapter = CampaignAdapter(campaignList)
        }

        viewModel.fetchCampaigns()

        return binding.root
    }
}

