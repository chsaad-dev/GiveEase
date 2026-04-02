package com.example.giveease.admin

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.R
import com.example.giveease.databinding.FragmentVerificationApprovalsBinding
import com.google.firebase.firestore.FirebaseFirestore
import com.example.giveease.utils.NotificationHelper

class VerificationApprovalsFragment : Fragment() {

    private var _binding: FragmentVerificationApprovalsBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var adapter: VerificationAdapter
    private val verificationList = mutableListOf<VerificationRequest>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentVerificationApprovalsBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        setupRecyclerView()
        loadPendingVerifications()
    }

    private fun setupRecyclerView() {
        adapter = VerificationAdapter(
            onApprove = { request -> showApproveDialog(request) },
            onReject = { request -> showRejectDialog(request) },
            onViewDocument = { url -> openDocument(url) }
        )

        binding.recyclerViewVerifications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewVerifications.adapter = adapter
    }

    private fun loadPendingVerifications() {
        if (!isAdded || _binding == null) return

        binding.progressBar.visibility = View.VISIBLE

        firestore.collection("users")
            .whereEqualTo("verificationStatus", "pending")
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                binding.progressBar.visibility = View.GONE
                verificationList.clear()

                for (doc in documents) {
                    val request = VerificationRequest(
                        userId = doc.id,
                        name = doc.getString("name") ?: "",
                        email = doc.getString("email") ?: "",
                        role = doc.getString("role") ?: "",
                        documentUrl = if (doc.getString("role") == "ngo") {
                            doc.getString("governmentDocumentUrl") ?: ""
                        } else {
                            doc.getString("identityDocumentUrl") ?: ""
                        },
                        registrationNumber = doc.getString("registrationNumber") ?: "",
                        createdAt = doc.getLong("createdAt") ?: 0
                    )
                    verificationList.add(request)
                }

                if (verificationList.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.recyclerViewVerifications.visibility = View.GONE
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                    binding.recyclerViewVerifications.visibility = View.VISIBLE
                    adapter.submitList(verificationList)
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener

                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "Error loading requests: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showApproveDialog(request: VerificationRequest) {
        if (!isAdded) return

        AlertDialog.Builder(requireContext())
            .setTitle("Approve Verification")
            .setMessage("Approve verification for ${request.name}?")
            .setPositiveButton("Approve") { _, _ ->
                approveVerification(request)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun approveVerification(request: VerificationRequest) {
        val updates = hashMapOf<String, Any>(
            "verificationStatus" to "verified",
            "verifiedAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("users").document(request.userId)
            .update(updates)
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener

                NotificationHelper.sendNotification(
                    userId = request.userId,
                    title = "Verification Approved 🎉",
                    message = "Congratulations ${request.name}, your account has been verified! You can now create campaigns.",
                    type = "verification"
                )

                Toast.makeText(requireContext(), "${request.name} approved successfully", Toast.LENGTH_SHORT).show()
                loadPendingVerifications()
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener

                Toast.makeText(requireContext(), "Error approving: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showRejectDialog(request: VerificationRequest) {
        if (!isAdded) return

        val input = EditText(requireContext()).apply {
            hint = "Reason for rejection"
            minLines = 3
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Reject Verification")
            .setMessage("Provide a reason for rejecting ${request.name}'s verification:")
            .setView(input)
            .setPositiveButton("Reject") { _, _ ->
                val reason = input.text.toString().trim()
                if (reason.isEmpty()) {
                    Toast.makeText(requireContext(), "Please provide a reason", Toast.LENGTH_SHORT).show()
                } else {
                    rejectVerification(request, reason)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectVerification(request: VerificationRequest, reason: String) {
        val updates = hashMapOf<String, Any>(
            "verificationStatus" to "rejected",
            "rejectionReason" to reason,
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("users").document(request.userId)
            .update(updates)
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener

                NotificationHelper.sendNotification(
                    userId = request.userId,
                    title = "Verification Rejected",
                    message = "Your verification request was rejected. Reason: $reason",
                    type = "verification"
                )

                Toast.makeText(requireContext(), "${request.name} rejected", Toast.LENGTH_SHORT).show()
                loadPendingVerifications()
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener

                Toast.makeText(requireContext(), "Error rejecting: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun openDocument(url: String) {
        if (!isAdded) return

        if (url.isEmpty()) {
            Toast.makeText(requireContext(), "No document available", Toast.LENGTH_SHORT).show()
            return
        }

        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Cannot open document", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

data class VerificationRequest(
    val userId: String,
    val name: String,
    val email: String,
    val role: String,
    val documentUrl: String,
    val registrationNumber: String,
    val createdAt: Long
)