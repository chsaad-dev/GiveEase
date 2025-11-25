package com.example.giveease.ngo

import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentNgoCreateNewCampaignBinding
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.app.AlertDialog
import com.example.giveease.verification.IdentityVerificationFragment
import java.text.SimpleDateFormat
import java.util.*

class CreateCampaignFragment : Fragment() {

    private var _binding: FragmentNgoCreateNewCampaignBinding? = null
    private val binding get() = _binding!!

    private val selectedImages = mutableListOf<Uri>()
    private var selectedEndDate: Long = 0

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            selectedImages.clear()
            selectedImages.addAll(uris.take(5))
            updateImageCount()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNgoCreateNewCampaignBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupClickListeners()
        setupTextWatchers()
    }

    private fun setupSpinners() {
        val categories = arrayOf(
            "Select Category",
            "Food & Nutrition",
            "Medical & Healthcare",
            "Education",
            "Disaster Relief",
            "Clothing",
            "Shelter",
            "Blood Donation",
            "Other"
        )
        binding.spinnerCategory.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val units = arrayOf("Items", "Kg", "Liters", "Boxes", "People", "PKR")
        binding.spinnerUnit.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            units
        ).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.btnAddImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.etEndDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnCreateCampaign.setOnClickListener {
            createCampaign()
        }
    }

    private fun setupTextWatchers() {
        binding.etDescription.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val count = s?.length ?: 0
                binding.tvCharCount.text = "$count/500"
            }
        })
    }

    private fun showDatePicker() {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1)

        DatePickerDialog(
            requireContext(),
            { _, year, month, day ->
                val selectedCalendar = Calendar.getInstance().apply {
                    set(year, month, day, 23, 59, 59)
                }
                selectedEndDate = selectedCalendar.timeInMillis

                val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
                binding.etEndDate.setText(sdf.format(selectedCalendar.time))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).apply {
            datePicker.minDate = calendar.timeInMillis
            show()
        }
    }

    private fun updateImageCount() {

    }

    private fun createCampaign() {
        checkVerificationBeforeAction {
            if (!validateInputs()) return@checkVerificationBeforeAction

            showLoading(true)

            val campaignData = collectCampaignData()

            if (selectedImages.isNotEmpty()) {
                uploadImages(campaignData)
            } else {
                saveCampaignToFirebase(campaignData, emptyList())
            }
        }
    }

    private fun checkVerificationBeforeAction(onVerified: () -> Unit) {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val verificationStatus = document.getString("verificationStatus") ?: "pending"

                if (verificationStatus == "verified") {
                    onVerified()
                } else {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Verification Required")
                        .setMessage("Please verify your NGO documents to create campaigns.")
                        .setPositiveButton("Verify Now") { _, _ ->
                            requireActivity().supportFragmentManager.beginTransaction()
                                .replace(R.id.fragment_container, IdentityVerificationFragment())
                                .addToBackStack(null)
                                .commit()
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            }
    }

    private fun validateInputs(): Boolean {
        val category = binding.spinnerCategory.selectedItemPosition
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val targetQuantity = binding.etTargetQuantity.text.toString().trim()

        when {
            category == 0 -> {
                showToast("Please select a category")
                return false
            }
            title.isEmpty() || title.length < 10 -> {
                showToast("Title must be at least 10 characters")
                return false
            }
            description.isEmpty() || description.length < 50 -> {
                showToast("Description must be at least 50 characters")
                return false
            }
            targetQuantity.isEmpty() -> {
                showToast("Please enter target quantity")
                return false
            }
            selectedEndDate == 0L -> {
                showToast("Please select end date")
                return false
            }
            binding.chipGroupUrgency.checkedChipId == -1 -> {
                showToast("Please select urgency level")
                return false
            }
        }

        return true
    }

    private fun collectCampaignData(): CampaignData {
        val urgency = when (binding.chipGroupUrgency.checkedChipId) {
            R.id.chipLow -> "Low"
            R.id.chipMedium -> "Medium"
            R.id.chipHigh -> "High"
            R.id.chipEmergency -> "Emergency"
            else -> "Low"
        }

        val condition = when (binding.chipGroupCondition.checkedChipId) {
            R.id.chipNew -> "New"
            R.id.chipUsed -> "Used - Good"
            R.id.chipRefurbished -> "Refurbished"
            else -> null
        }

        return CampaignData(
            ngoId = getCurrentNgoId(),
            ngoName = getCurrentNgoName(),
            category = binding.spinnerCategory.selectedItem.toString(),
            title = binding.etTitle.text.toString().trim(),
            description = binding.etDescription.text.toString().trim(),
            targetQuantity = binding.etTargetQuantity.text.toString().toInt(),
            unit = binding.spinnerUnit.selectedItem.toString(),
            endDate = selectedEndDate,
            urgencyLevel = urgency,
            itemCondition = condition,
            specificRequirements = binding.etSpecificNeeds.text.toString().trim(),
            autoClose = binding.switchAutoClose.isChecked,
            createdAt = System.currentTimeMillis(),
            status = "Active"
        )
    }

    private fun uploadImages(campaignData: CampaignData) {
        val storage = FirebaseStorage.getInstance()
        val imageUrls = mutableListOf<String>()
        var uploadCount = 0

        selectedImages.forEachIndexed { index, uri ->
            val ref = storage.reference
                .child("campaigns/${campaignData.ngoId}/${System.currentTimeMillis()}_$index.jpg")

            ref.putFile(uri)
                .addOnSuccessListener { _: com.google.firebase.storage.UploadTask.TaskSnapshot ->
                    ref.downloadUrl.addOnSuccessListener { downloadUri: Uri ->
                        imageUrls.add(downloadUri.toString())
                        uploadCount++

                        if (uploadCount == selectedImages.size) {
                            saveCampaignToFirebase(campaignData, imageUrls)
                        }
                    }
                }
                .addOnFailureListener { e: Exception ->
                    showLoading(false)
                    showToast("Image upload failed: ${e.message}")
                }
        }
    }

    private fun saveCampaignToFirebase(campaignData: CampaignData, imageUrls: List<String>) {
        val database = FirebaseDatabase.getInstance()
        val campaignsRef = database.getReference("campaigns")
        val campaignId = campaignsRef.push().key ?: return

        val campaign = campaignData.copy(
            id = campaignId,
            imageUrls = imageUrls
        )

        campaignsRef.child(campaignId).setValue(campaign)
            .addOnSuccessListener { _: Void? ->
                showLoading(false)
                showToast("Campaign created successfully!")
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .addOnFailureListener { e: Exception ->
                showLoading(false)
                showToast("Failed to create campaign: ${e.message}")
            }
    }

    private fun getCurrentNgoId(): String {
        return "ngo_${System.currentTimeMillis()}"
    }

    private fun getCurrentNgoName(): String {
        return "Edhi Foundation"
    }

    private fun showLoading(show: Boolean) {
        binding.btnCreateCampaign.isEnabled = !show
        binding.btnCreateCampaign.text = if (show) "Creating..." else "Create Campaign"
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}