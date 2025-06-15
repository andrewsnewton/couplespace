package com.newton.couplespace.screens.health.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.work.WorkManager
import com.newton.couplespace.R
import com.newton.couplespace.screens.health.data.repository.NutritionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager class for handling water reminder scheduling and initialization
 */
@Singleton
class WaterReminderManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val nutritionRepository: NutritionRepository,
    private val applicationScope: CoroutineScope
) {
    private val workManager = WorkManager.getInstance(context)
    
    /**
     * Initialize water reminders when the app starts
     */
    fun initialize() {
        applicationScope.launch(Dispatchers.IO) {
            try {
                // Check if reminders are enabled
                val reminderSettings = nutritionRepository.getWaterReminderSchedule().first()
                val enabled = reminderSettings["enabled"] as? Boolean ?: false
                
                if (enabled) {
                    // Schedule reminders
                    WaterReminderWorker.schedule(context, workManager)
                }
            } catch (e: Exception) {
                // Handle initialization error
            }
        }
    }
    
    /**
     * Update reminder schedule when settings change
     */
    fun updateReminderSchedule(enabled: Boolean) {
        if (enabled) {
            // Schedule or reschedule reminders
            WaterReminderWorker.schedule(context, workManager)
        } else {
            // Cancel reminders
            workManager.cancelUniqueWork(WaterReminderWorker.WORK_NAME)
        }
    }
    
    /**
     * Show a test notification immediately for debugging purposes
     */
    fun showTestNotification() {
        // Create a direct notification without using the worker
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create intent for when notification is tapped
        val intent = Intent(context, com.newton.couplespace.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO_HEALTH", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        val notification = NotificationCompat.Builder(context, com.newton.couplespace.BondedApplication.WATER_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water_drop) // Make sure this icon exists
            .setContentTitle("Test Water Reminder")
            .setContentText("This is a test notification. Your water reminder system is working!")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Enable sound, vibration, and lights
            .setVibrate(longArrayOf(0, 250, 250, 250)) // Custom vibration pattern
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI) // System default sound
            .build()
        
        // Show the notification with a unique ID for test notifications
        notificationManager.notify(2002, notification)
    }
}
