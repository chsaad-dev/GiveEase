package com.example.giveease.donor

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.R
import com.example.giveease.databinding.ItemDonorCampaignBinding
import java.text.SimpleDateFormat
import java.util.*

class DonorCampaignAdapter(
    private val campaigns: List<DonorCampaignsFragment.DonorCampaign>,
    private val onViewContacts: (DonorCampaignsFragment.DonorCampaign) -> Unit,
    private val onEditCampaign: (DonorCampaignsFragment.DonorCampaign) -> Unit,
    private val onItemClick: (DonorCampaignsFragment.DonorCampaign) -> Unit
) : RecyclerView.Adapter<DonorCampaignAdapter.CampaignViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CampaignViewHolder {
        val binding = ItemDonorCampaignBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return CampaignViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CampaignViewHolder, position: Int) {
        holder.bind(campaigns[position])
    }

    override fun getItemCount(): Int = campaigns.size

    inner class CampaignViewHolder(private val binding: ItemDonorCampaignBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(campaign: DonorCampaignsFragment.DonorCampaign) {
            binding.apply {
                tvCampaignTitle.text = campaign.title
                tvCampaignDescription.text = campaign.description
                tvDonationAmount.text = "Rs ${String.format("%,d", campaign.amount.toInt())}"
                tvCampaignCategory.text = campaign.category
                tvViewCount.text = campaign.viewCount.toString()
                tvContactCount.text = campaign.contactCount.toString()
                tvCreatedDate.text = formatDate(campaign.createdAt)

                when (campaign.status.lowercase()) {
                    "active" -> {
                        tvCampaignStatus.text = "Active"
                        tvCampaignStatus.setBackgroundResource(R.drawable.status_completed_bg)
                    }
                    "completed" -> {
                        tvCampaignStatus.text = "Completed"
                        tvCampaignStatus.setBackgroundResource(R.drawable.status_completed_bg)
                    }
                    "cancelled" -> {
                        tvCampaignStatus.text = "Cancelled"
                        tvCampaignStatus.setBackgroundResource(R.drawable.status_failed_bg)
                    }
                    else -> {
                        tvCampaignStatus.text = "Unknown"
                        tvCampaignStatus.setBackgroundResource(R.drawable.status_pending_bg)
                    }
                }

                if (campaign.hasProof) {
                    layoutProofIndicator.visibility = View.VISIBLE
                } else {
                    layoutProofIndicator.visibility = View.GONE
                }

                if (campaign.status == "active") {
                    btnViewContacts.isEnabled = true
                    btnEditCampaign.isEnabled = true
                    btnViewContacts.text = "View Contacts (${campaign.contactCount})"
                } else {
                    btnViewContacts.isEnabled = false
                    btnEditCampaign.isEnabled = false
                    btnViewContacts.text = "View Contacts"
                }

                btnViewContacts.setOnClickListener {
                    onViewContacts(campaign)
                }

                btnEditCampaign.setOnClickListener {
                    onEditCampaign(campaign)
                }

                root.setOnClickListener {
                    onItemClick(campaign)
                }
            }
        }

        private fun formatDate(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 86400000 -> "Today"
                diff < 172800000 -> "Yesterday"
                diff < 604800000 -> "${diff / 86400000} days ago"
                else -> {
                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }
}