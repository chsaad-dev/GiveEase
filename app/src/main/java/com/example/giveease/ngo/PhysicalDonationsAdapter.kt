package com.example.giveease.ngo

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.giveease.databinding.ItemPhysicalDonationBinding
import java.text.SimpleDateFormat
import java.util.*

class PhysicalDonationsAdapter(
    private var donationList: List<Map<String, Any>>,
    private val isVerificationMode: Boolean,
    private val onApproveClick: (Map<String, Any>) -> Unit,
    private val onRejectClick: (Map<String, Any>) -> Unit,
    private val onReceivedClick: (Map<String, Any>) -> Unit,
    private val onViewPhotoClick: (String) -> Unit
) : RecyclerView.Adapter<PhysicalDonationsAdapter.ViewHolder>() {

    fun updateData(newList: List<Map<String, Any>>) {
        donationList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemPhysicalDonationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(donationList[position])
    }

    override fun getItemCount(): Int = donationList.size

    inner class ViewHolder(private val binding: ItemPhysicalDonationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(donation: Map<String, Any>) {
            val donorName = donation["donorName"]?.toString() ?: "Unknown Donor"
            binding.tvDonorName.text = donorName
            if (donorName.isNotEmpty()) {
                binding.tvDonorInitial.text = donorName.first().uppercaseChar().toString()
            }

            val qty = (donation["quantity"] as? Number)?.toLong() ?: 0L
            val unit = donation["unit"] ?: ""
            binding.tvQuantity.text = "Quantity: $qty $unit"
            
            binding.tvCampaignTitle.text = donation["campaignTitle"]?.toString() ?: "Campaign"
            binding.tvCondition.text = donation["condition"]?.toString() ?: "Not specified"
            
            val handover = donation["handoverMethod"]?.toString() ?: "Drop-off"
            binding.tvLogistics.text = handover

            if (handover == "Pickup") {
                binding.llAddressContainer.visibility = View.VISIBLE
                binding.tvPickupPhone.text = "Phone: ${donation["pickupPhone"]?.toString() ?: "N/A"}"
                binding.tvPickupAddress.text = "Address: ${donation["pickupAddress"]?.toString() ?: "N/A"}"
            } else {
                binding.llAddressContainer.visibility = View.GONE
            }

            val timestamp = donation["timestamp"] as? Long ?: 0L
            binding.tvTime.text = formatTimeAgo(timestamp)

            val photoUrl = donation["itemPhotoUrl"]?.toString() ?: ""
            if (photoUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(photoUrl)
                    .into(binding.ivItemProof)
            }

            binding.ivItemProof.setOnClickListener {
                if (photoUrl.isNotEmpty()) {
                    onViewPhotoClick(photoUrl)
                }
            }

            // Mode Logic
            if (isVerificationMode) {
                binding.llVerificationActions.visibility = View.VISIBLE
                binding.llLogisticsActions.visibility = View.GONE
            } else {
                binding.llVerificationActions.visibility = View.GONE
                binding.llLogisticsActions.visibility = View.VISIBLE
            }

            binding.btnApprove.setOnClickListener { onApproveClick(donation) }
            binding.btnReject.setOnClickListener { onRejectClick(donation) }
            binding.btnReceived.setOnClickListener { onReceivedClick(donation) }
        }
    }

    private fun formatTimeAgo(timestamp: Long): String {
        if (timestamp == 0L) return "N/A"
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            diff < 604800000 -> "${diff / 86400000}d ago"
            else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
        }
    }
}
