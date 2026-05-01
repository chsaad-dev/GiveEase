package com.example.giveease.donor

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.example.giveease.R
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.example.giveease.databinding.FragmentDonorProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class DonorProfileFragment : Fragment() {
    private var _binding: FragmentDonorProfileBinding? = null
    private val binding get() = _binding!!
    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentDonorProfileBinding.inflate(inflater, container, false)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        setupListeners()
        setupProgressBar()
        loadUserData()
        loadDonationStats()

        return binding.root
    }

    private fun loadUserData() {
        val userId = auth.currentUser?.uid ?: return
        if (!isAdded || _binding == null) return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (document.exists()) {
                    binding.apply {
                        tvDonorName.text = document.getString("name") ?: "GiveEase User"
                        tvDonorEmail.text = document.getString("email") ?: auth.currentUser?.email

                        val profileImageUrl = document.getString("profileImageUrl")
                        if (!profileImageUrl.isNullOrEmpty()) {
                            context?.let { ctx ->
                                Glide.with(ctx)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.sample_profile)
                                    .error(R.drawable.sample_profile)
                                    .circleCrop()
                                    .into(imgProfile)
                            }
                        } else {
                            imgProfile.setImageResource(R.drawable.sample_profile)
                        }
                    }
                } else {
                    binding.apply {
                        tvDonorName.text = auth.currentUser?.displayName ?: "GiveEase User"
                        tvDonorEmail.text = auth.currentUser?.email ?: "user@example.com"
                        imgProfile.setImageResource(R.drawable.sample_profile)
                    }
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                binding.apply {
                    tvDonorName.text = auth.currentUser?.displayName ?: "GiveEase User"
                    tvDonorEmail.text = auth.currentUser?.email ?: "user@example.com"
                    imgProfile.setImageResource(R.drawable.sample_profile)
                }
            }
    }

    private fun loadDonationStats() {
        val userId = auth.currentUser?.uid ?: return
        if (!isAdded || _binding == null) return

        firestore.collection("donations")
            .whereEqualTo("donorId", userId)
            .get()
            .addOnSuccessListener { documents ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                val totalDonations = documents.size()
                val totalItems = documents.sumOf { doc ->
                    (doc.getLong("quantity") ?: 0).toInt()
                }

                val uniqueNGOs = documents.mapNotNull { doc ->
                    doc.getString("ngoId")
                }.distinct().size

                val peopleHelped = (totalItems / 10).coerceAtLeast(0)

                binding.apply {
                    tvTotalDonations.text = totalDonations.toString()
                    tvNGOsSupported.text = uniqueNGOs.toString()
                    tvTotalAmount.text = "$totalItems Items"
                    tvPeopleHelped.text = peopleHelped.toString()
                }

                val monthlyGoal = 100
                val monthlyProgress = if (totalItems > 0) {
                    ((totalItems.toFloat() / monthlyGoal) * 100).toInt().coerceAtMost(100)
                } else {
                    0
                }
                binding.progressDonationGoal.progress = monthlyProgress
                binding.tvProgressPercent.text = "$monthlyProgress%"
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                binding.apply {
                    tvTotalDonations.text = "0"
                    tvNGOsSupported.text = "0"
                    tvTotalAmount.text = "0 Items"
                    tvPeopleHelped.text = "0"
                    progressDonationGoal.progress = 0
                    tvProgressPercent.text = "0%"
                }
            }
    }

    private fun setupListeners() {
        binding.apply {
            imgProfile.setOnClickListener {
                if (!isAdded) return@setOnClickListener
                parentFragmentManager.beginTransaction()
                    .hide(this@DonorProfileFragment)
                    .add(R.id.fragment_container_donor, EditProfileFragment())
                    .addToBackStack(null)
                    .commit()
            }

            btnDonationHistory.setOnClickListener {
                navigateToDonationHistory()
            }

            btnMyCampaigns.setOnClickListener {
                navigateToImpactDashboard()
            }

            btnGoToSettings.setOnClickListener {
                navigateToSettings()
            }
        }
    }

    private fun navigateToImpactDashboard() {
        if (!isAdded) return
        parentFragmentManager.beginTransaction()
            .hide(this)
            .add(R.id.fragment_container_donor, ImpactDashboardFragment())
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
        if (!isAdded) return
        parentFragmentManager.beginTransaction()
            .hide(this)
            .add(R.id.fragment_container_donor, DonationHistoryFragment())
            .addToBackStack(null)
            .commit()
    }

    private fun navigateToSettings() {
        if (!isAdded) return
        parentFragmentManager.beginTransaction()
            .hide(this)
            .add(R.id.fragment_container_donor, DonorSettingsFragment())
            .addToBackStack(null)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        loadUserData()
        loadDonationStats()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = DonorProfileFragment()
    }
}