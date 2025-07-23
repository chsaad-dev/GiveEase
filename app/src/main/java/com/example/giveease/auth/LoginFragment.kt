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
                        val uid = auth.currentUser?.uid ?: ""
                        firestore.collection("users").document(uid).get()
                            .addOnSuccessListener { document ->
                                loadingDialog.dismiss()
                                if (document.exists()) {
                                    val role = document.getString("role") ?: "donor"
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
                        loadingDialog.dismiss()
                        Toast.makeText(requireContext(), "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
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
