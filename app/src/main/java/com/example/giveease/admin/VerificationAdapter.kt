package com.example.giveease.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.giveease.databinding.ItemVerificationRequestBinding
import java.text.SimpleDateFormat
import java.util.*

class VerificationAdapter(
    private val onApprove: (VerificationRequest) -> Unit,
    private val onReject: (VerificationRequest) -> Unit,
    private val onViewDocument: (String) -> Unit
) : RecyclerView.Adapter<VerificationAdapter.ViewHolder>() {

    private var items = listOf<VerificationRequest>()

    fun submitList(list: List<VerificationRequest>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemVerificationRequestBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(private val binding: ItemVerificationRequestBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(request: VerificationRequest) {
            binding.apply {
                tvName.text = request.name
                tvEmail.text = request.email
                tvRole.text = if (request.role == "ngo") "NGO" else "Donor"

                if (request.role == "ngo" && request.registrationNumber.isNotEmpty()) {
                    tvRegistrationNumber.text = "Reg: ${request.registrationNumber}"
                    tvRegistrationNumber.visibility = android.view.View.VISIBLE
                } else {
                    tvRegistrationNumber.visibility = android.view.View.GONE
                }

                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                tvDate.text = "Submitted: ${sdf.format(Date(request.createdAt))}"

                btnViewDocument.setOnClickListener {
                    onViewDocument(request.documentUrl)
                }

                btnApprove.setOnClickListener {
                    onApprove(request)
                }

                btnReject.setOnClickListener {
                    onReject(request)
                }
            }
        }
    }
}