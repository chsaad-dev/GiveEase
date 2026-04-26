package com.example.giveease.donor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.databinding.ItemCategoryImpactBinding

class CategoryImpactAdapter : RecyclerView.Adapter<CategoryImpactAdapter.ViewHolder>() {

    private var categories = listOf<ImpactDashboardFragment.CategoryImpact>()
    private var totalDonations = 0

    fun submitList(list: List<ImpactDashboardFragment.CategoryImpact>, total: Int) {
        categories = list
        totalDonations = total
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryImpactBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position], totalDonations)
    }

    override fun getItemCount() = categories.size

    class ViewHolder(private val binding: ItemCategoryImpactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(categoryImpact: ImpactDashboardFragment.CategoryImpact, total: Int) {
            val percentage = if (total > 0) (categoryImpact.donationCount * 100) / total else 0
            val icon = getCategoryIcon(categoryImpact.category)

            binding.tvCategoryIcon.text = icon
            binding.tvCategoryName.text = categoryImpact.category
            binding.tvDonationCount.text = "${categoryImpact.donationCount} donations"
            binding.tvPercentage.text = "$percentage%"
            binding.tvItemCount.text = "${categoryImpact.itemCount} items"
            binding.progressCategory.progress = percentage
        }

        private fun getCategoryIcon(category: String): String {
            return when (category.lowercase()) {
                "clothes", "clothing" -> "👕"
                "food", "food & nutrition" -> "🍲"
                "books", "education" -> "📚"
                "electronics" -> "💻"
                "medical", "medical supplies" -> "💊"
                "furniture" -> "🪑"
                "blood", "blood donation" -> "🩸"
                else -> "📦"
            }
        }
    }
}