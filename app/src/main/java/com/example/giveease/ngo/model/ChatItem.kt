package com.example.giveease.ngo.model

data class ChatItem(
    val id: String,
    val donorId: String,
    val donorName: String,
    val donorProfileUrl: String?,
    val lastMessage: String,
    val timestamp: String,
    val campaignName: String,
    val unreadCount: Int,
    val isOnline: Boolean
)
