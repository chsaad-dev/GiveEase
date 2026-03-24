package com.example.giveease.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class Message(
    @get:Exclude @set:Exclude var id: String = "",
    @get:Exclude @set:Exclude var chatRoomId: String = "",
    @get:PropertyName("senderId") @set:PropertyName("senderId") var senderId: String = "",
    @get:PropertyName("receiverId") @set:PropertyName("receiverId") var receiverId: String = "",
    @get:PropertyName("senderName") @set:PropertyName("senderName") var senderName: String = "",
    @get:PropertyName("content") @set:PropertyName("content") var message: String = "",
    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl") var imageUrl: String = "",
    @get:PropertyName("messageType") @set:PropertyName("messageType") var type: MessageType = MessageType.TEXT,
    @get:Exclude @set:Exclude var status: MessageStatus = MessageStatus.SENT,
    @get:PropertyName("timestamp") @set:PropertyName("timestamp") @ServerTimestamp var timestamp: Timestamp? = null,
    @get:PropertyName("isRead") @set:PropertyName("isRead") var isRead: Boolean = false,
    @get:PropertyName("isDelivered") @set:PropertyName("isDelivered") var isDelivered: Boolean = false
)

enum class MessageType {
    TEXT, IMAGE
}

enum class MessageStatus {
    SENDING, SENT, DELIVERED, READ
}