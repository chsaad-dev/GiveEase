package com.example.giveease.donor

import android.os.Bundle
import android.view.*
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.commit
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.app.AlertDialog
import android.widget.Toast
import com.example.giveease.verification.IdentityVerificationFragment

class DonorMainFragment : Fragment() {

    private lateinit var bottomNav: BottomNavigationView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_donor_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        bottomNav = view.findViewById(R.id.bottom_nav_donor)

        checkVerificationStatus()

        childFragmentManager.addOnBackStackChangedListener {
            val currentFragment = childFragmentManager.findFragmentById(R.id.fragment_container_donor)
            if (currentFragment is ChatDetailFragment || currentFragment is CampaignDetailsFragment) {
                bottomNav.visibility = View.GONE
            } else {
                bottomNav.visibility = View.VISIBLE
            }
        }

        if (savedInstanceState == null) {
            childFragmentManager.commit {
                replace(R.id.fragment_container_donor, DonorHomeFragment())
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            val newFragment = when (item.itemId) {
                R.id.nav_home -> DonorHomeFragment()
                R.id.nav_feed -> DonorFeedFragment()
                R.id.nav_chat -> DonorChatFragment()
                R.id.nav_profile -> DonorProfileFragment()
                else -> DonorHomeFragment()
            }

            val currentFragment =
                childFragmentManager.findFragmentById(R.id.fragment_container_donor)

            if (currentFragment?.javaClass != newFragment.javaClass) {
                childFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

                childFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_donor, newFragment)
                    .commit()
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
            .setTitle("Verify Your Identity")
            .setMessage("To donate and use all features, please verify your identity.\n\nYou can browse campaigns but cannot donate until verified.")
            .setPositiveButton("Verify Now") { _, _ ->
                navigateToVerification()
            }
            .setNegativeButton("Later", null)
            .show()
    }

    private fun showRejectedPopup(reason: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Verification Rejected")
            .setMessage("Your verification was rejected.\n\nReason: $reason\n\nPlease resubmit your documents.")
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
        val currentFragment = childFragmentManager.findFragmentById(R.id.fragment_container_donor)

        if (childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStack()
            return true
        }

        if (currentFragment !is DonorHomeFragment) {
            bottomNav.selectedItemId = R.id.nav_home
            childFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_donor, DonorHomeFragment())
                .commit()
            return true
        }

        return false
    }
}