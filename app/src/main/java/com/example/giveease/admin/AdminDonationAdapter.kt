package com.example.giveease.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.databinding.ItemAdminDonationBinding
import java.text.SimpleDateFormat
import java.util.*

data class AdminDonation(
    val id: String = "",
    val donorName: String = "",
    val ngoName: String = "",
    val campaignTitle: String = "",
    val quantity: Long = 0,
    val unit: String = "",
    val timestamp: Long = 0
)

class AdminDonationAdapter : RecyclerView.Adapter<AdminDonationAdapter.ViewHolder>() {

    private var items = listOf<AdminDonation>()

    fun submitList(list: List<AdminDonation>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminDonationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemAdminDonationBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(donation: AdminDonation) {
            binding.apply {
                tvDonorName.text = donation.donorName.ifEmpty { "Anonymous" }
                tvNgoName.text = donation.ngoName.ifEmpty { "Unknown NGO" }
                tvCampaignTitle.text = donation.campaignTitle.ifEmpty { "General Campaign" }
                tvQuantity.text = "${donation.quantity} ${donation.unit}"

                val sdf = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                tvDate.text = sdf.format(Date(donation.timestamp))
            }
        }
    }
}
