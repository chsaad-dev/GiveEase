package com.example.giveease.donor

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.R
import com.example.giveease.databinding.ItemDonationBinding
import java.text.SimpleDateFormat
import java.util.*

class DonationAdapter(
    private val onItemClick: (DonationHistoryFragment.Donation) -> Unit
) : ListAdapter<DonationHistoryFragment.Donation, DonationAdapter.DonationViewHolder>(DonationDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonationViewHolder {
        val binding = ItemDonationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DonationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DonationViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class DonationViewHolder(private val binding: ItemDonationBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(donation: DonationHistoryFragment.Donation) {
            binding.apply {
                tvNgoName.text = donation.ngoName
                tvCampaignTitle.text = donation.campaignTitle

                val quantity = donation.amount.toInt()
                tvDonationAmount.text = "$quantity ${if (quantity == 1) "item" else "items"}"

                tvCategory.text = donation.category
                tvDonationDate.text = formatDate(donation.createdAt)

                val currentStatus = donation.status.lowercase()
                when {
                    currentStatus == "delivered" -> {
                        tvStatus.text = "Delivered"
                        tvStatus.setBackgroundResource(R.drawable.status_completed_bg)
                        statusIndicator.setBackgroundColor(ContextCompat.getColor(root.context, android.R.color.holo_green_dark))
                    }
                    currentStatus == "completed" -> {
                        tvStatus.text = "Completed"
                        tvStatus.setBackgroundResource(R.drawable.status_completed_bg)
                        statusIndicator.setBackgroundColor(ContextCompat.getColor(root.context, android.R.color.holo_green_dark))
                    }
                    currentStatus.contains("pending") -> {
                        // Dynamically use the precise status string (e.g. "Pending Verification") but proper caps
                        tvStatus.text = donation.status 
                        tvStatus.setBackgroundResource(R.drawable.status_pending_bg)
                        statusIndicator.setBackgroundColor(ContextCompat.getColor(root.context, android.R.color.holo_orange_dark))
                    }
                    currentStatus == "failed" || currentStatus == "rejected" -> {
                        tvStatus.text = donation.status // Shows Failed or Rejected
                        tvStatus.setBackgroundResource(R.drawable.status_failed_bg)
                        statusIndicator.setBackgroundColor(ContextCompat.getColor(root.context, R.color.error))
                    }
                    else -> {
                        tvStatus.text = "Unknown"
                        tvStatus.setBackgroundResource(R.drawable.status_pending_bg)
                        statusIndicator.setBackgroundColor(ContextCompat.getColor(root.context, R.color.gray))
                    }
                }

                if (donation.proof != null || currentStatus == "delivered") {
                    btnViewProof.visibility = View.VISIBLE
                    btnViewProof.setOnClickListener {
                        onItemClick(donation) // Let the fragment handle navigation to ViewProof
                    }
                } else {
                    btnViewProof.visibility = View.GONE
                }

                if (donation.receiptUrl != null && donation.status.lowercase() == "completed") {
                    ivReceiptDownload.visibility = View.VISIBLE
                    ivReceiptDownload.setOnClickListener {
                        android.widget.Toast.makeText(root.context, "Downloading receipt...", android.widget.Toast.LENGTH_SHORT).show()
                    }
                } else {
                    ivReceiptDownload.visibility = View.INVISIBLE
                }

                root.setOnClickListener {
                    onItemClick(donation)
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

    class DonationDiffCallback : DiffUtil.ItemCallback<DonationHistoryFragment.Donation>() {
        override fun areItemsTheSame(
            oldItem: DonationHistoryFragment.Donation,
            newItem: DonationHistoryFragment.Donation
        ): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(
            oldItem: DonationHistoryFragment.Donation,
            newItem: DonationHistoryFragment.Donation
        ): Boolean {
            return oldItem == newItem
        }
    }
}