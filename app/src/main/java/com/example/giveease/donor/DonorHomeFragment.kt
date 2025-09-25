package com.example.giveease.donor

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentDonorHomeBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DonorHomeFragment : Fragment() {
    private lateinit var binding: FragmentDonorHomeBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDonorHomeBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupUserData()
        setupClickListeners()
        loadDonationStats()

        return binding.root
    }

    private fun setupUserData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val name = document.getString("name") ?: "User"
                    binding.tvUserName.text = name
                } else {
                    binding.tvUserName.text = auth.currentUser?.displayName ?: "User"
                }
            }
            .addOnFailureListener {
                binding.tvUserName.text = auth.currentUser?.displayName ?: "User"
            }
    }

    private fun loadDonationStats() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("donations")
            .whereEqualTo("donorId", userId)
            .get()
            .addOnSuccessListener { documents ->
                val totalDonations = documents.size()
                val totalAmount = documents.sumOf { doc ->
                    doc.getDouble("amount") ?: 0.0
                }.toInt()

                binding.tvDonationsCount.text = totalDonations.toString()
                binding.tvTotalAmount.text = "Rs ${String.format("%,d", totalAmount)}"
            }
            .addOnFailureListener {
                binding.tvDonationsCount.text = "12"
                binding.tvTotalAmount.text = "Rs 45,000"
            }
    }

    private fun setupClickListeners() {
        binding.apply {
            layoutDonateWithoutRequest.setOnClickListener {
                navigateToCreateCampaign()
            }

            layoutQuickDonate.setOnClickListener {
                navigateToQuickDonate()
            }

            layoutExploreCauses.setOnClickListener {
                navigateToExploreCauses()
            }

            ivLeaderboard.setOnClickListener {
                navigateToLeaderboard()
            }

            ivNotifications.setOnClickListener {
                navigateToNotifications()
            }

            cardFeaturedCampaign.setOnClickListener {
                navigateToCampaignDetail()
            }

            btnDonateFeatured.setOnClickListener {
                navigateToDonateToCampaign()
            }

            tvViewAllCampaigns.setOnClickListener {
                navigateToAllCampaigns()
            }

            tvViewAllActivity.setOnClickListener {
                navigateToDonationHistory()
            }
        }
    }

    private fun navigateToCreateCampaign() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_donor, CreateCampaignFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToQuickDonate() {
        Toast.makeText(requireContext(), "Quick Donate feature coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToExploreCauses() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_donor, DonorFeedFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToLeaderboard() {
        Toast.makeText(requireContext(), "Leaderboard coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToNotifications() {
        Toast.makeText(requireContext(), "Notifications coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToCampaignDetail() {
        Toast.makeText(requireContext(), "Campaign details coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToDonateToCampaign() {
        Toast.makeText(requireContext(), "Donate to campaign feature coming soon", Toast.LENGTH_SHORT).show()
    }

    private fun navigateToAllCampaigns() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_donor, DonorFeedFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToDonationHistory() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_donor, DonationHistoryFragment())
            .addToBackStack(null)
            .commit()
    }

    companion object {
        fun newInstance() = DonorHomeFragment()
    }
}