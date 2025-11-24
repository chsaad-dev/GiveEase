package com.example.giveease.ngo

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.databinding.NgoItemChatBinding
import android.view.View
import com.example.giveease.ngo.model.ChatItem

class NgoChatAdapter(
    private val onChatClick: (ChatItem) -> Unit
) : ListAdapter<ChatItem, NgoChatAdapter.ChatViewHolder>(ChatDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val binding = NgoItemChatBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChatViewHolder(binding, onChatClick)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ChatViewHolder(
        private val binding: NgoItemChatBinding,
        private val onChatClick: (ChatItem) -> Unit
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(chatItem: ChatItem) {
            binding.apply {
                tvDonorName.text = chatItem.donorName
                tvLastMessage.text = chatItem.lastMessage
                tvTime.text = chatItem.timestamp
                tvCampaignName.text = chatItem.campaignName

                // Handle unread count
                if (chatItem.unreadCount > 0) {
                    tvUnreadCount.visibility = View.VISIBLE
                    tvUnreadCount.text = chatItem.unreadCount.toString()
                } else {
                    tvUnreadCount.visibility = View.GONE
                }

                root.setOnClickListener {
                    onChatClick(chatItem)
                }
            }
        }
    }

    private class ChatDiffCallback : DiffUtil.ItemCallback<ChatItem>() {
        override fun areItemsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ChatItem, newItem: ChatItem): Boolean {
            return oldItem == newItem
        }
    }
}
