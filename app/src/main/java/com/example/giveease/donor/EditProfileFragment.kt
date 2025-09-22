package com.example.giveease.donor

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.giveease.databinding.FragmentEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID

class EditProfileFragment : Fragment() {
    private lateinit var binding: FragmentEditProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private var selectedImageUri: Uri? = null
    private var isDataChanged = false
    private lateinit var loadingDialog: AlertDialog

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
        binding = FragmentEditProfileBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupProgressDialog()
        setupListeners()
        loadUserData()

        return binding.root
    }

    private fun setupProgressDialog() {
        loadingDialog = AlertDialog.Builder(requireContext())
            .setView(com.example.giveease.R.layout.dialog_loading)
            .setCancelable(false)
            .create()
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
                Toast.makeText(requireContext(), "Photo upload feature coming soon", Toast.LENGTH_SHORT).show()
                // Temporarily disabled until Firebase Storage is available
            }

            btnChangePhoto.setOnClickListener {
                Toast.makeText(requireContext(), "Photo upload feature coming soon", Toast.LENGTH_SHORT).show()
            }

            etFullName.setOnFocusChangeListener { _, _ -> isDataChanged = true }
            etPhone.setOnFocusChangeListener { _, _ -> isDataChanged = true }
            etCity.setOnFocusChangeListener { _, _ -> isDataChanged = true }
            etBio.setOnFocusChangeListener { _, _ -> isDataChanged = true }

            switchEmailNotifications.setOnCheckedChangeListener { _, _ -> isDataChanged = true }
            switchPushNotifications.setOnCheckedChangeListener { _, _ -> isDataChanged = true }
        }
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        loadingDialog.show()

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                loadingDialog.dismiss()
                if (document.exists()) {
                    binding.apply {
                        etFullName.setText(document.getString("name") ?: "")
                        etEmail.setText(document.getString("email") ?: "")
                        etPhone.setText(document.getString("phone") ?: "")
                        etCity.setText(document.getString("city") ?: "")
                        etBio.setText(document.getString("bio") ?: "")

                        switchEmailNotifications.isChecked = document.getBoolean("emailNotifications") ?: true
                        switchPushNotifications.isChecked = document.getBoolean("pushNotifications") ?: true

                        val profileImageUrl = document.getString("profileImageUrl")
                        if (!profileImageUrl.isNullOrEmpty()) {
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Error loading profile: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveProfile() {
        val userId = auth.currentUser?.uid ?: return

        if (!validateInputs()) return

        loadingDialog.show()

        val userData = hashMapOf<String, Any>(
            "name" to binding.etFullName.text.toString().trim(),
            "phone" to binding.etPhone.text.toString().trim(),
            "city" to binding.etCity.text.toString().trim(),
            "bio" to binding.etBio.text.toString().trim(),
            "emailNotifications" to binding.switchEmailNotifications.isChecked,
            "pushNotifications" to binding.switchPushNotifications.isChecked,
            "updatedAt" to System.currentTimeMillis()
        )

        updateUserData(userId, userData)
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
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
                isDataChanged = false
                parentFragmentManager.popBackStack()
            }
            .addOnFailureListener { exception ->
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

    private fun showImagePickerDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Photo Upload")
            .setMessage("Photo upload feature will be available when Firebase Storage is enabled.")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }
}