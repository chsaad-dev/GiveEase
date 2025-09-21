package com.example.giveease.auth

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentSignupBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SignupFragment : Fragment() {

    private lateinit var binding: FragmentSignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var loadingDialog: AlertDialog
    private var selectedRole: String = "donor" // Default role

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignupBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupProgressDialog()
        setupRoleSelection()
        setupClickListeners()
        setupFormValidation()

        return binding.root
    }

    private fun setupRoleSelection() {
        // Set initial state - Donor selected by default
        updateRoleSelection("donor")

        binding.cardDonor.setOnClickListener {
            updateRoleSelection("donor")
        }

        binding.cardNGO.setOnClickListener {
            updateRoleSelection("ngo")
        }
    }

    private fun updateRoleSelection(role: String) {
        selectedRole = role

        if (role == "donor") {
            // Donor selected
            binding.cardDonor.apply {
                strokeColor = ContextCompat.getColor(requireContext(), R.color.primary)
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
            }
            binding.cardNGO.apply {
                strokeColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
        } else {
            // NGO selected
            binding.cardNGO.apply {
                strokeColor = ContextCompat.getColor(requireContext(), R.color.primary)
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), R.color.primary))
            }
            binding.cardDonor.apply {
                strokeColor = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
                setCardBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))
            }
        }
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
        when {
            password.isEmpty() -> binding.tilPassword.error = null
            password.length < 6 -> binding.tilPassword.error = "Password must be at least 6 characters"
            else -> binding.tilPassword.error = null
        }
    }

    private fun setupClickListeners() {
        binding.btnSignup.setOnClickListener {
            handleSignup()
        }

        binding.tvLoginLink.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.tvTermsLink.setOnClickListener {
            Toast.makeText(requireContext(), "Terms & Conditions", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to terms screen
        }

        binding.tvPrivacyLink.setOnClickListener {
            Toast.makeText(requireContext(), "Privacy Policy", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to privacy screen
        }
    }

    private fun handleSignup() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Clear previous errors
        binding.tilEmail.error = null
        binding.tilPassword.error = null

        // Validation
        when {
            name.isEmpty() -> {
                binding.etName.error = "Name is required"
                binding.etName.requestFocus()
                return
            }
            name.length < 2 -> {
                binding.etName.error = "Name must be at least 2 characters"
                binding.etName.requestFocus()
                return
            }
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
            !binding.cbAgree.isChecked -> {
                Toast.makeText(requireContext(), "Please agree to Terms & Conditions and Privacy Policy", Toast.LENGTH_LONG).show()
                return
            }
        }

        loadingDialog.show()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    user?.sendEmailVerification()
                        ?.addOnCompleteListener { verificationTask ->
                            if (verificationTask.isSuccessful) {
                                // Save user data to Firestore
                                saveUserToFirestore(user.uid, name, email, selectedRole)
                            } else {
                                loadingDialog.dismiss()
                                showError("Failed to send verification email: ${verificationTask.exception?.message}")
                            }
                        }
                } else {
                    loadingDialog.dismiss()
                    val errorMessage = when {
                        task.exception?.message?.contains("email address is already in use") == true ->
                            "This email is already registered. Please use a different email or login."
                        task.exception?.message?.contains("weak password") == true ->
                            "Please choose a stronger password"
                        else -> task.exception?.message ?: "Registration failed"
                    }
                    showError(errorMessage)
                }
            }
    }

    private fun saveUserToFirestore(uid: String, name: String, email: String, role: String) {
        val userMap = hashMapOf<String, Any>(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "role" to role,
            "emailVerified" to false,
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                loadingDialog.dismiss()
                showEmailVerificationDialog(email)
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                showError("Failed to create profile: ${e.message}")
            }
    }

    private fun showEmailVerificationDialog(email: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Verify Your Email")
            .setMessage("We've sent a verification link to $email.\n\nPlease check your email and click the verification link to activate your account.\n\nAfter verification, you can login to your account.")
            .setPositiveButton("OK") { _, _ ->
                // Sign out user until they verify email
                auth.signOut()
                navigateToLogin()
            }
            .setNeutralButton("Resend Email") { _, _ ->
                resendVerificationEmail()
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
                    Toast.makeText(requireContext(), "Verification email sent again", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(requireContext(), "Failed to resend email: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun navigateToLogin() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment())
            .commit()
    }

    private fun showError(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Registration Error")
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

    override fun onDestroyView() {
        super.onDestroyView()
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }

    companion object {
        fun newInstance() = SignupFragment()
    }
}