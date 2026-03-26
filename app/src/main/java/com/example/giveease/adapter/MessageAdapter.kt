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
import com.example.giveease.model.Message
import com.example.giveease.model.MessageType
import com.example.giveease.model.MessageStatus
import com.google.android.material.imageview.ShapeableImageView
import java.text.SimpleDateFormat
import java.util.*

class MessageAdapter(
    private val currentUserId: String,
    private val onImageClick: (String) -> Unit
) : ListAdapter<Message, RecyclerView.ViewHolder>(MessageDiffCallback()) {

    companion object {
        private const val VIEW_TYPE_SENT = 1
        private const val VIEW_TYPE_RECEIVED = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).senderId == currentUserId) {
            VIEW_TYPE_SENT
        } else {
            VIEW_TYPE_RECEIVED
        }
    }

    inner class SentMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val imgMessage: ShapeableImageView = view.findViewById(R.id.imgMessage)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)
        private val imgStatus: ImageView = view.findViewById(R.id.imgStatus)

        fun bind(message: Message) {
            if (message.type == MessageType.IMAGE && message.imageUrl.isNotEmpty()) {
                imgMessage.visibility = View.VISIBLE
                tvMessage.visibility = if (message.message.isEmpty()) View.GONE else View.VISIBLE

                Glide.with(itemView.context)
                    .load(message.imageUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .into(imgMessage)

                imgMessage.setOnClickListener { onImageClick(message.imageUrl) }
            } else {
                imgMessage.visibility = View.GONE
                tvMessage.visibility = View.VISIBLE
            }

            if (message.message.isNotEmpty()) {
                tvMessage.text = message.message
            }

            message.timestamp?.let {
                tvTime.text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(it.toDate())
            }

            when (message.status) {
                MessageStatus.SENDING -> {
                    imgStatus.setImageResource(R.drawable.ic_access_time)
                    imgStatus.setColorFilter(android.graphics.Color.parseColor("#777777"))
                }
                MessageStatus.SENT -> {
                    imgStatus.setImageResource(R.drawable.ic_check)
                    imgStatus.setColorFilter(android.graphics.Color.parseColor("#777777"))
                }
                MessageStatus.DELIVERED -> {
                    imgStatus.setImageResource(R.drawable.ic_check_double)
                    imgStatus.setColorFilter(android.graphics.Color.parseColor("#777777"))
                }
                MessageStatus.READ -> {
                    imgStatus.setImageResource(R.drawable.ic_check_double)
                    imgStatus.setColorFilter(itemView.context.getColor(R.color.read_receipt_blue))
                }
            }
        }
    }

    inner class ReceivedMessageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val tvMessage: TextView = view.findViewById(R.id.tvMessage)
        private val imgMessage: ShapeableImageView = view.findViewById(R.id.imgMessage)
        private val tvTime: TextView = view.findViewById(R.id.tvTime)

        fun bind(message: Message) {
            if (message.type == MessageType.IMAGE && message.imageUrl.isNotEmpty()) {
                imgMessage.visibility = View.VISIBLE
                tvMessage.visibility = if (message.message.isEmpty()) View.GONE else View.VISIBLE

                Glide.with(itemView.context)
                    .load(message.imageUrl)
                    .placeholder(R.drawable.image_placeholder)
                    .into(imgMessage)

                imgMessage.setOnClickListener { onImageClick(message.imageUrl) }
            } else {
                imgMessage.visibility = View.GONE
                tvMessage.visibility = View.VISIBLE
            }

            if (message.message.isNotEmpty()) {
                tvMessage.text = message.message
            }

            message.timestamp?.let {
                tvTime.text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(it.toDate())
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_SENT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_sent, parent, false)
                SentMessageViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_message_received, parent, false)
                ReceivedMessageViewHolder(view)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is SentMessageViewHolder -> holder.bind(getItem(position))
            is ReceivedMessageViewHolder -> holder.bind(getItem(position))
        }
    }

    class MessageDiffCallback : DiffUtil.ItemCallback<Message>() {
        override fun areItemsTheSame(oldItem: Message, newItem: Message) =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: Message, newItem: Message) =
            oldItem == newItem
    }
}