package com.example.giveease.donor

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.giveease.MainActivity
import com.example.giveease.R
import com.example.giveease.databinding.FragmentDonorSettingsBinding
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.example.giveease.ui.OurTeamFragment

class DonorSettingsFragment : Fragment() {
    private var _binding: FragmentDonorSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var loadingDialog: AlertDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDonorSettingsBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupProgressDialog()
        loadUserData()
        setupClickListeners()

        return binding.root
    }

    private fun setupProgressDialog() {
        loadingDialog = AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun loadUserData() {
        val user = auth.currentUser
        if (user != null && isAdded && _binding != null) {
            val uid = user.uid
            firestore.collection("users").document(uid).get()
                .addOnSuccessListener { document ->
                    if (!isAdded || _binding == null) return@addOnSuccessListener

                    if (document.exists()) {
                        binding.tvUserName.text = document.getString("name") ?: "User Name"
                        binding.tvUserEmail.text = document.getString("email") ?: user.email

                        val profileImageUrl = document.getString("profileImageUrl")
                        if (!profileImageUrl.isNullOrEmpty()) {
                            context?.let { ctx ->
                                Glide.with(ctx)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.sample_profile)
                                    .error(R.drawable.sample_profile)
                                    .circleCrop()
                                    .into(binding.imgProfile)
                            }
                        }
                    } else {
                        binding.tvUserName.text = user.displayName ?: "User Name"
                        binding.tvUserEmail.text = user.email ?: "No email"
                    }
                }
                .addOnFailureListener {
                    if (!isAdded || _binding == null) return@addOnFailureListener
                    binding.tvUserName.text = user.displayName ?: "User Name"
                    binding.tvUserEmail.text = user.email ?: "No email"
                }
        }
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.btnEditProfile.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            parentFragmentManager.beginTransaction()
                .hide(this@DonorSettingsFragment)
                .add(R.id.fragment_container_donor, EditProfileFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardEditProfile.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            parentFragmentManager.beginTransaction()
                .hide(this@DonorSettingsFragment)
                .add(R.id.fragment_container_donor, ChangeEmailFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardChangePassword.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            parentFragmentManager.beginTransaction()
                .hide(this@DonorSettingsFragment)
                .add(R.id.fragment_container_donor, ChangePasswordFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardFaq.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            parentFragmentManager.beginTransaction()
                .hide(this@DonorSettingsFragment)
                .add(R.id.fragment_container_donor, FAQFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardSupport.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            parentFragmentManager.beginTransaction()
                .hide(this@DonorSettingsFragment)
                .add(R.id.fragment_container_donor, ContactSupportFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardPrivacy.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            parentFragmentManager.beginTransaction()
                .hide(this@DonorSettingsFragment)
                .add(R.id.fragment_container_donor, PrivacyPolicyFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardTerms.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            parentFragmentManager.beginTransaction()
                .hide(this@DonorSettingsFragment)
                .add(R.id.fragment_container_donor, TermsConditionsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.cardOurTeam.setOnClickListener {
            if (!isAdded) return@setOnClickListener
            parentFragmentManager.beginTransaction()
                .hide(this@DonorSettingsFragment)
                .add(R.id.fragment_container_donor, OurTeamFragment.newInstance())
                .addToBackStack(null)
                .commit()
        }

        binding.switchNotifications.setOnCheckedChangeListener { _, isChecked ->
            updateNotificationPreference(isChecked)
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.btnDeleteAccount.setOnClickListener {
            showDeleteAccountWarning()
        }
    }

    private fun updateNotificationPreference(enabled: Boolean) {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid)
            .update("pushNotifications", enabled)
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener
                val message = if (enabled) "Notifications enabled" else "Notifications disabled"
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                Toast.makeText(requireContext(), "Failed to update notification preference", Toast.LENGTH_SHORT).show()
                binding.switchNotifications.isChecked = !enabled
            }
    }

    private fun showLogoutConfirmation() {
        AlertDialog.Builder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        try {
            auth.signOut()

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

    private fun showDeleteAccountWarning() {
        AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Delete Account")
            .setMessage("This action will permanently delete your account and all associated data including:\n\n• Your donation history\n• Profile information\n• Account preferences\n• All personal data\n\nThis action cannot be undone. Are you sure you want to continue?")
            .setPositiveButton("Continue") { _, _ ->
                showDeleteAccountConfirmation()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun showDeleteAccountConfirmation() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_delete_account, null)
        val passwordInput = dialogView.findViewById<TextInputEditText>(R.id.etPassword)

        AlertDialog.Builder(requireContext())
            .setTitle("Confirm Account Deletion")
            .setMessage("Enter your password to confirm account deletion:")
            .setView(dialogView)
            .setPositiveButton("Delete Account") { _, _ ->
                val password = passwordInput.text.toString()
                if (password.isNotEmpty()) {
                    deleteAccount(password)
                } else {
                    Toast.makeText(requireContext(), "Password is required", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteAccount(password: String) {
        val user = auth.currentUser
        if (user?.email == null) {
            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            return
        }

        loadingDialog.show()

        val credential = EmailAuthProvider.getCredential(user.email!!, password)

        user.reauthenticate(credential)
            .addOnCompleteListener { reAuthTask ->
                if (reAuthTask.isSuccessful) {
                    val uid = user.uid
                    firestore.collection("users").document(uid).delete()
                        .addOnCompleteListener { deleteDataTask ->
                            if (deleteDataTask.isSuccessful) {
                                user.delete()
                                    .addOnCompleteListener { deleteAccountTask ->
                                        loadingDialog.dismiss()
                                        if (deleteAccountTask.isSuccessful) {
                                            showAccountDeletedDialog()
                                        } else {
                                            Toast.makeText(
                                                requireContext(),
                                                "Failed to delete account: ${deleteAccountTask.exception?.message}",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    }
                            } else {
                                loadingDialog.dismiss()
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to delete user data: ${deleteDataTask.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                } else {
                    loadingDialog.dismiss()
                    Toast.makeText(
                        requireContext(),
                        "Incorrect password. Account deletion cancelled.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    private fun showAccountDeletedDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Account Deleted")
            .setMessage("Your account has been permanently deleted. We're sorry to see you go.\n\nThank you for being part of the GiveEase community.")
            .setPositiveButton("OK") { _, _ ->
                navigateToLogin()
            }
            .setCancelable(false)
            .show()
    }

    private fun navigateToLogin() {
        val intent = Intent(requireActivity(), MainActivity::class.java).apply {
            putExtra("role", "login")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        startActivity(intent)
        requireActivity().finish()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
        _binding = null
    }
}