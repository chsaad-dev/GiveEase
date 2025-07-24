package com.example.giveease.donor

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.example.giveease.R
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.giveease.databinding.FragmentDonorProfileBinding

class DonorProfileFragment : Fragment() {
    private lateinit var binding: FragmentDonorProfileBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentDonorProfileBinding.inflate(inflater, container, false)

        setupProfile()
        setupListeners()
        setupProgressBar()
        return binding.root
    }

    private fun setupProfile() {
        binding.apply {
            // Set profile information
            tvProfileTitle.text = "Profile"
            tvDonorName.text = "Muhammad Saad"
            tvDonorEmail.text = "saad@example.com"

            // Set statistics
            tvTotalDonations.text = "15"
            tvNGOsSupported.text = "6"

            // Set progress
            progressDonationGoal.progress = 70
            tvProgressPercent.text = "70%"

            // Load profile image (assuming you're using Glide)
            /*
            Glide.with(requireContext())
                .load(R.drawable.sample_profile)
                .placeholder(R.drawable.profile_placeholder)
                .error(R.drawable.profile_error)
                .circleCrop()
                .into(imgProfile)
            */
        }
    }

    private fun setupListeners() {
        binding.apply {
            // Edit Profile Button
            btnEditProfile.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    "Edit profile coming soon",
                    Toast.LENGTH_SHORT
                ).show()
            }

            // Profile Image Click
            imgProfile.setOnClickListener {
                Toast.makeText(
                    requireContext(),
                    "Change profile picture coming soon",
                    Toast.LENGTH_SHORT
                ).show()
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