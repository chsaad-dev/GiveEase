package com.example.giveease.donor

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.R
import com.example.giveease.databinding.FragmentDonationHistoryBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class DonationHistoryFragment : Fragment() {
    private lateinit var binding: FragmentDonationHistoryBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var donationAdapter: DonationAdapter
    private val donationsList = mutableListOf<Donation>()
    private var currentFilter = "all"

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDonationHistoryBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupClickListeners()
        loadDonations()

        return binding.root
    }

    private fun setupRecyclerView() {
        donationAdapter = DonationAdapter(donationsList) { donation ->
            // Handle donation item click
            showDonationDetails(donation)
        }

        binding.recyclerViewDonations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = donationAdapter
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            btnStartDonating.setOnClickListener {
                navigateToHome()
            }

            btnFilterAll.setOnClickListener {
                updateFilter("all")
            }

            btnFilterCompleted.setOnClickListener {
                updateFilter("completed")
            }

            btnFilterPending.setOnClickListener {
                updateFilter("pending")
            }
        }
    }

    private fun updateFilter(filter: String) {
        currentFilter = filter

        binding.apply {
            btnFilterAll.backgroundTintList = if (filter == "all")
                ContextCompat.getColorStateList(requireContext(), R.color.secondary)
            else ContextCompat.getColorStateList(requireContext(), android.R.color.transparent)

            btnFilterCompleted.backgroundTintList = if (filter == "completed")
                ContextCompat.getColorStateList(requireContext(), R.color.secondary)
            else ContextCompat.getColorStateList(requireContext(), android.R.color.transparent)

            btnFilterPending.backgroundTintList = if (filter == "pending")
                ContextCompat.getColorStateList(requireContext(), R.color.secondary)
            else ContextCompat.getColorStateList(requireContext(), android.R.color.transparent)
        }

        loadDonations()
    }

    private fun loadDonations() {
        val userId = auth.currentUser?.uid ?: return

        var query = firestore.collection("donations")
            .whereEqualTo("donorId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)

        // Apply filter
        if (currentFilter != "all") {
            query = query.whereEqualTo("status", currentFilter)
        }

        query.get()
            .addOnSuccessListener { documents ->
                val donations = documents.mapNotNull { doc ->
                    try {
                        Donation(
                            id = doc.id,
                            ngoId = doc.getString("ngoId") ?: "",
                            ngoName = doc.getString("ngoName") ?: "Unknown NGO",
                            campaignTitle = doc.getString("campaignTitle") ?: "Donation",
                            amount = doc.getDouble("amount") ?: 0.0,
                            status = doc.getString("status") ?: "pending",
                            category = doc.getString("category") ?: "General",
                            createdAt = doc.getLong("createdAt") ?: 0L,
                            receiptUrl = doc.getString("receiptUrl")
                        )
                    } catch (e: Exception) {
                        null
                    }
                }

                updateUI(donations)
                updateSummary(donations)
            }
            .addOnFailureListener {
                // Show dummy data for testing
                showDummyData()
            }
    }

    private fun updateUI(donations: List<Donation>) {
        donationsList.clear()
        donationsList.addAll(donations)
        donationAdapter.notifyDataSetChanged()

        if (donations.isEmpty()) {
            binding.recyclerViewDonations.visibility = View.GONE
            binding.layoutEmptyState.visibility = View.VISIBLE
        } else {
            binding.recyclerViewDonations.visibility = View.VISIBLE
            binding.layoutEmptyState.visibility = View.GONE
        }
    }

    private fun updateSummary(donations: List<Donation>) {
        val totalDonations = donations.size
        val totalAmount = donations.sumOf { it.amount }

        binding.tvTotalDonations.text = totalDonations.toString()
        binding.tvTotalAmount.text = "Rs ${String.format("%,d", totalAmount.toInt())}"
    }

    private fun showDummyData() {
        val dummyDonations = listOf(
            Donation(
                id = "1",
                ngoId = "ngo1",
                ngoName = "Edhi Foundation",
                campaignTitle = "Flood Relief Campaign",
                amount = 5000.0,
                status = "completed",
                category = "Disaster Relief",
                createdAt = System.currentTimeMillis() - 172800000, // 2 days ago
                receiptUrl = null
            ),
            Donation(
                id = "2",
                ngoId = "ngo2",
                ngoName = "Saylani Welfare",
                campaignTitle = "Food Distribution Drive",
                amount = 2500.0,
                status = "completed",
                category = "Food & Nutrition",
                createdAt = System.currentTimeMillis() - 604800000, // 1 week ago
                receiptUrl = null
            ),
            Donation(
                id = "3",
                ngoId = "ngo3",
                ngoName = "Shaukat Khanum",
                campaignTitle = "Cancer Treatment Support",
                amount = 10000.0,
                status = "pending",
                category = "Healthcare",
                createdAt = System.currentTimeMillis() - 86400000, // 1 day ago
                receiptUrl = null
            )
        )

        updateUI(dummyDonations)
        updateSummary(dummyDonations)
    }

    private fun showDonationDetails(donation: Donation) {
        Toast.makeText(requireContext(), "Donation to ${donation.ngoName}", Toast.LENGTH_SHORT).show()
        // TODO: Implement donation details screen
    }

    private fun navigateToHome() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_donor, DonorHomeFragment())
            .commit()
    }

    data class Donation(
        val id: String,
        val ngoId: String,
        val ngoName: String,
        val campaignTitle: String,
        val amount: Double,
        val status: String,
        val category: String,
        val createdAt: Long,
        val receiptUrl: String? = null
    )
}