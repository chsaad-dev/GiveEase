package com.example.giveease.donor

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.example.giveease.ngo.CampaignData
import com.google.firebase.firestore.FirebaseFirestore

class DonorFeedViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()

    private val _campaigns = MutableLiveData<List<CampaignData>>()
    val campaigns: LiveData<List<CampaignData>> = _campaigns

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private var allCampaigns = listOf<CampaignData>()

    fun loadCampaigns(isNetworkAvailable: Boolean) {
        if (!isNetworkAvailable) {
            _error.value = "No internet connection. Showing offline data if available."
        }
        
        _isLoading.value = true

        firestore.collection("campaigns")
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { documents ->
                val currentTime = System.currentTimeMillis()
                allCampaigns = documents.mapNotNull { doc ->
                    try {
                        val endDate = doc.getLong("endDate") ?: 0L
                        val currentQuantity = doc.getLong("currentQuantity")?.toInt() ?: 0
                        val targetQuantity = doc.getLong("targetQuantity")?.toInt() ?: 0
                        val autoClose = doc.getBoolean("autoClose") ?: false

                        // Filter out expired campaigns
                        if (endDate > 0 && endDate < currentTime) {
                            return@mapNotNull null
                        }
                        
                        // Filter out auto-closed completed campaigns
                        if (autoClose && targetQuantity > 0 && currentQuantity >= targetQuantity) {
                            return@mapNotNull null
                        }
                        
                        CampaignData(
                            id = doc.id,
                            ngoId = doc.getString("ngoId") ?: "",
                            ngoName = doc.getString("ngoName") ?: "",
                            category = doc.getString("category") ?: "",
                            title = doc.getString("title") ?: "",
                            description = doc.getString("description") ?: "",
                            targetQuantity = targetQuantity,
                            currentQuantity = currentQuantity,
                            unit = doc.getString("unit") ?: "",
                            endDate = endDate,
                            urgencyLevel = doc.getString("urgencyLevel") ?: "",
                            itemCondition = doc.getString("itemCondition") ?: "",
                            specificRequirements = doc.getString("specificRequirements") ?: "",
                            autoClose = autoClose,
                            imageUrls = (doc.get("imageUrls") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                            createdAt = doc.getLong("createdAt") ?: 0,
                            status = doc.getString("status") ?: "Active",
                            donorCount = doc.getLong("donorCount")?.toInt() ?: 0,
                            shareCount = doc.getLong("shareCount")?.toInt() ?: 0
                        )
                    } catch (e: Exception) {
                        null
                    }
                }
                
                _campaigns.value = allCampaigns
                _isLoading.value = false
            }
            .addOnFailureListener { e ->
                _isLoading.value = false
                _error.value = "Error loading campaigns: ${e.localizedMessage}"
            }
    }

    fun filterCampaigns(category: String) {
        if (category == "All") {
            _campaigns.value = allCampaigns
        } else {
            _campaigns.value = allCampaigns.filter { it.category == category }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
