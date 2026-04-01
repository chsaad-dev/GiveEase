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
import com.example.giveease.R
import com.example.giveease.databinding.FragmentNgoPhysicalDonationsBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class NgoPhysicalDonationsFragment : Fragment() {

    private var _binding: FragmentNgoPhysicalDonationsBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: PhysicalDonationsAdapter

    private var currentMode = Mode.VERIFICATION

    enum class Mode(val statusTarget: String) {
        VERIFICATION("Pending Verification"),
        LOGISTICS("Pending Handover")
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNgoPhysicalDonationsBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupClickListeners()
        loadData()
    }

    private fun setupRecyclerView() {
        adapter = PhysicalDonationsAdapter(
            donationList = emptyList(),
            isVerificationMode = currentMode == Mode.VERIFICATION,
            onApproveClick = { donation -> approveDonation(donation) },
            onRejectClick = { donation -> rejectDonation(donation) },
            onReceivedClick = { donation -> markAsReceived(donation) },
            onViewPhotoClick = { url -> showPhotoDialog(url) }
        )
        binding.rvPhysicalDonations.layoutManager = LinearLayoutManager(requireContext())
        binding.rvPhysicalDonations.adapter = adapter
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                currentMode = if (checkedId == R.id.btnPending) {
                    Mode.VERIFICATION
                } else {
                    Mode.LOGISTICS
                }
                
                // Re-initialize adapter to change visibility modes
                setupRecyclerView()
                loadData()
            }
        }
    }

    private fun loadData() {
        val ngoId = auth.currentUser?.uid ?: return

        binding.rvPhysicalDonations.visibility = View.GONE
        binding.emptyState.visibility = View.GONE

        firestore.collection("donations")
            .whereEqualTo("ngoId", ngoId)
            .whereEqualTo("status", currentMode.statusTarget)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                // Filter to ensure these are physical donations and sort locally
                val physicalList = documents.map { doc ->
                    val map = doc.data as MutableMap<String, Any>
                    map["docId"] = doc.id
                    map
                }
                .filter { it.containsKey("condition") || it.containsKey("itemPhotoUrl") }
                .sortedByDescending { (it["timestamp"] as? Number)?.toLong() ?: 0L }

                if (physicalList.isEmpty()) {
                    binding.emptyState.visibility = View.VISIBLE
                    binding.tvEmptyMessage.text = if (currentMode == Mode.VERIFICATION) {
                        "No pending verifications"
                    } else {
                        "No items pending handover"
                    }
                } else {
                    binding.rvPhysicalDonations.visibility = View.VISIBLE
                    adapter.updateData(physicalList)
                }
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener
                Toast.makeText(requireContext(), "Failed to load data: ${e.message}", Toast.LENGTH_LONG).show()
                binding.emptyState.visibility = View.VISIBLE
            }
    }

    private fun approveDonation(donation: Map<String, Any>) {
        val docId = donation["docId"] as? String ?: return

        AlertDialog.Builder(requireContext())
            .setTitle("Approve Items")
            .setMessage("Are you sure these items meet your conditions? This will move them to the Logistics/Handover queue.")
            .setPositiveButton("Approve") { _, _ ->
                firestore.collection("donations").document(docId)
                    .update("status", Mode.LOGISTICS.statusTarget)
                    .addOnSuccessListener {
                        if (isAdded) Toast.makeText(requireContext(), "Moved to Logistics Queue!", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                    .addOnFailureListener { e ->
                        if (isAdded) Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun rejectDonation(donation: Map<String, Any>) {
        val docId = donation["docId"] as? String ?: return

        AlertDialog.Builder(requireContext())
            .setTitle("Reject Items")
            .setMessage("Are you sure you want to reject these items?")
            .setPositiveButton("Reject") { _, _ ->
                firestore.collection("donations").document(docId)
                    .update("status", "Rejected")
                    .addOnSuccessListener {
                        if (isAdded) Toast.makeText(requireContext(), "Donation rejected", Toast.LENGTH_SHORT).show()
                        loadData()
                    }
                    .addOnFailureListener { e ->
                        if (isAdded) Toast.makeText(requireContext(), "Failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun markAsReceived(donation: Map<String, Any>) {
        val docId = donation["docId"] as? String ?: return
        val campaignId = donation["campaignId"] as? String ?: return
        val quantity = (donation["quantity"] as? Number)?.toLong() ?: 0L

        AlertDialog.Builder(requireContext())
            .setTitle("Mark as Received")
            .setMessage("Have you physically received these items? This will complete the donation and increment the campaign goal.")
            .setPositiveButton("Confirm Receive") { _, _ ->
                firestore.runBatch { batch ->
                    val donationRef = firestore.collection("donations").document(docId)
                    batch.update(donationRef, "status", "Completed")

                    val campaignRef = firestore.collection("campaigns").document(campaignId)
                    batch.update(campaignRef, "currentQuantity", FieldValue.increment(quantity))
                    batch.update(campaignRef, "donorCount", FieldValue.increment(1L))
                }.addOnSuccessListener {
                    if (isAdded) Toast.makeText(requireContext(), "Items received! Campaign updated.", Toast.LENGTH_SHORT).show()
                    loadData()
                }.addOnFailureListener { e ->
                    if (isAdded) Toast.makeText(requireContext(), "Update failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showPhotoDialog(url: String) {
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
        imageView.setOnClickListener { dialog.dismiss() }
        dialog.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
