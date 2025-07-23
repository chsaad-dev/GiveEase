package com.example.giveease

import android.app.AlertDialog
import android.os.Bundle
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.giveease.auth.LoginFragment
import com.example.giveease.donor.DonorMainFragment
import com.example.giveease.ngo.NgoMainFragment
import com.example.giveease.admin.AdminMainFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_GiveEase)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val role = intent.getStringExtra("role")

        val fragment: Fragment = when (role) {
            "donor" -> DonorMainFragment()
            "ngo" -> NgoMainFragment()
            "admin" -> AdminMainFragment()
            "login" -> LoginFragment()
            else -> LoginFragment()
        }

        supportFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, fragment)
            .commit()

        // Handle system back press with confirmation
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                showExitConfirmation()
            }
        })
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
}
