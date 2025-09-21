package com.example.giveease.donor

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.giveease.MainActivity
import com.example.giveease.R
import com.example.giveease.databinding.FragmentDonorSettingsBinding
import com.google.firebase.auth.FirebaseAuth

class DonorSettingsFragment : Fragment() {
    private lateinit var binding: FragmentDonorSettingsBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDonorSettingsBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.cardEditProfile.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_donor, ChangeEmailFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardChangePassword.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_donor, ChangePasswordFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardFaq.setOnClickListener {
            Toast.makeText(requireContext(), "FAQs clicked", Toast.LENGTH_SHORT).show()
        }

        binding.cardSupport.setOnClickListener {
            Toast.makeText(requireContext(), "Contact Support clicked", Toast.LENGTH_SHORT).show()
        }

        binding.cardPrivacy.setOnClickListener {
            Toast.makeText(requireContext(), "Privacy Policy clicked", Toast.LENGTH_SHORT).show()
        }

        binding.cardTerms.setOnClickListener {
            Toast.makeText(requireContext(), "Terms & Conditions clicked", Toast.LENGTH_SHORT).show()
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "Notifications: $isChecked", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountConfirmation()
        }

        return binding.root
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Yes") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        try {
            // Sign out from Firebase
            auth.signOut()

            // Navigate to MainActivity with login flag
            val intent = Intent(requireActivity(), MainActivity::class.java).apply {
                putExtra("role", "login")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            startActivity(intent)
            requireActivity().finish()

            Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Logout failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDeleteAccountConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Delete Account")
            .setMessage("Are you sure you want to permanently delete your account? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                Toast.makeText(requireContext(), "Account deletion functionality coming soon", Toast.LENGTH_SHORT).show()
                // TODO: Implement account deletion
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}