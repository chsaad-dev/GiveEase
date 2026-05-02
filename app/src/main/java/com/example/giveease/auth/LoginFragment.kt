package com.example.giveease.auth

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentLoginBinding
import com.example.giveease.donor.DonorMainFragment
import com.example.giveease.ngo.NgoMainFragment
import com.example.giveease.admin.AdminMainFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class LoginFragment : Fragment() {

    private lateinit var binding: FragmentLoginBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var loadingDialog: AlertDialog
    private lateinit var googleSignInClient: GoogleSignInClient

    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account?.idToken?.let { idToken ->
                firebaseAuthWithGoogle(idToken)
            } ?: run {
                loadingDialog.dismiss()
                showError("Google Sign-In Error", "Failed to get ID token from Google.")
            }
        } catch (e: ApiException) {
            loadingDialog.dismiss()
            Log.e("LoginFragment", "Google sign in failed: ${e.statusCode}", e)
            val errorMessage = when (e.statusCode) {
                12500 -> "Google Sign-In failed. Please make sure Google Play Services is up to date."
                12501 -> "Sign-In cancelled."
                10 -> "Developer error: SHA-1 fingerprint not configured in Firebase. Please contact support."
                else -> "Google Sign-In failed (code: ${e.statusCode}). Please try again."
            }
            if (e.statusCode != 12501) {
                showError("Google Sign-In Error", errorMessage)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentLoginBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupGoogleSignIn()
        setupProgressDialog()
        setupFormValidation()
        setupClickListeners()

        return binding.root
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun setupFormValidation() {
        binding.etEmail.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                validateEmail(s.toString())
            }
        })

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
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, com.example.giveease.donor.TermsConditionsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.tvPrivacyLink.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, com.example.giveease.donor.PrivacyPolicyFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnGoogleLogin.setOnClickListener {
            loadingDialog.show()
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }
    }

    private fun handleLogin() {
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        binding.tilEmail.error = null
        binding.tilPassword.error = null


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

                        val uid = user.uid
                        firestore.collection("users").document(uid).get()
                            .addOnSuccessListener { document ->
                                loadingDialog.dismiss()
                                if (document.exists()) {
                                    val role = document.getString("role") ?: "donor"

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
                auth.signOut()
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
                    auth.signOut()
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
                    val uid = user.uid
                    firestore.collection("users").document(uid).get()
                        .addOnSuccessListener { document ->
                            loadingDialog.dismiss()
                            if (document.exists()) {
                                val role = document.getString("role") ?: "donor"
                                val verificationStatus = document.getString("verificationStatus") ?: "pending"

                                updateEmailVerificationStatus(uid)

                                if (verificationStatus == "pending") {
                                    showIdentityVerificationDialog(role)
                                } else if (verificationStatus == "rejected") {
                                    showRejectedVerificationDialog(document.getString("rejectionReason") ?: "Unknown reason")
                                } else {
                                    loadMainFragment(role)
                                    Toast.makeText(requireContext(), "Welcome back!", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            loadingDialog.dismiss()
                            Toast.makeText(requireContext(), "Error fetching user data: ${it.message}", Toast.LENGTH_SHORT).show()
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

    private fun showIdentityVerificationDialog(role: String) {
        val message = if (role == "ngo") {
            "To use GiveEase, please verify your NGO identity by uploading your government registration documents."
        } else {
            "To donate and use GiveEase fully, please verify your identity by uploading a government-issued ID."
        }

        AlertDialog.Builder(requireContext())
            .setTitle("Identity Verification Required")
            .setMessage(message)
            .setPositiveButton("Verify Now") { _, _ ->
                loadMainFragment(role)
            }
            .setNegativeButton("Later") { _, _ ->
                loadMainFragment(role)

            }
            .setCancelable(false)
            .show()
    }

    private fun showRejectedVerificationDialog(reason: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Verification Rejected")
            .setMessage("Your identity verification was rejected.\n\nReason: $reason\n\nPlease contact support or resubmit your documents.")
            .setPositiveButton("Contact Support") { _, _ ->
                Toast.makeText(requireContext(), "Support: support@giveease.com", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Close") { _, _ ->
                auth.signOut()
            }
            .setCancelable(false)
            .show()
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(requireActivity()) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    if (user != null) {
                        // Check if user already exists in Firestore
                        firestore.collection("users").document(user.uid).get()
                            .addOnSuccessListener { document ->
                                loadingDialog.dismiss()
                                if (document.exists()) {
                                    // Existing user — log them in directly
                                    val role = document.getString("role") ?: "donor"
                                    updateEmailVerificationStatus(user.uid)
                                    loadMainFragment(role)
                                    Toast.makeText(requireContext(), "Welcome back!", Toast.LENGTH_SHORT).show()
                                } else {
                                    // New user via Google on Login page — ask for role
                                    showRoleSelectionDialog(user.uid, user.displayName ?: "User", user.email ?: "")
                                }
                            }
                            .addOnFailureListener { e ->
                                loadingDialog.dismiss()
                                showError("Error", "Failed to check user data: ${e.message}")
                            }
                    } else {
                        loadingDialog.dismiss()
                        showError("Error", "Authentication succeeded but user is null.")
                    }
                } else {
                    loadingDialog.dismiss()
                    showError("Google Sign-In Error", "Authentication failed: ${task.exception?.message}")
                }
            }
    }

    private fun showRoleSelectionDialog(uid: String, name: String, email: String) {
        val roles = arrayOf("Donor", "NGO")
        AlertDialog.Builder(requireContext())
            .setTitle("Welcome to GiveEase!")
            .setMessage("Please select how you'd like to use GiveEase:")
            .setPositiveButton("Join as Donor") { _, _ ->
                saveGoogleUserToFirestore(uid, name, email, "donor")
            }
            .setNegativeButton("Join as NGO") { _, _ ->
                saveGoogleUserToFirestore(uid, name, email, "ngo")
            }
            .setCancelable(false)
            .show()
    }

    private fun saveGoogleUserToFirestore(uid: String, name: String, email: String, role: String) {
        loadingDialog.show()
        val userMap = hashMapOf<String, Any>(
            "uid" to uid,
            "name" to name,
            "email" to email,
            "role" to role,
            "emailVerified" to true,
            "verificationStatus" to "pending",
            "identityDocumentUrl" to "",
            "createdAt" to System.currentTimeMillis(),
            "updatedAt" to System.currentTimeMillis()
        )

        if (role == "ngo") {
            userMap["ngoName"] = ""
            userMap["registrationNumber"] = ""
            userMap["governmentDocumentUrl"] = ""
        }

        firestore.collection("users").document(uid).set(userMap)
            .addOnSuccessListener {
                loadingDialog.dismiss()
                loadMainFragment(role)
                Toast.makeText(requireContext(), "Welcome to GiveEase!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                loadingDialog.dismiss()
                showError("Error", "Failed to create profile: ${e.message}")
            }
    }

    private fun setupProgressDialog() {
        loadingDialog = AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun loadMainFragment(role: String) {
        if (role == "donor" || role == "ngo") {
            loadingDialog.show()

            com.example.giveease.utils.MaintenanceManager.checkMaintenanceStatus { isActive ->
                loadingDialog.dismiss()

                if (isActive) {
                    val intent = android.content.Intent(requireContext(), com.example.giveease.MaintenanceActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                } else {
                    val (fragment, tag) = when (role) {
                        "donor" -> DonorMainFragment() to "DONOR_MAIN"
                        "ngo" -> NgoMainFragment() to "NGO_MAIN"
                        else -> DonorMainFragment() to "DONOR_MAIN"
                    }

                    val existingFragment = parentFragmentManager.findFragmentByTag(tag)
                    if (existingFragment == null) {
                        parentFragmentManager.beginTransaction()
                            .replace(R.id.fragment_container, fragment, tag)
                            .commit()
                    }
                }
            }
        } else {
            val existingFragment = parentFragmentManager.findFragmentByTag("ADMIN_MAIN")
            if (existingFragment == null) {
                parentFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container, AdminMainFragment(), "ADMIN_MAIN")
                    .commit()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }
}