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
import com.example.giveease.ui.NotificationsFragment
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.firestore.ListenerRegistration

class DonorHomeFragment : Fragment() {
    private var _binding: FragmentDonorHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var featuredCampaignId: String? = null
    private var notificationListener: ListenerRegistration? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDonorHomeBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupUserData()
        setupClickListeners()
        loadDonationStats()
        loadFeaturedCampaign()
        loadRecentActivity()
        listenForUnreadNotifications()

        return binding.root
    }

    private fun setupUserData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (document.exists()) {
                    val name = document.getString("name") ?: "User"
                    binding.tvUserName.text = name
                } else {
                    binding.tvUserName.text = auth.currentUser?.displayName ?: "User"
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                binding.tvUserName.text = auth.currentUser?.displayName ?: "User"
            }
    }

    private fun listenForUnreadNotifications() {
        val userId = auth.currentUser?.uid ?: return
        notificationListener = firestore.collection("users").document(userId).collection("notifications")
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

    private fun loadDonationStats() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("donations")
            .whereEqualTo("donorId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                val totalDonations = documents.size()
                val totalItems = documents.sumOf { doc ->
                    val status = doc.getString("status") ?: ""
                    val paymentMethod = doc.getString("paymentMethod") ?: ""
                    if (status != "Rejected" && paymentMethod != "Bank Transfer") {
                        doc.getLong("quantity") ?: 0L
                    } else {
                        0L
                    }
                }.toInt()

                binding.tvDonationsCount.text = totalDonations.toString()
                binding.tvTotalAmount.text = totalItems.toString()
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                binding.tvDonationsCount.text = "0"
                binding.tvTotalAmount.text = "0"
            }
    }

    private fun loadFeaturedCampaign() {
        android.util.Log.d("DonorHome", "Starting to load featured campaign")

        firestore.collection("campaigns")
            .whereEqualTo("status", "Active")
            .limit(10)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null || context == null) {
                    android.util.Log.d("DonorHome", "Fragment not attached, skipping UI update")
                    return@addOnSuccessListener
                }

                if (!documents.isEmpty) {
                    val currentTime = System.currentTimeMillis()
                    
                    // Find the first campaign that is truly active (not expired, not completed if autoClose)
                    val validCampaign = documents.documents.firstOrNull { doc ->
                        val endDate = doc.getLong("endDate") ?: 0L
                        val currentQuantity = doc.getLong("currentQuantity")?.toInt() ?: 0
                        val targetQuantity = doc.getLong("targetQuantity")?.toInt() ?: 0
                        val autoClose = doc.getBoolean("autoClose") ?: false
                        
                        val isExpired = endDate > 0 && endDate < currentTime
                        val isCompleted = autoClose && targetQuantity > 0 && currentQuantity >= targetQuantity
                        
                        !isExpired && !isCompleted
                    }

                    if (validCampaign != null) {
                        featuredCampaignId = validCampaign.id

                        android.util.Log.d("DonorHome", "Featured campaign loaded - ID: $featuredCampaignId")

                        val imageUrls = validCampaign.get("imageUrls") as? List<String>
                        val ivCampaignImage = binding.cardFeaturedCampaign.findViewById<ImageView>(R.id.ivCampaignImage)

                        if (!imageUrls.isNullOrEmpty() && ivCampaignImage != null) {
                            android.util.Log.d("DonorHome", "Loading image: ${imageUrls[0]}")

                            context?.let { ctx ->
                                Glide.with(ctx)
                                    .load(imageUrls[0])
                                    .centerCrop()
                                    .placeholder(R.drawable.sample_compaign1)
                                    .error(R.drawable.sample_compaign1)
                                    .into(ivCampaignImage)
                            }
                        } else {
                            android.util.Log.d("DonorHome", "No images found or ImageView is null")
                        }

                        binding.tvFeaturedNgo.text = validCampaign.getString("title") ?: "Campaign"

                        val currentQty = validCampaign.getLong("currentQuantity") ?: 0L
                        val targetQty = validCampaign.getLong("targetQuantity") ?: 1L
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
                        android.util.Log.d("DonorHome", "No active campaigns found after filtering")
                        binding.cardFeaturedCampaign.visibility = View.GONE
                    }
                } else {
                    android.util.Log.d("DonorHome", "No active campaigns found")
                    binding.cardFeaturedCampaign.visibility = View.GONE
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("DonorHome", "Failed to load featured campaign: ${exception.message}")
                if (isAdded && _binding != null) {
                    binding.cardFeaturedCampaign.visibility = View.GONE
                }
            }
    }

    private fun loadRecentActivity() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("donations")
            .whereEqualTo("donorId", userId)
            .limit(1)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

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
                if (isAdded && _binding != null) {
                    binding.recentActivityCard.visibility = View.GONE
                }
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
        com.example.giveease.utils.AnimUtils.applyButtonPressEffect(
            binding.layoutImpactDashboard, binding.layoutSavedCampaigns,
            binding.layoutExploreCauses, binding.btnDonateFeatured
        )

        binding.apply {
            layoutImpactDashboard.setOnClickListener {
                navigateToImpactDashboard()
            }

            layoutSavedCampaigns.setOnClickListener {
                navigateToSavedCampaigns()
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
                    context?.let {
                        Toast.makeText(it, "Campaign not loaded yet", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            btnDonateFeatured.setOnClickListener {
                if (featuredCampaignId != null) {
                    android.util.Log.d("DonorHome", "Donate button clicked - Navigating to campaign: $featuredCampaignId")
                    navigateToCampaignDetail(featuredCampaignId!!)
                } else {
                    android.util.Log.e("DonorHome", "Campaign ID is null when donate button clicked")
                    context?.let {
                        Toast.makeText(it, "Campaign not loaded yet", Toast.LENGTH_SHORT).show()
                    }
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

    private fun selectBottomNavTab(itemId: Int) {
        val bottomNav = parentFragment?.view?.findViewById<BottomNavigationView>(R.id.bottom_nav_donor)
        bottomNav?.selectedItemId = itemId
    }

    private fun navigateToMyCampaigns() {
        if (!isAdded) return
        selectBottomNavTab(R.id.nav_feed)
    }

    private fun navigateToImpactDashboard() {
        if (!isAdded) return
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .hide(this)
            .add((requireView().parent as android.view.ViewGroup).id, ImpactDashboardFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToSavedCampaigns() {
        if (!isAdded) return
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .hide(this)
            .add((requireView().parent as android.view.ViewGroup).id, SavedCampaignsFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToExploreCauses() {
        if (!isAdded) return
        selectBottomNavTab(R.id.nav_feed)
    }

    private fun navigateToLeaderboard() {
        context?.let {
            Toast.makeText(it, "Leaderboard coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToNotifications() {
        if (!isAdded) return
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .hide(this)
            .add((requireView().parent as android.view.ViewGroup).id, NotificationsFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToCampaignDetail(campaignId: String) {
        if (!isAdded) return

        android.util.Log.d("DonorHome", "Creating CampaignDetailsFragment with campaignId: $campaignId")

        val fragment = CampaignDetailsFragment().apply {
            arguments = Bundle().apply {
                putString("campaignId", campaignId)
            }
        }

        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .hide(this)
            .add((requireView().parent as android.view.ViewGroup).id, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToDonationHistory() {
        if (!isAdded) return
        parentFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .hide(this)
            .add((requireView().parent as android.view.ViewGroup).id, DonationHistoryFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        notificationListener?.remove()
        _binding = null
    }

    private fun checkVerificationBeforeAction(onVerified: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (!isAdded) return@addOnSuccessListener

                val verificationStatus = document.getString("verificationStatus") ?: "pending"

                if (verificationStatus == "verified") {
                    onVerified()
                } else {
                    showVerificationRequiredDialog()
                }
            }
    }

    private fun showVerificationRequiredDialog() {
        context?.let { ctx ->
            AlertDialog.Builder(ctx)
                .setTitle("Verification Required")
                .setMessage("Please verify your identity to donate.")
                .setPositiveButton("Verify Now") { _, _ ->
                    navigateToVerification()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun navigateToVerification() {
        if (!isAdded) return
        requireActivity().supportFragmentManager.beginTransaction()
            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right)
            .replace(R.id.fragment_container, IdentityVerificationFragment())
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun newInstance() = DonorHomeFragment()
    }
}