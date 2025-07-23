package com.example.giveease.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

object RoleManager {

    suspend fun getCurrentUserRole(): String? {
        val auth = FirebaseAuth.getInstance()
        val db = FirebaseFirestore.getInstance()

        val currentUser = auth.currentUser ?: return null
        val uid = currentUser.uid

        if (uid.isBlank()) return null // Extra safety

        val snapshot = db.collection("users").document(uid).get().await()
        return snapshot.getString("role")
    }
}
