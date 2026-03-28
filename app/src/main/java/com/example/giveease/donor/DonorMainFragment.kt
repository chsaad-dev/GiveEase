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

    private lateinit var homeFragment: Fragment
    private lateinit var feedFragment: Fragment
    private lateinit var chatFragment: Fragment
    private lateinit var profileFragment: Fragment
    private lateinit var activeFragment: Fragment

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
            homeFragment = DonorHomeFragment()
            feedFragment = DonorFeedFragment()
            chatFragment = DonorChatFragment()
            profileFragment = DonorProfileFragment()
            activeFragment = homeFragment

            childFragmentManager.beginTransaction().apply {
                add(R.id.fragment_container_donor, profileFragment, "profile").hide(profileFragment)
                add(R.id.fragment_container_donor, chatFragment, "chat").hide(chatFragment)
                add(R.id.fragment_container_donor, feedFragment, "feed").hide(feedFragment)
                add(R.id.fragment_container_donor, homeFragment, "home")
            }.commit()
        } else {
            homeFragment = childFragmentManager.findFragmentByTag("home") ?: DonorHomeFragment()
            feedFragment = childFragmentManager.findFragmentByTag("feed") ?: DonorFeedFragment()
            chatFragment = childFragmentManager.findFragmentByTag("chat") ?: DonorChatFragment()
            profileFragment = childFragmentManager.findFragmentByTag("profile") ?: DonorProfileFragment()

            activeFragment = when (bottomNav.selectedItemId) {
                R.id.nav_home -> homeFragment
                R.id.nav_feed -> feedFragment
                R.id.nav_chat -> chatFragment
                R.id.nav_profile -> profileFragment
                else -> homeFragment
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            val targetFragment = when (item.itemId) {
                R.id.nav_home -> homeFragment
                R.id.nav_feed -> feedFragment
                R.id.nav_chat -> chatFragment
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

        if (bottomNav.selectedItemId != R.id.nav_home) {
            bottomNav.selectedItemId = R.id.nav_home
            return true
        }

        return false
    }
}