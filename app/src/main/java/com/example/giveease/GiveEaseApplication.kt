package com.example.giveease

import android.app.Application
import com.google.firebase.FirebaseApp

class GiveEaseApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
