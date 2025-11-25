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
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.app.AlertDialog
import com.example.giveease.verification.IdentityVerificationFragment
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

class CreateCampaignFragment : Fragment() {

    private var _binding: FragmentNgoCreateNewCampaignBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private val selectedImages = mutableListOf<Uri>()
    private var selectedEndDate: Long = 0
    private var ngoName: String = ""

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
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        loadNgoName()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupClickListeners()
        setupTextWatchers()
    }

    private fun loadNgoName() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                ngoName = document.getString("name") ?: "NGO"
            }
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
        Toast.makeText(requireContext(), "${selectedImages.size} image(s) selected", Toast.LENGTH_SHORT).show()
    }

    private fun createCampaign() {
        checkVerificationBeforeAction {
            if (!validateInputs()) return@checkVerificationBeforeAction

            showLoading(true)

            val campaignData = collectCampaignData()

            if (selectedImages.isNotEmpty()) {
                uploadImages(campaignData)
            } else {
                saveCampaignToFirestore(campaignData, emptyList())
            }
        }
    }

    private fun checkVerificationBeforeAction(onVerified: () -> Unit) {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid).get()
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
            ngoId = auth.currentUser?.uid ?: "",
            ngoName = ngoName,
            category = binding.spinnerCategory.selectedItem.toString(),
            title = binding.etTitle.text.toString().trim(),
            description = binding.etDescription.text.toString().trim(),
            targetQuantity = binding.etTargetQuantity.text.toString().toInt(),
            unit = binding.spinnerUnit.selectedItem.toString(),
            endDate = selectedEndDate,
            urgencyLevel = urgency,
            itemCondition = condition ?: "",
            specificRequirements = binding.etSpecificNeeds.text.toString().trim(),
            autoClose = binding.switchAutoClose.isChecked,
            createdAt = System.currentTimeMillis(),
            status = "Active"
        )
    }

    private fun compressImage(uri: Uri): ByteArray {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

        // Calculate scaling to max 1200px width while maintaining aspect ratio
        val maxWidth = 1200
        val scale = if (bitmap.width > maxWidth) {
            maxWidth.toFloat() / bitmap.width.toFloat()
        } else {
            1.0f
        }

        val scaledBitmap = if (scale < 1.0f) {
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }

        // Compress to JPEG with 80% quality
        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

        // Clean up
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        bitmap.recycle()

        return outputStream.toByteArray()
    }

    private fun uploadImages(campaignData: CampaignData) {
        val imageUrls = mutableListOf<String>()
        var uploadCount = 0

        selectedImages.forEachIndexed { index, uri ->
            try {
                // Compress image before upload
                val compressedImage = compressImage(uri)

                val ref = storage.reference
                    .child("campaigns/${campaignData.ngoId}/${System.currentTimeMillis()}_$index.jpg")

                ref.putBytes(compressedImage)
                    .addOnSuccessListener {
                        ref.downloadUrl.addOnSuccessListener { downloadUri ->
                            imageUrls.add(downloadUri.toString())
                            uploadCount++

                            if (uploadCount == selectedImages.size) {
                                saveCampaignToFirestore(campaignData, imageUrls)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        uploadCount++
                        showToast("Image upload failed: ${e.message}")

                        if (uploadCount == selectedImages.size) {
                            if (imageUrls.isNotEmpty()) {
                                saveCampaignToFirestore(campaignData, imageUrls)
                            } else {
                                showLoading(false)
                                showToast("All images failed to upload")
                            }
                        }
                    }
            } catch (e: Exception) {
                uploadCount++
                showToast("Error processing image: ${e.message}")

                if (uploadCount == selectedImages.size) {
                    if (imageUrls.isNotEmpty()) {
                        saveCampaignToFirestore(campaignData, imageUrls)
                    } else {
                        showLoading(false)
                        showToast("All images failed to process")
                    }
                }
            }
        }
    }

    private fun saveCampaignToFirestore(campaignData: CampaignData, imageUrls: List<String>) {
        val campaignMap = hashMapOf<String, Any>(
            "ngoId" to campaignData.ngoId,
            "ngoName" to campaignData.ngoName,
            "category" to campaignData.category,
            "title" to campaignData.title,
            "description" to campaignData.description,
            "targetQuantity" to campaignData.targetQuantity,
            "currentQuantity" to 0,
            "unit" to campaignData.unit,
            "endDate" to campaignData.endDate,
            "urgencyLevel" to campaignData.urgencyLevel,
            "itemCondition" to (campaignData.itemCondition ?: ""),
            "specificRequirements" to campaignData.specificRequirements,
            "autoClose" to campaignData.autoClose,
            "imageUrls" to imageUrls,
            "createdAt" to campaignData.createdAt,
            "status" to campaignData.status,
            "donorCount" to 0,
            "shareCount" to 0
        )

        firestore.collection("campaigns")
            .add(campaignMap)
            .addOnSuccessListener {
                showLoading(false)
                showToast("Campaign created successfully!")
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showToast("Failed to create campaign: ${e.message}")
            }
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