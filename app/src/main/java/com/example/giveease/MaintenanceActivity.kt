package com.example.giveease

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.giveease.databinding.ActivityMaintenanceBinding
import com.example.giveease.utils.MaintenanceManager
import com.google.firebase.auth.FirebaseAuth

class MaintenanceActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMaintenanceBinding
    private val maintenanceListener: (Boolean) -> Unit = { isActive ->
        if (!isActive) {
            returnToApp()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMaintenanceBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupListeners()
        startMaintenanceMonitoring()
    }

    private fun setupListeners() {
        binding.btnRefresh.setOnClickListener {
            checkMaintenanceStatus()
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun startMaintenanceMonitoring() {
        MaintenanceManager.startListening()
        MaintenanceManager.addListener(maintenanceListener)
    }

    private fun checkMaintenanceStatus() {
        binding.progressBar.visibility = View.VISIBLE
        binding.btnRefresh.isEnabled = false

        MaintenanceManager.checkMaintenanceStatus { isActive ->
            binding.progressBar.visibility = View.GONE
            binding.btnRefresh.isEnabled = true

            if (!isActive) {
                returnToApp()
            }
        }
    }

    private fun returnToApp() {
        MaintenanceManager.removeListener(maintenanceListener)
        MaintenanceManager.stopListening()

        val intent = Intent(this, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun logout() {
        FirebaseAuth.getInstance().signOut()

        MaintenanceManager.removeListener(maintenanceListener)
        MaintenanceManager.stopListening()

        val intent = Intent(this, MainActivity::class.java)
        intent.putExtra("role", "login")
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        MaintenanceManager.removeListener(maintenanceListener)
    }
}