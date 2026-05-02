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
import com.bumptech.glide.Glide
import android.widget.TextView
import com.example.giveease.ui.NotificationsFragment

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
        listenForUnreadNotifications()
    }

    private fun loadNgoData() {
        val userId = auth.currentUser?.uid ?: return
        if (!isAdded || _binding == null) return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (document.exists()) {
                    val ngoName = document.getString("name") ?: "NGO"
                    binding.tvNgoName.text = ngoName
                    
                    val imageUrl = document.getString("profileImageUrl")
                    if (!imageUrl.isNullOrEmpty()) {
                        binding.ivNgoLogo.imageTintList = null // Remove primary tint so image shows in full color
                        Glide.with(this@NgoHomeFragment)
                            .load(imageUrl)
                            .placeholder(R.drawable.ic_ngo)
                            .into(binding.ivNgoLogo)
                    }
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                binding.tvNgoName.text = "NGO Dashboard"
            }
    }

    private fun listenForUnreadNotifications() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).collection("notifications")
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshots, error ->
                if (!isAdded || _binding == null) return@addSnapshotListener
                if (error != null) return@addSnapshotListener

                val count = snapshots?.size() ?: 0
                val badge = binding.root.findViewById<TextView>(R.id.tvNotificationBadge)
                if (count > 0) {
                    badge?.visibility = View.VISIBLE
                    badge?.text = if (count > 99) "99+" else count.toString()
                } else {
                    badge?.visibility = View.GONE
                }
            }
    }

    private fun loadCampaignStats() {
        val userId = auth.currentUser?.uid ?: return
        if (!isAdded || _binding == null) return

        firestore.collection("campaigns")
            .whereEqualTo("ngoId", userId)
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                val activeCampaigns = documents.size()
                binding.tvActiveCampaigns.text = activeCampaigns.toString()
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
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                val totalItems = documents.sumOf { doc ->
                    val status = doc.getString("status") ?: ""
                    val paymentMethod = doc.getString("paymentMethod") ?: ""
                    if (status != "Rejected" && paymentMethod != "Bank Transfer") {
                        (doc.getLong("quantity") ?: 0).toInt()
                    } else {
                        0
                    }
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

        binding.shimmerRecentCampaign.visibility = View.VISIBLE
        binding.shimmerRecentCampaign.startShimmer()
        binding.cardRecentCampaign.visibility = View.GONE

        firestore.collection("campaigns")
            .whereEqualTo("ngoId", userId)
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                
                binding.shimmerRecentCampaign.stopShimmer()
                binding.shimmerRecentCampaign.visibility = View.GONE

                val recentCampaign = documents.documents.maxByOrNull { doc ->
                    doc.getLong("createdAt") ?: 0L
                }

                if (recentCampaign != null) {
                    val campaign = recentCampaign

                    binding.tvCampaignTitle.text = campaign.getString("title") ?: "Campaign"
                    binding.tvCampaignDescription.text = campaign.getString("description") ?: ""

                    val status = campaign.getString("status") ?: "Active"
                    binding.tvCampaignStatus.text = status

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

                    binding.cardRecentCampaign.visibility = View.VISIBLE
                    binding.layoutEmptyState.root.visibility = View.GONE
                } else {
                    binding.cardRecentCampaign.visibility = View.GONE
                    binding.layoutEmptyState.root.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                binding.shimmerRecentCampaign.stopShimmer()
                binding.shimmerRecentCampaign.visibility = View.GONE
                binding.cardRecentCampaign.visibility = View.GONE
                binding.layoutEmptyState.root.visibility = View.VISIBLE
            }
    }

    private fun setupClickListeners() {
        // Create New Campaign button
        binding.btnCreateNewCampaign.setOnClickListener {
            firestore.collection("users").document(auth.currentUser?.uid ?: "")
                .get()
                .addOnSuccessListener { document ->
                    val verificationStatus = document.getString("verificationStatus") ?: "pending"

                    if (verificationStatus == "verified") {
                        navigateToFragment(CreateCampaignFragment())
                    } else {
                        showVerificationRequiredDialog()
                    }
                }
        }

        // View All Campaigns button
        binding.btnViewAllCampaigns.setOnClickListener {
            navigateToFragment(MyCampaignsFragment())
        }

        // View All text link
        binding.btnVerifyReceipts.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .hide(this)
                .add((requireView().parent as ViewGroup).id, MonetaryVerificationFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnManageItems.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .hide(this)
                .add((requireView().parent as ViewGroup).id, NgoPhysicalDonationsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.tvViewAllCampaigns.setOnClickListener {
            navigateToFragment(MyCampaignsFragment())
        }

        binding.ivNotifications.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .hide(this)
                .add((requireView().parent as ViewGroup).id, NotificationsFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun navigateToFragment(fragment: Fragment) {
        if (!isAdded) return

        // Get the container ID from the current fragment's parent
        val containerId = (view?.parent as? View)?.id ?: return

        parentFragmentManager.beginTransaction()
            .hide(this)
            .add(containerId, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun showVerificationRequiredDialog() {
        android.app.AlertDialog.Builder(requireContext())
            .setTitle("Verification Required")
            .setMessage("Your organization must be verified before creating campaigns. Please complete the verification process.")
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
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