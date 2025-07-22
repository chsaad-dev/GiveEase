package com.example.giveease

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import com.example.giveease.admin.AdminMainFragment
import com.example.giveease.auth.LoginFragment
import com.example.giveease.donor.DonorMainFragment
import com.example.giveease.ngo.NgoMainFragment

object AppNavigation {

    fun routeUser(activity: AppCompatActivity, role: String) {
        when (role) {
            "donor" -> {
                activity.supportFragmentManager.commit {
                    replace(android.R.id.content, DonorMainFragment())
                }
            }
            "ngo" -> {
                activity.supportFragmentManager.commit {
                    replace(android.R.id.content, NgoMainFragment())
                }
            }
            "admin" -> {
                activity.supportFragmentManager.commit {
                    replace(android.R.id.content, AdminMainFragment())
                }
            }
            else -> {
                activity.supportFragmentManager.commit {
                    replace(android.R.id.content, LoginFragment())
                }
            }
        }
    }
}
