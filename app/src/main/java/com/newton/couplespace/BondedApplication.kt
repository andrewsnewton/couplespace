package com.newton.couplespace

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

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
        try {
            // Detect if we're running in an emulator
            val isEmulator = isEmulator()
            
            if (isEmulator) {
                // In emulator, we'll completely skip App Check to avoid attestation failures
                Log.d("BondedApp", "Running in emulator, skipping App Check initialization")
                Log.d("BondedApp", "This is a development environment, so App Check is not required")
                return
            }
            
            // Only initialize App Check on real devices
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            
            // For development, we'll use the Debug provider
            val debugFactory = DebugAppCheckProviderFactory.getInstance()
            firebaseAppCheck.installAppCheckProviderFactory(debugFactory)
            
            // Enable auto refresh for the token
            firebaseAppCheck.setTokenAutoRefreshEnabled(true)
            
            // Log that the debug token has been registered in the Firebase Console
            Log.d("BondedApp", "Using debug token registered in Firebase Console: A18529EF-72BD-44FD-B3A2-5D3B36DE52F7")
            
            Log.d("BondedApp", "Firebase App Check initialized successfully")
        } catch (e: Exception) {
            // If there's any error initializing App Check, log it but don't crash the app
            Log.e("BondedApp", "Error initializing Firebase App Check", e)
            Log.w("BondedApp", "App will continue without App Check verification")
        }
    }
    
    // Helper method to detect if we're running in an emulator
    private fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
                || Build.FINGERPRINT.startsWith("generic")
                || Build.FINGERPRINT.startsWith("unknown")
                || Build.HARDWARE.contains("goldfish")
                || Build.HARDWARE.contains("ranchu")
                || Build.MODEL.contains("google_sdk")
                || Build.MODEL.contains("Emulator")
                || Build.MODEL.contains("Android SDK built for x86")
                || Build.MANUFACTURER.contains("Genymotion")
                || Build.PRODUCT.contains("sdk_google")
                || Build.PRODUCT.contains("google_sdk")
                || Build.PRODUCT.contains("sdk")
                || Build.PRODUCT.contains("sdk_x86")
                || Build.PRODUCT.contains("vbox86p")
                || Build.PRODUCT.contains("emulator")
                || Build.PRODUCT.contains("simulator"))
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
