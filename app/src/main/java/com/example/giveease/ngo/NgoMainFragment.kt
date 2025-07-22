package com.example.giveease.ngo

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentNgoMainBinding

class NgoMainFragment : Fragment() {

    private var _binding: FragmentNgoMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentNgoMainBinding.inflate(inflater, container, false)
        setupBottomNav()
        return binding.root
    }

    private fun setupBottomNav() {
        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment? = when (item.itemId) {
                R.id.nav_home -> NgoHomeFragment()
                R.id.nav_profile -> NgoProfileFragment()
                R.id.nav_settings -> NgoSettingsFragment()
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
