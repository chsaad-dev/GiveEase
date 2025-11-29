package com.example.giveease.utils

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

object MaintenanceManager {

    private const val TAG = "MaintenanceManager"
    private var maintenanceListener: ListenerRegistration? = null
    private var isMaintenanceMode = false
    private val listeners = mutableListOf<(Boolean) -> Unit>()

    fun startListening() {
        val firestore = FirebaseFirestore.getInstance()

        Log.d(TAG, "Starting to listen for maintenance mode changes")

        maintenanceListener = firestore.collection("settings")
            .document("app_config")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error listening to maintenance mode: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    val maintenanceMode = snapshot.getBoolean("maintenanceMode") ?: false
                    Log.d(TAG, "Maintenance mode value from Firestore: $maintenanceMode")

                    if (isMaintenanceMode != maintenanceMode) {
                        isMaintenanceMode = maintenanceMode
                        Log.d(TAG, "Maintenance mode changed to: $maintenanceMode, notifying ${listeners.size} listeners")
                        notifyListeners(maintenanceMode)
                    }
                } else {
                    Log.d(TAG, "Snapshot is null or doesn't exist")
                }
            }
    }

    fun stopListening() {
        Log.d(TAG, "Stopping maintenance mode listener")
        maintenanceListener?.remove()
        maintenanceListener = null
        listeners.clear()
    }

    fun isMaintenanceActive(): Boolean {
        return isMaintenanceMode
    }

    fun addListener(listener: (Boolean) -> Unit) {
        Log.d(TAG, "Adding listener, total listeners: ${listeners.size + 1}")
        listeners.add(listener)
    }

    fun removeListener(listener: (Boolean) -> Unit) {
        Log.d(TAG, "Removing listener")
        listeners.remove(listener)
    }

    private fun notifyListeners(isActive: Boolean) {
        Log.d(TAG, "Notifying listeners: maintenance is $isActive")
        listeners.forEach {
            Log.d(TAG, "Calling listener callback")
            it(isActive)
        }
    }

    fun checkMaintenanceStatus(callback: (Boolean) -> Unit) {
        val firestore = FirebaseFirestore.getInstance()

        Log.d(TAG, "Checking maintenance status once")

        firestore.collection("settings")
            .document("app_config")
            .get()
            .addOnSuccessListener { snapshot ->
                val maintenanceMode = snapshot?.getBoolean("maintenanceMode") ?: false
                Log.d(TAG, "One-time check result: maintenanceMode = $maintenanceMode")
                callback(maintenanceMode)
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error checking maintenance status: ${e.message}")
                callback(false)
            }
    }
}