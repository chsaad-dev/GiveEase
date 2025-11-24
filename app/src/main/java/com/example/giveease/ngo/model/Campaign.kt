package com.example.giveease.ngo.model

data class Campaign(
    val id: String,
    val title: String,
    val description: String,
    val targetAmount: Double,
    val raisedAmount: Double,
    val status: String,
    val daysLeft: Int,
    val imageUrl: String? = null,
    val category: String? = null,
    val createdAt: String? = null
) {
    fun getProgress(): Int {
        return ((raisedAmount / targetAmount) * 100).toInt()
    }

    fun getProgressText(): String {
        return "${getProgress()}% complete"
    }

    fun getRaisedAmountFormatted(): String {
        return "₨${formatAmount(raisedAmount)}"
    }

    fun getTargetAmountFormatted(): String {
        return "₨${formatAmount(targetAmount)}"
    }

    private fun formatAmount(amount: Double): String {
        return when {
            amount >= 1000000 -> String.format("%.2fM", amount / 1000000)
            amount >= 1000 -> String.format("%.0fK", amount / 1000)
            else -> String.format("%.0f", amount)
        }
    }
}
