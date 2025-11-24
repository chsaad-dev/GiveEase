package com.example.giveease.ngo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.databinding.ItemRecentCampaignBinding
import com.example.giveease.ngo.model.Campaign

class RecentCampaignAdapter(
    private val onCampaignClick: (Campaign) -> Unit
) : ListAdapter<Campaign, RecentCampaignAdapter.CampaignViewHolder>(CampaignDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CampaignViewHolder {
        val binding = ItemRecentCampaignBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CampaignViewHolder(binding, onCampaignClick)
    }

    override fun onBindViewHolder(holder: CampaignViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class CampaignViewHolder(
        private val binding: ItemRecentCampaignBinding,
        private val onCampaignClick: (Campaign) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(campaign: Campaign) {
            binding.apply {
                tvCampaignTitle.text = campaign.title
                tvCampaignDescription.text = campaign.description
                tvRaisedAmount.text = campaign.getRaisedAmountFormatted()
                tvTargetAmount.text = " of ${campaign.getTargetAmountFormatted()}"
                tvProgress.text = campaign.getProgressText()
                tvStatus.text = campaign.status
                progressBar.progress = campaign.getProgress()

                root.setOnClickListener {
                    onCampaignClick(campaign)
                }
            }
        }
    }

    private class CampaignDiffCallback : DiffUtil.ItemCallback<Campaign>() {
        override fun areItemsTheSame(oldItem: Campaign, newItem: Campaign): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Campaign, newItem: Campaign): Boolean {
            return oldItem == newItem
        }
    }
}