package com.example.giveease.verification

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.giveease.databinding.FragmentIdentityVerificationBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.example.giveease.utils.NotificationHelper
import java.util.UUID

class IdentityVerificationFragment : Fragment() {

    private var _binding: FragmentIdentityVerificationBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var loadingDialog: AlertDialog
    private var selectedFileUri: Uri? = null
    private var userRole: String = "donor"

    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            selectedFileUri = result.data?.data
            binding.tvFileName.text = selectedFileUri?.lastPathSegment ?: "File selected"
            binding.btnUpload.isEnabled = true
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentIdentityVerificationBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupProgressDialog()
        loadUserRole()
        setupClickListeners()

        return binding.root
    }

    private fun loadUserRole() {
        val uid = auth.currentUser?.uid ?: return
        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                userRole = document.getString("role") ?: "donor"
                updateUIForRole()
            }
    }

    private fun updateUIForRole() {
        if (userRole == "ngo") {
            binding.tvTitle.text = "NGO Verification"
            binding.tvDescription.text = "Upload your government registration documents and provide your registration number"
            binding.tilRegistrationNumber.visibility = View.VISIBLE
            binding.tvDocumentHint.text = "Required: Registration certificate, tax exemption documents"
        } else {
            binding.tvTitle.text = "Identity Verification"
            binding.tvDescription.text = "Upload a government-issued ID (CNIC, Passport, Driving License)"
            binding.tilRegistrationNumber.visibility = View.GONE
            binding.tvDocumentHint.text = "Accepted: CNIC (front & back), Passport, Driving License"
        }
    }

    private fun setupClickListeners() {
        binding.btnSelectFile.setOnClickListener {
            openFilePicker()
        }

        binding.btnUpload.setOnClickListener {
            if (validateInput()) {
                uploadDocument()
            }
        }

        binding.btnSkipForNow.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "*/*"
            addCategory(Intent.CATEGORY_OPENABLE)
            putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/*", "application/pdf"))
        }
        filePickerLauncher.launch(Intent.createChooser(intent, "Select Document"))
    }

    private fun validateInput(): Boolean {
        if (selectedFileUri == null) {
            Toast.makeText(requireContext(), "Please select a document", Toast.LENGTH_SHORT).show()
            return false
        }

        if (userRole == "ngo") {
            val regNumber = binding.etRegistrationNumber.text.toString().trim()
            if (regNumber.isEmpty()) {
                binding.tilRegistrationNumber.error = "Registration number required"
                return false
            }
        }

        return true
    }

    private fun uploadDocument() {
        loadingDialog.show()

        val uid = auth.currentUser?.uid ?: return
        val fileUri = selectedFileUri ?: return
        val fileName = "verification_${uid}_${UUID.randomUUID()}.${getFileExtension(fileUri)}"
        val storageRef = storage.reference.child("verification_documents/$fileName")

        storageRef.putFile(fileUri)
            .addOnSuccessListener { taskSnapshot ->
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    saveVerificationData(downloadUrl.toString())
                }
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Upload failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveVerificationData(documentUrl: String) {
        val uid = auth.currentUser?.uid ?: return
        val updateMap = mutableMapOf<String, Any>(
            "verificationStatus" to "pending",
            "updatedAt" to System.currentTimeMillis()
        )

        if (userRole == "ngo") {
            val regNumber = binding.etRegistrationNumber.text.toString().trim()
            updateMap["registrationNumber"] = regNumber
            updateMap["governmentDocumentUrl"] = documentUrl
        } else {
            updateMap["identityDocumentUrl"] = documentUrl
        }

        firestore.collection("users").document(uid)
            .update(updateMap)
            .addOnSuccessListener {
                loadingDialog.dismiss()
                notifyAdmins(uid)
                showSuccessDialog()
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                Toast.makeText(requireContext(), "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun notifyAdmins(userId: String) {
        firestore.collection("users")
            .whereEqualTo("role", "admin")
            .get()
            .addOnSuccessListener { documents ->
                val userName = auth.currentUser?.displayName ?: "A user"
                val roleLabel = if (userRole == "ngo") "NGO" else "Donor"
                for (doc in documents) {
                    NotificationHelper.sendNotification(
                        userId = doc.id,
                        title = "New Verification Request 📋",
                        message = "$userName ($roleLabel) has submitted verification documents for review.",
                        type = "verification",
                        referenceId = userId
                    )
                }
            }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Verification Submitted")
            .setMessage("Your documents have been submitted for verification. You'll be notified once approved by our admin team.\n\nYou can browse campaigns but cannot donate or create campaigns until verified.")
            .setPositiveButton("OK") { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    private fun getFileExtension(uri: Uri): String {
        return requireContext().contentResolver.getType(uri)?.split("/")?.last() ?: "jpg"
    }

    private fun setupProgressDialog() {
        loadingDialog = AlertDialog.Builder(requireContext())
            .setView(com.example.giveease.R.layout.dialog_loading)
            .setCancelable(false)
            .create()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }
}