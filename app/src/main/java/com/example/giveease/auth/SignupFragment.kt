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

class SignupFragment : Fragment() {

    private lateinit var binding: FragmentSignupBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var loadingDialog: AlertDialog

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        binding = FragmentSignupBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupProgressDialog()

        binding.btnSignup.setOnClickListener {
            val name = binding.etName.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (!binding.cbAgree.isChecked) {
                Toast.makeText(requireContext(), "Please agree to terms", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(requireContext(), "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            loadingDialog.show()

            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val uid = auth.currentUser?.uid ?: ""
                        val role = if (binding.rbDonor.isChecked) "donor" else "ngo"

                        val userMap = hashMapOf(
                            "uid" to uid,
                            "name" to name,
                            "email" to email,
                            "role" to role
                        )


                        firestore.collection("users").document(uid).set(userMap)
                            .addOnSuccessListener {
                                loadingDialog.dismiss()
                                Toast.makeText(requireContext(), "Signup successful", Toast.LENGTH_SHORT).show()
                                loadMainFragment(role)
                            }
                            .addOnFailureListener {
                                loadingDialog.dismiss()
                                Toast.makeText(requireContext(), "Firestore error: ${it.message}", Toast.LENGTH_SHORT).show()
                            }

                    } else {
                        loadingDialog.dismiss()
                        Toast.makeText(requireContext(), "Signup Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        binding.tvLoginLink.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, LoginFragment())
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
}
