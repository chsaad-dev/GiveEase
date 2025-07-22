package com.example.giveease.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentAdminMainBinding

class AdminMainFragment : Fragment() {

    private var _binding: FragmentAdminMainBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminMainBinding.inflate(inflater, container, false)
        setupBottomNav()
        return binding.root
    }

    private fun setupBottomNav() {
        val fragmentManager = childFragmentManager

        binding.bottomNavigationView.setOnItemSelectedListener { item ->
            val selectedFragment: Fragment? = when (item.itemId) {
                R.id.nav_home -> AdminDashboardFragment()
                R.id.nav_profile -> AdminProfileFragment()
                R.id.nav_settings -> AdminSettingsFragment()
                else -> null
            }
            selectedFragment?.let {
                fragmentManager.beginTransaction()
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
