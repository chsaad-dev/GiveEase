package com.example.giveease.ngo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.databinding.FragmentNgoCampaignHistoryBinding
import com.google.android.material.tabs.TabLayout
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

class NgoHistoryFragment : Fragment() {

    private var _binding: FragmentNgoCampaignHistoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: NgoDonationAdapter

    private val allDonations = mutableListOf<NgoDonation>()
    private var currentTab = 0 // 0=All, 1=By Campaign, 2=Top Donors
    private var donationsListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNgoCampaignHistoryBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        setupTabs()
        loadDonations()
    }

    private fun setupRecyclerView() {
        adapter = NgoDonationAdapter { donation ->
            showDonationDetails(donation)
        }

        binding.recyclerViewDonations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@NgoHistoryFragment.adapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
    }

    private fun setupTabs() {
        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                applyFilter()
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadDonations() {
        val ngoId = auth.currentUser?.uid ?: return
        if (!isAdded || _binding == null) return

        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        // Remove old listener if exists
        donationsListener?.remove()

        // Set up real-time listener for donations
        donationsListener = firestore.collection("donations")
            .whereEqualTo("ngoId", ngoId)
            .addSnapshotListener { snapshots, error ->
                if (!isAdded || _binding == null) {
                    donationsListener?.remove()
                    return@addSnapshotListener
                }

                binding.progressBar.visibility = View.GONE

                if (error != null) {
                    Toast.makeText(requireContext(), "Error loading donations", Toast.LENGTH_SHORT).show()
                    binding.emptyState.visibility = View.VISIBLE
                    return@addSnapshotListener
                }

                allDonations.clear()

                snapshots?.documents?.forEach { doc ->
                    val donation = NgoDonation(
                        id = doc.id,
                        donorId = doc.getString("donorId") ?: "",
                        donorName = doc.getString("donorName") ?: "Anonymous",
                        campaignId = doc.getString("campaignId") ?: "",
                        campaignTitle = doc.getString("campaignTitle") ?: "Unknown Campaign",
                        quantity = doc.getLong("quantity")?.toInt() ?: 0,
                        unit = doc.getString("unit") ?: "items",
                        message = doc.getString("message") ?: "",
                        timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                        status = doc.getString("status") ?: "Completed"
                    )
                    allDonations.add(donation)
                }
                
                allDonations.sortByDescending { it.timestamp }

                updateStats()
                applyFilter()
            }
    }

    private fun updateStats() {
        if (!isAdded || _binding == null) return

        val totalDonations = allDonations.size
        binding.tvTotalCampaigns.text = "$totalDonations Total Donations"
    }

    private fun applyFilter() {
        if (!isAdded || _binding == null) return

        val filteredList = when (currentTab) {
            0 -> allDonations // All Donations
            1 -> allDonations.sortedBy { it.campaignTitle } // By Campaign
            2 -> { // Top Donors
                val donorMap = allDonations.groupBy { it.donorId }
                val topDonors = donorMap.map { (_, donations) ->
                    donations.first().copy(
                        quantity = donations.sumOf { it.quantity }
                    )
                }.sortedByDescending { it.quantity }
                topDonors
            }
            else -> allDonations
        }

        adapter.submitList(filteredList)

        // Show/hide empty state
        if (filteredList.isEmpty()) {
            binding.recyclerViewDonations.visibility = View.GONE
            binding.emptyState.visibility = View.VISIBLE
        } else {
            binding.recyclerViewDonations.visibility = View.VISIBLE
            binding.emptyState.visibility = View.GONE
        }
    }

    private fun showDonationDetails(donation: NgoDonation) {
        if (!isAdded) return

        val message = buildString {
            append("Donor: ${donation.donorName}\n")
            append("Campaign: ${donation.campaignTitle}\n")
            append("Quantity: ${donation.quantity} ${donation.unit}\n")
            append("Status: ${donation.status}")
            if (donation.message.isNotEmpty()) {
                append("\n\nMessage:\n${donation.message}")
            }
        }

        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Donation Details")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        donationsListener?.remove()
        donationsListener = null
        _binding = null
    }
}