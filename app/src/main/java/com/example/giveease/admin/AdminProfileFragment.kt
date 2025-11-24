package com.example.giveease.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.giveease.databinding.FragmentAdminProfileBinding

class AdminProfileFragment : Fragment() {

    private var _binding: FragmentAdminProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadProfileData()
    }

    private fun setupClickListeners() {
        binding.btnChangePhoto.setOnClickListener {
            Toast.makeText(requireContext(), "Change photo feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnChangePassword.setOnClickListener {
            Toast.makeText(requireContext(), "Change password feature coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnSaveChanges.setOnClickListener {
            saveProfile()
        }
    }

    private fun loadProfileData() {
        // TODO: Load from Firebase
        binding.etAdminName.setText("Admin User")
        binding.etAdminEmail.setText("admin@giveease.com")
    }

    private fun saveProfile() {
        val name = binding.etAdminName.text.toString()
        val phone = binding.etAdminPhone.text.toString()

        // TODO: Save to Firebase
        Toast.makeText(requireContext(), "Profile updated successfully", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}