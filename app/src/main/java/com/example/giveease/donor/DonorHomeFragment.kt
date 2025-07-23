package com.example.giveease.donor

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.example.giveease.databinding.FragmentDonorHomeBinding

class DonorHomeFragment : Fragment() {
    private lateinit var binding: FragmentDonorHomeBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDonorHomeBinding.inflate(inflater, container, false)
        return binding.root
    }
}
