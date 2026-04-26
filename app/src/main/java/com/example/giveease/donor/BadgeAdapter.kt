package com.example.giveease.donor

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.R
import com.example.giveease.databinding.ItemBadgeBinding

data class BadgeItem(
    val icon: String,
    val name: String,
    val status: String,
    val isEarned: Boolean
)

class BadgeAdapter(private val badges: List<BadgeItem>) :
    RecyclerView.Adapter<BadgeAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemBadgeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(badges[position])
    }

    override fun getItemCount() = badges.size

    class ViewHolder(private val binding: ItemBadgeBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(badge: BadgeItem) {
            binding.tvBadgeIcon.text = badge.icon
            binding.tvBadgeName.text = badge.name
            binding.tvBadgeStatus.text = badge.status

            if (badge.isEarned) {
                binding.root.setBackgroundResource(R.drawable.bg_badge_earned)
                binding.tvBadgeIcon.alpha = 1f
                binding.tvBadgeName.alpha = 1f
            } else {
                binding.root.setBackgroundResource(R.drawable.bg_badge_locked)
                binding.tvBadgeIcon.alpha = 0.4f
                binding.tvBadgeName.alpha = 0.5f
            }
        }
    }
}
