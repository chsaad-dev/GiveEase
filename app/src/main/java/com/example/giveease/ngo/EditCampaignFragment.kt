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
import com.example.giveease.databinding.FragmentEditCampaignBinding
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.ByteArrayOutputStream

class EditCampaignFragment : Fragment() {

    private var _binding: FragmentEditCampaignBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage

    private lateinit var campaign: CampaignData
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
        _binding = FragmentEditCampaignBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        campaign = arguments?.getSerializable("campaign") as? CampaignData
            ?: return binding.root

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupSpinners()
        setupClickListeners()
        setupTextWatchers()
        loadCampaignData()
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

    private fun loadCampaignData() {
        binding.apply {
            // Set category
            val categoryPosition = (spinnerCategory.adapter as ArrayAdapter<String>)
                .getPosition(campaign.category)
            spinnerCategory.setSelection(categoryPosition)

            // Set basic info
            etTitle.setText(campaign.title)
            etDescription.setText(campaign.description)
            etTargetQuantity.setText(campaign.targetQuantity.toString())

            // Set unit
            val unitPosition = (spinnerUnit.adapter as ArrayAdapter<String>)
                .getPosition(campaign.unit)
            spinnerUnit.setSelection(unitPosition)

            // Set end date
            selectedEndDate = campaign.endDate
            val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
            etEndDate.setText(sdf.format(Date(campaign.endDate)))

            // Set urgency
            when (campaign.urgencyLevel) {
                "Low" -> chipLow.isChecked = true
                "Medium" -> chipMedium.isChecked = true
                "High" -> chipHigh.isChecked = true
                "Emergency" -> chipEmergency.isChecked = true
            }

            // Set condition
            when (campaign.itemCondition) {
                "New" -> chipNew.isChecked = true
                "Used - Good" -> chipUsed.isChecked = true
                "Refurbished" -> chipRefurbished.isChecked = true
            }

            // Set specific requirements
            etSpecificNeeds.setText(campaign.specificRequirements)

            // Set auto-close
            switchAutoClose.isChecked = campaign.autoClose
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnAddImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        binding.etEndDate.setOnClickListener {
            showDatePicker()
        }

        binding.btnUpdateCampaign.setOnClickListener {
            updateCampaign()
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
        calendar.timeInMillis = selectedEndDate

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
            datePicker.minDate = System.currentTimeMillis()
            show()
        }
    }

    private fun updateImageCount() {
        Toast.makeText(requireContext(), "${selectedImages.size} image(s) selected", Toast.LENGTH_SHORT).show()
    }

    private fun updateCampaign() {
        if (!validateInputs()) return

        showLoading(true)

        val updatedData = collectUpdatedData()

        if (selectedImages.isNotEmpty()) {
            uploadNewImages(updatedData)
        } else {
            saveCampaignToFirestore(updatedData, campaign.imageUrls)
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

    private fun collectUpdatedData(): HashMap<String, Any> {
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
            else -> ""
        }

        return hashMapOf(
            "category" to binding.spinnerCategory.selectedItem.toString(),
            "title" to binding.etTitle.text.toString().trim(),
            "description" to binding.etDescription.text.toString().trim(),
            "targetQuantity" to binding.etTargetQuantity.text.toString().toInt(),
            "unit" to binding.spinnerUnit.selectedItem.toString(),
            "endDate" to selectedEndDate,
            "urgencyLevel" to urgency,
            "itemCondition" to condition,
            "specificRequirements" to binding.etSpecificNeeds.text.toString().trim(),
            "autoClose" to binding.switchAutoClose.isChecked,
            "updatedAt" to System.currentTimeMillis()
        )
    }

    private fun compressImage(uri: Uri): ByteArray {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()

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

        val outputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        bitmap.recycle()

        return outputStream.toByteArray()
    }

    private fun uploadNewImages(updatedData: HashMap<String, Any>) {
        val imageUrls = mutableListOf<String>()
        var uploadCount = 0

        selectedImages.forEachIndexed { index, uri ->
            try {
                val compressedImage = compressImage(uri)

                val ref = storage.reference
                    .child("campaigns/${campaign.ngoId}/${System.currentTimeMillis()}_$index.jpg")

                ref.putBytes(compressedImage)
                    .addOnSuccessListener {
                        ref.downloadUrl.addOnSuccessListener { downloadUri ->
                            imageUrls.add(downloadUri.toString())
                            uploadCount++

                            if (uploadCount == selectedImages.size) {
                                saveCampaignToFirestore(updatedData, imageUrls)
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        uploadCount++
                        if (uploadCount == selectedImages.size) {
                            if (imageUrls.isNotEmpty()) {
                                saveCampaignToFirestore(updatedData, imageUrls)
                            } else {
                                showLoading(false)
                                showToast("Image upload failed")
                            }
                        }
                    }
            } catch (e: Exception) {
                uploadCount++
                if (uploadCount == selectedImages.size) {
                    saveCampaignToFirestore(updatedData, campaign.imageUrls)
                }
            }
        }
    }

    private fun saveCampaignToFirestore(updatedData: HashMap<String, Any>, imageUrls: List<String>) {
        updatedData["imageUrls"] = imageUrls

        firestore.collection("campaigns").document(campaign.id)
            .update(updatedData)
            .addOnSuccessListener {
                showLoading(false)
                showToast("Campaign updated successfully!")
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                showLoading(false)
                showToast("Failed to update: ${e.message}")
            }
    }

    private fun showLoading(show: Boolean) {
        binding.btnUpdateCampaign.isEnabled = !show
        binding.btnUpdateCampaign.text = if (show) "Updating..." else "Update Campaign"
    }

    private fun showToast(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}