package com.example.giveease.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FirebaseFirestore

data class AdminProfileData(
    val name: String = "",
    val email: String = "",
    val phone: String = "",
    val profileImageUrl: String? = null
)

class AdminProfileViewModel : ViewModel() {
    private val firestore = FirebaseFirestore.getInstance()

    private val _profileData = MutableLiveData<AdminProfileData>()
    val profileData: LiveData<AdminProfileData> = _profileData

    var isDataLoaded = false

    fun loadProfileData(uid: String?, emailFallback: String?) {
        if (isDataLoaded || uid == null) return
        
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: ""
                    val email = document.getString("email") ?: emailFallback ?: ""
                    val phone = document.getString("phone") ?: ""
                    val profileImageUrl = document.getString("profileImageUrl")
                    
                    _profileData.value = AdminProfileData(name, email, phone, profileImageUrl)
                    isDataLoaded = true
                }
            }
            .addOnFailureListener {
                val email = emailFallback ?: ""
                _profileData.value = AdminProfileData(email = email)
            }
    }
    
    fun updateLocalData(newData: AdminProfileData) {
        _profileData.value = newData
    }
}
