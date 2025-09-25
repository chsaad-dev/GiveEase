package com.example.giveease.donor

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentCreateCampaignBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class CreateCampaignFragment : Fragment() {
    private lateinit var binding: FragmentCreateCampaignBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var loadingDialog: AlertDialog
    private var selectedProofUri: Uri? = null

    private val categories = arrayOf(
        "Education",
        "Healthcare",
        "Food & Nutrition",
        "Disaster Relief",
        "Environment",
        "Child Welfare",
        "Women Empowerment",
        "Elderly Care",
        "Animal Welfare",
        "Community Development",
        "Other"
    )

    private val documentPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedProofUri = result.data?.data
            selectedProofUri?.let {
                showSelectedFile(it)
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentCreateCampaignBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupProgressDialog()
        setupCategorySpinner()
        setupClickListeners()

        return binding.root
    }

    private fun setupProgressDialog() {
        loadingDialog = AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
    }

    private fun setupCategorySpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(adapter)
    }

    private fun setupClickListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            btnCancel.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            btnUploadProof.setOnClickListener {
                selectProofDocument()
            }

            btnRemoveFile.setOnClickListener {
                removeSelectedFile()
            }

            btnCreateCampaign.setOnClickListener {
                createCampaign()
            }
        }
    }

    private fun selectProofDocument() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(
                "image/*",
                "application/pdf",
                "text/plain"
            ))
        }
        documentPickerLauncher.launch(intent)
    }

    private fun showSelectedFile(uri: Uri) {
        val fileName = getFileName(uri)
        binding.apply {
            tvSelectedFileName.text = fileName
            layoutSelectedFile.visibility = View.VISIBLE
            btnUploadProof.text = "Change File"
        }
    }

    private fun removeSelectedFile() {
        selectedProofUri = null
        binding.apply {
            layoutSelectedFile.visibility = View.GONE
            btnUploadProof.text = "Upload Proof"
        }
    }

    private fun getFileName(uri: Uri): String {
        var result = "Unknown File"
        try {
            requireContext().contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val nameIndex = cursor.getColumnIndex(MediaStore.MediaColumns.DISPLAY_NAME)
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex)
                    }
                }
            }
        } catch (e: Exception) {
            result = uri.lastPathSegment ?: "Unknown File"
        }
        return result
    }

    private fun createCampaign() {
        if (!validateInputs()) return

        loadingDialog.show()

        val userId = auth.currentUser?.uid ?: return
        val campaignId = firestore.collection("donor_campaigns").document().id

        val campaignData = hashMapOf(
            "id" to campaignId,
            "donorId" to userId,
            "donorName" to (auth.currentUser?.displayName ?: "Anonymous Donor"),
            "title" to binding.etCampaignTitle.text.toString().trim(),
            "description" to binding.etCampaignDescription.text.toString().trim(),
            "amount" to binding.etDonationAmount.text.toString().toDoubleOrNull(),
            "category" to binding.spinnerCategory.text.toString(),
            "status" to "active", // active, completed, cancelled
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis(),
            "hasProof" to (selectedProofUri != null),
            "contactCount" to 0,
            "interestedNGOs" to emptyList<String>()
        )

        firestore.collection("donor_campaigns").document(campaignId)
            .set(campaignData)
            .addOnSuccessListener {
                loadingDialog.dismiss()
                showSuccessDialog()
            }
            .addOnFailureListener { exception ->
                loadingDialog.dismiss()
                Toast.makeText(
                    requireContext(),
                    "Failed to create campaign: ${exception.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    private fun validateInputs(): Boolean {
        binding.apply {
            val title = etCampaignTitle.text.toString().trim()
            val description = etCampaignDescription.text.toString().trim()
            val amountStr = etDonationAmount.text.toString().trim()
            val category = spinnerCategory.text.toString()

            if (title.isEmpty()) {
                etCampaignTitle.error = "Campaign title is required"
                etCampaignTitle.requestFocus()
                return false
            }

            if (title.length < 10) {
                etCampaignTitle.error = "Title should be at least 10 characters"
                etCampaignTitle.requestFocus()
                return false
            }

            if (description.isEmpty()) {
                etCampaignDescription.error = "Campaign description is required"
                etCampaignDescription.requestFocus()
                return false
            }

            if (description.length < 50) {
                etCampaignDescription.error = "Description should be at least 50 characters"
                etCampaignDescription.requestFocus()
                return false
            }

            if (amountStr.isEmpty()) {
                etDonationAmount.error = "Donation amount is required"
                etDonationAmount.requestFocus()
                return false
            }

            val amount = amountStr.toDoubleOrNull()
            if (amount == null || amount <= 0) {
                etDonationAmount.error = "Enter a valid amount"
                etDonationAmount.requestFocus()
                return false
            }

            if (amount < 100) {
                etDonationAmount.error = "Minimum donation amount is Rs 100"
                etDonationAmount.requestFocus()
                return false
            }

            if (category.isEmpty()) {
                spinnerCategory.error = "Please select a category"
                spinnerCategory.requestFocus()
                return false
            }

            return true
        }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Campaign Created!")
            .setMessage("Your donation campaign has been created successfully. NGOs can now view and contact you about this opportunity.\n\nYou can manage your campaigns from your profile.")
            .setPositiveButton("View Campaigns") { _, _ ->
                navigateToDonorCampaigns()
            }
            .setNegativeButton("Go Home") { _, _ ->
                navigateToHome()
            }
            .setCancelable(false)
            .show()
    }

    private fun navigateToDonorCampaigns() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_donor, DonorCampaignsFragment())
            .commit()
    }

    private fun navigateToHome() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_donor, DonorHomeFragment())
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }
}