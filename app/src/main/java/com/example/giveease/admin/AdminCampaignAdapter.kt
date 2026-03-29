package com.example.giveease.admin

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.giveease.R
import com.example.giveease.databinding.ItemAdminCampaignBinding
import java.text.SimpleDateFormat
import java.util.*

data class AdminCampaign(
    val id: String = "",
    val title: String = "",
    val ngoName: String = "",
    val status: String = "Active",
    val category: String = "",
    val urgencyLevel: String = "",
    val imageUrl: String = "",
    val createdAt: Long = 0,
    val targetQuantity: Long = 0,
    val currentQuantity: Long = 0
)

class AdminCampaignAdapter(
    private val onActionClick: (AdminCampaign) -> Unit
) : RecyclerView.Adapter<AdminCampaignAdapter.ViewHolder>() {

    private var items = listOf<AdminCampaign>()

    fun submitList(list: List<AdminCampaign>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminCampaignBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemAdminCampaignBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(campaign: AdminCampaign) {
            binding.apply {
                tvCampaignTitle.text = campaign.title
                tvNgoName.text = campaign.ngoName

                // Status badge
                tvStatus.text = campaign.status
                val statusBg = GradientDrawable().apply {
                    cornerRadius = 20f
                    setColor(
                        when (campaign.status) {
                            "Active" -> 0xFF4CAF50.toInt()
                            "Completed" -> 0xFF1565C0.toInt()
                            "Deactivated" -> 0xFFF44336.toInt()
                            else -> 0xFF9E9E9E.toInt()
                        }
                    )
                }
                tvStatus.background = statusBg

                // Date
                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                tvDate.text = sdf.format(Date(campaign.createdAt))

                // Image
                if (campaign.imageUrl.isNotEmpty()) {
                    Glide.with(itemView.context)
                        .load(campaign.imageUrl)
                        .centerCrop()
                        .placeholder(R.drawable.sample_compaign1)
                        .into(ivCampaignImage)
                } else {
                    ivCampaignImage.setImageResource(R.drawable.sample_compaign1)
                }

                // Action
                btnAction.setOnClickListener { onActionClick(campaign) }
                root.setOnClickListener { onActionClick(campaign) }
            }
        }
    }
}
