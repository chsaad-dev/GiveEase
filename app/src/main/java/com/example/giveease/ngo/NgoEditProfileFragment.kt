package com.example.giveease.ngo

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.databinding.FragmentNgoEditProfileBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class NgoEditProfileFragment : Fragment() {

    private var _binding: FragmentNgoEditProfileBinding? = null
    private val binding get() = _binding!!
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNgoEditProfileBinding.inflate(inflater, container, false)
        setupClickListeners()
        loadCurrentProfile()
        return binding.root
    }

    private fun setupClickListeners() {
        binding.btnBack.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }

        binding.btnSave.setOnClickListener {
            saveProfile()
        }

        binding.btnChangeLogo.setOnClickListener {
            Toast.makeText(requireContext(), "Logo upload - Coming Soon", Toast.LENGTH_SHORT).show()
        }
    }

    private fun loadCurrentProfile() {
        val uid = auth.currentUser?.uid ?: return

        firestore.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    binding.etNgoName.setText(document.getString("ngoName") ?: "")
                    binding.etTagline.setText(document.getString("tagline") ?: "")
                    binding.etContactEmail.setText(document.getString("email") ?: "")
                    binding.etPhoneNumber.setText(document.getString("phoneNumber") ?: "")
                    binding.etWebsite.setText(document.getString("website") ?: "")
                    binding.etMission.setText(document.getString("mission") ?: "")
                    binding.etVision.setText(document.getString("vision") ?: "")
                    binding.etCoreValues.setText(document.getString("coreValues") ?: "")
                    binding.etHeadquarters.setText(document.getString("headquarters") ?: "")
                    binding.etCoverage.setText(document.getString("coverage") ?: "")

                    val services = document.get("serviceCategories") as? List<String> ?: emptyList()
                    binding.chipEducation.isChecked = services.contains("Education")
                    binding.chipHealthcare.isChecked = services.contains("Healthcare")
                    binding.chipDisasterRelief.isChecked = services.contains("Disaster Relief")
                    binding.chipPovertyAlleviation.isChecked = services.contains("Poverty Alleviation")
                    binding.chipWomenEmpowerment.isChecked = services.contains("Women Empowerment")
                    binding.chipEnvironment.isChecked = services.contains("Environment")
                }
            }
    }

    private fun saveProfile() {
        val ngoName = binding.etNgoName.text.toString().trim()
        val tagline = binding.etTagline.text.toString().trim()
        val contactEmail = binding.etContactEmail.text.toString().trim()
        val phoneNumber = binding.etPhoneNumber.text.toString().trim()
        val website = binding.etWebsite.text.toString().trim()
        val mission = binding.etMission.text.toString().trim()
        val vision = binding.etVision.text.toString().trim()
        val coreValues = binding.etCoreValues.text.toString().trim()
        val headquarters = binding.etHeadquarters.text.toString().trim()
        val coverage = binding.etCoverage.text.toString().trim()

        if (ngoName.isEmpty()) {
            binding.etNgoName.error = "NGO name is required"
            binding.etNgoName.requestFocus()
            return
        }

        if (tagline.isEmpty()) {
            binding.etTagline.error = "Tagline is required"
            binding.etTagline.requestFocus()
            return
        }

        if (contactEmail.isEmpty()) {
            binding.etContactEmail.error = "Contact email is required"
            binding.etContactEmail.requestFocus()
            return
        }

        if (mission.isEmpty()) {
            binding.etMission.error = "Mission statement is required"
            binding.etMission.requestFocus()
            return
        }

        if (headquarters.isEmpty()) {
            binding.etHeadquarters.error = "Headquarters address is required"
            binding.etHeadquarters.requestFocus()
            return
        }

        val serviceCategories = mutableListOf<String>()
        if (binding.chipEducation.isChecked) serviceCategories.add("Education")
        if (binding.chipHealthcare.isChecked) serviceCategories.add("Healthcare")
        if (binding.chipDisasterRelief.isChecked) serviceCategories.add("Disaster Relief")
        if (binding.chipPovertyAlleviation.isChecked) serviceCategories.add("Poverty Alleviation")
        if (binding.chipWomenEmpowerment.isChecked) serviceCategories.add("Women Empowerment")
        if (binding.chipEnvironment.isChecked) serviceCategories.add("Environment")

        if (serviceCategories.isEmpty()) {
            Toast.makeText(requireContext(), "Please select at least one service category", Toast.LENGTH_SHORT).show()
            return
        }

        val uid = auth.currentUser?.uid ?: return

        binding.btnSave.isEnabled = false
        binding.btnSave.text = "Saving..."

        val updates = hashMapOf<String, Any>(
            "name" to ngoName,
            "ngoName" to ngoName,
            "tagline" to tagline,
            "phoneNumber" to phoneNumber,
            "website" to website,
            "mission" to mission,
            "vision" to vision,
            "coreValues" to coreValues,
            "headquarters" to headquarters,
            "coverage" to coverage,
            "serviceCategories" to serviceCategories,
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("users").document(uid)
            .update(updates)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack()
            }
            .addOnFailureListener { e ->
                Log.e("NgoEditProfile", "Error updating profile", e)
                Toast.makeText(requireContext(), "Failed to update profile", Toast.LENGTH_SHORT).show()
                binding.btnSave.isEnabled = true
                binding.btnSave.text = "Save"
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}