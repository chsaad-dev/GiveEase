package com.example.giveease.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.giveease.databinding.FragmentAdminSettingsBinding
import com.google.firebase.auth.FirebaseAuth

class AdminSettingsFragment : Fragment() {

    private var _binding: FragmentAdminSettingsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminSettingsBinding.inflate(inflater, container, false)
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
            Toast.makeText(requireContext(), "GiveEase v1.0.0", Toast.LENGTH_SHORT).show()
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }

        binding.switchEmailNotif.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "Email notifications: ${if (isChecked) "ON" else "OFF"}", Toast.LENGTH_SHORT).show()
        }

        binding.switchMaintenance.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                Toast.makeText(requireContext(), "Maintenance mode enabled", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun loadSettings() {
        // TODO: Load from Firebase/SharedPreferences
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()
        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
        // TODO: Navigate back to login
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}