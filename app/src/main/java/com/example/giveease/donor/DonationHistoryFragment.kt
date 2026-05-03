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
    private var allDonations = listOf<Donation>()
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
        donationAdapter = DonationAdapter { donation ->
            showDonationDetails(donation)
        }

        binding.recyclerViewDonations.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = donationAdapter
        }
        com.example.giveease.utils.AnimUtils.applyStaggeredEntrance(binding.recyclerViewDonations)
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
            val secondaryColor = ContextCompat.getColorStateList(requireContext(), R.color.secondary)
            val transparentColor = ContextCompat.getColorStateList(requireContext(), android.R.color.transparent)

            when (filter) {
                "all" -> {
                    btnFilterAll.backgroundTintList = secondaryColor
                    btnFilterAll.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    btnFilterCompleted.backgroundTintList = transparentColor
                    btnFilterCompleted.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary))
                    btnFilterPending.backgroundTintList = transparentColor
                    btnFilterPending.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary))
                }
                "completed" -> {
                    btnFilterAll.backgroundTintList = transparentColor
                    btnFilterAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary))
                    btnFilterCompleted.backgroundTintList = secondaryColor
                    btnFilterCompleted.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                    btnFilterPending.backgroundTintList = transparentColor
                    btnFilterPending.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary))
                }
                "pending" -> {
                    btnFilterAll.backgroundTintList = transparentColor
                    btnFilterAll.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary))
                    btnFilterCompleted.backgroundTintList = transparentColor
                    btnFilterCompleted.setTextColor(ContextCompat.getColor(requireContext(), R.color.secondary))
                    btnFilterPending.backgroundTintList = secondaryColor
                    btnFilterPending.setTextColor(ContextCompat.getColor(requireContext(), android.R.color.white))
                }
            }
        }

        applyFilter()
    }

    private fun applyFilter() {
        val filtered = when (currentFilter) {
            "completed" -> allDonations.filter { it.status.lowercase() == "completed" }
            "pending" -> allDonations.filter { it.status.lowercase().contains("pending") }
            else -> allDonations
        }

        updateUI(filtered)
    }

    private fun loadDonations() {
        val userId = auth.currentUser?.uid ?: return

        android.util.Log.d("DonationHistory", "Loading donations for userId: $userId")

        binding.shimmerLayout.visibility = View.VISIBLE
        binding.shimmerLayout.startShimmer()
        binding.recyclerViewDonations.visibility = View.GONE
        binding.layoutEmptyState.visibility = View.GONE

        firestore.collection("donations")
            .whereEqualTo("donorId", userId)
            .get()
            .addOnSuccessListener { documents ->
                android.util.Log.d("DonationHistory", "Total documents found: ${documents.size()}")

                val donations = documents.mapNotNull { doc ->
                    try {
                        android.util.Log.d("DonationHistory", "Processing: ${doc.id}")

                        val proofMap = doc.get("proof") as? Map<String, Any>
                        val proofData = proofMap?.let {
                            com.example.giveease.models.DonationProof(
                                beneficiaryName = it["beneficiaryName"] as? String ?: "",
                                contactNumber = it["contactNumber"] as? String ?: "",
                                cnicOrAdditionalInfo = it["cnicOrAdditionalInfo"] as? String ?: "",
                                addressProofImageUrl = it["addressProofImageUrl"] as? String ?: "",
                                handoverImageUrl = it["handoverImageUrl"] as? String ?: "",
                                uploadedAt = it["uploadedAt"] as? Long ?: 0L
                            )
                        }

                        Donation(
                            id = doc.id,
                            ngoId = doc.getString("ngoId") ?: "",
                            ngoName = doc.getString("ngoName") ?: "Unknown NGO",
                            campaignTitle = doc.getString("campaignTitle") ?: "Unknown Campaign",
                            amount = doc.getLong("quantity")?.toDouble() ?: 0.0,
                            status = doc.getString("status") ?: "Completed",
                            category = "General",
                            createdAt = doc.getLong("timestamp") ?: System.currentTimeMillis(),
                            receiptUrl = null,
                            proof = proofData
                        )
                    } catch (e: Exception) {
                        android.util.Log.e("DonationHistory", "Error parsing: ${e.message}")
                        null
                    }
                }.sortedByDescending { it.createdAt }

                android.util.Log.d("DonationHistory", "Successfully parsed: ${donations.size} donations")

                allDonations = donations
                applyFilter()
                updateSummary(donations)
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("DonationHistory", "Query failed: ${exception.message}")
                Toast.makeText(requireContext(), "Error: ${exception.message}", Toast.LENGTH_LONG).show()
                binding.shimmerLayout.stopShimmer()
                binding.shimmerLayout.visibility = View.GONE
                binding.layoutEmptyState.visibility = View.VISIBLE
                binding.recyclerViewDonations.visibility = View.GONE
            }
    }

    private fun updateUI(donations: List<Donation>) {
        donationsList.clear()
        donationsList.addAll(donations)
        donationAdapter.submitList(donationsList.toList())

        binding.shimmerLayout.stopShimmer()
        binding.shimmerLayout.visibility = View.GONE

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
        val totalItems = donations.sumOf { it.amount.toInt() }

        binding.tvTotalDonations.text = totalDonations.toString()
        binding.tvTotalAmount.text = totalItems.toString()
    }

    private fun showDonationDetails(donation: Donation) {
        if (donation.proof != null || donation.status.lowercase() == "delivered") {
            val proof = donation.proof
            if (proof != null) {
                val fragment = DonorViewProofFragment.newInstance(
                    name = proof.beneficiaryName,
                    contact = proof.contactNumber,
                    info = proof.cnicOrAdditionalInfo,
                    handoverUrl = proof.handoverImageUrl,
                    addressUrl = proof.addressProofImageUrl,
                    uploadedAt = proof.uploadedAt
                )
                parentFragmentManager.beginTransaction()
                    .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
                    .hide(this@DonationHistoryFragment)
                    .add((requireView().parent as android.view.ViewGroup).id, fragment)
                    .addToBackStack(null)
                    .commit()
            } else {
                Toast.makeText(requireContext(), "Proof data is still syncing. Please wait.", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(requireContext(), "Donated ${donation.amount.toInt()} items to ${donation.campaignTitle}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToHome() {
        parentFragmentManager.popBackStack()
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
        val receiptUrl: String? = null,
        val proof: com.example.giveease.models.DonationProof? = null
    )
}