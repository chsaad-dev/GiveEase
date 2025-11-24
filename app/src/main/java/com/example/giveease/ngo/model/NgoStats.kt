package com.example.giveease.ngo.model

data class NgoStats(
    val activeCampaigns: Int,
    val totalDonationsThisMonth: Double,
    val totalDonors: Int,
    val completedCampaigns: Int,
    val totalRaised: Double
)

data class NgoProfile(
    val id: String,
    val name: String,
    val description: String,
    val foundedYear: Int,
    val rating: Float,
    val isVerified: Boolean,
    val logoUrl: String?,
    val coverImageUrl: String?,
    val stats: NgoStats
)
