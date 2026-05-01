package com.example.giveease.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.giveease.databinding.FragmentOurTeamBinding

class OurTeamFragment : Fragment() {

    private var _binding: FragmentOurTeamBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentOurTeamBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnBack.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        binding.tvName1.text = "Muhammad Saad"
        binding.tvReg1.text = "FA22-BSE-075"
        binding.tvEmail1.text = "saadw7751@gmail.com"
        binding.tvPhone1.text = "+92 311 1223381"

        binding.tvName2.text = "Muhammad Ali"
        binding.tvReg2.text = "FA22-BSE-114"
        binding.tvEmail2.text = "chali@gmail.com"
        binding.tvPhone2.text = "+92 310 6063135"

        binding.tvName3.text = "Usman Javed"
        binding.tvReg3.text = "FA22-BSE-130"
        binding.tvEmail3.text = "uj45959@gmail.com"
        binding.tvPhone3.text = "+92 330 0288770"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance() = OurTeamFragment()
    }
}
