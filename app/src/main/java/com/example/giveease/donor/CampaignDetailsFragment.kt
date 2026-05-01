package com.example.giveease.donor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.giveease.utils.ChatHelper
import com.example.giveease.donor.ChatDetailFragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentCampaignDetailsBinding
import com.example.giveease.donor.adapter.ImageSliderAdapter
import com.example.giveease.ngo.CampaignData
import com.example.giveease.utils.UserManager
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import com.bumptech.glide.Glide

class CampaignDetailsFragment : Fragment() {

    private var _binding: FragmentCampaignDetailsBinding? = null
    private val binding get() = _binding!!
    private lateinit var campaign: CampaignData
    private val firestore = FirebaseFirestore.getInstance()

    companion object {
        private const val ARG_CAMPAIGN = "campaign"
        private const val ARG_CAMPAIGN_ID = "campaignId"

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

        // Check if we received a campaign object or just an ID
        val campaignObject = arguments?.getSerializable(ARG_CAMPAIGN) as? CampaignData
        val campaignId = arguments?.getString(ARG_CAMPAIGN_ID)

        android.util.Log.d("CampaignDetails", "campaignObject: $campaignObject")
        android.util.Log.d("CampaignDetails", "campaignId: $campaignId")

        if (campaignObject != null) {
            campaign = campaignObject
            setupUI()
            setupClickListeners()
        } else if (campaignId != null) {
            loadCampaignFromFirestore(campaignId)
        } else {
            Toast.makeText(requireContext(), "Campaign not found", Toast.LENGTH_SHORT).show()
            parentFragmentManager.popBackStack()
        }

        // Handle navigation bar overlap for the bottom action buttons
        ViewCompat.setOnApplyWindowInsetsListener(binding.bottomActionCard) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom + 8)
            insets
        }

        return binding.root
    }

    private fun loadCampaignFromFirestore(campaignId: String) {
        android.util.Log.d("CampaignDetails", "Loading campaign from Firestore: $campaignId")

        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("campaigns").document(campaignId).get()
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener
                try {
                    android.util.Log.d("CampaignDetails", "Campaign loaded: ${document.data}")

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

                    binding.progressBar.visibility = View.GONE
                    setupUI()
                    setupClickListeners()

                    android.util.Log.d("CampaignDetails", "UI setup complete")
                } catch (e: Exception) {
                    android.util.Log.e("CampaignDetails", "Error parsing campaign: ${e.message}")
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Error loading campaign: ${e.message}", Toast.LENGTH_SHORT).show()
                    parentFragmentManager.popBackStack()
                }
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("CampaignDetails", "Failed to load campaign: ${exception.message}")
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Failed to load campaign", Toast.LENGTH_SHORT).show()
                parentFragmentManager.popBackStack()
            }
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

            // Fetch and load NGO profile picture
            firestore.collection("users").document(campaign.ngoId).get()
                .addOnSuccessListener { document ->
                    if (_binding != null) {
                        val profileImageUrl = document.getString("profileImageUrl")
                        if (!profileImageUrl.isNullOrEmpty()) {
                            binding.ivNgoProfile.imageTintList = null
                            Glide.with(this@CampaignDetailsFragment)
                                .load(profileImageUrl)
                                .placeholder(R.drawable.ic_organization)
                                .circleCrop()
                                .into(binding.ivNgoProfile)
                        }
                    }
                }

            // Urgency Badge
            val urgency = campaign.urgencyLevel?.takeIf { it.isNotEmpty() } ?: "Low"
            tvUrgencyBadge.text = urgency
            tvUrgencyBadge.setBackgroundResource(getUrgencyBadgeDrawable(urgency))

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
            tvCreatedDate.text = createdDate
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
            
            layoutNgoProfile.setOnClickListener {
                parentFragmentManager.beginTransaction()
                    .hide(this@CampaignDetailsFragment)
                    .add(R.id.fragment_container_donor, NgoPublicProfileFragment.newInstance(campaign.ngoId))
                    .addToBackStack(null)
                    .commit()
            }
            
            setupChatButton(campaign)
        }
    }

    private fun getUrgencyBadgeDrawable(urgency: String): Int {
        return when (urgency) {
            "Emergency" -> R.drawable.urgency_badge_bg_emergency
            "High" -> R.drawable.urgency_badge_bg_high
            "Medium" -> R.drawable.urgency_badge_bg_medium
            else -> R.drawable.urgency_badge_bg_low
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

    private fun setupChatButton(campaign: CampaignData) {
        binding.btnContactNgo.setOnClickListener {
            val currentUserId = UserManager.getUserId(requireContext())
            
            binding.progressBar.visibility = View.VISIBLE
            firestore.collection("users").document(currentUserId).get()
                .addOnSuccessListener { document ->
                    binding.progressBar.visibility = View.GONE
                    val donorName = document.getString("name")?.takeIf { it.isNotEmpty() } ?: UserManager.getUserName(requireContext())
                    val donorImage = document.getString("profileImageUrl") ?: ""
                    
                    val ngoName = campaign.ngoName.ifEmpty { "NGO" }

                    // Force cache the name so ChatDetailFragment can use it locally
                    UserManager.saveUser(requireContext(), currentUserId, "donor", donorName)

                    // Fetch NGO profile image before creating chat
                    firestore.collection("users").document(campaign.ngoId).get()
                        .addOnSuccessListener { ngoDoc ->
                            if (_binding == null) return@addOnSuccessListener
                            val ngoImage = ngoDoc.getString("profileImageUrl") ?: ""

                            ChatHelper.openChatFromCampaign(
                                campaignId = campaign.id,
                                campaignName = campaign.title,
                                campaignImage = campaign.imageUrls.firstOrNull() ?: "",
                                ngoId = campaign.ngoId,
                                ngoName = ngoName,
                                ngoImage = ngoImage,
                                currentDonorId = currentUserId,
                                currentDonorName = donorName,
                                currentDonorImage = donorImage,
                                onChatRoomCreated = { chatRoomId ->
                                    openChatDetail(chatRoomId, campaign.ngoId, ngoName, ngoImage, campaign.title, campaign.id, campaign.imageUrls.firstOrNull() ?: "")
                                },
                                onError = {
                                    Toast.makeText(requireContext(), "Failed to start chat", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                        .addOnFailureListener {
                            // Fallback: open chat without NGO image
                            ChatHelper.openChatFromCampaign(
                                campaignId = campaign.id,
                                campaignName = campaign.title,
                                campaignImage = campaign.imageUrls.firstOrNull() ?: "",
                                ngoId = campaign.ngoId,
                                ngoName = ngoName,
                                ngoImage = "",
                                currentDonorId = currentUserId,
                                currentDonorName = donorName,
                                currentDonorImage = donorImage,
                                onChatRoomCreated = { chatRoomId ->
                                    openChatDetail(chatRoomId, campaign.ngoId, ngoName, "", campaign.title, campaign.id, campaign.imageUrls.firstOrNull() ?: "")
                                },
                                onError = {
                                    Toast.makeText(requireContext(), "Failed to start chat", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                }
                .addOnFailureListener {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(requireContext(), "Failed to check user details", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun openChatDetail(
        chatRoomId: String,
        ngoId: String,
        ngoName: String,
        ngoImage: String,
        campaignName: String,
        campaignId: String,
        campaignImage: String
    ) {
        val fragment = ChatDetailFragment().apply {
            arguments = Bundle().apply {
                putString("chatRoomId", chatRoomId)
                putString("otherUserId", ngoId)
                putString("otherUserName", ngoName)
                putString("otherUserImage", ngoImage)
                putString("campaignName", campaignName)
                putString("campaignId", campaignId)
                putString("campaignImage", campaignImage)
                putBoolean("isDonor", true)
            }
        }

        parentFragmentManager.beginTransaction()
            .hide(this)
            .add((requireView().parent as ViewGroup).id, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun handleDonate() {
        val dialog = DonationDialogFragment.newInstance(campaign)
        dialog.show(childFragmentManager, "DonationDialog")
    }

    fun refreshCampaignData() {
        firestore.collection("campaigns").document(campaign.id).get()
            .addOnSuccessListener { document ->
                if (_binding == null) return@addOnSuccessListener
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
                    setupUI()
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