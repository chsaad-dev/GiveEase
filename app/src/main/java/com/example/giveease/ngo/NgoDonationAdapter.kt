package com.example.giveease.ngo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.R
import com.example.giveease.databinding.ItemNgoDonationBinding
import java.text.SimpleDateFormat
import java.util.*

class NgoDonationAdapter(
    private val onDonationClick: (NgoDonation) -> Unit
) : RecyclerView.Adapter<NgoDonationAdapter.DonationViewHolder>() {

    private var donations = listOf<NgoDonation>()

    fun submitList(newList: List<NgoDonation>) {
        donations = newList
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = donations.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DonationViewHolder {
        val binding = ItemNgoDonationBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return DonationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: DonationViewHolder, position: Int) {
        holder.bind(donations[position])
    }

    inner class DonationViewHolder(
        private val binding: ItemNgoDonationBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(donation: NgoDonation) {
            binding.apply {
                // Donor info
                tvDonorName.text = donation.donorName
                tvDonationDate.text = formatDate(donation.timestamp)

                // Campaign title
                tvCampaignTitle.text = donation.campaignTitle

                // Quantity with unit
                tvQuantity.text = "${donation.quantity} ${donation.unit}"

                // Status badge
                when (donation.status.lowercase()) {
                    "completed" -> {
                        tvStatus.text = "Completed"
                        cardStatus.setCardBackgroundColor(
                            root.context.getColor(android.R.color.holo_green_light)
                        )
                        tvStatus.setTextColor(root.context.getColor(android.R.color.holo_green_dark))
                    }
                    "pending" -> {
                        tvStatus.text = "Pending"
                        cardStatus.setCardBackgroundColor(
                            root.context.getColor(android.R.color.holo_orange_light)
                        )
                        tvStatus.setTextColor(root.context.getColor(android.R.color.holo_orange_dark))
                    }
                    else -> {
                        tvStatus.text = donation.status
                        cardStatus.setCardBackgroundColor(
                            root.context.getColor(android.R.color.darker_gray)
                        )
                        tvStatus.setTextColor(root.context.getColor(android.R.color.white))
                    }
                }

                // Message handling
                if (donation.message.isNotEmpty()) {
                    cardMessage.visibility = View.VISIBLE
                    tvMessage.text = donation.message

                    // Show/hide message content on click
                    var isMessageExpanded = false
                    cardMessage.setOnClickListener {
                        isMessageExpanded = !isMessageExpanded
                        cardMessageContent.visibility = if (isMessageExpanded) View.VISIBLE else View.GONE
                    }

                    root.setOnClickListener {
                        isMessageExpanded = !isMessageExpanded
                        cardMessageContent.visibility = if (isMessageExpanded) View.VISIBLE else View.GONE
                    }
                } else {
                    cardMessage.visibility = View.GONE
                    cardMessageContent.visibility = View.GONE

                    root.setOnClickListener {
                        onDonationClick(donation)
                    }
                }
            }
        }

        private fun formatDate(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000}m ago"
                diff < 86400000 -> "${diff / 3600000}h ago"
                diff < 172800000 -> "Yesterday"
                diff < 604800000 -> "${diff / 86400000}d ago"
                else -> {
                    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }
}

data class NgoDonation(
    val id: String = "",
    val donorId: String = "",
    val donorName: String = "",
    val campaignId: String = "",
    val campaignTitle: String = "",
    val quantity: Int = 0,
    val unit: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val status: String = "Completed"
)