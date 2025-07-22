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
                R.id.nav_home -> childFragmentManager.commit {
                    replace(R.id.fragmentContainer, NgoHomeFragment())
                }
                R.id.nav_profile -> {}
                R.id.nav_settings -> {}
            }
            true
        }
    }

    private fun loadDefaultFragment() {
        childFragmentManager.commit {
            replace(R.id.fragmentContainer, NgoHomeFragment())
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
