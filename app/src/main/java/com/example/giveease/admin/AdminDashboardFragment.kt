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

class AdminDashboardFragment : Fragment() {

    private var _binding: FragmentAdminDashboardBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var activityAdapter: AdminActivityAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminDashboardBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupActivityRecyclerView()
        setupClickListeners()
        loadDashboardData()
        loadAdminName()
        loadRecentActivity()
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
    }

    private fun navigateToFragment(fragment: Fragment) {
        if (!isAdded) return

        parentFragmentManager.beginTransaction()
            .replace(R.id.fragmentContainer, fragment)
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

    private fun loadRecentActivity() {
        val activities = mutableListOf<AdminActivity>()
        val sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L)
        var queriesCompleted = 0
        val totalQueries = 3

        fun checkAndUpdate() {
            queriesCompleted++
            if (queriesCompleted >= totalQueries) {
                if (!isAdded || _binding == null) return

                activities.sortByDescending { it.timestamp }
                val topActivities = activities.take(10)
                activityAdapter.submitList(topActivities)
            }
        }

        // 1. Recent verifications (approved/rejected)
        firestore.collection("users")
            .whereIn("verificationStatus", listOf("verified", "rejected"))
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val updatedAt = doc.getLong("updatedAt") ?: doc.getLong("verifiedAt") ?: 0
                    if (updatedAt < sevenDaysAgo) continue

                    val status = doc.getString("verificationStatus") ?: ""
                    val name = doc.getString("name") ?: "Unknown"
                    val role = if (doc.getString("role") == "ngo") "NGO" else "Donor"

                    if (status == "verified") {
                        activities.add(AdminActivity(
                            type = "verification",
                            title = "$role Approved",
                            subtitle = "$name was verified",
                            timestamp = updatedAt
                        ))
                    } else if (status == "rejected") {
                        activities.add(AdminActivity(
                            type = "rejection",
                            title = "$role Rejected",
                            subtitle = "$name was rejected",
                            timestamp = updatedAt
                        ))
                    }
                }
                checkAndUpdate()
            }
            .addOnFailureListener { checkAndUpdate() }

        // 2. Recent donations
        firestore.collection("donations")
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val timestamp = doc.getLong("timestamp") ?: 0
                    if (timestamp < sevenDaysAgo) continue

                    val ngoName = doc.getString("ngoName") ?: "Unknown NGO"
                    val quantity = doc.getLong("quantity") ?: 0

                    activities.add(AdminActivity(
                        type = "donation",
                        title = "New Donation",
                        subtitle = "$quantity items donated to $ngoName",
                        timestamp = timestamp
                    ))
                }
                checkAndUpdate()
            }
            .addOnFailureListener { checkAndUpdate() }

        // 3. Recent campaigns
        firestore.collection("campaigns")
            .get()
            .addOnSuccessListener { documents ->
                for (doc in documents) {
                    val createdAt = doc.getLong("createdAt") ?: 0
                    if (createdAt < sevenDaysAgo) continue

                    val title = doc.getString("title") ?: "Unknown"
                    val ngoName = doc.getString("ngoName") ?: "Unknown"

                    activities.add(AdminActivity(
                        type = "campaign",
                        title = "Campaign Created",
                        subtitle = "\"$title\" by $ngoName",
                        timestamp = createdAt
                    ))
                }
                checkAndUpdate()
            }
            .addOnFailureListener { checkAndUpdate() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}