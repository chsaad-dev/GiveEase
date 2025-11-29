package com.example.giveease.ngo

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.giveease.MainActivity
import com.example.giveease.R
import com.example.giveease.databinding.FragmentNgoSettingsBinding
import com.google.firebase.auth.FirebaseAuth

class NgoSettingsFragment : Fragment() {

    private var _binding: FragmentNgoSettingsBinding? = null
    private val binding get() = _binding!!
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNgoSettingsBinding.inflate(inflater, container, false)
        setupClickListeners()
        return binding.root
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnChangeLanguage.setOnClickListener {
            Toast.makeText(requireContext(), "Language settings - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnChangePassword.setOnClickListener {
            showChangePasswordDialog()
        }

        binding.btnWithdrawalSettings.setOnClickListener {
            Toast.makeText(requireContext(), "Withdrawal settings - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnBankAccounts.setOnClickListener {
            Toast.makeText(requireContext(), "Bank account management - Coming Soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnPrivacyPolicy.setOnClickListener {
            Toast.makeText(requireContext(), "Privacy Policy", Toast.LENGTH_SHORT).show()
        }

        binding.btnExportData.setOnClickListener {
            showExportDataDialog()
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountDialog()
        }
    }

    private fun showChangePasswordDialog() {
        val email = auth.currentUser?.email ?: return

        AlertDialog.Builder(requireContext())
            .setTitle("Change Password")
            .setMessage("A password reset link will be sent to:\n$email")
            .setPositiveButton("Send Link") { _, _ ->
                sendPasswordResetEmail(email)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email)
            .addOnSuccessListener {
                AlertDialog.Builder(requireContext())
                    .setTitle("Email Sent")
                    .setMessage("Password reset instructions have been sent to $email")
                    .setPositiveButton("OK", null)
                    .show()
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to send reset email", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showExportDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Export Data")
            .setMessage("Export all your NGO data including campaigns, donations, and profile information?")
            .setPositiveButton("Export") { _, _ ->
                Toast.makeText(requireContext(), "Data export started - You'll receive an email", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAccountDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠ Delete Account")
            .setMessage("This action is PERMANENT and cannot be undone.\n\n• All your campaigns will be removed\n• All donation history will be deleted\n• Your NGO profile will be permanently deleted\n\nAre you absolutely sure?")
            .setPositiveButton("Delete Forever") { _, _ ->
                showFinalConfirmation()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showFinalConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Final Confirmation")
            .setMessage("Type 'DELETE' to confirm account deletion")
            .setView(android.widget.EditText(requireContext()).apply {
                hint = "Type DELETE"
            })
            .setPositiveButton("Confirm") { dialog, _ ->
                val editText = (dialog as AlertDialog).findViewById<android.widget.EditText>(android.R.id.edit)
                if (editText?.text.toString() == "DELETE") {
                    deleteAccount()
                } else {
                    Toast.makeText(requireContext(), "Confirmation failed", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount() {
        auth.currentUser?.delete()
            ?.addOnSuccessListener {
                Toast.makeText(requireContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show()
                navigateToLogin()
            }
            ?.addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to delete account. Try logging in again first.", Toast.LENGTH_LONG).show()
            }
    }

    private fun navigateToLogin() {
        auth.signOut()
        val intent = Intent(requireContext(), MainActivity::class.java)
        intent.putExtra("role", "login")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        requireActivity().finish()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}