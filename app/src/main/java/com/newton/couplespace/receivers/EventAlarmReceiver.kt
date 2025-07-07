package com.newton.couplespace.receivers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.newton.couplespace.MainActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * BroadcastReceiver that handles event notification alarms
 */
class EventAlarmReceiver : BroadcastReceiver() {
    
    companion object {
        private const val TAG = "EventAlarmReceiver"
        private const val CHANNEL_ID = "event_notifications"
    }
    
    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Received event alarm with action: ${intent.action}")
        
        // Log all extras for debugging
        intent.extras?.keySet()?.forEach { key ->
            Log.d(TAG, "Intent extra: $key = ${intent.extras?.get(key)}")
        }
        
        // Extract event details from intent
        val eventId = intent.getStringExtra("EVENT_ID") ?: return
        val eventTitle = intent.getStringExtra("EVENT_TITLE") ?: "Event"
        val eventDescription = intent.getStringExtra("EVENT_DESCRIPTION") ?: ""
        val eventLocation = intent.getStringExtra("EVENT_LOCATION") ?: ""
        val eventStartTime = intent.getLongExtra("EVENT_START_TIME", 0)
        val notificationId = intent.getIntExtra("NOTIFICATION_ID", eventId.hashCode())
        val reminderIndex = intent.getIntExtra("REMINDER_INDEX", 0)
        
        // Format event time
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val eventTimeString = timeFormat.format(Date(eventStartTime))
        
        // Create notification title and content
        val notificationTitle = eventTitle
        val notificationContent = buildString {
            append("At $eventTimeString")
            if (eventLocation.isNotEmpty()) {
                append(" • $eventLocation")
            }
        }
        
        // Show the notification
        showEventNotification(
            context,
            notificationId,
            notificationTitle,
            notificationContent,
            eventDescription,
            eventId
        )
    }
    
    private fun showEventNotification(
        context: Context,
        notificationId: Int,
        title: String,
        content: String,
        description: String,
        eventId: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Event Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                this.description = "Notifications for upcoming events"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Create an intent for when the notification is tapped
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EVENT_ID", eventId)
            putExtra("OPEN_EVENT_DETAILS", true)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your app's icon
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_EVENT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        
        // Add description as big text style if available
        if (description.isNotEmpty()) {
            notificationBuilder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$content\n\n$description")
            )
        }
        
        // Show the notification
        notificationManager.notify(notificationId, notificationBuilder.build())
        Log.d(TAG, "Event notification shown for event: $title")
    }
}
