package com.example.giveease.ngo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.giveease.R
import com.example.giveease.databinding.FragmentNgoMainBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.app.AlertDialog
import android.widget.Toast
import com.example.giveease.verification.IdentityVerificationFragment

class NgoMainFragment : Fragment() {

    private var _binding: FragmentNgoMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNgoMainBinding.inflate(inflater, container, false)
        checkVerificationStatus()
        return binding.root
    }

    private lateinit var homeFragment: Fragment
    private lateinit var chatFragment: Fragment
    private lateinit var campaignFragment: Fragment
    private lateinit var historyFragment: Fragment
    private lateinit var profileFragment: Fragment
    private lateinit var activeFragment: Fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        if (savedInstanceState == null) {
            homeFragment = NgoHomeFragment()
            chatFragment = NgoChatFragment()
            campaignFragment = CreateCampaignFragment()
            historyFragment = NgoHistoryFragment()
            profileFragment = NgoProfileFragment()
            activeFragment = homeFragment

            childFragmentManager.beginTransaction().apply {
                add(R.id.fragmentContainer, profileFragment, "profile").hide(profileFragment)
                add(R.id.fragmentContainer, historyFragment, "history").hide(historyFragment)
                add(R.id.fragmentContainer, campaignFragment, "campaign").hide(campaignFragment)
                add(R.id.fragmentContainer, chatFragment, "chat").hide(chatFragment)
                add(R.id.fragmentContainer, homeFragment, "home")
            }.commit()
        } else {
            homeFragment = childFragmentManager.findFragmentByTag("home") ?: NgoHomeFragment()
            chatFragment = childFragmentManager.findFragmentByTag("chat") ?: NgoChatFragment()
            campaignFragment = childFragmentManager.findFragmentByTag("campaign") ?: CreateCampaignFragment()
            historyFragment = childFragmentManager.findFragmentByTag("history") ?: NgoHistoryFragment()
            profileFragment = childFragmentManager.findFragmentByTag("profile") ?: NgoProfileFragment()

            activeFragment = when (binding.bottomNavigationView.selectedItemId) {
                R.id.nav_home -> homeFragment
                R.id.nav_chat -> chatFragment
                R.id.nav_campaign -> campaignFragment
                R.id.nav_history -> historyFragment
                R.id.nav_profile -> profileFragment
                else -> homeFragment
            }
        }

        setupBottomNavigation()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val targetFragment = when (item.itemId) {
                R.id.nav_home -> homeFragment
                R.id.nav_chat -> chatFragment
                R.id.nav_campaign -> campaignFragment
                R.id.nav_history -> historyFragment
                R.id.nav_profile -> profileFragment
                else -> homeFragment
            }

            if (activeFragment != targetFragment) {
                childFragmentManager.beginTransaction()
                    .hide(activeFragment)
                    .show(targetFragment)
                    .commit()
                activeFragment = targetFragment
            }
            true
        }
    }

    private fun checkVerificationStatus() {
        val uid = FirebaseAuth.getInstance().currentUser?.uid ?: return

        FirebaseFirestore.getInstance()
            .collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val verificationStatus = document.getString("verificationStatus") ?: "pending"

                if (verificationStatus == "pending") {
                    showVerificationPopup()
                } else if (verificationStatus == "rejected") {
                    val reason = document.getString("rejectionReason") ?: "Unknown reason"
                    showRejectedPopup(reason)
                }
            }
    }

    private fun showVerificationPopup() {
        AlertDialog.Builder(requireContext())
            .setTitle("Verify Your NGO")
            .setMessage("To create campaigns, please verify your NGO by uploading government registration documents.\n\nYou can browse but cannot create campaigns until verified.")
            .setPositiveButton("Verify Now") { _, _ ->
                navigateToVerification()
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun showRejectedPopup(reason: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Verification Rejected")
            .setMessage("Your NGO verification was rejected.\n\nReason: $reason\n\nPlease resubmit your documents.")
            .setPositiveButton("Resubmit") { _, _ ->
                navigateToVerification()
            }
            .setNegativeButton("Contact Support") { _, _ ->
                Toast.makeText(requireContext(), "support@giveease.com", Toast.LENGTH_LONG).show()
            }
            .show()
    }

    private fun navigateToVerification() {
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, IdentityVerificationFragment())
            .addToBackStack(null)
            .commit()
    }

    fun handleBackPress(): Boolean {
        val currentFragment = childFragmentManager.findFragmentById(R.id.fragmentContainer)

        if (childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStack()
            return true
        }

        if (binding.bottomNavigationView.selectedItemId != R.id.nav_home) {
            binding.bottomNavigationView.selectedItemId = R.id.nav_home
            return true
        }

        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}