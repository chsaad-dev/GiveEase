package com.example.giveease.admin

import android.graphics.Color
import android.text.format.DateUtils
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.databinding.ItemAdminTicketBinding

data class AdminTicket(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val issueType: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = "",
    val timestamp: Long = 0
)

class AdminTicketAdapter(
    private val onTicketActionClick: (AdminTicket) -> Unit
) : RecyclerView.Adapter<AdminTicketAdapter.ViewHolder>() {

    private var items = listOf<AdminTicket>()

    fun submitList(list: List<AdminTicket>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAdminTicketBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemAdminTicketBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.root.setOnClickListener {
                if (adapterPosition != RecyclerView.NO_POSITION) {
                    onTicketActionClick(items[adapterPosition])
                }
            }
        }

        fun bind(ticket: AdminTicket) {
            binding.apply {
                tvSubject.text = ticket.subject
                tvMessagePreview.text = ticket.message
                chipIssueType.text = ticket.issueType
                tvUserEmail.text = ticket.userEmail

                // Status formatting
                tvStatus.text = ticket.status
                if (ticket.status == "Open") {
                    tvStatus.setTextColor(Color.parseColor("#F57C00")) // Orange
                } else {
                    tvStatus.setTextColor(Color.parseColor("#4CAF50")) // Green
                }

                // Time formatting
                val now = System.currentTimeMillis()
                val timeAgo = DateUtils.getRelativeTimeSpanString(
                    ticket.timestamp, now, DateUtils.MINUTE_IN_MILLIS
                )
                tvTime.text = timeAgo
            }
        }
    }
}
