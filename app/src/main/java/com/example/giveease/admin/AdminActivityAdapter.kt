package com.example.giveease.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.R
import com.example.giveease.databinding.ItemAdminActivityBinding

data class AdminActivity(
    val type: String = "",   // "verification", "donation", "campaign"
    val title: String = "",
    val subtitle: String = "",
    val timestamp: Long = 0
)

class AdminActivityAdapter : RecyclerView.Adapter<AdminActivityAdapter.ViewHolder>() {

    private var items = listOf<AdminActivity>()

    fun submitList(list: List<AdminActivity>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminActivityBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemAdminActivityBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(activity: AdminActivity) {
            binding.apply {
                tvActivityTitle.text = activity.title
                tvActivitySubtitle.text = activity.subtitle
                tvActivityTime.text = formatTimeAgo(activity.timestamp)

                // Icon and color based on type
                when (activity.type) {
                    "verification" -> {
                        ivActivityIcon.setImageResource(R.drawable.ic_verified)
                        ivActivityIcon.setColorFilter(0xFF4CAF50.toInt())
                    }
                    "donation" -> {
                        ivActivityIcon.setImageResource(R.drawable.ic_gift)
                        ivActivityIcon.setColorFilter(0xFF1565C0.toInt())
                    }
                    "campaign" -> {
                        ivActivityIcon.setImageResource(R.drawable.ic_campaign)
                        ivActivityIcon.setColorFilter(0xFFFF9800.toInt())
                    }
                    "rejection" -> {
                        ivActivityIcon.setImageResource(R.drawable.ic_cancel)
                        ivActivityIcon.setColorFilter(0xFFF44336.toInt())
                    }
                    else -> {
                        ivActivityIcon.setImageResource(R.drawable.ic_info)
                        ivActivityIcon.setColorFilter(0xFF546E7A.toInt())
                    }
                }
            }
        }

        private fun formatTimeAgo(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < 60000 -> "Just now"
                diff < 3600000 -> "${diff / 60000}m ago"
                diff < 86400000 -> "${diff / 3600000}h ago"
                diff < 604800000 -> "${diff / 86400000}d ago"
                else -> {
                    val sdf = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())
                    sdf.format(java.util.Date(timestamp))
                }
            }
        }
    }
}
