package com.example.giveease.ngo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentNgoProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NgoProfileFragment : Fragment() {

    private var _binding: FragmentNgoProfileBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNgoProfileBinding.inflate(inflater, container, false)
        setupClickListeners()
        loadNgoProfile()
        return binding.root
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnEditProfile.setOnClickListener {
            navigateToEditProfile()
        }

        binding.btnAddBankDetails.setOnClickListener {
            Toast.makeText(requireContext(), "Add Bank Details - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnEditBankDetails.setOnClickListener {
            Toast.makeText(requireContext(), "Edit Bank Details - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnSettings.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, NgoSettingsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun loadNgoProfile() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Basic Info
                    val ngoName = document.getString("ngoName") ?: "Not Set"
                    val tagline = document.getString("tagline") ?: "Empowering communities"
                    val email = document.getString("email") ?: ""
                    val registrationNo = document.getString("registrationNumber") ?: "Not Set"
                    val phoneNumber = document.getString("phoneNumber") ?: "Not Set"
                    val website = document.getString("website") ?: "Not Set"
                    val headquarters = document.getString("headquarters") ?: "Not Set"
                    val verificationStatus = document.getString("verificationStatus") ?: "pending"
                    val createdAt = document.getLong("createdAt") ?: 0

                    // Update UI
                    binding.tvNgoName.text = ngoName
                    binding.tvNgoTagline.text = tagline
                    binding.tvContactEmail.text = email
                    binding.tvRegistrationNo.text = registrationNo
                    binding.tvHeadquarters.text = headquarters

                    // Join date
                    val year = java.text.SimpleDateFormat("yyyy", java.util.Locale.getDefault())
                        .format(java.util.Date(createdAt))
                    binding.tvJoinDate.text = "Member since $year"

                    // Stats - Load from campaigns
                    loadNgoStats(uid)

                } else {
                    Toast.makeText(requireContext(), "Profile data not found", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Log.e("NgoProfile", "Error loading profile", e)
                Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loadNgoStats(ngoId: String) {
        firestore.collection("campaigns")
            .whereEqualTo("ngoId", ngoId)
            .get()
            .addOnSuccessListener { documents ->
                val totalCampaigns = documents.size()
                var totalRaised = 0
                var successfulCampaigns = 0

                for (doc in documents) {
                    val currentQty = doc.getLong("currentQuantity")?.toInt() ?: 0
                    val targetQty = doc.getLong("targetQuantity")?.toInt() ?: 1
                    totalRaised += currentQty

                    if (currentQty >= targetQty) {
                        successfulCampaigns++
                    }
                }

                val successRate = if (totalCampaigns > 0) {
                    (successfulCampaigns * 100) / totalCampaigns
                } else 0

                binding.tvTotalCampaigns.text = totalCampaigns.toString()
                binding.tvTotalRaised.text = formatNumber(totalRaised)
                binding.tvSuccessRate.text = "$successRate%"
            }
    }

    private fun formatNumber(number: Int): String {
        return when {
            number >= 1000000 -> String.format("%.1fM", number / 1000000.0)
            number >= 1000 -> String.format("%.1fK", number / 1000.0)
            else -> number.toString()
        }
    }

    private fun navigateToEditProfile() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, NgoEditProfileFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}