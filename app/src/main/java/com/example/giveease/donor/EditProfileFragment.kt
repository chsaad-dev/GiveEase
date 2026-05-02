package com.example.giveease.donor

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.giveease.R
import com.example.giveease.databinding.FragmentEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream
import java.util.UUID

class EditProfileFragment : Fragment() {
    private var _binding: FragmentEditProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var selectedImageUri: Uri? = null
    private var isDataChanged = false
    private lateinit var loadingDialog: AlertDialog
    private var currentProfileImageUrl: String? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let {
                binding.imgProfile.setImageURI(it)
                isDataChanged = true
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentEditProfileBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupProgressDialog()
        setupListeners()
        loadUserData()

        return binding.root
    }

    private fun setupProgressDialog() {
        loadingDialog = AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun setupListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                if (isDataChanged) {
                    showUnsavedChangesDialog()
                } else {
                    parentFragmentManager.popBackStack()
                }
            }

            btnSave.setOnClickListener {
                saveProfile()
            }

            btnSaveProfile.setOnClickListener {
                saveProfile()
            }

            btnCancel.setOnClickListener {
                if (isDataChanged) {
                    showUnsavedChangesDialog()
                } else {
                    parentFragmentManager.popBackStack()
                }
            }

            imgProfile.setOnClickListener {
                showImagePickerDialog()
            }

            btnChangePhoto.setOnClickListener {
                showImagePickerDialog()
            }

            etFullName.setOnFocusChangeListener { _, _ -> isDataChanged = true }
            etPhone.setOnFocusChangeListener { _, _ -> isDataChanged = true }
            etCity.setOnFocusChangeListener { _, _ -> isDataChanged = true }
            etBio.setOnFocusChangeListener { _, _ -> isDataChanged = true }

        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        if (!isAdded || _binding == null) return

        loadingDialog.show()

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (!isAdded || _binding == null) {
                    loadingDialog.dismiss()
                    return@addOnSuccessListener
                }

