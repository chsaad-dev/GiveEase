package com.example.giveease

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.giveease.auth.LoginFragment
import com.example.giveease.utils.RoleManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.view.animation.AnimationUtils
import android.widget.ImageView


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

            val nextIntent = when (role) {
                "donor" -> Intent(this@SplashActivity, MainActivity::class.java).putExtra("role", "donor")
                "ngo" -> Intent(this@SplashActivity, MainActivity::class.java).putExtra("role", "ngo")
                "admin" -> Intent(this@SplashActivity, MainActivity::class.java).putExtra("role", "admin")
                else -> Intent(this@SplashActivity, MainActivity::class.java).putExtra("role", "login")  // ✅ Fixed!
            }

            startActivity(nextIntent)
            finish()
        }
    }
}
