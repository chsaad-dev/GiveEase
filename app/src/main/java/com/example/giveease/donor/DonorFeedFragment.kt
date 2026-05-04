package com.example.giveease.donor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.giveease.R
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.databinding.FragmentDonorFeedBinding
import com.example.giveease.donor.adapter.CampaignAdapter
import com.example.giveease.ngo.CampaignData
import com.example.giveease.utils.NetworkUtils
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.Snackbar

class DonorFeedFragment : Fragment() {

    private var _binding: FragmentDonorFeedBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: DonorFeedViewModel
    private lateinit var campaignAdapter: CampaignAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDonorFeedBinding.inflate(inflater, container, false)
        viewModel = ViewModelProvider(this)[DonorFeedViewModel::class.java]

        setupRecyclerView()
        setupFilters()
        observeViewModel()
        
        viewModel.loadCampaigns(NetworkUtils.isNetworkAvailable(requireContext()))

        return binding.root
    }

    private fun observeViewModel() {
        viewModel.campaigns.observe(viewLifecycleOwner) { campaigns ->
            campaignAdapter.submitList(campaigns)

            if (campaigns.isEmpty()) {
                binding.recyclerViewCampaigns.visibility = View.GONE
                binding.layoutEmptyState.root.visibility = View.VISIBLE
            } else {
                binding.recyclerViewCampaigns.visibility = View.VISIBLE
                binding.layoutEmptyState.root.visibility = View.GONE
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            if (isLoading) {
                binding.shimmerLayout.visibility = View.VISIBLE
                binding.shimmerLayout.startShimmer()
                binding.recyclerViewCampaigns.visibility = View.GONE
                binding.layoutEmptyState.root.visibility = View.GONE
            } else {
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
            }
        }

        viewModel.error.observe(viewLifecycleOwner) { errorMessage ->
            errorMessage?.let {
                Snackbar.make(binding.root, it, Snackbar.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun setupRecyclerView() {
        campaignAdapter = CampaignAdapter { campaign ->
            onCampaignClick(campaign)
        }

        binding.recyclerViewCampaigns.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = campaignAdapter
        }
    }

    private fun setupFilters() {
        binding.chipAll.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filterCampaigns("All")
        }

        binding.chipHealth.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filterCampaigns("Medical & Healthcare")
        }

        binding.chipFood.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filterCampaigns("Food & Nutrition")
        }

        binding.chipEducation.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filterCampaigns("Education")
        }
        
        binding.chipMonetary.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filterCampaigns("Monetary Funds")
        }
        
        binding.chipBlood.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filterCampaigns("Blood Donation")
        }
        
        binding.chipDisaster.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filterCampaigns("Disaster Relief")
        }
        
        binding.chipClothing.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filterCampaigns("Clothing & Essentials")
        }
        
        binding.chipAnimal.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) viewModel.filterCampaigns("Animal Welfare")
        }
    }



    private fun onCampaignClick(campaign: CampaignData) {
        val detailsFragment = CampaignDetailsFragment.newInstance(campaign)

        parentFragmentManager.beginTransaction()
            .hide(this)
            .add((requireView().parent as android.view.ViewGroup).id, detailsFragment)
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}