package com.example.giveease.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.MutableLiveData

class AuthViewModel : ViewModel() {
    val email = MutableLiveData<String>()
    val password = MutableLiveData<String>()
    val name = MutableLiveData<String>()
}
