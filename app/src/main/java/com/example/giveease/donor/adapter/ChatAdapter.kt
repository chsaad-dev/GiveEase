package com.example.giveease.donor.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.R
import com.example.giveease.donor.model.Chat

class ChatAdapter(private val chatList: List<Chat>) :
    RecyclerView.Adapter<ChatAdapter.ChatViewHolder>() {

    inner class ChatViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ngoName: TextView = view.findViewById(R.id.tvNgoName)
        val lastMessage: TextView = view.findViewById(R.id.tvLastMessage)
        val imgNgo: ImageView = view.findViewById(R.id.imgNgo)
        val tvUnread: TextView = view.findViewById(R.id.tvUnread)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChatViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return ChatViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChatViewHolder, position: Int) {
        val chat = chatList[position]
        holder.ngoName.text = chat.ngoName
        holder.lastMessage.text = chat.lastMessage
        holder.imgNgo.setImageResource(chat.imageRes)

        if (chat.unreadCount > 0) {
            holder.tvUnread.visibility = View.VISIBLE
            holder.tvUnread.text = chat.unreadCount.toString()
        } else {
            holder.tvUnread.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Opening chat with ${chat.ngoName}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = chatList.size
}
