package com.example.giveease.admin

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.giveease.R
import com.example.giveease.databinding.FragmentAdminProfileBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.storage.FirebaseStorage
import java.io.ByteArrayOutputStream

class AdminProfileFragment : Fragment() {

    private var _binding: FragmentAdminProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private var selectedImageUri: Uri? = null
    private var currentProfileImageUrl: String? = null
    private lateinit var loadingDialog: AlertDialog
    private lateinit var viewModel: AdminProfileViewModel

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedImageUri = result.data?.data
            selectedImageUri?.let {
                binding.ivAdminPhoto.apply {
                    setImageURI(it)
                    setPadding(0, 0, 0, 0)
                    imageTintList = null
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                }
            }
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminProfileBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[AdminProfileViewModel::class.java]

        setupProgressDialog()
        setupClickListeners()
        observeViewModel()
        
        viewModel.loadProfileData(auth.currentUser?.uid, auth.currentUser?.email)
    }

    private fun observeViewModel() {
        viewModel.profileData.observe(viewLifecycleOwner) { data ->
            binding.etAdminName.setText(data.name)
            binding.etAdminEmail.setText(data.email)
            binding.etAdminPhone.setText(data.phone)

            binding.tvAdminNameDisplay.text = data.name.ifEmpty { "Admin User" }
            binding.tvAdminEmailDisplay.text = data.email

            currentProfileImageUrl = data.profileImageUrl
            if (!data.profileImageUrl.isNullOrEmpty()) {
                context?.let { ctx ->
                    binding.ivAdminPhoto.apply {
                        setPadding(0, 0, 0, 0)
                        imageTintList = null
                        scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                    }
                    Glide.with(ctx)
                        .load(data.profileImageUrl)
                        .placeholder(R.drawable.ic_profile)
                        .error(R.drawable.ic_profile)
                        .circleCrop()
                        .into(binding.ivAdminPhoto)
                }
            } else {
                binding.ivAdminPhoto.apply {
                    setImageResource(R.drawable.ic_profile)
                    val pad = (30 * resources.displayMetrics.density).toInt()
                    setPadding(pad, pad, pad, pad)
                    imageTintList = android.content.res.ColorStateList.valueOf(0xFF1565C0.toInt())
                    scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
                }
            }
        }
    }

    private fun setupProgressDialog() {
        loadingDialog = AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun setupClickListeners() {
        binding.btnChangePhoto.setOnClickListener {
            showImagePickerDialog()
        }

        binding.ivAdminPhoto.setOnClickListener {
            showImagePickerDialog()
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnSaveChanges.setOnClickListener {
            saveProfile()
        }
    }

    // ========== FEATURE 1: LOAD & SAVE PROFILE DATA ==========

    // Data loading logic has been moved to AdminProfileViewModel to easily provide caching


    private fun saveProfile() {
        val uid = auth.currentUser?.uid ?: return

        val name = binding.etAdminName.text.toString().trim()
        val phone = binding.etAdminPhone.text.toString().trim()

        if (name.isEmpty()) {
            binding.etAdminName.error = "Name is required"
            binding.etAdminName.requestFocus()
            return
        }

        loadingDialog.show()

        if (selectedImageUri != null) {
            uploadProfileImage(uid, name, phone)
        } else {
            val userData = buildUserData(name, phone, currentProfileImageUrl)
            updateUserData(uid, userData)
        }
    }

    private fun buildUserData(name: String, phone: String, imageUrl: String?): HashMap<String, Any> {
        val data = hashMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "updatedAt" to System.currentTimeMillis()
        )

        if (imageUrl != null) {
            data["profileImageUrl"] = imageUrl
        } else if (selectedImageUri == null && currentProfileImageUrl != null) {
            data["profileImageUrl"] = ""
        }

        return data
    }

    private fun updateUserData(uid: String, userData: HashMap<String, Any>) {
        firestore.collection("users").document(uid)
            .update(userData)
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener
                loadingDialog.dismiss()

                // Update display fields locally in ViewModel
                val updatedData = AdminProfileData(
                    name = userData["name"] as? String ?: "",
                    phone = userData["phone"] as? String ?: "",
                    email = binding.etAdminEmail.text.toString(),
                    profileImageUrl = userData["profileImageUrl"] as? String ?: currentProfileImageUrl
                )
                viewModel.updateLocalData(updatedData)

                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // ========== FEATURE 2: PROFILE PICTURE UPLOAD ==========

    private fun showImagePickerDialog() {
        val options = arrayOf("Choose from Gallery", "Remove Photo")
        MaterialAlertDialogBuilder(requireContext())
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
        currentProfileImageUrl = null
        binding.ivAdminPhoto.apply {
            setImageResource(R.drawable.ic_profile)
            val pad = (30 * resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            imageTintList = android.content.res.ColorStateList.valueOf(0xFF1565C0.toInt())
            scaleType = android.widget.ImageView.ScaleType.CENTER_CROP
        }
    }

    private fun uploadProfileImage(uid: String, name: String, phone: String) {
        selectedImageUri?.let { uri ->
            try {
                val inputStream = requireContext().contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()

                val compressedBytes = compressImage(originalBitmap)

                val fileName = "${System.currentTimeMillis()}.jpg"
                val storagePath = "profile_images/$uid/$fileName"
                val storageRef = storage.reference.child(storagePath)

                storageRef.putBytes(compressedBytes)
                    .addOnSuccessListener {
                        storageRef.downloadUrl.addOnSuccessListener { downloadUri ->
                            if (!isAdded || _binding == null) return@addOnSuccessListener

                            // Delete old image if exists
                            if (!currentProfileImageUrl.isNullOrEmpty() &&
                                currentProfileImageUrl!!.contains("profile_images/")) {
                                try {
                                    storage.getReferenceFromUrl(currentProfileImageUrl!!).delete()
                                } catch (_: Exception) {}
                            }

                            val userData = buildUserData(name, phone, downloadUri.toString())
                            updateUserData(uid, userData)
                        }
                    }
                    .addOnFailureListener { e ->
                        if (!isAdded || _binding == null) return@addOnFailureListener
                        loadingDialog.dismiss()
                        Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_LONG).show()
                    }
            } catch (e: Exception) {
                loadingDialog.dismiss()
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

    // ========== FEATURE 3: CHANGE PASSWORD ==========

    private fun showChangePasswordDialog() {
        if (!isAdded) return

        val layout = LinearLayout(requireContext()).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(60, 40, 60, 20)
        }

        val params = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply { topMargin = 24 }

        // Old Password with eye toggle
        val tilOld = com.google.android.material.textfield.TextInputLayout(requireContext()).apply {
            hint = "Current Password"
            endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(16f, 16f, 16f, 16f)
        }
        val etOldPassword = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        tilOld.addView(etOldPassword)

        // New Password with eye toggle
        val tilNew = com.google.android.material.textfield.TextInputLayout(requireContext()).apply {
            hint = "New Password"
            endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(16f, 16f, 16f, 16f)
            layoutParams = params
        }
        val etNewPassword = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        tilNew.addView(etNewPassword)

        // Confirm Password with eye toggle
        val tilConfirm = com.google.android.material.textfield.TextInputLayout(requireContext()).apply {
            hint = "Confirm New Password"
            endIconMode = com.google.android.material.textfield.TextInputLayout.END_ICON_PASSWORD_TOGGLE
            boxBackgroundMode = com.google.android.material.textfield.TextInputLayout.BOX_BACKGROUND_OUTLINE
            setBoxCornerRadii(16f, 16f, 16f, 16f)
            layoutParams = params
        }
        val etConfirmPassword = com.google.android.material.textfield.TextInputEditText(requireContext()).apply {
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        tilConfirm.addView(etConfirmPassword)

        layout.addView(tilOld)
        layout.addView(tilNew)
        layout.addView(tilConfirm)

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Change Password")
            .setMessage("Enter your current password to verify, then set a new one.")
            .setView(layout)
            .setPositiveButton("Change") { _, _ ->
                val oldPass = etOldPassword.text.toString()
                val newPass = etNewPassword.text.toString()
                val confirmPass = etConfirmPassword.text.toString()

                when {
                    oldPass.isEmpty() -> {
                        Toast.makeText(requireContext(), "Enter current password", Toast.LENGTH_SHORT).show()
                    }
                    newPass.isEmpty() -> {
                        Toast.makeText(requireContext(), "Enter new password", Toast.LENGTH_SHORT).show()
                    }
                    newPass.length < 6 -> {
                        Toast.makeText(requireContext(), "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                    }
                    newPass != confirmPass -> {
                        Toast.makeText(requireContext(), "Passwords do not match", Toast.LENGTH_SHORT).show()
                    }
                    oldPass == newPass -> {
                        Toast.makeText(requireContext(), "New password must be different", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        changePassword(oldPass, newPass)
                    }
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun changePassword(oldPassword: String, newPassword: String) {
        val user = auth.currentUser ?: return
        val email = user.email ?: return

        loadingDialog.show()

        // Re-authenticate first
        val credential = EmailAuthProvider.getCredential(email, oldPassword)
        user.reauthenticate(credential)
            .addOnSuccessListener {
                // Now update password
                user.updatePassword(newPassword)
                    .addOnSuccessListener {
                        if (!isAdded || _binding == null) return@addOnSuccessListener
                        loadingDialog.dismiss()
                        Toast.makeText(requireContext(), "Password changed successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        if (!isAdded || _binding == null) return@addOnFailureListener
                        loadingDialog.dismiss()
                        Toast.makeText(requireContext(), "Failed to update: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Current password is incorrect", Toast.LENGTH_SHORT).show()
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