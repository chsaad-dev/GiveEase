package com.example.giveease.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.databinding.FragmentAdminDonationTrackerBinding
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class AdminDonationTrackerFragment : Fragment() {

    private var _binding: FragmentAdminDonationTrackerBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: AdminDonationAdapter
    private var allDonations = mutableListOf<AdminDonation>()
    private var currentFilter = "All Time"
    private var searchQuery = ""

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminDonationTrackerBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupChipFilters()
        setupSearch()
        setupClickListeners()
        loadDonations()
    }

    private fun setupRecyclerView() {
        adapter = AdminDonationAdapter()
        binding.recyclerViewDonations.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewDonations.adapter = adapter
    }

    private fun setupChipFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when {
                checkedIds.contains(binding.chipAll.id) -> "All Time"
                checkedIds.contains(binding.chipThisWeek.id) -> "This Week"
                checkedIds.contains(binding.chipThisMonth.id) -> "This Month"
                else -> "All Time"
            }
            applyFilters()
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                searchQuery = s.toString().trim().lowercase()
                applyFilters()
            }
        })
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadDonations() {
        if (!isAdded || _binding == null) return

        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        firestore.collection("donations")
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                binding.progressBar.visibility = View.GONE
                allDonations.clear()

                for (doc in documents) {
                    val donation = AdminDonation(
                        id = doc.id,
                        donorName = doc.getString("donorName") ?: "Anonymous",
                        ngoName = doc.getString("ngoName") ?: "Unknown NGO",
                        campaignTitle = doc.getString("campaignTitle") ?: "General Campaign",
                        quantity = doc.getLong("quantity") ?: 0,
                        unit = doc.getString("unit") ?: "Items",
                        timestamp = doc.getLong("timestamp") ?: 0
                    )
                    allDonations.add(donation)
                }

                allDonations.sortByDescending { it.timestamp }
                applyFilters()
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener

                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error loading donations: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyFilters() {
        var filtered = allDonations.toList()

        // Apply Time Filter
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        filtered = when (currentFilter) {
            "This Week" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                val oneWeekAgo = calendar.timeInMillis
                filtered.filter { it.timestamp >= oneWeekAgo }
            }
            "This Month" -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                val oneMonthAgo = calendar.timeInMillis
                filtered.filter { it.timestamp >= oneMonthAgo }
            }
            else -> filtered
        }

        // Apply Search Filter
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.donorName.lowercase().contains(searchQuery) ||
                it.ngoName.lowercase().contains(searchQuery) ||
                it.campaignTitle.lowercase().contains(searchQuery)
            }
        }

        binding.tvDonationCount.text = "${filtered.size} donations"

        if (filtered.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerViewDonations.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerViewDonations.visibility = View.VISIBLE
            adapter.submitList(filtered)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
