package com.example.giveease.ngo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.giveease.R
import com.example.giveease.databinding.FragmentNgoMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.app.AlertDialog
import android.widget.Toast
import com.example.giveease.verification.IdentityVerificationFragment

class NgoMainFragment : Fragment() {

    private var _binding: FragmentNgoMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNgoMainBinding.inflate(inflater, container, false)
        setupBottomNavigation()
        loadDefaultFragment()
        checkVerificationStatus()
        return binding.root
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    childFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    childFragmentManager.commit {
                        replace(R.id.fragmentContainer, NgoHomeFragment())
                    }
                }
                R.id.nav_chat -> {
                    childFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    childFragmentManager.commit {
                        replace(R.id.fragmentContainer, NgoChatFragment())
                    }
                }
                R.id.nav_campaign -> {
                    childFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    childFragmentManager.commit {
                        replace(R.id.fragmentContainer, CreateCampaignFragment())
                    }
                }
                R.id.nav_history -> {
                    childFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    childFragmentManager.commit {
                        replace(R.id.fragmentContainer, NgoHistoryFragment())
                    }
                }
                R.id.nav_profile -> {
                    childFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    childFragmentManager.commit {
                        replace(R.id.fragmentContainer, NgoProfileFragment())
                    }
                }
            }
            true
        }
    }

    private fun loadDefaultFragment() {
        childFragmentManager.commit {
            replace(R.id.fragmentContainer, NgoHomeFragment())
        }
    }

    private fun checkVerificationStatus() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val verificationStatus = document.getString("verificationStatus") ?: "pending"

                if (verificationStatus == "pending") {
                    showVerificationPopup()
                } else if (verificationStatus == "rejected") {
                    val reason = document.getString("rejectionReason") ?: "Unknown reason"
                    showRejectedPopup(reason)
                }
            }
    }

    private fun showVerificationPopup() {
        AlertDialog.Builder(requireContext())
            .setTitle("Verify Your NGO")
            .setMessage("To create campaigns, please verify your NGO by uploading government registration documents.\n\nYou can browse but cannot create campaigns until verified.")
            .setPositiveButton("Verify Now") { _, _ ->
                navigateToVerification()
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun showRejectedPopup(reason: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Verification Rejected")
            .setMessage("Your NGO verification was rejected.\n\nReason: $reason\n\nPlease resubmit your documents.")
            .setPositiveButton("Resubmit") { _, _ ->
                navigateToVerification()
            }
            .setNegativeButton("Contact Support") { _, _ ->
                Toast.makeText(requireContext(), "support@giveease.com", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private fun navigateToVerification() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, IdentityVerificationFragment())
            .addToBackStack(null)
            .commit()
    }

    fun handleBackPress(): Boolean {
        val currentFragment = childFragmentManager.findFragmentById(R.id.fragmentContainer)

        if (childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStack()
            return true
        }

        if (currentFragment !is NgoHomeFragment) {
            binding.bottomNavigationView.selectedItemId = R.id.nav_home
            childFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, NgoHomeFragment())
                .commit()
            return true
        }

        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}