package com.example.giveease.ngo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.giveease.ngo.CampaignData
import com.example.giveease.R
import com.example.giveease.databinding.ItemCampaignManagementBinding

class CampaignManagementAdapter(
    private val onEditClick: (CampaignData) -> Unit,
    private val onStatusChangeClick: (CampaignData) -> Unit,
    private val onDeleteClick: (CampaignData) -> Unit,
    private val onCampaignClick: (CampaignData) -> Unit
) : RecyclerView.Adapter<CampaignManagementAdapter.CampaignViewHolder>() {

    private var campaigns = listOf<CampaignData>()

    fun submitList(newList: List<CampaignData>) {
        campaigns = newList
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = campaigns.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CampaignViewHolder {
        val binding = ItemCampaignManagementBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CampaignViewHolder(binding)
    }

    override fun onBindViewHolder(holder: CampaignViewHolder, position: Int) {
        holder.bind(campaigns[position])
    }

    inner class CampaignViewHolder(
        private val binding: ItemCampaignManagementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(campaign: CampaignData) {
            binding.apply {
                // Campaign details
                tvCampaignTitle.text = campaign.title
                tvCampaignCategory.text = campaign.category
                tvCampaignDescription.text = campaign.description

                // Status badge
                tvStatus.text = campaign.status
                when (campaign.status) {
                    "Active" -> {
                        cardStatus.setCardBackgroundColor(
                            root.context.getColor(android.R.color.holo_green_light)
                        )
                        tvStatus.setTextColor(root.context.getColor(android.R.color.holo_green_dark))
                    }
                    "Paused" -> {
                        cardStatus.setCardBackgroundColor(
                            root.context.getColor(android.R.color.holo_orange_light)
                        )
                        tvStatus.setTextColor(root.context.getColor(android.R.color.holo_orange_dark))
                    }
                    "Completed" -> {
                        cardStatus.setCardBackgroundColor(
                            root.context.getColor(android.R.color.darker_gray)
                        )
                        tvStatus.setTextColor(root.context.getColor(android.R.color.white))
                    }
                }

                // Progress
                val progress = campaign.getProgress()
                progressBar.progress = progress
                tvCurrentQuantity.text = "${campaign.currentQuantity} ${campaign.unit}"
                tvTargetQuantity.text = " of ${campaign.targetQuantity} ${campaign.unit}"
                tvProgress.text = "$progress%"

                // Days left
                val daysLeft = campaign.getDaysLeft()
                tvDaysLeft.text = if (daysLeft > 0) {
                    "$daysLeft days left"
                } else {
                    "Expired"
                }
                tvDaysLeft.setTextColor(
                    if (daysLeft > 0)
                        root.context.getColor(android.R.color.holo_green_dark)
                    else
                        root.context.getColor(android.R.color.holo_red_dark)
                )

                // Donors count
                tvDonorCount.text = "${campaign.donorCount} donors"

                // Campaign image
                if (campaign.imageUrls.isNotEmpty()) {
                    Glide.with(root.context)
                        .load(campaign.imageUrls[0])
                        .placeholder(R.drawable.sample_ngo)
                        .error(R.drawable.sample_ngo)
                        .centerCrop()
                        .into(imgCampaign)
                } else {
                    imgCampaign.setImageResource(R.drawable.sample_ngo)
                }

                // Button states based on status
                when (campaign.status) {
                    "Completed" -> {
                        btnEdit.visibility = View.GONE
                        btnStatusChange.visibility = View.GONE
                    }
                    "Active" -> {
                        btnEdit.visibility = View.VISIBLE
                        btnStatusChange.visibility = View.VISIBLE
                        btnStatusChange.text = "Pause"
                        btnStatusChange.setIconResource(R.drawable.ic_pause)
                    }
                    "Paused" -> {
                        btnEdit.visibility = View.VISIBLE
                        btnStatusChange.visibility = View.VISIBLE
                        btnStatusChange.text = "Activate"
                        btnStatusChange.setIconResource(R.drawable.ic_play)
                    }
                }

                // Click listeners
                root.setOnClickListener { onCampaignClick(campaign) }
                btnEdit.setOnClickListener { onEditClick(campaign) }
                btnStatusChange.setOnClickListener { onStatusChangeClick(campaign) }
                btnDelete.setOnClickListener { onDeleteClick(campaign) }
            }
        }
    }
}