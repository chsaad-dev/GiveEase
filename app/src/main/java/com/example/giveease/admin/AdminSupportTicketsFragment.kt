package com.example.giveease.admin

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.databinding.FragmentAdminSupportTicketsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class AdminSupportTicketsFragment : Fragment() {

    private var _binding: FragmentAdminSupportTicketsBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: AdminTicketAdapter
    private var allTickets = mutableListOf<AdminTicket>()
    private var currentFilter = "All"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminSupportTicketsBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        setupChipFilters()
        loadTickets()
    }

    private fun setupRecyclerView() {
        adapter = AdminTicketAdapter { ticket ->
            showTicketDetails(ticket)
        }
        binding.recyclerViewTickets.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewTickets.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun setupChipFilters() {
        binding.chipGroupFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            currentFilter = when {
                checkedIds.contains(binding.chipAll.id) -> "All"
                checkedIds.contains(binding.chipOpen.id) -> "Open"
                checkedIds.contains(binding.chipResolved.id) -> "Resolved"
                else -> "All"
            }
            applyFilters()
        }
    }

    private fun loadTickets() {
        if (!isAdded || _binding == null) return

        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE

        firestore.collection("support_tickets")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                binding.progressBar.visibility = View.GONE
                allTickets.clear()

                for (doc in documents) {
                    val ticket = AdminTicket(
                        id = doc.id,
                        userId = doc.getString("userId") ?: "",
                        userName = doc.getString("userName") ?: "Unknown",
                        userEmail = doc.getString("userEmail") ?: "No email",
                        issueType = doc.getString("issueType") ?: "General",
                        subject = doc.getString("subject") ?: "No Subject",
                        message = doc.getString("message") ?: "No details provided.",
                        status = doc.getString("status") ?: "Open",
                        timestamp = doc.getLong("timestamp") ?: 0
                    )
                    allTickets.add(ticket)
                }
                
                applyFilters()
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener

                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error loading tickets: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun applyFilters() {
        val filtered = if (currentFilter == "All") {
            allTickets
        } else {
            allTickets.filter { it.status == currentFilter }
        }

        binding.tvTicketCount.text = "${filtered.size} tickets"

        if (filtered.isEmpty()) {
            binding.emptyState.visibility = View.VISIBLE
            binding.recyclerViewTickets.visibility = View.GONE
        } else {
            binding.emptyState.visibility = View.GONE
            binding.recyclerViewTickets.visibility = View.VISIBLE
            adapter.submitList(filtered)
        }
    }

    private fun showTicketDetails(ticket: AdminTicket) {
        if (!isAdded) return

        val detailsDialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(ticket.subject)
            .setMessage(
                "From: ${ticket.userName} (${ticket.userEmail})\n" +
                "Issue: ${ticket.issueType}\n\n" +
                "${ticket.message}"
            )

        detailsDialog.setNeutralButton("Close", null)

        detailsDialog.setNegativeButton("Email User") { _, _ ->
            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:${ticket.userEmail}")
                putExtra(Intent.EXTRA_SUBJECT, "Re: ${ticket.subject} (GiveEase Support)")
                putExtra(Intent.EXTRA_TEXT, "Hi ${ticket.userName},\n\nRegarding your ticket:\n\"${ticket.message}\"\n\n---\n")
            }
            try {
                startActivity(Intent.createChooser(emailIntent, "Reply to user"))
                AdminLogger.logAction("support_reply", "Support Reply", "Admin opened email to reply to ${ticket.userEmail}")
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "No email app found", Toast.LENGTH_SHORT).show()
            }
        }

        if (ticket.status == "Open") {
            detailsDialog.setPositiveButton("Mark Resolved") { _, _ ->
                resolveTicket(ticket)
            }
        } else {
             detailsDialog.setPositiveButton("Reopen Ticket") { _, _ ->
                reopenTicket(ticket)
            }
        }

        detailsDialog.show()
    }

    private fun resolveTicket(ticket: AdminTicket) {
        firestore.collection("support_tickets").document(ticket.id)
            .update("status", "Resolved")
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Ticket marked as resolved", Toast.LENGTH_SHORT).show()
                AdminLogger.logAction("resolve_ticket", "Ticket Resolved", "Admin resolved ticket from ${ticket.userEmail}")
                loadTickets()
            }
            .addOnFailureListener {
                if (isAdded) Toast.makeText(requireContext(), "Failed to update ticket", Toast.LENGTH_SHORT).show()
            }
    }

    private fun reopenTicket(ticket: AdminTicket) {
        firestore.collection("support_tickets").document(ticket.id)
            .update("status", "Open")
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener
                Toast.makeText(requireContext(), "Ticket opened", Toast.LENGTH_SHORT).show()
                loadTickets()
            }
            .addOnFailureListener {
                if (isAdded) Toast.makeText(requireContext(), "Failed to update ticket", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
