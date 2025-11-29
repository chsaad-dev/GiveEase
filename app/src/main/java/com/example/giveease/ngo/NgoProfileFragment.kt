package com.example.giveease.ngo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentNgoProfileBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.*

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
        loadProfileData()
        loadCampaignStats()
        return binding.root
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.btnEditProfile.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, NgoEditProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnSettings.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, NgoSettingsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun loadProfileData() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (!isAdded) return@addOnSuccessListener

                if (document.exists()) {
                    binding.tvNgoName.text = document.getString("ngoName") ?: "NGO Name"
                    binding.tvTagline.text = document.getString("tagline") ?: "Your tagline here"

                    val verificationStatus = document.getString("verificationStatus") ?: "pending"
                    binding.tvVerificationStatus.text = when (verificationStatus) {
                        "approved" -> "Verified"
                        "pending" -> "Pending"
                        "rejected" -> "Not Verified"
                        else -> "Pending"
                    }

                    binding.tvRegistrationNumber.text = document.getString("registrationNumber") ?: "N/A"
                    binding.tvOrgType.text = "Non-Profit Trust"

                    val createdAt = document.getLong("createdAt")
                    if (createdAt != null) {
                        val calendar = Calendar.getInstance()
                        calendar.timeInMillis = createdAt
                        binding.tvFounded.text = calendar.get(Calendar.YEAR).toString()
                    } else {
                        binding.tvFounded.text = "N/A"
                    }

                    binding.tvTeamSize.text = "45 members"
                    binding.tvContact.text = document.getString("email") ?: "N/A"
                    binding.tvMission.text = document.getString("mission") ?: "No mission statement provided"
                    binding.tvVision.text = document.getString("vision") ?: "No vision statement provided"
                    binding.tvHeadquarters.text = document.getString("headquarters") ?: "N/A"
                    binding.tvCoverage.text = document.getString("coverage") ?: "N/A"

                    loadServiceCategories(document.get("serviceCategories") as? List<String> ?: emptyList())
                }
            }
            .addOnFailureListener {
                if (isAdded) {
                    Toast.makeText(requireContext(), "Failed to load profile", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loadServiceCategories(categories: List<String>) {
        binding.chipGroupServices.removeAllViews()

        if (categories.isEmpty()) {
            val chip = Chip(requireContext()).apply {
                text = "No categories selected"
                isClickable = false
                setChipBackgroundColorResource(R.color.gray_light)
                setTextColor(resources.getColor(R.color.gray_dark, null))
            }
            binding.chipGroupServices.addView(chip)
        } else {
            categories.forEach { category ->
                val chip = Chip(requireContext()).apply {
                    text = category
                    isClickable = false
                    setChipBackgroundColorResource(R.color.primary)
                    setTextColor(resources.getColor(android.R.color.white, null))
                }
                binding.chipGroupServices.addView(chip)
            }
        }
    }

    private fun loadCampaignStats() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("campaigns")
            .whereEqualTo("ngoId", uid)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded) return@addOnSuccessListener

                val totalCampaigns = documents.size()
                var totalRaised = 0
                var successfulCampaigns = 0

                documents.forEach { doc ->
                    val currentAmount = doc.getLong("currentAmount")?.toInt() ?: 0
                    val targetAmount = doc.getLong("targetAmount")?.toInt() ?: 0

                    totalRaised += currentAmount

                    if (currentAmount >= targetAmount && targetAmount > 0) {
                        successfulCampaigns++
                    }
                }

                val successRate = if (totalCampaigns > 0) {
                    (successfulCampaigns * 100) / totalCampaigns
                } else {
                    0
                }

                binding.tvTotalCampaigns.text = totalCampaigns.toString()
                binding.tvTotalRaised.text = formatNumber(totalRaised)
                binding.tvSuccessRate.text = "$successRate%"
            }
            .addOnFailureListener {
                if (isAdded) {
                    binding.tvTotalCampaigns.text = "0"
                    binding.tvTotalRaised.text = "0"
                    binding.tvSuccessRate.text = "0%"
                }
            }
    }

    private fun formatNumber(number: Int): String {
        return when {
            number >= 1_000_000 -> String.format("%.1fM", number / 1_000_000.0)
            number >= 1_000 -> String.format("%.1fK", number / 1_000.0)
            else -> number.toString()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}