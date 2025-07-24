package com.example.giveease.donor

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import com.example.giveease.R
import android.widget.Toast
import com.example.giveease.databinding.FragmentDonorProfileBinding

class DonorProfileFragment : Fragment() {
    private lateinit var binding: FragmentDonorProfileBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        binding = FragmentDonorProfileBinding.inflate(inflater, container, false)

        binding.tvDonorName.text = "Muhammad Saad"
        binding.tvDonorEmail.text = "saad@example.com"
        binding.tvRole.text = "Role: Donor"

        binding.btnDonationHistory.setOnClickListener {
            Toast.makeText(requireContext(), "Donation history coming soon", Toast.LENGTH_SHORT).show()
        }

        binding.btnGoToSettings.setOnClickListener {
            parentFragmentManager.beginTransaction()
                .replace(R.id.fragment_container_donor, DonorSettingsFragment())
                .addToBackStack(null)
                .commit()
        }

        return binding.root
    }
}
