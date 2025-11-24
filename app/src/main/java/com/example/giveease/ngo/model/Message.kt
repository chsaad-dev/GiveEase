package com.example.giveease.ngo.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

enum class MessageType {
    TEXT, IMAGE, FILE
}

data class Message(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val content: String,
    val timestamp: Long,
    val isSent: Boolean,
    val isDelivered: Boolean,
    val isRead: Boolean,
    val messageType: MessageType,
    val imageUrl: String? = null,
    val fileUrl: String? = null
) {
    fun getFormattedTime(): String {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = timestamp

        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        return sdf.format(calendar.time)
    }

    fun isFromNgo(): Boolean {
        return senderId.startsWith("ngo_")
    }
}
