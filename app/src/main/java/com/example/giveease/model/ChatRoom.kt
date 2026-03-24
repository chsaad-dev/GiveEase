package com.example.giveease.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import com.google.firebase.firestore.ServerTimestamp

data class ChatRoom(
    @get:Exclude @set:Exclude var id: String = "",
    @get:PropertyName("donorId") @set:PropertyName("donorId") var donorId: String = "",
    @get:PropertyName("donorName") @set:PropertyName("donorName") var donorName: String = "",
    @get:PropertyName("donorImage") @set:PropertyName("donorImage") var donorImage: String = "",
    @get:PropertyName("ngoId") @set:PropertyName("ngoId") var ngoId: String = "",
    @get:PropertyName("ngoName") @set:PropertyName("ngoName") var ngoName: String = "",
    @get:PropertyName("ngoImage") @set:PropertyName("ngoImage") var ngoImage: String = "",
    @get:PropertyName("campaignId") @set:PropertyName("campaignId") var campaignId: String = "",
    @get:PropertyName("campaignName") @set:PropertyName("campaignName") var campaignName: String = "",
    @get:PropertyName("lastMessage") @set:PropertyName("lastMessage") var lastMessage: String = "",
    @get:PropertyName("lastMessageSenderId") @set:PropertyName("lastMessageSenderId") var lastMessageSenderId: String = "",
    @get:PropertyName("lastMessageTime") @set:PropertyName("lastMessageTime") var lastMessageTime: Timestamp? = null,
    @get:PropertyName("donorUnread") @set:PropertyName("donorUnread") var unreadCountDonor: Int = 0,
    @get:PropertyName("ngoUnread") @set:PropertyName("ngoUnread") var unreadCountNgo: Int = 0,
    @get:PropertyName("donorTyping") @set:PropertyName("donorTyping") var donorTyping: Boolean = false,
    @get:PropertyName("ngoTyping") @set:PropertyName("ngoTyping") var ngoTyping: Boolean = false,
    @get:PropertyName("donorOnline") @set:PropertyName("donorOnline") var donorOnline: Boolean = false,
    @get:PropertyName("ngoOnline") @set:PropertyName("ngoOnline") var ngoOnline: Boolean = false,
    @get:PropertyName("donorLastSeen") @set:PropertyName("donorLastSeen") var donorLastSeen: Timestamp? = null,
    @get:PropertyName("ngoLastSeen") @set:PropertyName("ngoLastSeen") var ngoLastSeen: Timestamp? = null,
    @get:PropertyName("createdAt") @set:PropertyName("createdAt") @ServerTimestamp var createdAt: Timestamp? = null,
    @get:PropertyName("participants") @set:PropertyName("participants") var participants: Map<String, Boolean> = emptyMap()
)