package com.newton.couplespace

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

class BondedApplication : Application() {
    
    companion object {
        const val CHANNEL_ID = "bonded_notifications"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize Firebase App Check
        initializeAppCheck()
        
        // Create notification channel for Android O and above
        createNotificationChannel()
    }
    
    private fun initializeAppCheck() {
        // Set up Firebase App Check
        val firebaseAppCheck = FirebaseAppCheck.getInstance()
        
        // For development and testing, use the debug provider
        // In a production app, you would use PlayIntegrityAppCheckProviderFactory instead
        firebaseAppCheck.installAppCheckProviderFactory(
            DebugAppCheckProviderFactory.getInstance()
        )
        
        // Uncomment for production use:
        // firebaseAppCheck.installAppCheckProviderFactory(
        //     PlayIntegrityAppCheckProviderFactory.getInstance()
        // )
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Bonded Notifications"
            val descriptionText = "Notifications from the Bonded app"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            
            // Register the channel with the system
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
