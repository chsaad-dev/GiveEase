package com.example.giveease.ngo

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.example.giveease.databinding.FragmentMonetaryVerificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.giveease.utils.NotificationHelper
import com.google.firebase.firestore.FieldValue

class MonetaryVerificationFragment : Fragment() {

    private var _binding: FragmentMonetaryVerificationBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var adapter: VerificationAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMonetaryVerificationBinding.inflate(inflater, container, false)
        setupRecyclerView()
        setupClickListeners()
        loadPendingVerifications()
        return binding.root
    }

    private fun setupRecyclerView() {
        adapter = VerificationAdapter(
            emptyList(),
            onVerifyClick = { donation ->
                verifyDonation(donation)
            },
            onRejectClick = { donation ->
                rejectDonation(donation)
            },
            onViewReceiptClick = { url ->
                showReceiptDialog(url)
            }
        )

        binding.recyclerViewVerifications.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewVerifications.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun loadPendingVerifications() {
        val uid = auth.currentUser?.uid ?: return

        binding.progressBar.visibility = View.VISIBLE
        binding.emptyState.visibility = View.GONE
        binding.recyclerViewVerifications.visibility = View.GONE

        firestore.collection("donations")
            .whereEqualTo("ngoId", uid)
            .whereEqualTo("status", "Pending")
            .whereEqualTo("paymentMethod", "Bank Transfer")
            .addSnapshotListener { snapshots, error ->
                if (!isAdded || _binding == null) return@addSnapshotListener
                
                binding.progressBar.visibility = View.GONE
                
                if (error != null) {
                    Toast.makeText(requireContext(), "Failed to load verifications", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val list = mutableListOf<Map<String, Any>>()
                snapshots?.documents?.forEach { doc ->
                    val data = doc.data?.toMutableMap() ?: mutableMapOf()
                    data["docId"] = doc.id
                    list.add(data)
                }

                if (list.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.recyclerViewVerifications.visibility = View.GONE
                } else {
                    binding.emptyState.visibility = View.GONE
                    binding.recyclerViewVerifications.visibility = View.VISIBLE
                    adapter.updateData(list)
                }
            }
    }

    private fun verifyDonation(donation: Map<String, Any>) {
        val docId = donation["docId"] as? String ?: return
        val campaignId = donation["campaignId"] as? String ?: return
        val quantity = (donation["quantity"] as? Number)?.toLong() ?: 0L

        AlertDialog.Builder(requireContext())
            .setTitle("Verify Donation")
            .setMessage("Are you sure you want to verify and accept this monetary transfer?")
            .setPositiveButton("Accept") { _, _ ->
                firestore.runBatch { batch ->
                    val donationRef = firestore.collection("donations").document(docId)
                    batch.update(donationRef, "status", "Pending Proof")

                    val campaignRef = firestore.collection("campaigns").document(campaignId)
                    batch.update(campaignRef, "currentQuantity", FieldValue.increment(quantity))
                    batch.update(campaignRef, "donorCount", FieldValue.increment(1L))
                }.addOnSuccessListener {
                    if (isAdded) Toast.makeText(requireContext(), "Donation verified and campaign updated!", Toast.LENGTH_SHORT).show()    
                    
                    val donorId = donation["donorId"] as? String ?: ""
                    val campaignTitle = donation["campaignTitle"] as? String ?: "a campaign"
                    NotificationHelper.sendNotification(
                        userId = donorId,
                        title = "Payment Verified ✅",
                        message = "Your monetary transfer for '$campaignTitle' has been successfully verified! Thank you.",
                        type = "donation",
                        referenceId = campaignId
                    )
                    
                    // Goal check!
                    firestore.collection("campaigns").document(campaignId).get()
                        .addOnSuccessListener { doc ->
                            val current = doc.getLong("currentQuantity") ?: 0L
                            val target = doc.getLong("targetQuantity") ?: 0L
                            val ngoId = doc.getString("ngoId") ?: ""
                            if (current >= target && target > 0) {
                                NotificationHelper.sendNotification(
                                    userId = ngoId,
                                    title = "Goal Reached! 🌟",
                                    message = "Congratulations! Your campaign '$campaignTitle' has reached 100% of its target goal.",
                                    type = "campaign",
                                    referenceId = campaignId
                                )
                            }
                        }
                    
                    loadPendingVerifications() // Refresh the list
                }.addOnFailureListener { e ->
                    if (isAdded) Toast.makeText(requireContext(), "Verification failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectDonation(donation: Map<String, Any>) {
        val docId = donation["docId"] as? String ?: return
        val campaignId = donation["campaignId"] as? String

        AlertDialog.Builder(requireContext())
            .setTitle("Reject Donation")
            .setMessage("Are you sure you want to reject this transfer?")
            .setPositiveButton("Reject") { _, _ ->
                firestore.collection("donations").document(docId)
                    .update("status", "Rejected")
                    .addOnSuccessListener {
                        if (isAdded) Toast.makeText(requireContext(), "Donation rejected", Toast.LENGTH_SHORT).show()

                        val donorId = donation["donorId"] as? String ?: ""
                        val campaignTitle = donation["campaignTitle"] as? String ?: "a campaign"
                        NotificationHelper.sendNotification(
                            userId = donorId,
                            title = "Payment Rejected",
                            message = "Your bank receipt for '$campaignTitle' could not be verified.",
                            type = "donation",
                            referenceId = campaignId
                        )
                    }
                    .addOnFailureListener {
                        if (isAdded) Toast.makeText(requireContext(), "Rejection failed", Toast.LENGTH_SHORT).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showReceiptDialog(url: String) {
        val imageView = ImageView(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
        }

        Glide.with(this)
            .load(url)
            .into(imageView)

        val dialog = Dialog(requireContext(), android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.setContentView(imageView)
        
        // Click to dismiss
        imageView.setOnClickListener {
            dialog.dismiss()
        }
        
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
