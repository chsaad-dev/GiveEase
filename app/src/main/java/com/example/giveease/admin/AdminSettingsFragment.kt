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

class AdminSettingsFragment : Fragment() {

    private var _binding: FragmentAdminSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminSettingsBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadSettings()
    }

    private fun setupClickListeners() {
        binding.btnViewLogs.setOnClickListener {
            Toast.makeText(requireContext(), "System logs feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnBackupData.setOnClickListener {
            Toast.makeText(requireContext(), "Database backup feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnAbout.setOnClickListener {
            showAboutDialog()
        }

        binding.btnLogout.setOnClickListener {
            showLogoutConfirmation()
        }

        binding.switchEmailNotif.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "Email notifications: ${if (isChecked) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        }

        binding.switchPushNotif.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "Push notifications: ${if (isChecked) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        }

        binding.switchNgoAlerts.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "NGO alerts: ${if (isChecked) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        }

        binding.switchAutoApprove.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(requireContext(), "Auto-approve enabled", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Auto-approve disabled", Toast.LENGTH_SHORT).show()
            }
        }

        binding.switchMaintenance.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                showMaintenanceModeConfirmation()
            } else {
                Toast.makeText(requireContext(), "Maintenance mode disabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadSettings() {
        // TODO: Load settings from Firebase/SharedPreferences
    }

    private fun showLogoutConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                performLogout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun performLogout() {
        // Sign out from Firebase
        auth.signOut()

        // Clear any saved preferences if needed
        // SharedPreferences can be cleared here if you're using them

        // Navigate back to LoginFragment and clear backstack
        val loginFragment = LoginFragment()

        // Use requireActivity() to get the parent FragmentManager
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, loginFragment)
            .commit()

        // Clear the entire backstack
        requireActivity().supportFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("About GiveEase")
            .setMessage("GiveEase v1.0.0\n\nA platform connecting donors with verified NGOs to make giving easier and more transparent.\n\nDeveloped for educational purposes.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showMaintenanceModeConfirmation() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Enable Maintenance Mode?")
            .setMessage("This will temporarily disable user access to the platform. Are you sure?")
            .setPositiveButton("Enable") { _, _ ->
                Toast.makeText(requireContext(), "Maintenance mode enabled", Toast.LENGTH_LONG).show()
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.switchMaintenance.isChecked = false
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}