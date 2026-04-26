package com.example.giveease.donor

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.R
import com.example.giveease.databinding.FragmentImpactDashboardBinding
import com.example.giveease.databinding.ItemBadgeBinding
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

    // Accumulated data
    private var totalDonations = 0
    private var totalItems = 0
    private var uniqueNGOs = 0
    private var impactScore = 0
    private var badgesEarned = 0
    private var monthlyCount = 0
    private var yearlyCount = 0
    private var peopleImpacted = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentImpactDashboardBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupClickListeners()
        loadDonorProfile()
        loadImpactData()

        return binding.root
    }

    private fun setupRecyclerView() {
        categoryAdapter = CategoryImpactAdapter()
        binding.recyclerViewCategories.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = categoryAdapter
            isNestedScrollingEnabled = false
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        binding.btnStartDonation.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadDonorProfile() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                val name = doc.getString("name") ?: "Donor"
                val verified = doc.getString("verificationStatus") == "verified"
                binding.tvGreeting.text = "Assalam-o-Alaikum, $name"
                binding.tvVerifiedBadge.text = if (verified) "✅ Verified Donor" else "⏳ Pending Verification"
            }
    }

    private fun loadImpactData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("donations")
            .whereEqualTo("donorId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                val donations = documents.documents
                totalDonations = donations.size
                totalItems = donations.sumOf { it.getLong("quantity") ?: 0L }.toInt()
                uniqueNGOs = donations.mapNotNull { it.getString("ngoId") }.distinct().size

                // Impact score: 20 points per item
                impactScore = totalItems * 20

                // People impacted estimate: ~3 people per item donated
                peopleImpacted = totalItems * 3

                // Timeline calculations
                val now = System.currentTimeMillis()
                val cal = Calendar.getInstance()

                val startOfMonth = cal.apply {
                    timeInMillis = now
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val startOfYear = cal.apply {
                    timeInMillis = now
                    set(Calendar.MONTH, 0); set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                monthlyCount = donations.count { (it.getLong("timestamp") ?: 0L) >= startOfMonth }
                yearlyCount = donations.count { (it.getLong("timestamp") ?: 0L) >= startOfYear }

                val monthlyItems = donations.filter { (it.getLong("timestamp") ?: 0L) >= startOfMonth }
                    .sumOf { it.getLong("quantity") ?: 0L }.toInt()

                // First donation date
                val firstDonation = donations.minByOrNull { it.getLong("timestamp") ?: Long.MAX_VALUE }
                firstDonation?.let { doc ->
                    val ts = doc.getLong("timestamp") ?: 0L
                    binding.tvFirstDonation.text = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(ts))
                }

                // Build badges
                val badges = buildBadges(totalDonations, impactScore)
                badgesEarned = badges.count { it.isEarned }

                // Update all UI
                updateKPIs(monthlyCount, monthlyItems)
                updateImpactScore(badges)
                updateBadges(badges)
                updateTimeline()
                loadCategoryBreakdown(donations)
                updateNextGoal(badges)
            }
    }

    private fun updateKPIs(monthlyDonations: Int, monthlyItems: Int) {
        animateCounter(binding.tvTotalDonations, totalDonations)
        animateCounter(binding.tvTotalItems, totalItems)
        animateCounter(binding.tvBadgesEarned, badgesEarned)
        animateCounter(binding.tvNGOsHelped, uniqueNGOs)

        binding.tvDonationsTrend.text = "+$monthlyDonations this month"
        binding.tvItemsTrend.text = "+$monthlyItems this month"
        binding.tvTotalDonationsPill.text = "Total: $totalDonations donations"
    }

    private fun updateImpactScore(badges: List<BadgeItem>) {
        animateCounter(binding.tvImpactScore, impactScore)

        // Find next locked badge for progress
        val nextBadge = badges.firstOrNull { !it.isEarned }
        val nextTarget = getNextMilestoneTarget(badges)

        if (nextTarget > 0) {
            val progress = ((impactScore.toFloat() / nextTarget) * 100).toInt().coerceAtMost(100)
            binding.progressToNextBadge.progress = progress
            binding.tvNextMilestone.text = "Next: ${nextBadge?.name ?: "Max Level"} ($impactScore/$nextTarget pts)"
        } else {
            binding.progressToNextBadge.progress = 100
            binding.tvNextMilestone.text = "🎉 All milestones achieved!"
        }
    }

    private fun updateTimeline() {
        binding.tvMonthlyDonations.text = "$monthlyCount donations"
        binding.tvYearlyDonations.text = "$yearlyCount donations"
        binding.tvPeopleImpacted.text = "$peopleImpacted people"
    }

    private fun updateBadges(badges: List<BadgeItem>) {
        binding.layoutBadges.removeAllViews()
        for (badge in badges) {
            val itemBinding = ItemBadgeBinding.inflate(layoutInflater, binding.layoutBadges, false)
            itemBinding.tvBadgeIcon.text = badge.icon
            itemBinding.tvBadgeName.text = badge.name
            itemBinding.tvBadgeStatus.text = badge.status

            if (badge.isEarned) {
                itemBinding.root.setBackgroundResource(R.drawable.bg_badge_earned)
                itemBinding.tvBadgeIcon.alpha = 1f
                itemBinding.tvBadgeName.alpha = 1f
            } else {
                itemBinding.root.setBackgroundResource(R.drawable.bg_badge_locked)
                itemBinding.tvBadgeIcon.alpha = 0.4f
                itemBinding.tvBadgeName.alpha = 0.5f
            }
            binding.layoutBadges.addView(itemBinding.root)
        }
    }

    private fun updateNextGoal(badges: List<BadgeItem>) {
        val nextBadge = badges.firstOrNull { !it.isEarned }
        if (nextBadge != null) {
            binding.tvNextGoal.text = "Keep donating to unlock '${nextBadge.name}' badge! ${nextBadge.status}"
        } else {
            binding.tvNextGoal.text = "Amazing! You've unlocked all badges. Keep making an impact!"
        }
    }

    private fun buildBadges(donationCount: Int, score: Int): List<BadgeItem> {
        return listOf(
            BadgeItem("🎁", "First Giver", if (donationCount >= 1) "✅ Earned" else "Need 1 donation", donationCount >= 1),
            BadgeItem("💯", "100 Points", if (score >= 100) "✅ Earned" else "Need ${100 - score} pts", score >= 100),
            BadgeItem("❤️", "Generous Heart", if (score >= 500) "✅ Earned" else "Need ${500 - score} pts", score >= 500),
            BadgeItem("⭐", "Rising Star", if (donationCount >= 10) "✅ Earned" else "Need ${10 - donationCount} donations", donationCount >= 10),
            BadgeItem("🏆", "Hero Badge", if (score >= 10000) "✅ Earned" else "Need ${10000 - score} pts", score >= 10000),
            BadgeItem("💎", "Platinum", if (donationCount >= 20) "✅ Earned" else "Need ${20 - donationCount} donations", donationCount >= 20)
        )
    }

    private fun getNextMilestoneTarget(badges: List<BadgeItem>): Int {
        val thresholds = listOf(1, 100, 500, 200, 10000, 400) // approximate point thresholds
        val scoreMilestones = listOf(20, 100, 500, 200, 10000, 400)
        for (i in badges.indices) {
            if (!badges[i].isEarned) return scoreMilestones[i]
        }
        return 0
    }

    private fun loadCategoryBreakdown(donations: List<com.google.firebase.firestore.DocumentSnapshot>) {
        if (donations.isEmpty()) {
            binding.tvEmptyCategories.visibility = View.VISIBLE
            return
        }

        val categoryMap = mutableMapOf<String, CategoryImpact>()
        var processed = 0

        for (donation in donations) {
            val campaignId = donation.getString("campaignId") ?: continue
            firestore.collection("campaigns").document(campaignId).get()
                .addOnSuccessListener { campaign ->
                    if (!isAdded || _binding == null) return@addOnSuccessListener
                    processed++

                    val category = campaign.getString("category") ?: "Other"
                    val quantity = donation.getLong("quantity")?.toInt() ?: 0

                    val existing = categoryMap[category]
                    categoryMap[category] = CategoryImpact(
                        category = category,
                        donationCount = (existing?.donationCount ?: 0) + 1,
                        itemCount = (existing?.itemCount ?: 0) + quantity
                    )

                    if (processed >= donations.size) {
                        val sorted = categoryMap.values.sortedByDescending { it.donationCount }
                        categoryAdapter.submitList(sorted, totalDonations)
                        binding.tvEmptyCategories.visibility = if (sorted.isEmpty()) View.VISIBLE else View.GONE
                    }
                }
        }
    }

    private fun animateCounter(textView: android.widget.TextView, target: Int) {
        val animator = ValueAnimator.ofInt(0, target)
        animator.duration = 1200
        animator.interpolator = DecelerateInterpolator()
        animator.addUpdateListener { animation ->
            if (_binding != null) {
                textView.text = (animation.animatedValue as Int).toString()
            }
        }
        animator.start()
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