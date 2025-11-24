package com.example.giveease.ngo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.giveease.R
import com.example.giveease.databinding.FragmentNgoMainBinding

class NgoMainFragment : Fragment() {

    private var _binding: FragmentNgoMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNgoMainBinding.inflate(inflater, container, false)
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
                        replace(R.id.fragmentContainer, NgoHomeFragment())
                    }
                }
                R.id.nav_chat -> {
                    childFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    childFragmentManager.commit {
                        replace(R.id.fragmentContainer, ChatListFragment())
                    }
                }
                R.id.nav_campaign -> {
                    childFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    childFragmentManager.commit {
                        replace(R.id.fragmentContainer, CreateCampaignFragment())
                    }
                }
                R.id.nav_history -> {
                    childFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    childFragmentManager.commit {
                        replace(R.id.fragmentContainer, NgoHistoryFragment())
                    }
                }
                R.id.nav_profile -> {
                    childFragmentManager.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)
                    childFragmentManager.commit {
                        replace(R.id.fragmentContainer, NgoProfileFragment())
                    }
                }
            }
            true
        }
    }

    private fun loadDefaultFragment() {
        childFragmentManager.commit {
            replace(R.id.fragmentContainer, NgoHomeFragment())
        }
    }

    fun handleBackPress(): Boolean {
        val currentFragment = childFragmentManager.findFragmentById(R.id.fragmentContainer)

        if (childFragmentManager.backStackEntryCount > 0) {
            childFragmentManager.popBackStack()
            return true
        }

        if (currentFragment !is NgoHomeFragment) {
            binding.bottomNavigationView.selectedItemId = R.id.nav_home
            childFragmentManager.beginTransaction()
                .replace(R.id.fragmentContainer, NgoHomeFragment())
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