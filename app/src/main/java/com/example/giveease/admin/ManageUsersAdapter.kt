package com.example.giveease.admin

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.R
import com.example.giveease.databinding.ItemManageUserBinding

data class AdminUser(
    val userId: String = "",
    val name: String = "",
    val email: String = "",
    val role: String = "",
    val verificationStatus: String = "",
    val phone: String = "",
    val createdAt: Long = 0
)

class ManageUsersAdapter(
    private val onActionClick: (AdminUser) -> Unit
) : RecyclerView.Adapter<ManageUsersAdapter.ViewHolder>() {

    private var items = listOf<AdminUser>()

    fun submitList(list: List<AdminUser>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemManageUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemManageUserBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(user: AdminUser) {
            binding.apply {
                tvUserName.text = user.name.ifEmpty { "Unknown" }
                tvUserEmail.text = user.email

                // Role badge
                val roleText = if (user.role == "ngo") "NGO" else "Donor"
                tvRole.text = roleText
                val roleBg = GradientDrawable().apply {
                    cornerRadius = 20f
                    setColor(
                        if (user.role == "ngo") 0xFF1565C0.toInt() else 0xFF4CAF50.toInt()
                    )
                }
                tvRole.background = roleBg

                // Status badge
                tvStatus.text = user.verificationStatus.replaceFirstChar { it.uppercase() }
                val statusBg = GradientDrawable().apply {
                    cornerRadius = 20f
                    setColor(
                        when (user.verificationStatus) {
                            "verified" -> 0xFF2E7D32.toInt()
                            "pending" -> 0xFFFF9800.toInt()
                            "rejected" -> 0xFFF44336.toInt()
                            else -> 0xFF9E9E9E.toInt()
                        }
                    )
                }
                tvStatus.background = statusBg

                // Avatar icon color
                val avatarTint = if (user.role == "ngo") 0xFF1565C0.toInt() else 0xFF4CAF50.toInt()
                ivUserAvatar.setColorFilter(avatarTint)

                // Action
                btnAction.setOnClickListener { onActionClick(user) }
                root.setOnClickListener { onActionClick(user) }
            }
        }
    }
}
