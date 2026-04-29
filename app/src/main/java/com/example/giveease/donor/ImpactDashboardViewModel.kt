package com.example.giveease.donor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import java.util.*

class ImpactDashboardViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _donorProfile = MutableLiveData<DonorProfile>()
    val donorProfile: LiveData<DonorProfile> = _donorProfile

    private val _impactData = MutableLiveData<ImpactData>()
    val impactData: LiveData<ImpactData> = _impactData

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    fun loadData(isNetworkAvailable: Boolean) {
        if (!isNetworkAvailable) {
            _error.value = "No internet connection. Data may be out of date."
        }
        
        _isLoading.value = true
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val name = doc.getString("name") ?: "Donor"
                val verified = doc.getString("verificationStatus") == "verified"
                val role = doc.getString("role") ?: ""
                _donorProfile.value = DonorProfile(name, verified, role)
                loadImpactData(userId)
            }
            .addOnFailureListener { e ->
                _error.value = "Error loading profile: ${e.localizedMessage}"
                _isLoading.value = false
            }
    }

    private fun loadImpactData(userId: String) {
        firestore.collection("donations")
            .whereEqualTo("donorId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val donations = documents.documents
                
                val totalDonations = donations.size
                val totalItems = donations.sumOf { it.getLong("quantity") ?: 0L }.toInt()
                val uniqueNGOs = donations.mapNotNull { it.getString("ngoId") }.distinct().size
                
                val impactScore = totalItems * 20
                val peopleImpacted = totalItems * 3

                val now = System.currentTimeMillis()
                val cal = Calendar.getInstance()

                val startOfMonth = cal.apply {
                    timeInMillis = now
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val startOfYear = cal.apply {
                    timeInMillis = now
                    set(Calendar.MONTH, 0); set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                }.timeInMillis

                val monthlyCount = donations.count { (it.getLong("timestamp") ?: 0L) >= startOfMonth }
                val yearlyCount = donations.count { (it.getLong("timestamp") ?: 0L) >= startOfYear }
                val monthlyItems = donations.filter { (it.getLong("timestamp") ?: 0L) >= startOfMonth }
                    .sumOf { it.getLong("quantity") ?: 0L }.toInt()

                val firstDonationTs = donations.minByOrNull { it.getLong("timestamp") ?: Long.MAX_VALUE }?.getLong("timestamp")

                _impactData.value = ImpactData(
                    totalDonations = totalDonations,
                    totalItems = totalItems,
                    uniqueNGOs = uniqueNGOs,
                    impactScore = impactScore,
                    peopleImpacted = peopleImpacted,
                    monthlyCount = monthlyCount,
                    yearlyCount = yearlyCount,
                    monthlyItems = monthlyItems,
                    firstDonationTimestamp = firstDonationTs,
                    donations = donations
                )
                
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _error.value = "Error loading impact data: ${e.localizedMessage}"
                _isLoading.value = false
            }
    }

    fun clearError() {
        _error.value = null
    }

    data class DonorProfile(
        val name: String,
        val isVerified: Boolean,
        val role: String
    )

    data class ImpactData(
        val totalDonations: Int,
        val totalItems: Int,
        val uniqueNGOs: Int,
        val impactScore: Int,
        val peopleImpacted: Int,
        val monthlyCount: Int,
        val yearlyCount: Int,
        val monthlyItems: Int,
        val firstDonationTimestamp: Long?,
        val donations: List<DocumentSnapshot>
    )
}
