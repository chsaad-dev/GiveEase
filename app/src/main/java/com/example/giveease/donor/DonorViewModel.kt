package com.example.giveease.donor

import androidx.lifecycle.*
import com.example.giveease.donor.model.Campaign
import com.example.giveease.donor.model.Chat
import com.example.giveease.donor.model.UserProfile
import com.example.giveease.repository.DonorRepository
import kotlinx.coroutines.launch

class DonorViewModel : ViewModel() {

    private val repository = DonorRepository()

    private val _campaigns = MutableLiveData<List<Campaign>>()
    val campaigns: LiveData<List<Campaign>> get() = _campaigns

    private val _profile = MutableLiveData<UserProfile>()
    val profile: LiveData<UserProfile> get() = _profile

    private val _chats = MutableLiveData<List<Chat>>()
    val chats: LiveData<List<Chat>> get() = _chats

    fun fetchCampaigns() {
        viewModelScope.launch {
            _campaigns.value = repository.getCampaigns()
        }
    }

    fun fetchProfile() {
        viewModelScope.launch {
            _profile.value = repository.getUserProfile()
        }
    }

    fun fetchChats() {
        viewModelScope.launch {
            _chats.value = repository.getChats()
        }
    }
}
