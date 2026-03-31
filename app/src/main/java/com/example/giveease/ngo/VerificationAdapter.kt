package com.example.giveease.ngo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.giveease.R
import com.example.giveease.databinding.ItemMonetaryVerificationBinding
import java.text.SimpleDateFormat
import java.util.*

class VerificationAdapter(
    private var verificationList: List<Map<String, Any>>,
    private val onVerifyClick: (Map<String, Any>) -> Unit,
    private val onRejectClick: (Map<String, Any>) -> Unit,
    private val onViewReceiptClick: (String) -> Unit
) : RecyclerView.Adapter<VerificationAdapter.VerificationViewHolder>() {

    fun updateData(newList: List<Map<String, Any>>) {
        verificationList = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VerificationViewHolder {
        val binding = ItemMonetaryVerificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VerificationViewHolder(binding)
    }

    override fun onBindViewHolder(holder: VerificationViewHolder, position: Int) {
        holder.bind(verificationList[position])
    }

    override fun getItemCount(): Int = verificationList.size

    inner class VerificationViewHolder(private val binding: ItemMonetaryVerificationBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(donation: Map<String, Any>) {
            binding.tvDonorName.text = donation["donorName"]?.toString() ?: "Unknown Donor"
            binding.tvAmount.text = "${donation["quantity"]} ${donation["unit"]}"
            binding.tvCampaignTitle.text = donation["campaignTitle"]?.toString() ?: "Campaign"
            
            val transactionId = donation["transactionId"]?.toString() ?: "N/A"
            binding.tvTransactionId.text = "Transaction ID: $transactionId"
            
            val timestamp = donation["timestamp"] as? Long ?: 0L
            binding.tvTime.text = formatTimeAgo(timestamp)

            val receiptUrl = donation["receiptUrl"]?.toString() ?: ""
            if (receiptUrl.isNotEmpty()) {
                Glide.with(binding.root.context)
                    .load(receiptUrl)
                    .placeholder(R.drawable.sample_ngo)
                    .into(binding.ivReceipt)
            } else {
                binding.ivReceipt.setImageResource(R.drawable.sample_ngo)
            }

            binding.ivReceipt.setOnClickListener {
                if (receiptUrl.isNotEmpty()) {
                    onViewReceiptClick(receiptUrl)
                }
            }

            binding.btnVerify.setOnClickListener {
                onVerifyClick(donation)
            }

            binding.btnReject.setOnClickListener {
                onRejectClick(donation)
            }
        }
    }

    private fun formatTimeAgo(timestamp: Long): String {
        if (timestamp == 0L) return "N/A"
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        return when {
            diff < 60000 -> "Just now"
            diff < 3600000 -> "${diff / 60000}m ago"
            diff < 86400000 -> "${diff / 3600000}h ago"
            diff < 604800000 -> "${diff / 86400000}d ago"
            else -> {
                val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}
