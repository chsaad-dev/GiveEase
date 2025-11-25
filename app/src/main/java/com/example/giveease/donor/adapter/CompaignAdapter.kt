package com.example.giveease.donor.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.giveease.R
import com.example.giveease.databinding.ItemCampaignBinding
import com.example.giveease.ngo.CampaignData
import java.text.SimpleDateFormat
import java.util.*

class CampaignAdapter(
    private val campaigns: List<CampaignData>,
    private val onCampaignClick: (CampaignData) -> Unit
) : RecyclerView.Adapter<CampaignAdapter.CampaignViewHolder>() {

    inner class CampaignViewHolder(private val binding: ItemCampaignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(campaign: CampaignData) {
            binding.apply {
                tvCampaignTitle.text = campaign.title
                tvCampaignDesc.text = campaign.description
                tvNgoName.text = campaign.ngoName
                tvCategory.text = campaign.category

                // Progress
                val progress = campaign.getProgress()
                progressBar.progress = progress
                tvProgress.text = "$progress%"

                // Quantity
                tvQuantity.text = "${campaign.currentQuantity} / ${campaign.targetQuantity} ${campaign.unit}"

                // Days left
                val daysLeft = campaign.getDaysLeft()
                tvDaysLeft.text = if (daysLeft > 0) "$daysLeft days left" else "Expired"

                // Urgency badge
                tvUrgency.text = campaign.urgencyLevel
                tvUrgency.setBackgroundColor(getUrgencyColor(campaign.urgencyLevel))

                // Load image
                if (campaign.imageUrls.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(campaign.imageUrls.first())
                        .placeholder(R.drawable.sample_compaign1)
                        .into(imgCampaign)
                } else {
                    imgCampaign.setImageResource(R.drawable.sample_compaign1)
                }

                // Click listeners
                root.setOnClickListener { onCampaignClick(campaign) }
                btnDonate.setOnClickListener { onCampaignClick(campaign) }
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
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CampaignViewHolder {
        val binding = ItemCampaignBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return CampaignViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CampaignViewHolder, position: Int) {
        holder.bind(campaigns[position])
    }

    override fun getItemCount() = campaigns.size
}