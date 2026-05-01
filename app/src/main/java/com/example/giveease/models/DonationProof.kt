package com.example.giveease.models

data class DonationProof(
    val beneficiaryName: String = "",
    val contactNumber: String = "",
    val cnicOrAdditionalInfo: String = "",
    val addressProofImageUrl: String = "",
    val handoverImageUrl: String = "",
    val uploadedAt: Long = 0L
)
