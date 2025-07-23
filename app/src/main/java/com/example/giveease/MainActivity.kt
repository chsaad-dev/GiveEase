package com.example.giveease

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.example.giveease.donor.DonorMainFragment
import com.example.giveease.ngo.NgoMainFragment
import com.example.giveease.admin.AdminMainFragment
import com.example.giveease.auth.LoginFragment

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
    }
}