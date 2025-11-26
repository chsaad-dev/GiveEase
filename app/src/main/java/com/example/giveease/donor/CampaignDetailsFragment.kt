package com.example.giveease.donor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.example.giveease.R
import com.example.giveease.databinding.FragmentCampaignDetailsBinding
import com.example.giveease.donor.adapter.ImageSliderAdapter
import com.example.giveease.ngo.CampaignData
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class CampaignDetailsFragment : Fragment() {

    private var _binding: FragmentCampaignDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var campaign: CampaignData

    companion object {
        private const val ARG_CAMPAIGN = "campaign"

        fun newInstance(campaign: CampaignData): CampaignDetailsFragment {
            val fragment = CampaignDetailsFragment()
            val args = Bundle()
            args.putSerializable(ARG_CAMPAIGN, campaign)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCampaignDetailsBinding.inflate(inflater, container, false)

        campaign = arguments?.getSerializable(ARG_CAMPAIGN) as? CampaignData
            ?: return binding.root

        setupUI()
        setupClickListeners()

        return binding.root
    }

    private fun setupUI() {
        binding.apply {
            // Image Gallery
            if (campaign.imageUrls.isNotEmpty()) {
                val adapter = ImageSliderAdapter(campaign.imageUrls)
                viewPagerImages.adapter = adapter

                TabLayoutMediator(tabIndicator, viewPagerImages) { _, _ -> }.attach()

                tvImageCount.text = "${1}/${campaign.imageUrls.size}"

                viewPagerImages.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
                    override fun onPageSelected(position: Int) {
                        tvImageCount.text = "${position + 1}/${campaign.imageUrls.size}"
                    }
                })
            } else {
                tvImageCount.visibility = View.GONE
            }

            // Campaign Info
            tvCampaignTitle.text = campaign.title.ifEmpty { "No Title" }
            tvNgoName.text = campaign.ngoName.ifEmpty { "NGO" }
            tvCategory.text = campaign.category.ifEmpty { "General" }
            tvDescription.text = campaign.description.ifEmpty { "No description available" }

            // Urgency Badge
            // Urgency Badge
            val urgency = campaign.urgencyLevel?.takeIf { it.isNotEmpty() } ?: "Low"
            tvUrgencyBadge.text = urgency
            tvUrgencyBadge.setBackgroundColor(getUrgencyColor(urgency))

            // Progress
            val progress = campaign.getProgress()
            progressBar.progress = progress
            tvProgressPercent.text = "$progress%"
            tvCurrentQuantity.text = "${campaign.currentQuantity} ${campaign.unit}"
            tvTargetQuantity.text = "${campaign.targetQuantity} ${campaign.unit}"

            // Stats
            val daysLeft = campaign.getDaysLeft()
            tvDaysLeft.text = if (daysLeft > 0) "$daysLeft" else "0"
            tvDonorCount.text = "${campaign.donorCount}"

            // End Date
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            tvEndDate.text = sdf.format(Date(campaign.endDate))

            // Item Condition
            if (!campaign.itemCondition.isNullOrEmpty()) {
                tvItemCondition.text = campaign.itemCondition
                cardItemCondition.visibility = View.VISIBLE
            } else {
                cardItemCondition.visibility = View.GONE
            }

            // Specific Requirements
            if (campaign.specificRequirements.isNotEmpty()) {
                tvSpecificRequirements.text = campaign.specificRequirements
                cardSpecificRequirements.visibility = View.VISIBLE
            } else {
                cardSpecificRequirements.visibility = View.GONE
            }

            // Created Date
            val createdDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                .format(Date(campaign.createdAt))
            tvCreatedDate.text = "Created on $createdDate"
        }
    }

    private fun setupClickListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            btnShare.setOnClickListener {
                shareCampaign()
            }

            btnDonate.setOnClickListener {
                handleDonate()
            }
        }
    }

    private fun getUrgencyColor(urgency: String): Int {
        return when (urgency) {
            "Emergency" -> 0xFFFF5252.toInt()
            "High" -> 0xFFFF9800.toInt()
            "Medium" -> 0xFFFFC107.toInt()
            else -> 0xFF4CAF50.toInt()
        }
    }

    private fun shareCampaign() {
        val shareText = """
            Help support: ${campaign.title}
            
            NGO: ${campaign.ngoName}
            Category: ${campaign.category}
            Target: ${campaign.targetQuantity} ${campaign.unit}
            
            Donate now through GiveEase app!
        """.trimIndent()

        val shareIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, shareText)
        }

        startActivity(android.content.Intent.createChooser(shareIntent, "Share Campaign"))
    }


    private fun handleDonate() {
        val dialog = DonationDialogFragment.newInstance(campaign)
        dialog.show(childFragmentManager, "DonationDialog")
    }

    fun refreshCampaignData() {
        // Reload campaign from Firestore to get updated values
        val firestore = FirebaseFirestore.getInstance()

        firestore.collection("campaigns").document(campaign.id).get()
            .addOnSuccessListener { document ->
                try {
                    campaign = CampaignData(
                        id = document.id,
                        ngoId = document.getString("ngoId") ?: "",
                        ngoName = document.getString("ngoName") ?: "",
                        category = document.getString("category") ?: "",
                        title = document.getString("title") ?: "",
                        description = document.getString("description") ?: "",
                        targetQuantity = document.getLong("targetQuantity")?.toInt() ?: 0,
                        currentQuantity = document.getLong("currentQuantity")?.toInt() ?: 0,
                        unit = document.getString("unit") ?: "",
                        endDate = document.getLong("endDate") ?: 0,
                        urgencyLevel = document.getString("urgencyLevel") ?: "",
                        itemCondition = document.getString("itemCondition"),
                        specificRequirements = document.getString("specificRequirements") ?: "",
                        autoClose = document.getBoolean("autoClose") ?: false,
                        imageUrls = (document.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        createdAt = document.getLong("createdAt") ?: 0,
                        status = document.getString("status") ?: "Active",
                        donorCount = document.getLong("donorCount")?.toInt() ?: 0,
                        shareCount = document.getLong("shareCount")?.toInt() ?: 0
                    )
                    setupUI() // Refresh the UI with new data
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), "Error refreshing: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}