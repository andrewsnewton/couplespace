package com.newton.couplespace.screens.health.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.google.firebase.auth.FirebaseAuth
import com.newton.couplespace.BondedApplication
import com.newton.couplespace.MainActivity
import com.newton.couplespace.R
import com.newton.couplespace.screens.health.data.repository.NutritionRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Worker class for scheduling and displaying water intake reminders
 */
class WaterReminderWorker @Inject constructor(
    @ApplicationContext private val context: Context,
    workerParams: WorkerParameters,
    private val nutritionRepository: NutritionRepository
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "water_reminder_worker"
        private const val NOTIFICATION_ID = 1001
        
        /**
         * Schedule water reminders based on user settings
         */
        fun schedule(context: Context, workManager: WorkManager) {
            // Cancel any existing work
            workManager.cancelUniqueWork(WORK_NAME)
            
            // Create a periodic work request that checks every minute for testing
            // The actual reminder frequency will be controlled by the worker logic
            val reminderRequest = PeriodicWorkRequestBuilder<WaterReminderWorker>(
                1, TimeUnit.MINUTES
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            
            // Enqueue the work as unique work to ensure only one instance runs
            workManager.enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                reminderRequest
            )
        }
    }
    
    override suspend fun doWork(): Result {
        try {
            // Get the current user ID
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return Result.failure()
            
            // Get the water reminder settings
            val reminderSettings = withContext(Dispatchers.IO) {
                nutritionRepository.getWaterReminderSchedule().first()
            }
            val enabled = reminderSettings["enabled"] as? Boolean ?: false
            
            if (!enabled) {
                return Result.success()
            }
            
            // Check if current time is within active hours
            val startHour = reminderSettings["startHour"] as? Int ?: 8
            val endHour = reminderSettings["endHour"] as? Int ?: 22
            
            val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
            if (currentHour < startHour || currentHour >= endHour) {
                return Result.success() // Outside active hours
            }
            
            // Check if user has logged water intake recently
            val currentDate = java.time.LocalDate.now()
            val waterIntakeMetrics = withContext(Dispatchers.IO) {
                nutritionRepository.getWaterIntakeForDate(currentDate).first()
            }
            
            // Check if there's any recent water intake
            val lastUpdated = if (waterIntakeMetrics.isNotEmpty()) {
                waterIntakeMetrics.maxOfOrNull { it.timestamp.toEpochMilli() } ?: 0L
            } else {
                0L
            }
            
            // Get reminder interval (default to 60 minutes)
            val intervalMinutes = reminderSettings["intervalMinutes"] as? Int ?: 60
            
            // If water was logged recently (based on interval), don't show notification
            val intervalMillis = intervalMinutes * 60 * 1000
            val intervalAgo = System.currentTimeMillis() - intervalMillis
            if (lastUpdated > intervalAgo) {
                return Result.success() // Recent water intake logged
            }
            
            // All conditions met, show reminder notification
            showReminderNotification()
            
            return Result.success()
        } catch (e: Exception) {
            return Result.failure()
        }
    }
    
    /**
     * Show a notification reminding the user to drink water
     */
    private fun showReminderNotification() {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create intent for when notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("NAVIGATE_TO_HEALTH", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        val notification = NotificationCompat.Builder(context, BondedApplication.WATER_REMINDER_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_water_drop)
            .setContentTitle("Water Reminder")
            .setContentText("Time to drink some water! Stay hydrated.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setDefaults(NotificationCompat.DEFAULT_ALL) // Enable sound, vibration, and lights
            .setVibrate(longArrayOf(0, 250, 250, 250)) // Custom vibration pattern
            .setSound(android.provider.Settings.System.DEFAULT_NOTIFICATION_URI) // System default sound
            .build()
        
        // Show the notification
        notificationManager.notify(NOTIFICATION_ID, notification)
    }
}
