package com.example.giveease.models

data class Notification(
    var id: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0L,
    var isRead: Boolean = false,
    val type: String = "general",
    val referenceId: String? = null
)
