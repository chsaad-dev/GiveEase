package com.example.giveease.repository

import com.example.giveease.donor.model.Campaign
import com.example.giveease.donor.model.Chat
import com.example.giveease.donor.model.UserProfile
import com.example.giveease.R
import kotlinx.coroutines.delay

class DonorRepository {

    suspend fun getCampaigns(): List<Campaign> {
        delay(500)
        return listOf(
            Campaign("Saylani Food Drive", "Help feed 1000+ people", R.drawable.sample_compaign1, 75),
            Campaign("Flood Relief", "Support flood victims", R.drawable.sample_compaign2, 40)
        )
    }

    suspend fun getUserProfile(): UserProfile {
        delay(300)
        return UserProfile("Muhammad Saad", "saad@example.com", "Donor", R.drawable.sample_profile)
    }

    suspend fun getChats(): List<Chat> {
        delay(400)
        return listOf(
            Chat("Edhi Foundation", "Thanks for donating!", R.drawable.sample_ngo, 1),
            Chat("Saylani Welfare", "Received your donation", R.drawable.sample_ngo, 0)
        )
    }
}
