package com.example.giveease

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.giveease.utils.RoleManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_GiveEase)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        lifecycleScope.launch {
            delay(1500) // splash delay
            navigateToNextScreen()
        }
    }

    private fun navigateToNextScreen() {
        val role = RoleManager.getCurrentUserRole()

        val nextIntent = when (role) {
            "donor" -> Intent(this, MainActivity::class.java).putExtra("role", "donor")
            "ngo" -> Intent(this, MainActivity::class.java).putExtra("role", "ngo")
            "admin" -> Intent(this, MainActivity::class.java).putExtra("role", "admin")
            else -> Intent(this, MainActivity::class.java)
        }

        startActivity(nextIntent)
        finish()
    }
}
