package com.example.giveease.ui

import android.graphics.Color
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.databinding.ItemNotificationBinding
import com.example.giveease.models.Notification
import java.text.SimpleDateFormat
import java.util.*

class NotificationsAdapter(
    private var notifications: List<Notification>,
    private val onNotificationClick: (Notification, Int) -> Unit,
    private val onSelectionChanged: (Int) -> Unit
) : RecyclerView.Adapter<NotificationsAdapter.ViewHolder>() {

    val selectedItems = mutableSetOf<String>()
    var isSelectionMode = false

    fun updateData(newList: List<Notification>) {
        notifications = newList
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemNotificationBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(notifications[position], position)
    }

    override fun getItemCount(): Int = notifications.size

    fun clearSelections() {
        selectedItems.clear()
        isSelectionMode = false
        notifyDataSetChanged()
        onSelectionChanged(0)
    }

    inner class ViewHolder(private val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(notification: Notification, position: Int) {
            binding.tvTitle.text = notification.title
            binding.tvMessage.text = notification.message
            binding.tvTime.text = getRelativeTime(notification.timestamp)

            // Unread visual indication
            if (!notification.isRead && !isSelectionMode) {
                binding.unreadDot.visibility = View.VISIBLE
                binding.innerLayout.setBackgroundColor(Color.parseColor("#F4F8FE")) // Light highlight
            } else {
                binding.unreadDot.visibility = View.GONE
                binding.innerLayout.setBackgroundColor(Color.TRANSPARENT)
            }

            // Selection Visuals
            val isSelected = selectedItems.contains(notification.id)
            if (isSelected) {
                binding.innerLayout.setBackgroundColor(Color.parseColor("#BBDEFB")) // Selected Blue
                binding.iconContainer.setCardBackgroundColor(Color.parseColor("#1976D2")) // Dark Blue
                binding.ivIcon.visibility = View.GONE
                binding.ivCheckMark.visibility = View.VISIBLE
            } else {
                binding.iconContainer.setCardBackgroundColor(Color.parseColor("#E3F2FD")) // Light Blue
                binding.ivIcon.visibility = View.VISIBLE
                binding.ivCheckMark.visibility = View.GONE
            }

            // Click Handlers
            binding.root.setOnClickListener {
                if (isSelectionMode) {
                    toggleSelection(notification.id)
                } else {
                    onNotificationClick(notification, position)
                }
            }

            binding.root.setOnLongClickListener {
                if (!isSelectionMode) {
                    isSelectionMode = true
                    toggleSelection(notification.id)
                }
                true
            }
        }

        private fun toggleSelection(id: String) {
            if (selectedItems.contains(id)) {
                selectedItems.remove(id)
                if (selectedItems.isEmpty()) {
                    isSelectionMode = false
                }
            } else {
                selectedItems.add(id)
            }
            notifyDataSetChanged()
            onSelectionChanged(selectedItems.size)
        }
    }

    private fun getRelativeTime(timestamp: Long): String {
        if (timestamp == 0L) return "Unknown"
        val now = System.currentTimeMillis()
        return DateUtils.getRelativeTimeSpanString(timestamp, now, DateUtils.MINUTE_IN_MILLIS).toString()
    }
}
