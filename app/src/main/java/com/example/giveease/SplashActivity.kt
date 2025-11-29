package com.example.giveease

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.AnimationUtils
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.giveease.utils.RoleManager
import com.example.giveease.utils.MaintenanceManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_GiveEase)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        findViewById<ImageView>(R.id.splash_logo).startAnimation(
            AnimationUtils.loadAnimation(this, R.anim.fade_in)
        )

        lifecycleScope.launch {
            delay(1500)
            val role = RoleManager.getCurrentUserRole()

            Log.d("SplashActivity", "Current role: $role")

            if (role == "donor" || role == "ngo") {
                Log.d("SplashActivity", "Checking maintenance for donor/ngo")

                val isMaintenanceActive = suspendCancellableCoroutine { continuation ->
                    MaintenanceManager.checkMaintenanceStatus { isActive ->
                        continuation.resume(isActive)
                    }
                }

                Log.d("SplashActivity", "Maintenance status: $isMaintenanceActive")

                if (isMaintenanceActive) {
                    val maintenanceIntent = Intent(this@SplashActivity, MaintenanceActivity::class.java)
                    startActivity(maintenanceIntent)
                    finish()
                } else {
                    navigateToMainActivity(role)
                }
            } else {
                navigateToMainActivity(role)
            }
        }
    }

    private fun navigateToMainActivity(role: String?) {
        val nextIntent = when (role) {
            "donor" -> Intent(this@SplashActivity, MainActivity::class.java).putExtra("role", "donor")
            "ngo" -> Intent(this@SplashActivity, MainActivity::class.java).putExtra("role", "ngo")
            "admin" -> Intent(this@SplashActivity, MainActivity::class.java).putExtra("role", "admin")
            else -> Intent(this@SplashActivity, MainActivity::class.java).putExtra("role", "login")
        }

        startActivity(nextIntent)
        finish()
    }
}