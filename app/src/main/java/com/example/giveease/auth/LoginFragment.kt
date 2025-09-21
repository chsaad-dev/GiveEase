package com.example.giveease.auth

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentLoginBinding
import com.example.giveease.donor.DonorMainFragment
import com.example.giveease.ngo.NgoMainFragment
import com.example.giveease.admin.AdminMainFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var loadingDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupProgressDialog()
        setupFormValidation()
        setupClickListeners()

        return binding.root
    }

    private fun setupFormValidation() {
        // Real-time email validation
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateEmail(s.toString())
            }
        })

        // Real-time password validation
        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validatePassword(s.toString())
            }
        })
    }

    private fun validateEmail(email: String) {
        if (email.isNotEmpty() && !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            binding.tilEmail.error = "Enter a valid email address"
        } else {
            binding.tilEmail.error = null
        }
    }

    private fun validatePassword(password: String) {
        if (password.isNotEmpty() && password.length < 6) {
            binding.tilPassword.error = "Password must be at least 6 characters"
        } else {
            binding.tilPassword.error = null
        }
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            handleLogin()
        }

        binding.tvSignupLink.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SignupFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.tvForgotPassword.setOnClickListener {
            showForgotPasswordDialog()
        }

        binding.tvTermsLink.setOnClickListener {
            Toast.makeText(requireContext(), "Terms & Conditions", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to terms screen
        }

        binding.tvPrivacyLink.setOnClickListener {
            Toast.makeText(requireContext(), "Privacy Policy", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to privacy screen
        }

        binding.btnGoogleLogin.setOnClickListener {
            Toast.makeText(requireContext(), "Google Sign-In coming soon", Toast.LENGTH_SHORT).show()
            // TODO: Implement Google Sign-In
        }
    }

    private fun handleLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Clear previous errors
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        // Validation
        when {
            email.isEmpty() -> {
                binding.tilEmail.error = "Email is required"
                binding.etEmail.requestFocus()
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.tilEmail.error = "Enter a valid email address"
                binding.etEmail.requestFocus()
                return
            }
            password.isEmpty() -> {
                binding.tilPassword.error = "Password is required"
                binding.etPassword.requestFocus()
                return
            }
            password.length < 6 -> {
                binding.tilPassword.error = "Password must be at least 6 characters"
                binding.etPassword.requestFocus()
                return
            }
            !binding.cbTerms.isChecked -> {
                Toast.makeText(requireContext(), "Please agree to Terms & Conditions and Privacy Policy", Toast.LENGTH_LONG).show()
                return
            }
        }

        loadingDialog.show()

        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user?.isEmailVerified == true) {
                        // Email is verified, proceed with login
                        val uid = user.uid
                        firestore.collection("users").document(uid).get()
                            .addOnSuccessListener { document ->
                                loadingDialog.dismiss()
                                if (document.exists()) {
                                    val role = document.getString("role") ?: "donor"
                                    // Update email verification status in Firestore
                                    updateEmailVerificationStatus(uid)
                                    loadMainFragment(role)
                                    Toast.makeText(requireContext(), "Welcome back!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .addOnFailureListener {
                                loadingDialog.dismiss()
                                Toast.makeText(requireContext(), "Error fetching user data: ${it.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        // Email not verified
                        loadingDialog.dismiss()
                        showEmailVerificationDialog(user?.email ?: email)
                    }
                } else {
                    loadingDialog.dismiss()
                    val errorMessage = when {
                        task.exception?.message?.contains("no user record") == true ->
                            "No account found with this email. Please sign up first."
                        task.exception?.message?.contains("wrong-password") == true ->
                            "Incorrect password. Please try again."
                        task.exception?.message?.contains("invalid-email") == true ->
                            "Invalid email address format."
                        task.exception?.message?.contains("user-disabled") == true ->
                            "This account has been disabled. Please contact support."
                        task.exception?.message?.contains("too-many-requests") == true ->
                            "Too many failed attempts. Please try again later."
                        else -> "Login failed: ${task.exception?.message}"
                    }
                    showError("Login Error", errorMessage)
                }
            }
    }

    private fun showForgotPasswordDialog() {
        val builder = AlertDialog.Builder(requireContext())
        val input = android.widget.EditText(requireContext()).apply {
            hint = "Enter your email address"
            inputType = android.text.InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
        }

        builder.setTitle("Reset Password")
            .setMessage("Enter your email address to receive password reset instructions:")
            .setView(input)
            .setPositiveButton("Send") { _, _ ->
                val email = input.text.toString().trim()
                if (email.isNotEmpty() && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                    sendPasswordResetEmail(email)
                } else {
                    Toast.makeText(requireContext(), "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendPasswordResetEmail(email: String) {
        loadingDialog.show()
        auth.sendPasswordResetEmail(email)
            .addOnCompleteListener { task ->
                loadingDialog.dismiss()
                if (task.isSuccessful) {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Reset Email Sent")
                        .setMessage("Password reset instructions have been sent to $email\n\nCheck your email and follow the instructions to reset your password.")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    val errorMessage = when {
                        task.exception?.message?.contains("user-not-found") == true ->
                            "No account found with this email address."
                        else -> "Failed to send reset email: ${task.exception?.message}"
                    }
                    showError("Reset Error", errorMessage)
                }
            }
    }

    private fun updateEmailVerificationStatus(uid: String) {
        firestore.collection("users").document(uid)
            .update("emailVerified", true, "updatedAt", System.currentTimeMillis())
    }

    private fun showEmailVerificationDialog(email: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Email Not Verified")
            .setMessage("Please verify your email address to continue.\n\nCheck your email for a verification link, or we can send you a new one.")
            .setPositiveButton("Resend Verification") { _, _ ->
                resendVerificationEmail()
            }
            .setNegativeButton("Cancel") { _, _ ->
                auth.signOut() // Sign out unverified user
            }
            .setNeutralButton("I Verified") { _, _ ->
                checkEmailVerificationStatus()
            }
            .setCancelable(false)
            .show()
    }

    private fun resendVerificationEmail() {
        loadingDialog.show()
        auth.currentUser?.sendEmailVerification()
            ?.addOnCompleteListener { task ->
                loadingDialog.dismiss()
                if (task.isSuccessful) {
                    Toast.makeText(requireContext(), "Verification email sent", Toast.LENGTH_SHORT).show()
                    auth.signOut() // Sign out until verified
                } else {
                    Toast.makeText(requireContext(), "Failed to send email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                }
            }
    }

    private fun checkEmailVerificationStatus() {
        loadingDialog.show()
        auth.currentUser?.reload()?.addOnCompleteListener { task ->
            loadingDialog.dismiss()
            if (task.isSuccessful) {
                val user = auth.currentUser
                if (user?.isEmailVerified == true) {
                    // Now verified, proceed with login
                    val uid = user.uid
                    firestore.collection("users").document(uid).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                val role = document.getString("role") ?: "donor"
                                updateEmailVerificationStatus(uid)
                                loadMainFragment(role)
                                Toast.makeText(requireContext(), "Email verified successfully!", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    Toast.makeText(requireContext(), "Email still not verified. Please check your email.", Toast.LENGTH_SHORT).show()
                    auth.signOut()
                }
            } else {
                Toast.makeText(requireContext(), "Error checking verification status", Toast.LENGTH_SHORT).show()
                auth.signOut()
            }
        }
    }

    private fun showError(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    private fun setupProgressDialog() {
        loadingDialog = AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
    }

    private fun loadMainFragment(role: String) {
        val (fragment, tag) = when (role) {
            "donor" -> DonorMainFragment() to "DONOR_MAIN"
            "ngo" -> NgoMainFragment() to "NGO_MAIN"
            "admin" -> AdminMainFragment() to "ADMIN_MAIN"
            else -> DonorMainFragment() to "DONOR_MAIN"
        }

        val existingFragment = parentFragmentManager.findFragmentByTag(tag)
        if (existingFragment == null) {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, fragment, tag)
                .commit()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }
}