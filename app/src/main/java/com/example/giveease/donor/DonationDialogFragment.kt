package com.example.giveease.donor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import com.example.giveease.databinding.FragmentDonationDialogBinding
import com.example.giveease.ngo.CampaignData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*

class DonationDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentDonationDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var campaign: CampaignData
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var donorName: String = ""

    companion object {
        private const val ARG_CAMPAIGN = "campaign"

        fun newInstance(campaign: CampaignData): DonationDialogFragment {
            val fragment = DonationDialogFragment()
            val args = Bundle()
            args.putSerializable(ARG_CAMPAIGN, campaign)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDonationDialogBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        campaign = arguments?.getSerializable(ARG_CAMPAIGN) as? CampaignData
            ?: return binding.root

        setupUI()
        setupClickListeners()
        loadDonorName()

        return binding.root
    }

    private fun setupUI() {
        binding.apply {
            tvCampaignTitle.text = campaign.title
            tvNgoName.text = campaign.ngoName
            tvUnit.text = campaign.unit
            val remaining = campaign.targetQuantity - campaign.currentQuantity
            tvRemaining.text = "Remaining: $remaining ${campaign.unit}"
            val progress = campaign.getProgress()
            tvProgress.text = "${campaign.currentQuantity} / ${campaign.targetQuantity} ${campaign.unit} ($progress%)"
        }
    }

    private fun loadDonorName() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                donorName = document.getString("name") ?: "Anonymous"
            }
    }

    private fun setupClickListeners() {
        binding.apply {
            btnCancel.setOnClickListener {
                dismiss()
            }

            btnDonate.setOnClickListener {
                validateAndDonate()
            }
        }
    }

    private fun validateAndDonate() {
        val quantityStr = binding.etQuantity.text.toString().trim()

        if (quantityStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter quantity", Toast.LENGTH_SHORT).show()
            return
        }

        val quantity = quantityStr.toIntOrNull()
        if (quantity == null || quantity <= 0) {
            Toast.makeText(requireContext(), "Please enter valid quantity", Toast.LENGTH_SHORT).show()
            return
        }

        val remaining = campaign.targetQuantity - campaign.currentQuantity
        if (quantity > remaining) {
            Toast.makeText(requireContext(), "Quantity exceeds remaining amount ($remaining ${campaign.unit})", Toast.LENGTH_LONG).show()
            return
        }

        checkVerificationAndDonate(quantity)
    }

    private fun checkVerificationAndDonate(quantity: Int) {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val verificationStatus = document.getString("verificationStatus") ?: "pending"

                if (verificationStatus == "verified") {
                    processDonation(quantity)
                } else {
                    Toast.makeText(
                        requireContext(),
                        "Please verify your identity to donate",
                        Toast.LENGTH_LONG
                    ).show()
                    dismiss()
                }
            }
    }

    private fun processDonation(quantity: Int) {
        showLoading(true)

        val donationData = hashMapOf(
            "donorId" to auth.currentUser?.uid,
            "donorName" to donorName,
            "campaignId" to campaign.id,
            "campaignTitle" to campaign.title,
            "ngoId" to campaign.ngoId,
            "ngoName" to campaign.ngoName,
            "quantity" to quantity,
            "unit" to campaign.unit,
            "message" to binding.etMessage.text.toString().trim(),
            "timestamp" to System.currentTimeMillis(),
            "status" to "Completed"
        )

        firestore.collection("donations")
            .add(donationData)
            .addOnSuccessListener { donationRef ->
                updateCampaignProgress(quantity, donationRef.id)
            }
            .addOnFailureListener { e ->
                showLoading(false)
                Toast.makeText(requireContext(), "Failed to record donation: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateCampaignProgress(quantity: Int, donationId: String) {
        val campaignRef = firestore.collection("campaigns").document(campaign.id)

        firestore.runTransaction { transaction ->
            val snapshot = transaction.get(campaignRef)
            val currentQuantity = snapshot.getLong("currentQuantity")?.toInt() ?: 0
            val donorCount = snapshot.getLong("donorCount")?.toInt() ?: 0

            val newQuantity = currentQuantity + quantity

            transaction.update(campaignRef, "currentQuantity", newQuantity)
            transaction.update(campaignRef, "donorCount", donorCount + 1)
            transaction.update(campaignRef, "updatedAt", System.currentTimeMillis())

            if (campaign.autoClose && newQuantity >= campaign.targetQuantity) {
                transaction.update(campaignRef, "status", "Completed")
            }
        }.addOnSuccessListener {
            showLoading(false)
            showSuccessAndDismiss(quantity, donationId)
        }.addOnFailureListener { e ->
            showLoading(false)
            Toast.makeText(requireContext(), "Failed to update campaign: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSuccessAndDismiss(quantity: Int, donationId: String) {
        (parentFragment as? CampaignDetailsFragment)?.refreshCampaignData()

        if (isAdded && context != null) {
            Toast.makeText(
                requireContext(),
                "Thank you! Your donation of $quantity ${campaign.unit} has been recorded.",
                Toast.LENGTH_SHORT  // Changed to SHORT
            ).show()
        }

        view?.postDelayed({
            if (isAdded) {
                dismiss()
            }
        }, 300)
    }

    private fun showLoading(show: Boolean) {
        binding.apply {
            btnDonate.isEnabled = !show
            btnCancel.isEnabled = !show
            btnDonate.text = if (show) "Processing..." else "Donate Now"
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}