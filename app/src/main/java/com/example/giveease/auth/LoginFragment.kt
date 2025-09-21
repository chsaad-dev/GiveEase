package com.example.giveease.auth

import android.app.AlertDialog
import android.os.Bundle
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

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (!binding.cbTerms.isChecked) {
                Toast.makeText(requireContext(), "Please agree to terms", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Email and password required", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
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
                                    } else {
                                        Toast.makeText(requireContext(), "User data not found", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .addOnFailureListener {
                                    loadingDialog.dismiss()
                                    Toast.makeText(requireContext(), "Error fetching role: ${it.message}", Toast.LENGTH_SHORT).show()
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
                            else -> "Login Failed: ${task.exception?.message}"
                        }
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
        }

        binding.tvSignupLink.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, SignupFragment())
                .addToBackStack(null)
                .commit()
        }

        return binding.root
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
}