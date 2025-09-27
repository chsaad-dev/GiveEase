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
import androidx.core.content.ContextCompat
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

    private val itemTypes = arrayOf(
        "Food Items",
        "Clothing",
        "Medicines",
        "Books & Stationery",
        "Toys & Games",
        "Household Items",
        "Electronics",
        "Furniture",
        "Medical Equipment",
        "Sports Equipment",
        "Other Items"
    )

    private var isDonatingMoney = true

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
        setupItemTypeSpinner()
        setupDonationTypeSelection()
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
        val categories = arrayOf(
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
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, categories)
        binding.spinnerCategory.setAdapter(adapter)
    }

    private fun setupItemTypeSpinner() {
        val adapter = ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, itemTypes)
        binding.spinnerItemType.setAdapter(adapter)
    }

    private fun setupDonationTypeSelection() {
        updateDonationTypeSelection(true)

        binding.cardMoneyDonation.setOnClickListener {
            updateDonationTypeSelection(true)
        }

        binding.cardItemDonation.setOnClickListener {
            updateDonationTypeSelection(false)
        }
    }

    private fun updateDonationTypeSelection(isMoney: Boolean) {
        isDonatingMoney = isMoney

        if (isMoney) {
            binding.cardMoneyDonation.apply {
                strokeColor = ContextCompat.getColor(requireContext(), R.color.secondary)
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondary))
            }
            binding.cardItemDonation.apply {
                strokeColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }

            binding.layoutMoneyDonation.visibility = View.VISIBLE
            binding.layoutItemDonation.visibility = View.GONE
        } else {
            binding.cardItemDonation.apply {
                strokeColor = ContextCompat.getColor(requireContext(), R.color.secondary)
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.secondary))
            }
            binding.cardMoneyDonation.apply {
                strokeColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }

            binding.layoutMoneyDonation.visibility = View.GONE
            binding.layoutItemDonation.visibility = View.VISIBLE
        }
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
            "donationType" to if (isDonatingMoney) "money" else "items",
            "category" to binding.spinnerCategory.text.toString(),
            "status" to "active",
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis(),
            "hasProof" to (selectedProofUri != null),
            "contactCount" to 0,
            "interestedNGOs" to emptyList<String>()
        )

        if (isDonatingMoney) {
            campaignData["amount"] = binding.etDonationAmount.text.toString().toDoubleOrNull() as Any
        } else {
            campaignData["itemType"] = binding.spinnerItemType.text.toString()
            campaignData["itemDetails"] = binding.etItemDetails.text.toString().trim()
            val estimatedValue = binding.etEstimatedValue.text.toString().toDoubleOrNull()
            if (estimatedValue != null && estimatedValue > 0) {
                campaignData["estimatedValue"] = estimatedValue
            }
        }

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

            if (category.isEmpty()) {
                spinnerCategory.error = "Please select a category"
                spinnerCategory.requestFocus()
                return false
            }

            if (isDonatingMoney) {
                val amountStr = etDonationAmount.text.toString().trim()

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
            } else {
                val itemType = spinnerItemType.text.toString()
                val itemDetails = etItemDetails.text.toString().trim()

                if (itemType.isEmpty()) {
                    spinnerItemType.error = "Please select an item type"
                    spinnerItemType.requestFocus()
                    return false
                }

                if (itemDetails.isEmpty()) {
                    etItemDetails.error = "Item details are required"
                    etItemDetails.requestFocus()
                    return false
                }

                if (itemDetails.length < 20) {
                    etItemDetails.error = "Please provide more detailed description (at least 20 characters)"
                    etItemDetails.requestFocus()
                    return false
                }
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