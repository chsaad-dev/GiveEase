package com.example.giveease.donor

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentChangeEmailBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class ChangeEmailFragment : Fragment() {
    private lateinit var binding: FragmentChangeEmailBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var loadingDialog: AlertDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentChangeEmailBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupProgressDialog()
        setupListeners()
        loadCurrentEmail()

        return binding.root
    }

    private fun setupProgressDialog() {
        loadingDialog = AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
    }

    private fun loadCurrentEmail() {
        val currentEmail = auth.currentUser?.email
        binding.tvCurrentEmail.text = currentEmail ?: "No email found"
    }

    private fun setupListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            btnCancel.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            btnChangeEmail.setOnClickListener {
                initiateEmailChange()
            }

            etNewEmail.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    validateNewEmail(s.toString())
                }
            })
        }
    }

    private fun validateNewEmail(email: String) {
        binding.apply {
            val currentEmail = auth.currentUser?.email

            when {
                email.isEmpty() -> tilNewEmail.error = null
                !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() ->
                    tilNewEmail.error = "Enter a valid email address"
                email.equals(currentEmail, ignoreCase = true) ->
                    tilNewEmail.error = "New email must be different from current email"
                else -> tilNewEmail.error = null
            }
        }
    }

    private fun initiateEmailChange() {
        if (!validateInputs()) return

        val currentPassword = binding.etCurrentPassword.text.toString()
        val newEmail = binding.etNewEmail.text.toString().trim()
        val currentUser = auth.currentUser

        if (currentUser?.email == null) {
            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            return
        }

        loadingDialog.show()

        val credential = EmailAuthProvider.getCredential(currentUser.email!!, currentPassword)

        currentUser.reauthenticate(credential)
            .addOnCompleteListener { reAuthTask ->
                if (reAuthTask.isSuccessful) {
                    currentUser.verifyBeforeUpdateEmail(newEmail)
                        .addOnCompleteListener { verifyTask ->
                            loadingDialog.dismiss()
                            if (verifyTask.isSuccessful) {
                                showEmailVerificationDialog(newEmail)
                            } else {
                                val errorMessage = when {
                                    verifyTask.exception?.message?.contains("already in use") == true ->
                                        "This email address is already in use by another account"
                                    verifyTask.exception?.message?.contains("invalid-email") == true ->
                                        "Invalid email address format"
                                    else -> "Failed to send verification email: ${verifyTask.exception?.message}"
                                }
                                showError(errorMessage)
                            }
                        }
                } else {
                    loadingDialog.dismiss()
                    binding.tilCurrentPassword.error = "Current password is incorrect"
                }
            }
    }

    private fun validateInputs(): Boolean {
        binding.apply {
            val currentPassword = etCurrentPassword.text.toString()
            val newEmail = etNewEmail.text.toString().trim()
            val currentEmail = auth.currentUser?.email

            tilCurrentPassword.error = null
            tilNewEmail.error = null

            if (currentPassword.isEmpty()) {
                tilCurrentPassword.error = "Current password is required"
                etCurrentPassword.requestFocus()
                return false
            }

            if (newEmail.isEmpty()) {
                tilNewEmail.error = "New email is required"
                etNewEmail.requestFocus()
                return false
            }

            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(newEmail).matches()) {
                tilNewEmail.error = "Enter a valid email address"
                etNewEmail.requestFocus()
                return false
            }

            if (newEmail.equals(currentEmail, ignoreCase = true)) {
                tilNewEmail.error = "New email must be different from current email"
                etNewEmail.requestFocus()
                return false
            }

            return true
        }
    }

    private fun showEmailVerificationDialog(newEmail: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Verification Email Sent")
            .setMessage("We've sent a verification link to $newEmail\n\nPlease check your email and click the verification link to complete the email change.\n\nAfter verification, you'll need to sign in again with your new email address.")
            .setPositiveButton("OK") { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .setNeutralButton("Resend Email") { _, _ ->
                resendVerificationEmail(newEmail)
            }
            .setCancelable(false)
            .show()
    }

    private fun resendVerificationEmail(newEmail: String) {
        loadingDialog.show()
        auth.currentUser?.verifyBeforeUpdateEmail(newEmail)
            ?.addOnCompleteListener { task ->
                loadingDialog.dismiss()
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Verification email sent again", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to resend email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun showError(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Email Change Error")
            .setMessage(message)
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