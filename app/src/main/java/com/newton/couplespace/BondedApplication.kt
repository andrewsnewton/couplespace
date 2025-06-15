package com.newton.couplespace

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.work.Configuration
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.newton.couplespace.BuildConfig
import com.newton.couplespace.screens.health.service.WaterReminderManager
import com.newton.couplespace.screens.health.service.WaterReminderWorkerFactory
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class BondedApplication : Application(), Configuration.Provider {    
    @Inject
    lateinit var waterReminderManager: WaterReminderManager
    
    @Inject
    lateinit var workerFactory: WaterReminderWorkerFactory
    
    companion object {
        const val CHANNEL_ID = "bonded_notifications"
        const val WATER_REMINDER_CHANNEL_ID = "water_reminder_notifications"
    }
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
        // Initialize Firebase App Check
        initializeAppCheck()
        
        // Create notification channel for Android O and above
        createNotificationChannel()
        
        // Initialize water reminder system
        initializeWaterReminders()
    }
    
    /**
     * Initialize water reminder system
     */
    private fun initializeWaterReminders() {
        try {
            // Initialize the water reminder manager
            waterReminderManager.initialize()
            Log.d("BondedApp", "Water reminder system initialized successfully")
        } catch (e: Exception) {
            Log.e("BondedApp", "Error initializing water reminder system", e)
        }
    }
    
    /**
     * Configure WorkManager with our custom factory
     */
    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(Log.INFO)
            .build()
    }
    
    private fun initializeAppCheck() {
        try {
            // Detect if we're running in an emulator
            val isEmulator = isEmulator()
            
            // Initialize Firebase first to ensure it's fully set up
            if (!FirebaseApp.getApps(this).isEmpty()) {
                Log.d("BondedApp", "Firebase already initialized")
            } else {
                Log.d("BondedApp", "Initializing Firebase")
                FirebaseApp.initializeApp(this)
            }
            
            if (isEmulator) {
                // In emulator, we'll completely skip App Check to avoid attestation failures
                Log.d("BondedApp", "Running in emulator, skipping App Check initialization")
                Log.d("BondedApp", "This is a development environment, so App Check is not required")
                return
            }
            
            // Only initialize App Check on real devices
            val firebaseAppCheck = FirebaseAppCheck.getInstance()
            
            // For development, we'll use the Debug provider with explicit activation
            // This helps avoid the "Unknown calling package name" error
            if (BuildConfig.DEBUG) {
                Log.d("BondedApp", "Using Debug App Check provider for development")
                val debugFactory = DebugAppCheckProviderFactory.getInstance()
                firebaseAppCheck.installAppCheckProviderFactory(debugFactory)
            } else {
                // For production, use Play Integrity provider
                Log.d("BondedApp", "Using Play Integrity App Check provider for production")
                // The Play Integrity provider is installed automatically when the dependency is added
            }
            
            // Enable auto refresh for the token
            firebaseAppCheck.setTokenAutoRefreshEnabled(true)
            
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
            val notificationManager: NotificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                
            // Main app notifications channel
            val mainChannel = NotificationChannel(
                CHANNEL_ID, 
                "Bonded Notifications", 
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications from the Bonded app"
            }
            
            // Water reminder notifications channel
            val waterChannel = NotificationChannel(
                WATER_REMINDER_CHANNEL_ID, 
                "Water Reminders", 
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Water intake reminder notifications"
            }
            
            // Register the channels with the system
            notificationManager.createNotificationChannel(mainChannel)
            notificationManager.createNotificationChannel(waterChannel)
        }
    }
}
