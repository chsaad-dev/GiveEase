package com.example.giveease.auth

import android.app.AlertDialog
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentSignupBinding
import com.example.giveease.donor.DonorMainFragment
import com.example.giveease.ngo.NgoMainFragment
import com.example.giveease.admin.AdminMainFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.material.button.MaterialButton

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

        return binding.root
    }

    private fun setupRoleSelection() {
        // Set initial state
        updateRoleButtonStates("donor")

        binding.btnDonor.setOnClickListener {
            updateRoleButtonStates("donor")
        }

        binding.btnNGO.setOnClickListener {
            updateRoleButtonStates("ngo")
        }
    }

    private fun updateRoleButtonStates(selectedRole: String) {
        this.selectedRole = selectedRole

        binding.btnDonor.apply {
            strokeWidth = if (selectedRole == "donor") 0 else 1
            setBackgroundColor(if (selectedRole == "donor")
                resources.getColor(R.color.primary_blue, null)
            else resources.getColor(android.R.color.transparent, null))
            setTextColor(if (selectedRole == "donor")
                resources.getColor(android.R.color.white, null)
            else resources.getColor(R.color.primary_blue, null))
        }

        binding.btnNGO.apply {
            strokeWidth = if (selectedRole == "ngo") 0 else 1
            setBackgroundColor(if (selectedRole == "ngo")
                resources.getColor(R.color.primary_blue, null)
            else resources.getColor(android.R.color.transparent, null))
            setTextColor(if (selectedRole == "ngo")
                resources.getColor(android.R.color.white, null)
            else resources.getColor(R.color.primary_blue, null))
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
    }

    private fun handleSignup() {
        val name = binding.etName.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()
        val password = binding.etPassword.text.toString().trim()

        // Validation
        when {
            name.isEmpty() -> {
                binding.etName.error = "Name is required"
                binding.etName.requestFocus()
                return
            }
            email.isEmpty() -> {
                binding.etEmail.error = "Email is required"
                binding.etEmail.requestFocus()
                return
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.etEmail.error = "Enter a valid email"
                binding.etEmail.requestFocus()
                return
            }
            password.isEmpty() -> {
                binding.etPassword.error = "Password is required"
                binding.etPassword.requestFocus()
                return
            }
            password.length < 6 -> {
                binding.etPassword.error = "Password must be at least 6 characters"
                binding.etPassword.requestFocus()
                return
            }
            !binding.cbAgree.isChecked -> {
                Toast.makeText(requireContext(), "Please agree to terms & conditions", Toast.LENGTH_SHORT).show()
                return
            }
        }

        loadingDialog.show()

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid ?: ""

                    val userMap = hashMapOf(
                        "uid" to uid,
                        "name" to name,
                        "email" to email,
                        "role" to selectedRole,
                        "createdAt" to System.currentTimeMillis()
                    )

                    firestore.collection("users").document(uid).set(userMap)
                        .addOnSuccessListener {
                            loadingDialog.dismiss()
                            showSuccessAndNavigate()
                        }
                        .addOnFailureListener { e ->
                            loadingDialog.dismiss()
                            showError("Failed to create profile: ${e.message}")
                        }
                } else {
                    loadingDialog.dismiss()
                    showError(task.exception?.message ?: "Signup failed")
                }
            }
    }

    private fun showSuccessAndNavigate() {
        Toast.makeText(requireContext(), "Account created successfully", Toast.LENGTH_SHORT).show()
        loadMainFragment(selectedRole)
    }

    private fun showError(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    private fun setupProgressDialog() {
        loadingDialog = AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
    }

    private fun loadMainFragment(role: String) {
        val fragment = when (role) {
            "donor" -> DonorMainFragment()
            "ngo" -> NgoMainFragment()
            "admin" -> AdminMainFragment()
            else -> DonorMainFragment()
        }

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()
    }

    companion object {
        fun newInstance() = SignupFragment()
    }
}