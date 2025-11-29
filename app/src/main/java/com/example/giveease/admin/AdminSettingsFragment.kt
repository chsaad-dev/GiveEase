package com.example.giveease.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.auth.LoginFragment
import com.example.giveease.databinding.FragmentAdminSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminSettingsFragment : Fragment() {

    private var _binding: FragmentAdminSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private var isMaintenanceMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminSettingsBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSettings()
        setupClickListeners()
    }

    private fun loadSettings() {
        firestore.collection("settings")
            .document("app_config")
            .get()
            .addOnSuccessListener { document ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (document.exists()) {
                    isMaintenanceMode = document.getBoolean("maintenanceMode") ?: false
                    binding.switchMaintenance.isChecked = isMaintenanceMode
                } else {
                    createDefaultSettings()
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                createDefaultSettings()
            }
    }

    private fun createDefaultSettings() {
        val defaultSettings = hashMapOf(
            "maintenanceMode" to false,
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection("settings")
            .document("app_config")
            .set(defaultSettings)
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener
                binding.switchMaintenance.isChecked = false
            }
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        binding.switchEmailNotif.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "Email notifications ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        binding.switchPushNotif.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "Push notifications ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        binding.switchNgoAlerts.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "NGO alerts ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        binding.switchAutoApprove.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "Auto-approve ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        binding.switchMaintenance.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !isMaintenanceMode) {
                showMaintenanceConfirmationDialog()
            } else if (!isChecked && isMaintenanceMode) {
                updateMaintenanceMode(false)
            }
        }

        binding.btnViewLogs.setOnClickListener {
            Toast.makeText(requireContext(), "View Logs feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnBackupData.setOnClickListener {
            Toast.makeText(requireContext(), "Backup Data feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnAbout.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun showMaintenanceConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Enable Maintenance Mode?")
            .setMessage("This will immediately block all donors and NGOs from using the app. Only admins will have access.\n\nAre you sure?")
            .setPositiveButton("Enable") { _, _ ->
                updateMaintenanceMode(true)
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.switchMaintenance.isChecked = false
            }
            .setCancelable(false)
            .show()
    }

    private fun updateMaintenanceMode(enable: Boolean) {
        val updates = hashMapOf<String, Any>(
            "maintenanceMode" to enable,
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("settings")
            .document("app_config")
            .set(updates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener

                isMaintenanceMode = enable
                Toast.makeText(
                    requireContext(),
                    "Maintenance mode ${if (enable) "enabled" else "disabled"}",
                    Toast.LENGTH_SHORT
                ).show()
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener

                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.switchMaintenance.isChecked = !enable
            }
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment())
            .commit()

        requireActivity().supportFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("About GiveEase")
            .setMessage("GiveEase Admin Panel\nVersion 1.0.0\n\nManage donations, NGOs, and user verifications all in one place.")
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}