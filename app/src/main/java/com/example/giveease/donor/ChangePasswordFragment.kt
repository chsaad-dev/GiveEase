package com.example.giveease.donor

import android.app.AlertDialog
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentChangePasswordBinding
import com.google.firebase.auth.EmailAuthProvider
import com.google.firebase.auth.FirebaseAuth

class ChangePasswordFragment : Fragment() {
    private lateinit var binding: FragmentChangePasswordBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var loadingDialog: AlertDialog

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentChangePasswordBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()

        setupProgressDialog()
        setupListeners()
        setupPasswordStrengthChecker()

        return binding.root
    }

    private fun setupProgressDialog() {
        loadingDialog = AlertDialog.Builder(requireContext())
            .setView(R.layout.dialog_loading)
            .setCancelable(false)
            .create()
        loadingDialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
    }

    private fun setupListeners() {
        binding.apply {
            btnBack.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            btnCancel.setOnClickListener {
                parentFragmentManager.popBackStack()
            }

            btnChangePassword.setOnClickListener {
                changePassword()
            }

            etNewPassword.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    checkPasswordStrength(s.toString())
                    updatePasswordRequirements(s.toString())
                }
            })

            etConfirmPassword.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) {
                    checkPasswordMatch()
                }
            })
        }
    }

    private fun setupPasswordStrengthChecker() {
        updateRequirementIndicator(binding.icLength, binding.tvLength, false)
        updateRequirementIndicator(binding.icUppercase, binding.tvUppercase, false)
        updateRequirementIndicator(binding.icNumber, binding.tvNumber, false)
        updateRequirementIndicator(binding.icSpecial, binding.tvSpecial, false)
    }

    private fun checkPasswordStrength(password: String) {
        var score = 0
        val requirements = mutableListOf<String>()

        if (password.length >= 8) score += 25 else requirements.add("At least 8 characters")
        if (password.any { it.isUpperCase() }) score += 25 else requirements.add("One uppercase letter")
        if (password.any { it.isDigit() }) score += 25 else requirements.add("One number")
        if (password.any { !it.isLetterOrDigit() }) score += 25 else requirements.add("One special character")

        binding.progressPasswordStrength.progress = score

        val (strengthText, color) = when (score) {
            0 -> "Enter new password" to ContextCompat.getColor(requireContext(), R.color.gray)
            25 -> "Weak" to ContextCompat.getColor(requireContext(), R.color.error)
            50 -> "Fair" to ContextCompat.getColor(requireContext(), android.R.color.holo_orange_dark)
            75 -> "Good" to ContextCompat.getColor(requireContext(), android.R.color.holo_blue_dark)
            100 -> "Strong" to ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
            else -> "Enter new password" to ContextCompat.getColor(requireContext(), R.color.gray)
        }

        binding.tvPasswordStrength.text = strengthText
        binding.tvPasswordStrength.setTextColor(color)
        binding.progressPasswordStrength.setProgressTintList(ContextCompat.getColorStateList(requireContext(),
            when (score) {
                25 -> R.color.error
                50 -> android.R.color.holo_orange_dark
                75 -> android.R.color.holo_blue_dark
                100 -> android.R.color.holo_green_dark
                else -> R.color.gray
            }
        ))
    }

    private fun updatePasswordRequirements(password: String) {
        updateRequirementIndicator(binding.icLength, binding.tvLength, password.length >= 8)
        updateRequirementIndicator(binding.icUppercase, binding.tvUppercase, password.any { it.isUpperCase() })
        updateRequirementIndicator(binding.icNumber, binding.tvNumber, password.any { it.isDigit() })
        updateRequirementIndicator(binding.icSpecial, binding.tvSpecial, password.any { !it.isLetterOrDigit() })
    }

    private fun updateRequirementIndicator(icon: android.widget.ImageView, text: android.widget.TextView, met: Boolean) {
        val color = if (met) {
            ContextCompat.getColor(requireContext(), android.R.color.holo_green_dark)
        } else {
            ContextCompat.getColor(requireContext(), R.color.gray)
        }

        icon.setColorFilter(color)
        text.setTextColor(color)
    }

    private fun checkPasswordMatch() {
        val newPassword = binding.etNewPassword.text.toString()
        val confirmPassword = binding.etConfirmPassword.text.toString()

        if (confirmPassword.isNotEmpty() && newPassword != confirmPassword) {
            binding.tilConfirmPassword.error = "Passwords do not match"
        } else {
            binding.tilConfirmPassword.error = null
        }
    }

    private fun changePassword() {
        if (!validateInputs()) return

        val currentPassword = binding.etCurrentPassword.text.toString()
        val newPassword = binding.etNewPassword.text.toString()

        loadingDialog.show()

        val user = auth.currentUser
        if (user?.email == null) {
            loadingDialog.dismiss()
            Toast.makeText(requireContext(), "User not found", Toast.LENGTH_SHORT).show()
            return
        }

        val credential = EmailAuthProvider.getCredential(user.email!!, currentPassword)

        user.reauthenticate(credential)
            .addOnCompleteListener { reAuthTask ->
                if (reAuthTask.isSuccessful) {
                    user.updatePassword(newPassword)
                        .addOnCompleteListener { updateTask ->
                            loadingDialog.dismiss()
                            if (updateTask.isSuccessful) {
                                showSuccessDialog()
                            } else {
                                Toast.makeText(
                                    requireContext(),
                                    "Failed to update password: ${updateTask.exception?.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                } else {
                    loadingDialog.dismiss()
                    binding.tilCurrentPassword.error = "Current password is incorrect"
                }
            }
    }

    private fun validateInputs(): Boolean {
        binding.apply {
            val currentPassword = etCurrentPassword.text.toString()
            val newPassword = etNewPassword.text.toString()
            val confirmPassword = etConfirmPassword.text.toString()

            tilCurrentPassword.error = null
            tilNewPassword.error = null
            tilConfirmPassword.error = null

            if (currentPassword.isEmpty()) {
                tilCurrentPassword.error = "Current password is required"
                etCurrentPassword.requestFocus()
                return false
            }

            if (newPassword.isEmpty()) {
                tilNewPassword.error = "New password is required"
                etNewPassword.requestFocus()
                return false
            }

            if (newPassword.length < 8) {
                tilNewPassword.error = "Password must be at least 8 characters"
                etNewPassword.requestFocus()
                return false
            }

            if (!newPassword.any { it.isUpperCase() }) {
                tilNewPassword.error = "Password must contain at least one uppercase letter"
                etNewPassword.requestFocus()
                return false
            }

            if (!newPassword.any { it.isDigit() }) {
                tilNewPassword.error = "Password must contain at least one number"
                etNewPassword.requestFocus()
                return false
            }

            if (!newPassword.any { !it.isLetterOrDigit() }) {
                tilNewPassword.error = "Password must contain at least one special character"
                etNewPassword.requestFocus()
                return false
            }

            if (confirmPassword.isEmpty()) {
                tilConfirmPassword.error = "Please confirm your new password"
                etConfirmPassword.requestFocus()
                return false
            }

            if (newPassword != confirmPassword) {
                tilConfirmPassword.error = "Passwords do not match"
                etConfirmPassword.requestFocus()
                return false
            }

            if (currentPassword == newPassword) {
                tilNewPassword.error = "New password must be different from current password"
                etNewPassword.requestFocus()
                return false
            }

            return true
        }
    }

    private fun showSuccessDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("Password Changed")
            .setMessage("Your password has been successfully changed.")
            .setPositiveButton("OK") { _, _ ->
                parentFragmentManager.popBackStack()
            }
            .setCancelable(false)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (::loadingDialog.isInitialized && loadingDialog.isShowing) {
            loadingDialog.dismiss()
        }
    }
}