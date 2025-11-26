package com.example.giveease.donor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.databinding.FragmentImpactDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class ImpactDashboardFragment : Fragment() {
    private var _binding: FragmentImpactDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var categoryAdapter: CategoryImpactAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImpactDashboardBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupClickListeners()
        loadImpactData()

        return binding.root
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryImpactAdapter()
        binding.recyclerViewCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadImpactData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("donations")
            .whereEqualTo("donorId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                val totalDonations = documents.size()
                val totalItems = documents.sumOf { doc ->
                    doc.getLong("quantity") ?: 0L
                }.toInt()

                val uniqueNGOs = documents.mapNotNull { doc ->
                    doc.getString("ngoId")
                }.distinct().size

                binding.tvTotalDonations.text = totalDonations.toString()
                binding.tvTotalItems.text = totalItems.toString()
                binding.tvNGOsHelped.text = uniqueNGOs.toString()

                loadTimelineData(documents.documents)
                loadCategoryBreakdown(documents.documents)
            }
    }

    private fun loadTimelineData(donations: List<com.google.firebase.firestore.DocumentSnapshot>) {
        if (donations.isEmpty()) {
            binding.tvMonthlyDonations.text = "0 donations"
            binding.tvYearlyDonations.text = "0 donations"
            binding.tvFirstDonation.text = "N/A"
            return
        }

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        val startOfMonth = calendar.apply {
            timeInMillis = now
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val startOfYear = calendar.apply {
            timeInMillis = now
            set(Calendar.MONTH, 0)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        val monthlyCount = donations.count { doc ->
            val timestamp = doc.getLong("timestamp") ?: 0L
            timestamp >= startOfMonth
        }

        val yearlyCount = donations.count { doc ->
            val timestamp = doc.getLong("timestamp") ?: 0L
            timestamp >= startOfYear
        }

        val firstDonation = donations.minByOrNull { doc ->
            doc.getLong("timestamp") ?: Long.MAX_VALUE
        }

        binding.tvMonthlyDonations.text = "$monthlyCount donations"
        binding.tvYearlyDonations.text = "$yearlyCount donations"

        firstDonation?.let { doc ->
            val timestamp = doc.getLong("timestamp") ?: 0L
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            binding.tvFirstDonation.text = sdf.format(Date(timestamp))
        }
    }

    private fun loadCategoryBreakdown(donations: List<com.google.firebase.firestore.DocumentSnapshot>) {
        val categoryMap = mutableMapOf<String, CategoryImpact>()

        for (donation in donations) {
            val campaignId = donation.getString("campaignId") ?: continue

            firestore.collection("campaigns").document(campaignId).get()
                .addOnSuccessListener { campaign ->
                    if (!isAdded || _binding == null) return@addOnSuccessListener

                    val category = campaign.getString("category") ?: "Other"
                    val quantity = donation.getLong("quantity")?.toInt() ?: 0

                    if (categoryMap.containsKey(category)) {
                        val existing = categoryMap[category]!!
                        categoryMap[category] = CategoryImpact(
                            category = category,
                            donationCount = existing.donationCount + 1,
                            itemCount = existing.itemCount + quantity
                        )
                    } else {
                        categoryMap[category] = CategoryImpact(
                            category = category,
                            donationCount = 1,
                            itemCount = quantity
                        )
                    }

                    categoryAdapter.submitList(categoryMap.values.sortedByDescending { it.donationCount })
                }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    data class CategoryImpact(
        val category: String,
        val donationCount: Int,
        val itemCount: Int
    )
}