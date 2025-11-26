package com.example.giveease.donor

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentDonorHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.app.AlertDialog
import com.bumptech.glide.Glide
import com.example.giveease.verification.IdentityVerificationFragment
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class DonorHomeFragment : Fragment() {
    private lateinit var binding: FragmentDonorHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var featuredCampaignId: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDonorHomeBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupUserData()
        setupClickListeners()
        loadDonationStats()
        loadFeaturedCampaign()
        loadRecentActivity()

        return binding.root
    }

    private fun setupUserData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "User"
                    binding.tvUserName.text = name
                } else {
                    binding.tvUserName.text = auth.currentUser?.displayName ?: "User"
                }
            }
            .addOnFailureListener {
                binding.tvUserName.text = auth.currentUser?.displayName ?: "User"
            }
    }

    private fun loadDonationStats() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("donations")
            .whereEqualTo("donorId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val totalDonations = documents.size()
                val totalItems = documents.sumOf { doc ->
                    doc.getLong("quantity") ?: 0L
                }.toInt()

                binding.tvDonationsCount.text = totalDonations.toString()
                binding.tvTotalAmount.text = totalItems.toString()
            }
            .addOnFailureListener {
                binding.tvDonationsCount.text = "0"
                binding.tvTotalAmount.text = "0"
            }
    }

    private fun loadFeaturedCampaign() {
        android.util.Log.d("DonorHome", "Starting to load featured campaign")

        firestore.collection("campaigns")
            .whereEqualTo("status", "Active")
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val campaign = documents.documents[0]
                    featuredCampaignId = campaign.id

                    android.util.Log.d("DonorHome", "Featured campaign loaded - ID: $featuredCampaignId")
                    android.util.Log.d("DonorHome", "Campaign data: ${campaign.data}")

                    val imageUrls = campaign.get("imageUrls") as? List<String>
                    val ivCampaignImage = binding.cardFeaturedCampaign.findViewById<ImageView>(R.id.ivCampaignImage)

                    if (!imageUrls.isNullOrEmpty() && ivCampaignImage != null) {
                        android.util.Log.d("DonorHome", "Loading image: ${imageUrls[0]}")
                        Glide.with(requireContext())
                            .load(imageUrls[0])
                            .placeholder(R.drawable.sample_compaign1)
                            .error(R.drawable.sample_compaign1)
                            .into(ivCampaignImage)
                    } else {
                        android.util.Log.d("DonorHome", "No images found or ImageView is null")
                    }

                    binding.tvFeaturedNgo.text = campaign.getString("title") ?: "Campaign"

                    val currentQty = campaign.getLong("currentQuantity") ?: 0L
                    val targetQty = campaign.getLong("targetQuantity") ?: 1L
                    val progress = ((currentQty.toFloat() / targetQty.toFloat()) * 100).toInt()

                    val progressBar = binding.cardFeaturedCampaign.findViewById<ProgressBar>(R.id.progressBar)
                    val tvProgress = binding.cardFeaturedCampaign.findViewById<TextView>(R.id.tvProgress)
                    val tvCurrentAmount = binding.cardFeaturedCampaign.findViewById<TextView>(R.id.tvCurrentAmount)
                    val tvTargetAmount = binding.cardFeaturedCampaign.findViewById<TextView>(R.id.tvTargetAmount)

                    progressBar?.progress = progress
                    tvProgress?.text = "$progress%"
                    tvCurrentAmount?.text = "$currentQty items"
                    tvTargetAmount?.text = "of $targetQty items"

                    binding.cardFeaturedCampaign.visibility = View.VISIBLE
                    android.util.Log.d("DonorHome", "Featured campaign UI updated successfully")
                } else {
                    android.util.Log.d("DonorHome", "No active campaigns found")
                    binding.cardFeaturedCampaign.visibility = View.GONE
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("DonorHome", "Failed to load featured campaign: ${exception.message}")
                binding.cardFeaturedCampaign.visibility = View.GONE
            }
    }

    private fun loadRecentActivity() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("donations")
            .whereEqualTo("donorId", userId)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val donation = documents.documents[0]

                    val tvActivityTitle = binding.recentActivityCard.findViewById<TextView>(R.id.tvActivityTitle)
                    val tvActivitySubtitle = binding.recentActivityCard.findViewById<TextView>(R.id.tvActivitySubtitle)
                    val tvActivityAmount = binding.recentActivityCard.findViewById<TextView>(R.id.tvActivityAmount)

                    tvActivityTitle?.text = "Donation Successful"

                    val ngoName = donation.getString("ngoName") ?: "Unknown NGO"
                    val timeAgo = formatTimeAgo(donation.getLong("timestamp") ?: System.currentTimeMillis())
                    tvActivitySubtitle?.text = "$ngoName • $timeAgo"

                    val quantity = donation.getLong("quantity") ?: 0L
                    tvActivityAmount?.text = "$quantity items"

                    binding.recentActivityCard.visibility = View.VISIBLE
                } else {
                    binding.recentActivityCard.visibility = View.GONE
                }
            }
            .addOnFailureListener {
                binding.recentActivityCard.visibility = View.GONE
            }
    }

    private fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            diff < 604800000 -> "${diff / 86400000}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM dd", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            layoutDonateWithoutRequest.setOnClickListener {
                navigateToCreateCampaign()
            }

            layoutQuickDonate.setOnClickListener {
                navigateToQuickDonate()
            }

            layoutExploreCauses.setOnClickListener {
                navigateToExploreCauses()
            }

            ivLeaderboard.setOnClickListener {
                navigateToLeaderboard()
            }

            ivNotifications.setOnClickListener {
                navigateToNotifications()
            }

            cardFeaturedCampaign.setOnClickListener {
                if (featuredCampaignId != null) {
                    android.util.Log.d("DonorHome", "Card clicked - Navigating to campaign: $featuredCampaignId")
                    navigateToCampaignDetail(featuredCampaignId!!)
                } else {
                    android.util.Log.e("DonorHome", "Campaign ID is null when card clicked")
                    Toast.makeText(requireContext(), "Campaign not loaded yet", Toast.LENGTH_SHORT).show()
                }
            }

            btnDonateFeatured.setOnClickListener {
                if (featuredCampaignId != null) {
                    android.util.Log.d("DonorHome", "Donate button clicked - Navigating to campaign: $featuredCampaignId")
                    navigateToCampaignDetail(featuredCampaignId!!)
                } else {
                    android.util.Log.e("DonorHome", "Campaign ID is null when donate button clicked")
                    Toast.makeText(requireContext(), "Campaign not loaded yet", Toast.LENGTH_SHORT).show()
                }
            }

            tvViewAllCampaigns.setOnClickListener {
                navigateToMyCampaigns()
            }

            tvViewAllActivity.setOnClickListener {
                navigateToDonationHistory()
            }
        }
    }

    private fun navigateToMyCampaigns() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_donor, DonorFeedFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToCreateCampaign() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_donor, CreateCampaignFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToQuickDonate() {
        checkVerificationBeforeAction {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_donor, DonorFeedFragment())
                .addToBackStack(null)
                .commit()
        }
    }

    private fun navigateToExploreCauses() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_donor, DonorFeedFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToLeaderboard() {
        Toast.makeText(requireContext(), "Leaderboard coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToNotifications() {
        Toast.makeText(requireContext(), "Notifications coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToCampaignDetail(campaignId: String) {
        android.util.Log.d("DonorHome", "Creating CampaignDetailsFragment with campaignId: $campaignId")

        val fragment = CampaignDetailsFragment().apply {
            arguments = Bundle().apply {
                putString("campaignId", campaignId)
            }
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_donor, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToDonationHistory() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_donor, DonationHistoryFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun checkVerificationBeforeAction(onVerified: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val verificationStatus = document.getString("verificationStatus") ?: "pending"

                if (verificationStatus == "verified") {
                    onVerified()
                } else {
                    showVerificationRequiredDialog()
                }
            }
    }

    private fun showVerificationRequiredDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Verification Required")
            .setMessage("Please verify your identity to donate.")
            .setPositiveButton("Verify Now") { _, _ ->
                navigateToVerification()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun navigateToVerification() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, IdentityVerificationFragment())
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun newInstance() = DonorHomeFragment()
    }
}