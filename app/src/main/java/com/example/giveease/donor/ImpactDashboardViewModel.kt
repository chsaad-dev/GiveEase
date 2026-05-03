package com.example.giveease.donor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
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

        viewModelScope.launch {
            try {
                val doc = firestore.collection("users").document(userId).get().await()
                val name = doc.getString("name") ?: "Donor"
                val verified = doc.getString("verificationStatus") == "verified"
                val role = doc.getString("role") ?: ""
                _donorProfile.value = DonorProfile(name, verified, role)
                
                loadImpactData(userId)
            } catch (e: Exception) {
                _error.value = "Error loading profile: ${e.localizedMessage}"
                _isLoading.value = false
            }
        }
    }

    private suspend fun loadImpactData(userId: String) {
        try {
            val documents = firestore.collection("donations")
                .whereEqualTo("donorId", userId)
                .get()
                .await()
                
            val donations = documents.documents
            
            var totalDonations = 0
            var totalItems = 0
            val uniqueNGOsSet = mutableSetOf<String>()
            var calculatedImpactScore = 0
            
            var monetaryAmount = 0
            var bloodQuantity = 0
            var medicineQuantity = 0
            var physicalItemsQuantity = 0
            
            for (donation in donations) {
                totalDonations++
                val quantity = donation.getLong("quantity")?.toInt() ?: 0
                totalItems += quantity
                
                donation.getString("ngoId")?.let { uniqueNGOsSet.add(it) }
                
                val status = donation.getString("status") ?: ""
                val isCompleted = status.equals("Completed", ignoreCase = true) || 
                                  status.equals("Delivered", ignoreCase = true)
                
                val campaignId = donation.getString("campaignId")
                var category = "Other"
                val unit = donation.getString("unit") ?: ""
                
                if (campaignId != null) {
                    try {
                        val campaignDoc = firestore.collection("campaigns").document(campaignId).get().await()
                        category = campaignDoc.getString("category") ?: "Other"
                    } catch (e: Exception) {
                        // ignore
                    }
                }
                
                val isMonetary = category == "Monetary Funds" || unit.equals("PKR", ignoreCase = true)
                val isBlood = category == "Blood Donation"
                val isMedicine = category == "Medical & Healthcare" || category == "Medicine"
                
                if (isMonetary) {
                    monetaryAmount += quantity
                } else if (isBlood) {
                    bloodQuantity += quantity
                } else if (isMedicine) {
                    medicineQuantity += quantity
                } else {
                    physicalItemsQuantity += quantity
                }
                
                if (isCompleted) {
                    when {
                        isMonetary -> calculatedImpactScore += quantity / 100
                        isBlood -> calculatedImpactScore += quantity * 25
                        else -> calculatedImpactScore += quantity * 5
                    }
                }
            }
            
            val uniqueNGOs = uniqueNGOsSet.size
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
                monetaryAmount = monetaryAmount,
                bloodQuantity = bloodQuantity,
                medicineQuantity = medicineQuantity,
                physicalItemsQuantity = physicalItemsQuantity,
                uniqueNGOs = uniqueNGOs,
                impactScore = calculatedImpactScore,
                peopleImpacted = peopleImpacted,
                monthlyCount = monthlyCount,
                yearlyCount = yearlyCount,
                monthlyItems = monthlyItems,
                firstDonationTimestamp = firstDonationTs,
                donations = donations
            )
            
            _isLoading.value = false
        } catch (e: Exception) {
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
        val monetaryAmount: Int,
        val bloodQuantity: Int,
        val medicineQuantity: Int,
        val physicalItemsQuantity: Int,
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
