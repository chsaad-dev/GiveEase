package com.example.giveease.ngo

data class CampaignData(
    val id: String = "",
    val ngoId: String = "",
    val ngoName: String = "",
    val category: String = "",
    val title: String = "",
    val description: String = "",
    val targetQuantity: Int = 0,
    val currentQuantity: Int = 0,
    val unit: String = "",
    val endDate: Long = 0,
    val urgencyLevel: String = "",
    val itemCondition: String? = null,
    val specificRequirements: String = "",
    val autoClose: Boolean = false,
    val imageUrls: List<String> = emptyList(),
    val createdAt: Long = 0,
    val status: String = "Active",
    val donorCount: Int = 0,
    val shareCount: Int = 0
) {
    fun getProgress(): Int {
        return if (targetQuantity > 0) {
            ((currentQuantity.toFloat() / targetQuantity) * 100).toInt()
        } else 0
    }

    fun getDaysLeft(): Int {
        val diff = endDate - System.currentTimeMillis()
        return (diff / (1000 * 60 * 60 * 24)).toInt()
    }

    fun isExpired(): Boolean {
        return System.currentTimeMillis() > endDate
    }
}
