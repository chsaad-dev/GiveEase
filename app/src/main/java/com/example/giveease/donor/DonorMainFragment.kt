package com.example.giveease.donor

import android.os.Bundle
import android.view.*
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.commit

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

        childFragmentManager.addOnBackStackChangedListener {
            val currentFragment = childFragmentManager.findFragmentById(R.id.fragment_container_donor)
            if (currentFragment is ChatDetailFragment) {
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

    /**
     * Handles back press navigation
     * @return true if back press was handled, false if should exit app
     */
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