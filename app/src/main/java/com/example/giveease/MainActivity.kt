package com.example.giveease

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.giveease.auth.LoginFragment
import com.example.giveease.donor.DonorMainFragment
import com.example.giveease.ngo.NgoMainFragment
import com.example.giveease.admin.AdminMainFragment
import com.example.giveease.utils.MaintenanceManager

class MainActivity : AppCompatActivity() {

    private var currentRole: String? = null
    private val maintenanceListener: (Boolean) -> Unit = { isActive ->
        if (isActive && currentRole != "admin") {
            redirectToMaintenance()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_GiveEase)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        currentRole = intent.getStringExtra("role")

        val fragment: Fragment = when (currentRole) {
            "donor" -> DonorMainFragment()
            "ngo" -> NgoMainFragment()
            "admin" -> AdminMainFragment()
            "login" -> LoginFragment()
            else -> LoginFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        if (currentRole != "admin" && currentRole != "login") {
            MaintenanceManager.startListening()
            MaintenanceManager.addListener(maintenanceListener)
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                val currentFragment = supportFragmentManager.findFragmentById(R.id.fragment_container)

                when (currentFragment) {
                    is DonorMainFragment -> {
                        if (!currentFragment.handleBackPress()) {
                            showExitConfirmation()
                        }
                    }
                    is NgoMainFragment -> {
                        if (!currentFragment.handleBackPress()) {
                            showExitConfirmation()
                        }
                    }
                    is AdminMainFragment -> {
                        if (!currentFragment.handleBackPress()) {
                            showExitConfirmation()
                        }
                    }
                    else -> showExitConfirmation()
                }
            }
        })
    }

    fun onAuthSuccess(role: String) {
        Log.d("MainActivity", "Auth success with role: $role")
        currentRole = role

        if (role == "donor" || role == "ngo") {
            MaintenanceManager.checkMaintenanceStatus { isActive ->
                Log.d("MainActivity", "Maintenance check after login: $isActive")

                if (isActive) {
                    redirectToMaintenance()
                } else {
                    val fragment: Fragment = when (role) {
                        "donor" -> DonorMainFragment()
                        "ngo" -> NgoMainFragment()
                        else -> LoginFragment()
                    }

                    supportFragmentManager.beginTransaction()
                        .replace(R.id.fragment_container, fragment)
                        .commit()

                    MaintenanceManager.startListening()
                    MaintenanceManager.addListener(maintenanceListener)
                }
            }
        } else {
            supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_container, AdminMainFragment())
                .commit()
        }
    }

    private fun redirectToMaintenance() {
        val intent = Intent(this, MaintenanceActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showExitConfirmation() {
        AlertDialog.Builder(this)
            .setTitle("Exit App")
            .setMessage("Are you sure you want to exit?")
            .setPositiveButton("Yes") { _, _ ->
                finish()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (currentRole != "admin" && currentRole != "login") {
            MaintenanceManager.removeListener(maintenanceListener)
            MaintenanceManager.stopListening()
        }
    }
}