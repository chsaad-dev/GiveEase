package com.example.giveease.admin

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object AdminLogger {

    fun logAction(actionType: String, actionTitle: String, actionDetail: String) {
        val auth = FirebaseAuth.getInstance()
        val firestore = FirebaseFirestore.getInstance()
        val adminId = auth.currentUser?.uid ?: return

        val logEntry = hashMapOf(
            "actionType" to actionType,
            "actionTitle" to actionTitle,
            "actionDetail" to actionDetail,
            "timestamp" to System.currentTimeMillis(),
            "adminId" to adminId
        )

        firestore.collection("admin_logs").add(logEntry)
    }
}
