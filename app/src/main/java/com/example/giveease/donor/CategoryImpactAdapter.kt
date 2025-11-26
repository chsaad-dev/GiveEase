package com.example.giveease.donor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.databinding.ItemCategoryImpactBinding

class CategoryImpactAdapter : RecyclerView.Adapter<CategoryImpactAdapter.ViewHolder>() {

    private var categories = listOf<ImpactDashboardFragment.CategoryImpact>()

    fun submitList(list: List<ImpactDashboardFragment.CategoryImpact>) {
        categories = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemCategoryImpactBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(categories[position])
    }

    override fun getItemCount() = categories.size

    class ViewHolder(private val binding: ItemCategoryImpactBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(categoryImpact: ImpactDashboardFragment.CategoryImpact) {
            binding.apply {
                tvCategoryName.text = categoryImpact.category
                tvDonationCount.text = "${categoryImpact.donationCount} donations"
                tvItemCount.text = "${categoryImpact.itemCount} items"
            }
        }
    }
}