                loadingDialog.dismiss()
                if (document.exists()) {
                    binding.apply {
                        etFullName.setText(document.getString("name") ?: "")
                        etEmail.setText(document.getString("email") ?: "")
                        etPhone.setText(document.getString("phone") ?: "")
                        etCity.setText(document.getString("city") ?: "")
                        etBio.setText(document.getString("bio") ?: "")


                        currentProfileImageUrl = document.getString("profileImageUrl")
                        if (!currentProfileImageUrl.isNullOrEmpty()) {
                            context?.let { ctx ->
                                Glide.with(ctx)
                                    .load(currentProfileImageUrl)
                                    .placeholder(R.drawable.sample_profile)
                                    .error(R.drawable.sample_profile)
                                    .circleCrop()
                                    .into(imgProfile)
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                if (!isAdded || _binding == null) return@addOnFailureListener
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Error loading profile: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showImagePickerDialog() {
        val options = arrayOf("Choose from Gallery", "Remove Photo")
        AlertDialog.Builder(requireContext())
            .setTitle("Profile Photo")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> openImagePicker()
                    1 -> removeProfilePhoto()
                }
            }
            .show()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        imagePickerLauncher.launch(intent)
    }

    private fun removeProfilePhoto() {
        selectedImageUri = null
        binding.imgProfile.setImageResource(R.drawable.sample_profile)
        isDataChanged = true
    }

    private fun saveProfile() {
        val userId = auth.currentUser?.uid ?: return

        if (!validateInputs()) return

        loadingDialog.show()

        if (selectedImageUri != null) {
            uploadProfileImage(userId)
        } else {
            val userData = buildUserData(currentProfileImageUrl)
            updateUserData(userId, userData)
        }
    }

    private fun uploadProfileImage(userId: String) {
        selectedImageUri?.let { uri ->
            try {
                // Log for debugging
                android.util.Log.d("ProfileUpload", "User ID: $userId")
                android.util.Log.d("ProfileUpload", "Auth UID: ${auth.currentUser?.uid}")

                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                val compressedBytes = compressImage(originalBitmap)

                val fileName = "${System.currentTimeMillis()}.jpg"
                val storagePath = "profile_images/$userId/$fileName"

                // Log the path
                android.util.Log.d("ProfileUpload", "Storage path: $storagePath")

                val storageRef = storage.reference.child(storagePath)

                storageRef.putBytes(compressedBytes)
                    .addOnSuccessListener {
                        android.util.Log.d("ProfileUpload", "Upload successful")
                        storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            if (!isAdded || _binding == null) return@addOnSuccessListener

                            if (currentProfileImageUrl != null && currentProfileImageUrl!!.contains("profile_images/")) {
                                deleteOldProfileImage(currentProfileImageUrl!!)
                            }

                            val userData = buildUserData(downloadUri.toString())
                            updateUserData(userId, userData)
                        }
                    }
                    .addOnFailureListener { exception ->
                        if (!isAdded || _binding == null) return@addOnFailureListener
                        loadingDialog.dismiss()

                        // Detailed error logging
                        android.util.Log.e("ProfileUpload", "Upload failed: ${exception.message}", exception)
                        Toast.makeText(
                            requireContext(),
                            "Upload failed: ${exception.message}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
            } catch (e: Exception) {
                loadingDialog.dismiss()
                android.util.Log.e("ProfileUpload", "Processing error: ${e.message}", e)
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun compressImage(bitmap: Bitmap): ByteArray {
        val maxWidth = 1024
        val maxHeight = 1024

        val width = bitmap.width
        val height = bitmap.height

        val scale = Math.min(
            maxWidth.toFloat() / width,
            maxHeight.toFloat() / height
        )

        val scaledBitmap = if (scale < 1) {
            Bitmap.createScaledBitmap(
                bitmap,
                (width * scale).toInt(),
                (height * scale).toInt(),
                true
            )
        } else {
            bitmap
        }

        val outputStream = ByteArrayOutputStream()
        var quality = 85
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)

        while (outputStream.size() > 500 * 1024 && quality > 20) {
            outputStream.reset()
            quality -= 10
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }

        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }

        return outputStream.toByteArray()
    }

    private fun deleteOldProfileImage(imageUrl: String) {
        try {
            val oldImageRef = storage.getReferenceFromUrl(imageUrl)
            oldImageRef.delete()
        } catch (e: Exception) {
        }
    }

    private fun buildUserData(profileImageUrl: String?): HashMap<String, Any> {
        val userData = hashMapOf<String, Any>(
            "name" to binding.etFullName.text.toString().trim(),
            "phone" to binding.etPhone.text.toString().trim(),
            "city" to binding.etCity.text.toString().trim(),
            "bio" to binding.etBio.text.toString().trim(),
            "updatedAt" to System.currentTimeMillis()
        )

        if (profileImageUrl != null) {
            userData["profileImageUrl"] = profileImageUrl
        } else if (selectedImageUri == null && currentProfileImageUrl != null) {
            userData["profileImageUrl"] = ""
        }

        return userData
    }

    private fun showUnsavedChangesDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Unsaved Changes")
            .setMessage("You have unsaved changes. Do you want to save them?")
            .setPositiveButton("Save") { _, _ ->
                saveProfile()
            }
            .setNegativeButton("Discard") { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .setNeutralButton("Cancel", null)
            .show()
    }

    private fun updateUserData(userId: String, userData: HashMap<String, Any>) {
        firestore.collection("users").document(userId)
            .update(userData)
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                isDataChanged = false
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { exception ->
                if (!isAdded || _binding == null) return@addOnFailureListener
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Error updating profile: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun validateInputs(): Boolean {
        binding.apply {
            val name = etFullName.text.toString().trim()
            val phone = etPhone.text.toString().trim()

            if (name.isEmpty()) {
                etFullName.error = "Name is required"
                etFullName.requestFocus()
                return false
            }

            if (phone.isNotEmpty() && phone.length < 11) {
                etPhone.error = "Enter valid phone number"
                etPhone.requestFocus()
                return false
            }

            return true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
        _binding = null
    }
}