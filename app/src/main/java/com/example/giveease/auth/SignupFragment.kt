package com.example.giveease.auth

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentSignupBinding
import com.example.giveease.donor.DonorMainFragment
import com.example.giveease.ngo.NgoMainFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore

class SignupFragment : Fragment() {

    private lateinit var binding: FragmentSignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var loadingDialog: AlertDialog
    private lateinit var googleSignInClient: GoogleSignInClient
    private var selectedRole: String = "donor"

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
                showError("Failed to get ID token from Google.")
            }
        } catch (e: ApiException) {
            loadingDialog.dismiss()
            Log.e("SignupFragment", "Google sign in failed: ${e.statusCode}", e)
            val errorMessage = when (e.statusCode) {
                12500 -> "Google Sign-In failed. Please make sure Google Play Services is up to date."
                12501 -> "Sign-In cancelled."
                10 -> "Developer error: SHA-1 fingerprint not configured in Firebase. Please contact support."
                else -> "Google Sign-In failed (code: ${e.statusCode}). Please try again."
            }
            if (e.statusCode != 12501) {
                showError(errorMessage)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignupBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupGoogleSignIn()
        setupProgressDialog()
        setupRoleSelection()
        setupClickListeners()
        setupFormValidation()

        return binding.root
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)
    }

    private fun setupRoleSelection() {
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

        val white = ContextCompat.getColor(requireContext(), android.R.color.white)
        val primary = ContextCompat.getColor(requireContext(), R.color.primary)
        val gray = ContextCompat.getColor(requireContext(), android.R.color.darker_gray)
        val darkText = android.graphics.Color.parseColor("#333333")
        val subtitleText = android.graphics.Color.parseColor("#666666")

        if (role == "donor") {
            // Donor = SELECTED (colored bg, white text)
            binding.cardDonor.apply {
                strokeColor = primary
                setCardBackgroundColor(primary)
            }
            binding.ivDonorIcon.imageTintList = android.content.res.ColorStateList.valueOf(white)
            binding.tvDonorTitle.setTextColor(white)
            binding.tvDonorSubtitle.setTextColor(white)

            // NGO = UNSELECTED (white bg, colored text)
            binding.cardNGO.apply {
                strokeColor = gray
                setCardBackgroundColor(white)
            }
            binding.ivNgoIcon.imageTintList = android.content.res.ColorStateList.valueOf(primary)
            binding.tvNgoTitle.setTextColor(primary)
            binding.tvNgoSubtitle.setTextColor(subtitleText)
        } else {
            // NGO = SELECTED (colored bg, white text)
            binding.cardNGO.apply {
                strokeColor = primary
                setCardBackgroundColor(primary)
            }
            binding.ivNgoIcon.imageTintList = android.content.res.ColorStateList.valueOf(white)
            binding.tvNgoTitle.setTextColor(white)
            binding.tvNgoSubtitle.setTextColor(white)

            // Donor = UNSELECTED (white bg, colored text)
            binding.cardDonor.apply {
                strokeColor = gray
                setCardBackgroundColor(white)
            }
            binding.ivDonorIcon.imageTintList = android.content.res.ColorStateList.valueOf(primary)
            binding.tvDonorTitle.setTextColor(primary)
            binding.tvDonorSubtitle.setTextColor(subtitleText)
        }
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

        binding.btnGoogleSignup.setOnClickListener {
            loadingDialog.show()
            googleSignInClient.signOut().addOnCompleteListener {
                val signInIntent = googleSignInClient.signInIntent
                googleSignInLauncher.launch(signInIntent)
            }
        }
    }

    private fun handleSignup() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()


        binding.tilEmail.error = null
        binding.tilPassword.error = null


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
                                    loadMainFragment(role)
                                    Toast.makeText(requireContext(), "Welcome back!", Toast.LENGTH_SHORT).show()
                                } else {
                                    // New user — use the role selected on the signup screen
                                    saveGoogleUserToFirestore(
                                        user.uid,
                                        user.displayName ?: "User",
                                        user.email ?: "",
                                        selectedRole
                                    )
                                }
                            }
                            .addOnFailureListener { e ->
                                loadingDialog.dismiss()
                                showError("Failed to check user data: ${e.message}")
                            }
                    } else {
                        loadingDialog.dismiss()
                        showError("Authentication succeeded but user is null.")
                    }
                } else {
                    loadingDialog.dismiss()
                    showError("Authentication failed: ${task.exception?.message}")
                }
            }
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
                showError("Failed to create profile: ${e.message}")
            }
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
        }
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