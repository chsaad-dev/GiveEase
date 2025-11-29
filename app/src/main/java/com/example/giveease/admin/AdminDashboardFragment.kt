package com.example.giveease.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentAdminDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupClickListeners()
        loadDashboardData()
        loadAdminName()
    }

    private fun setupClickListeners() {
        binding.btnNotifications.setOnClickListener {
            Toast.makeText(requireContext(), "Notifications", Toast.LENGTH_SHORT).show()
        }

        binding.btnApproveNgos.setOnClickListener {
            navigateToVerificationApprovals()
        }

        binding.btnReviewCampaigns.setOnClickListener {
            Toast.makeText(requireContext(), "Campaign Reviews coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnManageUsers.setOnClickListener {
            Toast.makeText(requireContext(), "User Management coming soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateToVerificationApprovals() {
        if (!isAdded) return

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, VerificationApprovalsFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun loadAdminName() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                val name = document.getString("name") ?: "Admin"
                binding.tvAdminName.text = "Welcome, $name"
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener

                binding.tvAdminName.text = "Welcome, Admin"
            }
    }

    private fun loadDashboardData() {
        loadTotalNGOs()
        loadTotalDonors()
        loadPendingApprovals()
        loadActiveCampaigns()
    }

    private fun loadTotalNGOs() {
        firestore.collection("users")
            .whereEqualTo("role", "ngo")
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                binding.tvTotalNgos.text = documents.size().toString()
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener

                binding.tvTotalNgos.text = "0"
            }
    }

    private fun loadTotalDonors() {
        firestore.collection("users")
            .whereEqualTo("role", "donor")
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                binding.tvTotalDonors.text = documents.size().toString()
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener

                binding.tvTotalDonors.text = "0"
            }
    }

    private fun loadPendingApprovals() {
        firestore.collection("users")
            .whereEqualTo("verificationStatus", "pending")
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                val count = documents.size()
                binding.tvPendingApprovals.text = count.toString()
                binding.tvNotificationBadge.text = count.toString()

                if (count == 0) {
                    binding.tvNotificationBadge.visibility = View.GONE
                } else {
                    binding.tvNotificationBadge.visibility = View.VISIBLE
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener

                binding.tvPendingApprovals.text = "0"
                binding.tvNotificationBadge.visibility = View.GONE
            }
    }

    private fun loadActiveCampaigns() {
        firestore.collection("campaigns")
            .whereEqualTo("status", "Active")
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                binding.tvActiveCampaigns.text = documents.size().toString()
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener

                binding.tvActiveCampaigns.text = "0"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}