package com.example.giveease.donor

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.R
import com.example.giveease.databinding.ItemDonationBinding
import java.text.SimpleDateFormat
import java.util.*

class DonationAdapter(
    private val donations: List<DonationHistoryFragment.Donation>,
    private val onItemClick: (DonationHistoryFragment.Donation) -> Unit
) : RecyclerView.Adapter<DonationAdapter.DonationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonationViewHolder {
        val binding = ItemDonationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return DonationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DonationViewHolder, position: Int) {
        holder.bind(donations[position])
    }

    override fun getItemCount(): Int = donations.size

    inner class DonationViewHolder(private val binding: ItemDonationBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(donation: DonationHistoryFragment.Donation) {
            binding.apply {
                tvNgoName.text = donation.ngoName
                tvCampaignTitle.text = donation.campaignTitle

                val quantity = donation.amount.toInt()
                tvDonationAmount.text = "$quantity ${if (quantity == 1) "item" else "items"}"

                tvCategory.text = donation.category
                tvDonationDate.text = formatDate(donation.createdAt)

                when (donation.status.lowercase()) {
                    "completed" -> {
                        tvStatus.text = "Completed"
                        tvStatus.setBackgroundResource(R.drawable.status_completed_bg)
                        statusIndicator.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.holo_green_dark))
                    }
                    "pending" -> {
                        tvStatus.text = "Pending"
                        tvStatus.setBackgroundResource(R.drawable.status_pending_bg)
                        statusIndicator.setBackgroundColor(ContextCompat.getColor(itemView.context, android.R.color.holo_orange_dark))
                    }
                    "failed" -> {
                        tvStatus.text = "Failed"
                        tvStatus.setBackgroundResource(R.drawable.status_failed_bg)
                        statusIndicator.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.error))
                    }
                    else -> {
                        tvStatus.text = "Unknown"
                        tvStatus.setBackgroundResource(R.drawable.status_pending_bg)
                        statusIndicator.setBackgroundColor(ContextCompat.getColor(itemView.context, R.color.gray))
                    }
                }

                if (donation.receiptUrl != null && donation.status.lowercase() == "completed") {
                    ivReceiptDownload.visibility = View.VISIBLE
                    ivReceiptDownload.setOnClickListener {
                        android.widget.Toast.makeText(itemView.context, "Downloading receipt...", android.widget.Toast.LENGTH_SHORT).show()
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
}