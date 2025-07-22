package com.example.giveease.utils

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object RoleManager {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    fun getCurrentUserRole(): String? {
        val uid = auth.currentUser?.uid ?: return null
        var role: String? = null

        val task = db.collection("users").document(uid).get()
        val result = com.google.android.gms.tasks.Tasks.await(task)
        role = result.getString("role")

        return role
    }
}
