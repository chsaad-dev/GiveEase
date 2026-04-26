package com.example.giveease.donor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.giveease.R
import com.example.giveease.databinding.FragmentNgoProfileBinding
import com.google.android.material.chip.Chip
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar
import java.io.File
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import android.app.AlertDialog

class NgoPublicProfileFragment : Fragment() {

    private var _binding: FragmentNgoProfileBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private var ngoId: String? = null

    companion object {
        private const val ARG_NGO_ID = "ngoId"

        fun newInstance(ngoId: String) = NgoPublicProfileFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_NGO_ID, ngoId)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ngoId = arguments?.getString(ARG_NGO_ID)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNgoProfileBinding.inflate(inflater, container, false)
        
        // Hide controls not applicable for public view
        binding.btnEditProfile.visibility = View.GONE
        binding.btnSettings.visibility = View.GONE
        
        setupClickListeners()
        
        if (ngoId != null) {
            loadProfileData(ngoId!!)
            loadCampaignStats(ngoId!!)
        } else {
            Toast.makeText(requireContext(), "NGO ID is missing", Toast.LENGTH_SHORT).show()
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }
        
        return binding.root
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadProfileData(uid: String) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (!isAdded) return@addOnSuccessListener

                if (document.exists()) {
                    val imageUrl = document.getString("profileImageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        Glide.with(this@NgoPublicProfileFragment)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_ngo_placeholder)
                            .into(binding.ivProfilePicture)
                    }

                    binding.tvNgoName.text = document.getString("ngoName") ?: document.getString("name") ?: "NGO Name"
                    binding.tvTagline.text = document.getString("tagline") ?: "Your tagline here"

                    val verificationStatus = document.getString("verificationStatus") ?: "pending"
                    binding.tvVerificationStatus.text = when (verificationStatus) {
                        "verified", "approved" -> "Verified"
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

                    val documentUrl = document.getString("governmentDocumentUrl")
                    if (!documentUrl.isNullOrEmpty()) {
                        binding.btnViewDocument.visibility = View.VISIBLE
                        binding.btnViewDocument.setOnClickListener {
                            downloadAndOpenDocument(documentUrl)
                        }
                    } else {
                        binding.btnViewDocument.visibility = View.GONE
                    }

                    loadServiceCategories(document.get("serviceCategories") as? List<String> ?: emptyList())
                } else {
                    if (isAdded) {
                        Toast.makeText(requireContext(), "NGO not found", Toast.LENGTH_SHORT).show()
                    }
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

    private fun loadCampaignStats(uid: String) {
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

    private fun downloadAndOpenDocument(documentUrl: String) {
        val loadingDialog = AlertDialog.Builder(requireContext())
            .setView(com.example.giveease.R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.show()

        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(documentUrl)
        
        // Determine file extension
        storageRef.metadata.addOnSuccessListener { metadata ->
            val ext = if (metadata.contentType?.contains("pdf") == true) ".pdf" else ".jpg"
            val localFile = File(requireContext().cacheDir, "ngo_document$ext")

            storageRef.getFile(localFile)
                .addOnSuccessListener {
                    loadingDialog.dismiss()
                    if (isAdded) {
                        openLocalFile(localFile, metadata.contentType ?: "image/jpeg")
                    }
                }
                .addOnFailureListener { e ->
                    loadingDialog.dismiss()
                    if (isAdded) {
                        if (e.message?.contains("does not exist") == true) {
                            Toast.makeText(requireContext(), "This document is no longer available.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(requireContext(), "Download failed: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }.addOnFailureListener { e ->
            // Fallback if metadata fails
            val localFile = File(requireContext().cacheDir, "ngo_document.jpg")
            storageRef.getFile(localFile)
                .addOnSuccessListener {
                    loadingDialog.dismiss()
                    if (isAdded) {
                        openLocalFile(localFile, "image/jpeg")
                    }
                }
                .addOnFailureListener { e2 ->
                    loadingDialog.dismiss()
                    if (isAdded) {
                        if (e2.message?.contains("does not exist") == true) {
                            Toast.makeText(requireContext(), "This document is no longer available.", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(requireContext(), "Download failed: ${e2.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
        }
    }

    private fun openLocalFile(file: File, mimeType: String) {
        try {
            val uri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "No app found to open this document", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
