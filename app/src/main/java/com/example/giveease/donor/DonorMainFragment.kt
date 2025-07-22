package com.example.giveease.donor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import com.example.giveease.R
import com.example.giveease.databinding.FragmentDonorMainBinding

class DonorMainFragment : Fragment() {

    private var _binding: FragmentDonorMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDonorMainBinding.inflate(inflater, container, false)
        setupBottomNavigation()
        loadDefaultFragment()
        return binding.root
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> childFragmentManager.commit {
                    replace(R.id.fragmentContainer, DonorHomeFragment())
                }
                R.id.nav_profile -> {} // Add your Profile fragment
                R.id.nav_settings -> {} // Add your Settings fragment
            }
            true
        }
    }

    private fun loadDefaultFragment() {
        childFragmentManager.commit {
            replace(R.id.fragmentContainer, DonorHomeFragment())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
