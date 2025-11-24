package com.example.giveease.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.giveease.databinding.FragmentAdminDashboardBinding

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadDashboardData()
    }

    private fun setupClickListeners() {
        binding.btnNotifications.setOnClickListener {
            Toast.makeText(requireContext(), "Notifications", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to notifications screen
        }

        binding.btnApproveNgos.setOnClickListener {
            Toast.makeText(requireContext(), "Opening NGO Approvals", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to NGO approval screen

        }

        binding.btnReviewCampaigns.setOnClickListener {
            Toast.makeText(requireContext(), "Opening Campaign Reviews", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to campaign review screen
        }

        binding.btnManageUsers.setOnClickListener {
            Toast.makeText(requireContext(), "Opening User Management", Toast.LENGTH_SHORT).show()
            // TODO: Navigate to user management screen
        }
    }

    private fun loadDashboardData() {
        // TODO: Load from Firebase
        binding.tvAdminName.text = "Welcome, Admin"

        binding.tvNotificationBadge.text = "5"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}