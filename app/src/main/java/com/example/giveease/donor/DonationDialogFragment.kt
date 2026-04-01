package com.example.giveease.donor

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.example.giveease.databinding.FragmentDonationDialogBinding
import com.example.giveease.ngo.CampaignData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.*

class DonationDialogFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentDonationDialogBinding? = null
    private val binding get() = _binding!!
    private lateinit var campaign: CampaignData
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val storage = FirebaseStorage.getInstance()
    private var donorName: String = ""
    private var receiptImageUri: Uri? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            receiptImageUri = uri
            binding.ivReceiptPreview.setImageURI(uri)
            binding.cardReceiptPreview.visibility = View.VISIBLE
            binding.btnUploadReceipt.visibility = View.GONE
        }
    }

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

            if (campaign.category == "Monetary Funds") {
                llMonetarySection.visibility = View.VISIBLE
                fetchNgoBankDetails()
            } else {
                llMonetarySection.visibility = View.GONE
            }
        }
    }

    private fun fetchNgoBankDetails() {
        firestore.collection("users").document(campaign.ngoId).get()
            .addOnSuccessListener { document ->
                if (!isAdded || _binding == null) return@addOnSuccessListener
                
                // Try new array format
                val accounts = document.get("bankAccounts") as? List<Map<String, String>>
                if (accounts != null && accounts.isNotEmpty()) {
                    val sb = StringBuilder()
                    accounts.forEachIndexed { index, map ->
                        val bankName = map["bankName"] ?: "N/A"
                        val title = map["accountTitle"] ?: "N/A"
                        val number = map["accountNumber"] ?: "N/A"
                        sb.append("🏦 $bankName\nTitle: $title\nAcc/IBAN: $number")
                        if (index < accounts.size - 1) sb.append("\n\n")
                    }
                    binding.tvNgoBankDetails.text = sb.toString()
                } else {
                    // Fallback to legacy
                    val legacy = document.get("bankDetails") as? Map<String, String>
                    if (legacy != null) {
                        val name = legacy["bankName"] ?: "N/A"
                        val title = legacy["accountTitle"] ?: "N/A"
                        val number = legacy["accountNumber"] ?: "N/A"
                        binding.tvNgoBankDetails.text = "🏦 $name\nTitle: $title\nAcc/IBAN: $number"
                    } else {
                        binding.tvNgoBankDetails.text = "No bank details provided by NGO."
                    }
                }
            }
            .addOnFailureListener {
                if (isAdded && _binding != null) {
                    binding.tvNgoBankDetails.text = "Failed to load bank details."
                }
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

            btnUploadReceipt.setOnClickListener {
                imagePickerLauncher.launch("image/*")
            }

            btnRemoveReceipt.setOnClickListener {
                receiptImageUri = null
                cardReceiptPreview.visibility = View.GONE
                btnUploadReceipt.visibility = View.VISIBLE
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

        if (campaign.category == "Monetary Funds" && receiptImageUri == null) {
            Toast.makeText(requireContext(), "Please upload a bank transfer receipt", Toast.LENGTH_SHORT).show()
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
        val isMonetary = campaign.category == "Monetary Funds"

        if (isMonetary && receiptImageUri != null) {
            // Upload receipt first
            val receiptRef = storage.reference.child("receipts/${UUID.randomUUID()}.jpg")
            receiptRef.putFile(receiptImageUri!!)
                .addOnSuccessListener {
                    receiptRef.downloadUrl.addOnSuccessListener { url ->
                        saveDonationRecord(quantity, "Pending", "Bank Transfer", url.toString())
                    }
                }
                .addOnFailureListener { e ->
                    showLoading(false)
                    Toast.makeText(requireContext(), "Failed to upload receipt: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        } else {
            // Non-monetary donation
            saveDonationRecord(quantity, "Completed", "In-App", null)
        }
    }

    private fun saveDonationRecord(quantity: Int, status: String, paymentMethod: String, receiptUrl: String?) {
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
            "status" to status,
            "paymentMethod" to paymentMethod
        )

        if (receiptUrl != null) {
            donationData["receiptUrl"] = receiptUrl
        }

        firestore.collection("donations")
            .add(donationData)
            .addOnSuccessListener { donationRef ->
                if (status == "Completed") {
                    updateCampaignProgress(quantity, donationRef.id)
                } else {
                    // For pending monetary donations, don't update campaign progress yet
                    showLoading(false)
                    showSuccessAndDismiss(quantity, donationRef.id, isPending = true)
                }
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
            showSuccessAndDismiss(quantity, donationId, isPending = false)
        }.addOnFailureListener { e ->
            showLoading(false)
            Toast.makeText(requireContext(), "Failed to update campaign: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showSuccessAndDismiss(quantity: Int, donationId: String, isPending: Boolean) {
        (parentFragment as? CampaignDetailsFragment)?.refreshCampaignData()

        if (isAdded && context != null) {
            val successMessage = if (isPending) {
                "Thank you! Your donation is pending NGO verification of the receipt."
            } else {
                "Thank you! Your donation of $quantity ${campaign.unit} has been recorded."
            }
            Toast.makeText(
                requireContext(),
                successMessage,
                Toast.LENGTH_LONG
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