package com.example.giveease.utils

import com.google.firebase.firestore.FirebaseFirestore
import java.util.HashMap

object NotificationHelper {

    fun sendNotification(
        userId: String,
        title: String,
        message: String,
        type: String = "general",
        referenceId: String? = null
    ) {
        if (userId.isEmpty()) return
        
        val db = FirebaseFirestore.getInstance()
        val notificationData = HashMap<String, Any>()
        notificationData["title"] = title
        notificationData["message"] = message
        notificationData["timestamp"] = System.currentTimeMillis()
        notificationData["isRead"] = false
        notificationData["type"] = type
        if (referenceId != null) {
            notificationData["referenceId"] = referenceId
        }

        db.collection("users").document(userId)
            .collection("notifications")
            .add(notificationData)
            .addOnFailureListener {
                // Log failure silently as notifications are supplementary
            }
    }
}
