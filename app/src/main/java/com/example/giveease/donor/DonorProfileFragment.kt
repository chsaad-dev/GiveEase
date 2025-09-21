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

                        // Load other profile data
                        // You can add more fields as needed
                    }
                }
            }
            .addOnFailureListener {
                // Handle error - maybe show default values
                binding.apply {
                    tvDonorName.text = auth.currentUser?.displayName ?: "User Name"
                    tvDonorEmail.text = auth.currentUser?.email ?: "user@example.com"
                }
            }
    }

    private fun setupProfile() {
        binding.apply {
            // Set profile information with Firebase data or defaults
            tvProfileTitle.text = "Profile"

            // Set statistics (you can load these from Firebase)
            tvTotalDonations.text = "15"
            tvNGOsSupported.text = "6"

            // Set progress
            progressDonationGoal.progress = 70
            tvProgressPercent.text = "70%"
        }
    }

    private fun setupListeners() {
        binding.apply {
            // Edit Profile Button
            btnEditProfile.setOnClickListener {
                navigateToEditProfile()
            }

            // Profile Image Click
            imgProfile.setOnClickListener {
                navigateToEditProfile()
            }

            // Donation History Button
            btnDonationHistory.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    "Donation history coming soon",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Settings Button
            btnGoToSettings.setOnClickListener {
                navigateToSettings()
            }
        }
    }

    private fun setupProgressBar() {
        binding.progressDonationGoal.apply {
            // Set progress bar color
            progressDrawable?.setColorFilter(
                ContextCompat.getColor(requireContext(), R.color.primary),
                android.graphics.PorterDuff.Mode.SRC_IN
            )

            // Set progress
            progress = 70
            max = 100
        }
    }

    private fun navigateToEditProfile() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_donor, EditProfileFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToSettings() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container_donor, DonorSettingsFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun getDummyDonationData(): List<DonationItem> {
        return listOf(
            DonationItem(
                ngoName = "Food for All",
                date = "Jul 10, 2025",
                amount = "Rs 15,000"
            ),
            DonationItem(
                ngoName = "Health Help",
                date = "Jun 26, 2025",
                amount = "Rs 1,500"
            )
        )
    }

    // Data class for donation items
    data class DonationItem(
        val ngoName: String,
        val date: String,
        val amount: String
    )

    companion object {
        fun newInstance() = DonorProfileFragment()
    }
}