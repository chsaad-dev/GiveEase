package com.example.giveease.ngo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.databinding.ItemManageCampaignBinding

class ManageCampaignAdapter(
    private val onEditClick: (CampaignData) -> Unit,
    private val onCampaignClick: (CampaignData) -> Unit
) : ListAdapter<CampaignData, ManageCampaignAdapter.CampaignViewHolder>(CampaignDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CampaignViewHolder {
        val binding = ItemManageCampaignBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CampaignViewHolder(binding, onEditClick, onCampaignClick)
    }

    override fun onBindViewHolder(holder: CampaignViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CampaignViewHolder(
        private val binding: ItemManageCampaignBinding,
        private val onEditClick: (CampaignData) -> Unit,
        private val onCampaignClick: (CampaignData) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(campaign: CampaignData) {
            binding.apply {
                tvCampaignTitle.text = campaign.title
                tvCampaignDescription.text = campaign.description
                tvStatus.text = campaign.status
                tvDaysLeft.text = "${campaign.getDaysLeft()} days left"
                tvProgress.text = "${campaign.getProgress()}%"

                btnEdit.setOnClickListener {
                    onEditClick(campaign)
                }

                root.setOnClickListener {
                    onCampaignClick(campaign)
                }
            }
        }
    }

    private class CampaignDiffCallback : DiffUtil.ItemCallback<CampaignData>() {
        override fun areItemsTheSame(oldItem: CampaignData, newItem: CampaignData): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: CampaignData, newItem: CampaignData): Boolean {
            return oldItem == newItem
        }
    }
}