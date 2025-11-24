package com.example.giveease.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.giveease.R
import com.example.giveease.databinding.FragmentAdminMainBinding

class AdminMainFragment : Fragment() {

    private var _binding: FragmentAdminMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminMainBinding.inflate(inflater, container, false)
        setupBottomNavigation()
        loadDefaultFragment()
        return binding.root
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    childFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    childFragmentManager.commit {
                        replace(R.id.fragmentContainer, AdminDashboardFragment())
                    }
                }
                R.id.nav_profile -> {
                    childFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    childFragmentManager.commit {
                        replace(R.id.fragmentContainer, AdminProfileFragment())
                    }
                }
                R.id.nav_settings -> {
                    childFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    childFragmentManager.commit {
                        replace(R.id.fragmentContainer, AdminSettingsFragment())
                    }
                }
            }
            true
        }
    }

    private fun loadDefaultFragment() {
        childFragmentManager.commit {
            replace(R.id.fragmentContainer, AdminDashboardFragment())
        }
    }

    fun handleBackPress(): Boolean {
        val currentFragment = childFragmentManager.findFragmentById(R.id.fragmentContainer)

        if (childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStack()
            return true
        }

        if (currentFragment !is AdminDashboardFragment) {
            binding.bottomNavigationView.selectedItemId = R.id.nav_home
            childFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, AdminDashboardFragment())
                .commit()
            return true
        }

        return false
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}