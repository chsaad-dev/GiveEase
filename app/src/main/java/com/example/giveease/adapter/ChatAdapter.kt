package com.example.giveease.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.giveease.R
import com.example.giveease.model.ChatRoom
import com.google.android.material.imageview.ShapeableImageView
import java.text.SimpleDateFormat
import java.util.*

class ChatAdapter(
    private val currentUserId: String,
    private val onChatClick: (ChatRoom) -> Unit
) : ListAdapter<ChatRoom, ChatAdapter.ChatViewHolder>(ChatDiffCallback()) {

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val imgProfile: ShapeableImageView = view.findViewById(R.id.imgProfile)
        private val onlineIndicator: View = view.findViewById(R.id.onlineIndicator)
        private val tvName: TextView = view.findViewById(R.id.tvName)
        private val tvLastMessage: TextView = view.findViewById(R.id.tvLastMessage)
        private val tvCampaignTag: TextView = view.findViewById(R.id.tvCampaignTag)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)
        private val tvUnreadBadge: TextView = view.findViewById(R.id.tvUnreadBadge)
        private val imgMessageStatus: ImageView = view.findViewById(R.id.imgMessageStatus)

        fun bind(chatRoom: ChatRoom) {
            val isDonor = currentUserId == chatRoom.donorId
            val otherUserName = if (isDonor) chatRoom.ngoName else chatRoom.donorName
            val otherUserImage = if (isDonor) chatRoom.ngoImage else chatRoom.donorImage
            val isOtherUserOnline = if (isDonor) chatRoom.ngoOnline else chatRoom.donorOnline
            val unreadCount = if (isDonor) chatRoom.unreadCountDonor else chatRoom.unreadCountNgo

            tvName.text = otherUserName

            Glide.with(itemView.context)
                .load(otherUserImage)
                .placeholder(R.drawable.sample_profile)
                .into(imgProfile)

            onlineIndicator.visibility = if (isOtherUserOnline) View.VISIBLE else View.GONE

            if (chatRoom.campaignName.isNotEmpty()) {
                tvCampaignTag.visibility = View.VISIBLE
                tvCampaignTag.text = chatRoom.campaignName
            } else {
                tvCampaignTag.visibility = View.GONE
            }

            tvLastMessage.text = when {
                chatRoom.lastMessage.isEmpty() -> "Tap to start conversation"
                chatRoom.lastMessageSenderId == currentUserId -> "You: ${chatRoom.lastMessage}"
                else -> chatRoom.lastMessage
            }

            chatRoom.lastMessageTime?.let {
                tvTime.text = formatTime(it.toDate())
            }

            if (unreadCount > 0) {
                tvUnreadBadge.visibility = View.VISIBLE
                tvUnreadBadge.text = if (unreadCount > 99) "99+" else unreadCount.toString()
            } else {
                tvUnreadBadge.visibility = View.GONE
            }

            if (chatRoom.lastMessageSenderId == currentUserId) {
                imgMessageStatus.visibility = View.VISIBLE
                imgMessageStatus.setImageResource(R.drawable.ic_check_double)
            } else {
                imgMessageStatus.visibility = View.GONE
            }

            itemView.setOnClickListener { onChatClick(chatRoom) }
        }

        private fun formatTime(date: Date): String {
            val now = Calendar.getInstance()
            val messageTime = Calendar.getInstance().apply { time = date }

            return when {
                now.get(Calendar.DAY_OF_YEAR) == messageTime.get(Calendar.DAY_OF_YEAR) &&
                        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
                    SimpleDateFormat("h:mm a", Locale.getDefault()).format(date)
                }
                now.get(Calendar.DAY_OF_YEAR) - messageTime.get(Calendar.DAY_OF_YEAR) == 1 &&
                        now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
                    "Yesterday"
                }
                now.get(Calendar.YEAR) == messageTime.get(Calendar.YEAR) -> {
                    SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
                }
                else -> {
                    SimpleDateFormat("MM/dd/yy", Locale.getDefault()).format(date)
                }
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChatDiffCallback : DiffUtil.ItemCallback<ChatRoom>() {
        override fun areItemsTheSame(oldItem: ChatRoom, newItem: ChatRoom) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: ChatRoom, newItem: ChatRoom) =
            oldItem == newItem
    }
}