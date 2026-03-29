package com.example.giveease.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.giveease.R
import com.example.giveease.auth.LoginFragment
import com.example.giveease.databinding.FragmentAdminSettingsBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminSettingsFragment : Fragment() {

    private var _binding: FragmentAdminSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private var isMaintenanceMode = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentAdminSettingsBinding.inflate(inflater, container, false)
        firestore = FirebaseFirestore.getInstance()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        loadSettings()
        setupClickListeners()
    }

    private fun loadSettings() {
        firestore.collection("settings")
            .document("app_config")
            .get()
            .addOnSuccessListener { document ->
                if (!isAdded || _binding == null) return@addOnSuccessListener

                if (document.exists()) {
                    isMaintenanceMode = document.getBoolean("maintenanceMode") ?: false
                    binding.switchMaintenance.isChecked = isMaintenanceMode
                } else {
                    createDefaultSettings()
                }
            }
            .addOnFailureListener {
                if (!isAdded || _binding == null) return@addOnFailureListener
                createDefaultSettings()
            }
    }

    private fun createDefaultSettings() {
        val defaultSettings = hashMapOf(
            "maintenanceMode" to false,
            "createdAt" to System.currentTimeMillis()
        )

        firestore.collection("settings")
            .document("app_config")
            .set(defaultSettings)
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener
                binding.switchMaintenance.isChecked = false
            }
    }

    private fun setupClickListeners() {
        binding.btnLogout.setOnClickListener {
            showLogoutDialog()
        }

        binding.switchEmailNotif.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "Email notifications ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        binding.switchPushNotif.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "Push notifications ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        binding.switchNgoAlerts.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "NGO alerts ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        binding.switchAutoApprove.setOnCheckedChangeListener { _, isChecked ->
            Toast.makeText(requireContext(), "Auto-approve ${if (isChecked) "enabled" else "disabled"}", Toast.LENGTH_SHORT).show()
        }

        binding.switchMaintenance.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked && !isMaintenanceMode) {
                showMaintenanceConfirmationDialog()
            } else if (!isChecked && isMaintenanceMode) {
                updateMaintenanceMode(false)
            }
        }

        binding.btnViewLogs.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AdminLogsFragment())
                .addToBackStack(null)
                .commit()
        }

        binding.btnBackupData.setOnClickListener {
            showExportDialog()
        }

        binding.btnAbout.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun showMaintenanceConfirmationDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Enable Maintenance Mode?")
            .setMessage("This will immediately block all donors and NGOs from using the app. Only admins will have access.\n\nAre you sure?")
            .setPositiveButton("Enable") { _, _ ->
                updateMaintenanceMode(true)
            }
            .setNegativeButton("Cancel") { _, _ ->
                binding.switchMaintenance.isChecked = false
            }
            .setCancelable(false)
            .show()
    }

    private fun updateMaintenanceMode(enable: Boolean) {
        val updates = hashMapOf<String, Any>(
            "maintenanceMode" to enable,
            "updatedAt" to System.currentTimeMillis()
        )

        firestore.collection("settings")
            .document("app_config")
            .set(updates, com.google.firebase.firestore.SetOptions.merge())
            .addOnSuccessListener {
                if (!isAdded || _binding == null) return@addOnSuccessListener

                isMaintenanceMode = enable
                val statusText = if (enable) "enabled" else "disabled"
                Toast.makeText(
                    requireContext(),
                    "Maintenance mode $statusText",
                    Toast.LENGTH_SHORT
                ).show()
                AdminLogger.logAction("settings_change", "Maintenance Mode", "Admin turned maintenance mode ${if (enable) "ON" else "OFF"}")
            }
            .addOnFailureListener { e ->
                if (!isAdded || _binding == null) return@addOnFailureListener

                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                binding.switchMaintenance.isChecked = !enable
            }
    }

    private fun showLogoutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Logout")
            .setMessage("Are you sure you want to logout?")
            .setPositiveButton("Logout") { _, _ ->
                logout()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()

        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, LoginFragment())
            .commit()

        requireActivity().supportFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
        )

        Toast.makeText(requireContext(), "Logged out successfully", Toast.LENGTH_SHORT).show()
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("About GiveEase")
            .setMessage("GiveEase Admin Panel\nVersion 1.0.0\n\nManage donations, NGOs, and user verifications all in one place.")
            .setPositiveButton("OK", null)
            .show()
    }

    private fun showExportDialog() {
        if (!isAdded) return
        
        val options = arrayOf("Export Users Dataset", "Export All Donations")
        
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Select Data to Export")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> exportUsers()
                    1 -> exportDonations()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun exportUsers() {
        Toast.makeText(requireContext(), "Preparing users export...", Toast.LENGTH_SHORT).show()
        
        firestore.collection("users").get()
            .addOnSuccessListener { documents ->
                if (!isAdded) return@addOnSuccessListener
                
                val csvContent = StringBuilder()
                csvContent.append("ID,Name,Email,Role,Phone,Status\n")
                
                for (doc in documents) {
                    val id = doc.id
                    val name = (doc.getString("name") ?: "").replace(",", " ")
                    val email = doc.getString("email") ?: ""
                    val role = doc.getString("role") ?: ""
                    val phone = doc.getString("phone") ?: ""
                    val status = doc.getString("verificationStatus") ?: "pending"
                    
                    csvContent.append("$id,$name,$email,$role,$phone,$status\n")
                }
                
                saveAndShareCsv(csvContent.toString(), "GiveEase_Users")
            }
            .addOnFailureListener {
                if (isAdded) Toast.makeText(requireContext(), "Failed to fetch users", Toast.LENGTH_SHORT).show()
            }
    }

    private fun exportDonations() {
        Toast.makeText(requireContext(), "Preparing donations export...", Toast.LENGTH_SHORT).show()
        
        firestore.collection("donations").get()
            .addOnSuccessListener { documents ->
                if (!isAdded) return@addOnSuccessListener
                
                val csvContent = StringBuilder()
                csvContent.append("DonationID,DonorName,NGOName,CampaignTitle,Quantity,Unit,Status\n")
                
                for (doc in documents) {
                    val id = doc.id
                    val donor = (doc.getString("donorName") ?: "Anonymous").replace(",", " ")
                    val ngo = (doc.getString("ngoName") ?: "Unknown NGO").replace(",", " ")
                    val campaign = (doc.getString("campaignTitle") ?: "General").replace(",", " ")
                    val qty = doc.getLong("quantity") ?: 0
                    val unit = doc.getString("unit") ?: "Items"
                    val status = doc.getString("status") ?: "Completed"
                    
                    csvContent.append("$id,$donor,$ngo,$campaign,$qty,$unit,$status\n")
                }
                
                saveAndShareCsv(csvContent.toString(), "GiveEase_Donations")
            }
            .addOnFailureListener {
                if (isAdded) Toast.makeText(requireContext(), "Failed to fetch donations", Toast.LENGTH_SHORT).show()
            }
    }

    private fun saveAndShareCsv(csvData: String, fileNamePrefix: String) {
        try {
            val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
            val fileName = "${fileNamePrefix}_$dateStr.csv"
            
            // Create a specific folder in cache to hold exports
            val exportsDir = File(requireContext().cacheDir, "exports")
            if (!exportsDir.exists()) {
                exportsDir.mkdirs()
            }
            
            val file = File(exportsDir, fileName)
            val writer = FileWriter(file)
            writer.write(csvData)
            writer.flush()
            writer.close()
            
            val uri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.fileprovider", file)
            
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            startActivity(Intent.createChooser(shareIntent, "Save or Share Data"))
            AdminLogger.logAction("settings_change", "Data Export", "Admin exported $fileNamePrefix CSV")
            
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(requireContext(), "Error exporting data: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}