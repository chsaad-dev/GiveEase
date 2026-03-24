package com.example.giveease.utils

import android.content.Context
import android.content.SharedPreferences

object UserManager {
    private const val PREFS_NAME = "GiveEasePrefs"
    private const val KEY_USER_ID = "userId"
    private const val KEY_USER_TYPE = "userType"
    private const val KEY_USER_NAME = "userName"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun getUserId(context: Context): String {
        val authId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
        if (!authId.isNullOrBlank()) return authId
        return getPrefs(context).getString(KEY_USER_ID, "donor_123") ?: "donor_123"
    }

    fun getUserType(context: Context): String {
        return getPrefs(context).getString(KEY_USER_TYPE, "donor") ?: "donor"
    }

    fun getUserName(context: Context): String {
        val authName = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.displayName
        if (!authName.isNullOrBlank()) return authName
        return getPrefs(context).getString(KEY_USER_NAME, "User") ?: "User"
    }

    fun isDonor(context: Context): Boolean {
        return getUserType(context) == "donor"
    }

    fun saveUser(context: Context, userId: String, userType: String, userName: String) {
        getPrefs(context).edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_TYPE, userType)
            putString(KEY_USER_NAME, userName)
            apply()
        }
    }
}
