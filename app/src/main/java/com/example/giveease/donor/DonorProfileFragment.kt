package com.example.giveease.donor

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.example.giveease.R
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.giveease.databinding.FragmentDonorProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DonorProfileFragment : Fragment() {
    private lateinit var binding: FragmentDonorProfileBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDonorProfileBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupProfile()
        setupListeners()
        setupProgressBar()
        loadUserData()
        loadDonationStats()

        return binding.root
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.apply {
                        tvDonorName.text = document.getString("name") ?: "User Name"
                        tvDonorEmail.text = document.getString("email") ?: auth.currentUser?.email
                    }
                } else {
                    binding.apply {
                        tvDonorName.text = auth.currentUser?.displayName ?: "User Name"
                        tvDonorEmail.text = auth.currentUser?.email ?: "user@example.com"
                    }
                }
            }
            .addOnFailureListener {
                binding.apply {
                    tvDonorName.text = auth.currentUser?.displayName ?: "User Name"
                    tvDonorEmail.text = auth.currentUser?.email ?: "user@example.com"
                }
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

                val uniqueNGOs = documents.mapNotNull { doc ->
                    doc.getString("ngoId")
                }.distinct().size

                val peopleHelped = (totalAmount / 500).coerceAtLeast(0)

                binding.apply {
                    tvTotalDonations.text = totalDonations.toString()
                    tvNGOsSupported.text = uniqueNGOs.toString()
                    tvTotalAmount.text = "Rs ${String.format("%,d", totalAmount)}"
                    tvPeopleHelped.text = peopleHelped.toString()
                }

                val monthlyGoal = 10000
                val monthlyProgress = ((totalAmount % monthlyGoal) * 100 / monthlyGoal).coerceAtMost(100)
                binding.progressDonationGoal.progress = monthlyProgress
                binding.tvProgressPercent.text = "$monthlyProgress%"
            }
            .addOnFailureListener {
                setupDummyStats()
            }
    }

    private fun setupDummyStats() {
        binding.apply {
            tvTotalDonations.text = "15"
            tvNGOsSupported.text = "6"
            tvTotalAmount.text = "Rs 45,500"
            tvPeopleHelped.text = "127"
            progressDonationGoal.progress = 70
            tvProgressPercent.text = "70%"
        }
    }

    private fun setupProfile() {
        binding.apply {
        }
    }

    private fun setupListeners() {
        binding.apply {
            imgProfile.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    "Profile picture update available in Settings",
                    Toast.LENGTH_SHORT
                ).show()
            }

            btnDonationHistory.setOnClickListener {
                navigateToDonationHistory()
            }

            btnMyCampaigns.setOnClickListener {
                navigateToCampaigns()
            }

            btnGoToSettings.setOnClickListener {
                navigateToSettings()
            }
        }
    }

    private fun navigateToCampaigns() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_donor, DonorCampaignsFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun setupProgressBar() {
        binding.progressDonationGoal.apply {
            progressDrawable?.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.secondary),
                android.graphics.PorterDuff.Mode.SRC_IN
            )
            max = 100
        }
    }

    private fun navigateToDonationHistory() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_donor, DonationHistoryFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToSettings() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_donor, DonorSettingsFragment())
            .addToBackStack(null)
            .commit()
    }

    data class DonationStats(
        val totalDonations: Int,
        val totalAmount: Double,
        val ngosSupported: Int,
        val peopleHelped: Int
    )

    data class Achievement(
        val title: String,
        val description: String,
        val icon: String,
        val dateEarned: Long
    )

    companion object {
        fun newInstance() = DonorProfileFragment()
    }
}