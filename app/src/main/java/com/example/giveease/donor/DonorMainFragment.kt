package com.example.giveease.donor

import android.os.Bundle
import android.view.*
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.google.android.material.bottomnavigation.BottomNavigationView
import androidx.fragment.app.commit

class DonorMainFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_donor_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val bottomNav = view.findViewById<BottomNavigationView>(R.id.bottom_nav_donor)

        // Handle bottom nav visibility on backstack changes
        childFragmentManager.addOnBackStackChangedListener {
            val currentFragment = childFragmentManager.findFragmentById(R.id.fragment_container_donor)
            if (currentFragment is ChatDetailFragment) {
                bottomNav.visibility = View.GONE
            } else {
                bottomNav.visibility = View.VISIBLE
            }
        }

        // Load default fragment
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
                childFragmentManager.beginTransaction()
                    .replace(R.id.fragment_container_donor, newFragment)
                    .addToBackStack(null)
                    .commit()
            }

            true
        }
    }
}
