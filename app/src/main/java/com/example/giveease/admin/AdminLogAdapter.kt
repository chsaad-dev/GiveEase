package com.example.giveease.admin

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.R
import com.example.giveease.databinding.ItemAdminLogBinding
import java.text.SimpleDateFormat
import java.util.*

data class AdminLog(
    val id: String = "",
    val actionType: String = "", // e.g., "approve_user", "deactivate_campaign"
    val actionTitle: String = "",
    val actionDetail: String = "",
    val timestamp: Long = 0,
    val adminId: String = ""
)

class AdminLogAdapter : RecyclerView.Adapter<AdminLogAdapter.ViewHolder>() {

    private var items = listOf<AdminLog>()

    fun submitList(list: List<AdminLog>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminLogBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemAdminLogBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(log: AdminLog) {
            binding.apply {
                tvActionTitle.text = log.actionTitle
                tvActionDetail.text = log.actionDetail

                val sdf = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())
                tvTime.text = sdf.format(Date(log.timestamp))

                // Configure Icon and Color based on actionType
                val (iconRes, colorHex) = when (log.actionType) {
                    "approve_user" -> Pair(R.drawable.ic_check, "#4CAF50") // Green
                    "reject_user", "revoke_user" -> Pair(R.drawable.ic_close, "#F44336") // Red
                    "deactivate_campaign" -> Pair(R.drawable.ic_block, "#FF9800") // Orange
                    "reactivate_campaign" -> Pair(R.drawable.ic_refresh, "#2196F3") // Blue
                    "settings_change" -> Pair(R.drawable.ic_settings, "#9C27B0") // Purple
                    else -> Pair(R.drawable.ic_info, "#607D8B") // Grey
                }

                ivActionIcon.setImageResource(iconRes)
                val colorInt = Color.parseColor(colorHex)
                ivActionIcon.imageTintList = ColorStateList.valueOf(colorInt)
                
                // Set light background for the icon circle
                val bgHex = colorHex.replace("#", "#20") // 12% opacity roughly
                cardIcon.setCardBackgroundColor(Color.parseColor(bgHex))
            }
        }
    }
}
