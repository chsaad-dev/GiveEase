package com.example.giveease.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

class AdminDashboardViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _totalNgos = MutableLiveData<String>()
    val totalNgos: LiveData<String> = _totalNgos

    private val _totalDonors = MutableLiveData<String>()
    val totalDonors: LiveData<String> = _totalDonors

    private val _pendingApprovals = MutableLiveData<String>()
    val pendingApprovals: LiveData<String> = _pendingApprovals
    
    // Also store Int for logic like badge visibility
    private val _pendingApprovalsCount = MutableLiveData<Int>()
    val pendingApprovalsCount: LiveData<Int> = _pendingApprovalsCount

    private val _activeCampaigns = MutableLiveData<String>()
    val activeCampaigns: LiveData<String> = _activeCampaigns

    private val _recentActivities = MutableLiveData<List<AdminActivity>>()
    val recentActivities: LiveData<List<AdminActivity>> = _recentActivities

    private val _unreadNotifications = MutableLiveData<Int>()
    val unreadNotifications: LiveData<Int> = _unreadNotifications

    private var notificationListener: ListenerRegistration? = null

    private val _adminName = MutableLiveData<String>()
    val adminName: LiveData<String> = _adminName

    var isDataLoaded = false

    fun loadData(uid: String?) {
        if (isDataLoaded) return
        
        if (uid != null) {
            loadAdminName(uid)
        }
        
        loadTotalNGOs()
        loadTotalDonors()
        loadPendingApprovals()
        loadActiveCampaigns()
        loadRecentActivity()
        listenForUnreadNotifications()
        
        isDataLoaded = true
    }

    private fun loadAdminName(uid: String) {
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val name = document.getString("name") ?: "Admin"
                _adminName.value = "Welcome, $name"
            }
            .addOnFailureListener {
                _adminName.value = "Welcome, Admin"
            }
    }

    private fun loadTotalNGOs() {
        firestore.collection("users")
            .whereEqualTo("role", "ngo")
            .get()
            .addOnSuccessListener { documents ->
                _totalNgos.value = documents.size().toString()
            }
            .addOnFailureListener {
                _totalNgos.value = "0"
            }
    }

    private fun loadTotalDonors() {
        firestore.collection("users")
            .whereEqualTo("role", "donor")
            .get()
            .addOnSuccessListener { documents ->
                _totalDonors.value = documents.size().toString()
            }
            .addOnFailureListener {
                _totalDonors.value = "0"
            }
    }

    private fun loadPendingApprovals() {
        firestore.collection("users")
            .whereEqualTo("verificationStatus", "pending")
            .get()
            .addOnSuccessListener { documents ->
                val count = documents.size()
                _pendingApprovals.value = count.toString()
                _pendingApprovalsCount.value = count
            }
            .addOnFailureListener {
                _pendingApprovals.value = "0"
                _pendingApprovalsCount.value = 0
            }
    }

    private fun loadActiveCampaigns() {
        firestore.collection("campaigns")
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { documents ->
                _activeCampaigns.value = documents.size().toString()
            }
            .addOnFailureListener {
                _activeCampaigns.value = "0"
            }
    }

    private fun listenForUnreadNotifications() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        notificationListener?.remove()
        notificationListener = firestore.collection("users")
            .document(uid).collection("notifications")
            .whereEqualTo("isRead", false)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener
                _unreadNotifications.value = snapshots?.size() ?: 0
            }
    }

    fun loadRecentActivity() {
        val activities = mutableListOf<AdminActivity>()
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        var queriesCompleted = 0
        val totalQueries = 3

        fun checkAndUpdate() {
            queriesCompleted++
            if (queriesCompleted >= totalQueries) {
                activities.sortByDescending { it.timestamp }
                _recentActivities.value = activities.take(10)
            }
        }

        // 1. Recent verifications
        firestore.collection("users")
            .whereIn("verificationStatus", listOf("verified", "rejected"))
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val updatedAt = doc.getLong("updatedAt") ?: doc.getLong("verifiedAt") ?: 0
                    if (updatedAt < sevenDaysAgo) continue

                    val status = doc.getString("verificationStatus") ?: ""
                    val name = doc.getString("name") ?: "Unknown"
                    val role = if (doc.getString("role") == "ngo") "NGO" else "Donor"

                    if (status == "verified") {
                        activities.add(AdminActivity(
                            type = "verification",
                            title = "$role Approved",
                            subtitle = "$name was verified",
                            timestamp = updatedAt
                        ))
                    } else if (status == "rejected") {
                        activities.add(AdminActivity(
                            type = "rejection",
                            title = "$role Rejected",
                            subtitle = "$name was rejected",
                            timestamp = updatedAt
                        ))
                    }
                }
                checkAndUpdate()
            }
            .addOnFailureListener { checkAndUpdate() }

        // 2. Recent donations
        firestore.collection("donations")
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val timestamp = doc.getLong("timestamp") ?: 0
                    if (timestamp < sevenDaysAgo) continue

                    val ngoName = doc.getString("ngoName") ?: "Unknown NGO"
                    val quantity = doc.getLong("quantity") ?: 0

                    activities.add(AdminActivity(
                        type = "donation",
                        title = "New Donation",
                        subtitle = "$quantity items donated to $ngoName",
                        timestamp = timestamp
                    ))
                }
                checkAndUpdate()
            }
            .addOnFailureListener { checkAndUpdate() }

        // 3. Recent campaigns
        firestore.collection("campaigns")
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val createdAt = doc.getLong("createdAt") ?: 0
                    if (createdAt < sevenDaysAgo) continue

                    val title = doc.getString("title") ?: "Unknown"
                    val ngoName = doc.getString("ngoName") ?: "Unknown"

                    activities.add(AdminActivity(
                        type = "campaign",
                        title = "Campaign Created",
                        subtitle = "\"$title\" by $ngoName",
                        timestamp = createdAt
                    ))
                }
                checkAndUpdate()
            }
            .addOnFailureListener { checkAndUpdate() }
    }
    
    // Call this if an admin action forces a UI data refresh (e.g. they approved someone)
    fun refreshData() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid
        isDataLoaded = false
        loadData(uid)
    }

    override fun onCleared() {
        super.onCleared()
        notificationListener?.remove()
    }
}
