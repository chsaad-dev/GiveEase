package com.example.giveease.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.giveease.R
import com.example.giveease.databinding.FragmentAdminDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

import androidx.lifecycle.ViewModelProvider
import com.example.giveease.admin.AdminActivity
import com.example.giveease.admin.AdminActivityAdapter

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var activityAdapter: AdminActivityAdapter
    private lateinit var viewModel: AdminDashboardViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        viewModel = ViewModelProvider(requireActivity())[AdminDashboardViewModel::class.java]

        setupActivityRecyclerView()
        setupClickListeners()
        observeViewModel()
        
        // Trigger data load if not loaded
        viewModel.loadData(auth.currentUser?.uid)
    }

    private fun observeViewModel() {
        viewModel.totalNgos.observe(viewLifecycleOwner) { binding.tvTotalNgos.text = it }
        viewModel.totalDonors.observe(viewLifecycleOwner) { binding.tvTotalDonors.text = it }
        viewModel.pendingApprovals.observe(viewLifecycleOwner) { binding.tvPendingApprovals.text = it }
        
        viewModel.pendingApprovalsCount.observe(viewLifecycleOwner) { count ->
            binding.tvNotificationBadge.text = count.toString()
            binding.tvNotificationBadge.visibility = if (count > 0) View.VISIBLE else View.GONE
        }
        
        viewModel.activeCampaigns.observe(viewLifecycleOwner) { binding.tvActiveCampaigns.text = it }
        
        viewModel.recentActivities.observe(viewLifecycleOwner) { activities ->
            activityAdapter.submitList(activities)
        }
        
        viewModel.adminName.observe(viewLifecycleOwner) { name ->
            binding.tvAdminName.text = name
        }
    }

    private fun setupActivityRecyclerView() {
        activityAdapter = AdminActivityAdapter()
        binding.recyclerViewActivity.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerViewActivity.adapter = activityAdapter
    }

    private fun setupClickListeners() {
        binding.btnNotifications.setOnClickListener {
            Toast.makeText(requireContext(), "Notifications", Toast.LENGTH_SHORT).show()
        }

        binding.btnApproveNgos.setOnClickListener {
            navigateToFragment(VerificationApprovalsFragment())
        }

        binding.btnReviewCampaigns.setOnClickListener {
            navigateToFragment(AdminCampaignReviewFragment())
        }

        binding.btnManageUsers.setOnClickListener {
            navigateToFragment(ManageUsersFragment())
        }
        
        binding.btnDonationTracker.setOnClickListener {
            navigateToFragment(AdminDonationTrackerFragment())
        }
        
        binding.btnSystemLogs.setOnClickListener {
            navigateToFragment(AdminLogsFragment())
        }
        
        binding.btnSupportTickets.setOnClickListener {
            navigateToFragment(AdminSupportTicketsFragment())
        }
    }

    private fun navigateToFragment(fragment: Fragment) {
        if (!isAdded) return

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack(null)
            .commit()
    }

    // Data loading logic has been moved to AdminDashboardViewModel to provide caching


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}