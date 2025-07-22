package com.example.giveease.donor

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentDonorMainBinding

class DonorMainFragment : Fragment() {

    private var _binding: FragmentDonorMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentDonorMainBinding.inflate(inflater, container, false)
        setupBottomNav()
        return binding.root
    }

    private fun setupBottomNav() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment? = when (item.itemId) {
                R.id.nav_home -> DonorHomeFragment()
                R.id.nav_profile -> DonorProfileFragment()
                R.id.nav_settings -> DonorSettingsFragment()
                else -> null
            }

            selectedFragment?.let {
                childFragmentManager.beginTransaction()
                    .replace(R.id.fragmentContainer, it)
                    .commit()
            }
            true
        }

        binding.bottomNavigationView.selectedItemId = R.id.nav_home
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
