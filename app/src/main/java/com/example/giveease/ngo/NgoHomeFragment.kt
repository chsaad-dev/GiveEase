package com.example.giveease.ngo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentNgoHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import android.widget.Toast

class NgoHomeFragment : Fragment() {

    private var _binding: FragmentNgoHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNgoHomeBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadNgoData()
        loadCampaignStats()
        loadRecentCampaign()
        setupClickListeners()
    }

    private fun loadNgoData() {
        val userId = auth.currentUser?.uid ?: return
        if (!isAdded || _binding == null) return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (document.exists()) {
                    val ngoName = document.getString("name") ?: "NGO"
                    val verificationStatus = document.getString("verificationStatus") ?: "pending"

                    binding.tvNgoName.text = ngoName

                    // Show/hide verification badge based on status
                    if (verificationStatus == "verified") {
                        // Badge is visible by default in XML
                    } else {
                        // You can hide the badge or show different status
                    }
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                binding.tvNgoName.text = "NGO Dashboard"
            }
    }

    private fun loadCampaignStats() {
        val userId = auth.currentUser?.uid ?: return
        if (!isAdded || _binding == null) return

        // Load active campaigns count
        firestore.collection("campaigns")
            .whereEqualTo("ngoId", userId)
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                val activeCampaigns = documents.size()
                binding.tvActiveCampaigns.text = activeCampaigns.toString()

                // Load total donations
                loadDonationStats(userId)
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                binding.tvActiveCampaigns.text = "0"
            }
    }

    private fun loadDonationStats(ngoId: String) {
        if (!isAdded || _binding == null) return

        firestore.collection("donations")
            .whereEqualTo("ngoId", ngoId)
            .whereEqualTo("status", "Completed")
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                val totalItems = documents.sumOf { doc ->
                    (doc.getLong("quantity") ?: 0).toInt()
                }

                binding.tvTotalDonations.text = "$totalItems Items"
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                binding.tvTotalDonations.text = "0 Items"
            }
    }

    private fun loadRecentCampaign() {
        val userId = auth.currentUser?.uid ?: return
        if (!isAdded || _binding == null) return

        firestore.collection("campaigns")
            .whereEqualTo("ngoId", userId)
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (!documents.isEmpty) {
                    val campaign = documents.documents[0]

                    binding.tvCampaignTitle.text = campaign.getString("title") ?: "Campaign"
                    binding.tvCampaignDescription.text = campaign.getString("description") ?: ""

                    val status = campaign.getString("status") ?: "Active"
                    binding.tvCampaignStatus.text = status

                    // Update status badge color
                    when (status) {
                        "Active" -> {
                            binding.cardCampaignStatus.setCardBackgroundColor(
                                resources.getColor(android.R.color.holo_green_light, null)
                            )
                        }
                        "Paused" -> {
                            binding.cardCampaignStatus.setCardBackgroundColor(
                                resources.getColor(android.R.color.holo_orange_light, null)
                            )
                        }
                        "Completed" -> {
                            binding.cardCampaignStatus.setCardBackgroundColor(
                                resources.getColor(android.R.color.darker_gray, null)
                            )
                        }
                    }

                    // Load campaign progress
                    val targetQuantity = (campaign.getLong("targetQuantity") ?: 100).toInt()
                    val currentQuantity = (campaign.getLong("currentQuantity") ?: 0).toInt()
                    val progress = if (targetQuantity > 0) {
                        (currentQuantity * 100 / targetQuantity).coerceAtMost(100)
                    } else {
                        0
                    }

                    binding.tvCampaignRaised.text = "$currentQuantity Items"
                    binding.tvCampaignTarget.text = " of $targetQuantity Items"
                    binding.tvCampaignProgress.text = "$progress% complete"
                    binding.progressCampaign.progress = progress

                    // Show the campaign card
                    binding.cardRecentCampaign.visibility = View.VISIBLE
                } else {
                    // No campaigns yet - hide the card
                    binding.cardRecentCampaign.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                binding.cardRecentCampaign.visibility = View.GONE
            }
    }

    private fun setupClickListeners() {
        binding.ivNotifications.setOnClickListener {
            Toast.makeText(requireContext(), "Notifications coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnCreateNewCampaign.setOnClickListener {
            navigateToCreateCampaign()
        }

        binding.tvViewAllCampaigns.setOnClickListener {
            navigateToMyCampaigns()
        }

        binding.btnViewAllCampaigns.setOnClickListener {
            navigateToMyCampaigns()
        }
    }

    private fun navigateToCreateCampaign() {
        if (!isAdded) return

        // Check verification status first
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val verificationStatus = document.getString("verificationStatus") ?: "pending"

                if (verificationStatus == "verified") {
                    parentFragmentManager.beginTransaction()
                        .replace(R.id.fragmentContainer, CreateCampaignFragment())
                        .addToBackStack(null)
                        .commit()
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please complete verification to create campaigns",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun navigateToMyCampaigns() {
        if (!isAdded) return
        Toast.makeText(requireContext(), "My Campaigns coming soon", Toast.LENGTH_SHORT).show()
        // Will implement MyCampaignsFragment next
    }

    override fun onResume() {
        super.onResume()
        loadNgoData()
        loadCampaignStats()
        loadRecentCampaign()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}