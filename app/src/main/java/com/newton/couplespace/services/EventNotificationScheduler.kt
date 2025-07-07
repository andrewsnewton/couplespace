package com.newton.couplespace.services

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.newton.couplespace.models.TimelineEvent
import com.newton.couplespace.receivers.EventAlarmReceiver
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date

/**
 * Service responsible for scheduling notifications for user events
 */
class EventNotificationScheduler(private val context: Context) {
    
    companion object {
        private const val TAG = "EventNotificationScheduler"
        
        // Request code base for event notifications
        private const val EVENT_NOTIFICATION_REQUEST_CODE_BASE = 1000
    }
    
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    
    /**
     * Schedule notifications for a single event
     */
    fun scheduleEventNotification(event: TimelineEvent) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        // Schedule notifications for all events in the user's calendar
        // This includes events created by the partner that appear in the user's calendar
        Log.d(TAG, "Scheduling notification for event: ${event.title} (ID: ${event.id})")
        Log.d(TAG, "  Created by: ${if (event.userId == currentUserId) "user" else "partner"}")
        Log.d(TAG, "  Event start time: ${event.startTime.toDate()}")
        
        // Get notification settings from the event
        val notificationSettings = event.notificationSettings
        
        // Skip if push notifications are disabled for this event
        if (!notificationSettings.pushNotification) {
            Log.d(TAG, "Push notifications disabled for event: ${event.title}")
            return
        }
        
        // Check if there are any reminders configured
        if (notificationSettings.reminders.isEmpty()) {
            Log.d(TAG, "No reminders configured for event: ${event.title}, using default 15-minute reminder")
            // Use a default 15-minute reminder
            scheduleDefaultReminder(event)
            return
        }
        
        Log.d(TAG, "Event has ${notificationSettings.reminders.size} reminders configured")
        
        // Schedule notifications for each reminder
        notificationSettings.reminders.forEachIndexed { index, reminder ->
            try {
                // Calculate notification time based on event start time and reminder settings
                val eventZoneId: ZoneId = try {
                    val zoneId = ZoneId.of(event.sourceTimezone)
                    Log.d(TAG, "Using event source timezone: ${event.sourceTimezone}")
                    zoneId
                } catch (e: Exception) {
                    Log.w(TAG, "Invalid timezone ${event.sourceTimezone}, using system default", e)
                    ZoneId.systemDefault()
                }

                val eventStartDateTime = event.startTime.toDate().toInstant()
                    .atZone(eventZoneId).toLocalDateTime()
                
                Log.d(TAG, "Reminder #$index: ${reminder.value} ${reminder.unit}")
                
                val notificationTime = when (reminder.unit) {
                    com.newton.couplespace.models.ReminderUnit.MINUTES -> eventStartDateTime.minusMinutes(reminder.value.toLong())
                    com.newton.couplespace.models.ReminderUnit.HOURS -> eventStartDateTime.minusHours(reminder.value.toLong())
                    com.newton.couplespace.models.ReminderUnit.DAYS -> eventStartDateTime.minusDays(reminder.value.toLong())
                    com.newton.couplespace.models.ReminderUnit.WEEKS -> eventStartDateTime.minusWeeks(reminder.value.toLong())
                }
                
                // Convert to milliseconds
                val notificationTimeMillis = notificationTime
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli()
                
                // Log detailed notification timing information
                val currentTimeMillis = System.currentTimeMillis()
                val timeUntilNotification = notificationTimeMillis - currentTimeMillis
                val minutesUntil = timeUntilNotification / (1000 * 60)
                val hoursUntil = minutesUntil / 60
                val daysUntil = hoursUntil / 24
                
                val timeUntilFormatted = when {
                    daysUntil > 0 -> "$daysUntil days"
                    hoursUntil > 0 -> "$hoursUntil hours"
                    else -> "$minutesUntil minutes"
                }
                
                Log.d(TAG, "Notification details for event: ${event.title} (ID: ${event.id})")
                Log.d(TAG, "  Source timezone: ${event.sourceTimezone}")
                Log.d(TAG, "  Event start: ${event.startTime.toDate()}")
                Log.d(TAG, "  Notification time: ${Date(notificationTimeMillis)}")
                Log.d(TAG, "  Current time: ${Date(currentTimeMillis)}")
                Log.d(TAG, "  Time until notification: $timeUntilFormatted ($minutesUntil minutes)")
                
                // Skip if notification time is in the past
                if (notificationTimeMillis <= currentTimeMillis) {
                    Log.d(TAG, "  SKIPPING past notification for event: ${event.title}")
                    return@forEachIndexed
                }
                
                // Create intent for the alarm
                val intent = Intent(context, EventAlarmReceiver::class.java).apply {
                    action = "com.newton.couplespace.ACTION_EVENT_REMINDER"
                    putExtra("EVENT_ID", event.id)
                    putExtra("EVENT_TITLE", event.title)
                    putExtra("EVENT_DESCRIPTION", event.description)
                    putExtra("EVENT_LOCATION", event.location)
                    putExtra("EVENT_START_TIME", event.startTime.seconds * 1000)
                    putExtra("NOTIFICATION_ID", generateNotificationId(event.id, index))
                    putExtra("REMINDER_INDEX", index)
                }
                
                // Create unique request code for this event and reminder
                val requestCode = generateRequestCode(event.id, index)
                
                // Create pending intent
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    requestCode,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
                
                // Schedule the alarm
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        // For Android 12+ check if we can use exact alarms
                        if (alarmManager.canScheduleExactAlarms()) {
                            Log.d(TAG, "Using setExactAndAllowWhileIdle for Android 12+")
                            alarmManager.setExactAndAllowWhileIdle(
                                AlarmManager.RTC_WAKEUP,
                                notificationTimeMillis,
                                pendingIntent
                            )
                        } else {
                            // Fallback to inexact alarms if permission not granted
                            Log.d(TAG, "SCHEDULE_EXACT_ALARM permission not granted, using inexact alarm")
                            alarmManager.set(
                                AlarmManager.RTC_WAKEUP,
                                notificationTimeMillis,
                                pendingIntent
                            )
                        }
                    } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        Log.d(TAG, "Using setExactAndAllowWhileIdle for Android M-R")
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            notificationTimeMillis,
                            pendingIntent
                        )
                    } else {
                        Log.d(TAG, "Using setExact for pre-Android M")
                        alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            notificationTimeMillis,
                            pendingIntent
                        )
                    }
                    Log.d(TAG, "Successfully scheduled alarm with request code: $requestCode")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to schedule alarm", e)
                    // Fallback to inexact alarms if exact alarms failed
                    try {
                        Log.d(TAG, "Falling back to inexact alarm after error")
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            notificationTimeMillis,
                            pendingIntent
                        )
                        Log.d(TAG, "Successfully scheduled inexact fallback alarm")
                    } catch (e2: Exception) {
                        Log.e(TAG, "Failed to schedule even inexact alarm", e2)
                    }
                }
                
                val notificationDate = Date(notificationTimeMillis)
                Log.d(TAG, "Scheduled notification for event '${event.title}' at $notificationDate")
                
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling notification for event: ${event.title}", e)
            }
        }
    }
    
    /**
     * Schedule notifications for multiple events
     */
    fun scheduleEventsNotifications(events: List<TimelineEvent>) {
        events.forEach { event ->
            scheduleEventNotification(event)
        }
    }
    
    /**
     * Cancel all notifications for an event
     */
    fun cancelEventNotifications(eventId: String) {
        // We don't know how many reminders the event had, so we'll try to cancel a reasonable number
        for (i in 0 until 5) {
            val requestCode = generateRequestCode(eventId, i)
            val intent = Intent(context, EventAlarmReceiver::class.java).apply {
                action = "com.newton.couplespace.ACTION_EVENT_REMINDER"
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            
            pendingIntent?.let {
                alarmManager.cancel(it)
                it.cancel()
                Log.d(TAG, "Cancelled notification for event ID: $eventId, reminder index: $i")
            }
        }
    }
    
    /**
     * Generate a unique request code for an event and reminder
     */
    private fun generateRequestCode(eventId: String, reminderIndex: Int): Int {
        // Use a hash of the event ID to create a unique code
        val eventHash = eventId.hashCode() and 0xFFFF
        return EVENT_NOTIFICATION_REQUEST_CODE_BASE + (eventHash * 10) + reminderIndex
    }
    
    /**
     * Generate a unique notification ID for an event and reminder
     */
    private fun generateNotificationId(eventId: String, reminderIndex: Int): Int {
        // Use a hash of the event ID to create a unique notification ID
        val eventHash = eventId.hashCode() and 0xFFFF
        return eventHash * 10 + reminderIndex
    }
    
    /**
     * Schedule a default 15-minute reminder for an event with no reminders
     */
    private fun scheduleDefaultReminder(event: TimelineEvent) {
        try {
            // Calculate notification time based on event start time and default 15-minute reminder
            val eventZoneId: ZoneId = try {
                val zoneId = ZoneId.of(event.sourceTimezone)
                Log.d(TAG, "Using event source timezone: ${event.sourceTimezone}")
                zoneId
            } catch (e: Exception) {
                Log.w(TAG, "Invalid timezone ${event.sourceTimezone}, using system default", e)
                ZoneId.systemDefault()
            }

            val eventStartDateTime = event.startTime.toDate().toInstant()
                .atZone(eventZoneId).toLocalDateTime()
            
            Log.d(TAG, "Using default reminder: 15 minutes before event")
            
            val notificationTime = eventStartDateTime.minusMinutes(15)
            
            // Convert to milliseconds
            val notificationTimeMillis = notificationTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
            
            // Log detailed notification timing information
            val currentTimeMillis = System.currentTimeMillis()
            val timeUntilNotification = notificationTimeMillis - currentTimeMillis
            val minutesUntil = timeUntilNotification / (1000 * 60)
            val hoursUntil = minutesUntil / 60
            val daysUntil = hoursUntil / 24
            
            val timeUntilFormatted = when {
                daysUntil > 0 -> "$daysUntil days"
                hoursUntil > 0 -> "$hoursUntil hours"
                else -> "$minutesUntil minutes"
            }
            
            Log.d(TAG, "Default notification details for event: ${event.title} (ID: ${event.id})")
            Log.d(TAG, "  Source timezone: ${event.sourceTimezone}")
            Log.d(TAG, "  Event start: ${event.startTime.toDate()}")
            Log.d(TAG, "  Notification time: ${Date(notificationTimeMillis)}")
            Log.d(TAG, "  Current time: ${Date(currentTimeMillis)}")
            Log.d(TAG, "  Time until notification: $timeUntilFormatted ($minutesUntil minutes)")
            
            // Skip if notification time is in the past
            if (notificationTimeMillis <= currentTimeMillis) {
                Log.d(TAG, "  SKIPPING past default notification for event: ${event.title}")
                return
            }
            
            // Create intent for the alarm
            val intent = Intent(context, EventAlarmReceiver::class.java).apply {
                action = "com.newton.couplespace.ACTION_EVENT_REMINDER"
                putExtra("EVENT_ID", event.id)
                putExtra("EVENT_TITLE", event.title)
                putExtra("EVENT_DESCRIPTION", event.description)
                putExtra("EVENT_LOCATION", event.location)
                putExtra("EVENT_START_TIME", event.startTime.seconds * 1000)
                putExtra("NOTIFICATION_ID", generateNotificationId(event.id, 0))
                putExtra("REMINDER_INDEX", 0)
            }
            
            // Create unique request code for this event and reminder
            val requestCode = generateRequestCode(event.id, 0)
            
            // Create pending intent
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            // Schedule the alarm
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    // For Android 12+ check if we can use exact alarms
                    if (alarmManager.canScheduleExactAlarms()) {
                        Log.d(TAG, "Using setExactAndAllowWhileIdle for Android 12+ (default reminder)")
                        alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            notificationTimeMillis,
                            pendingIntent
                        )
                    } else {
                        // Fallback to inexact alarms if permission not granted
                        Log.d(TAG, "SCHEDULE_EXACT_ALARM permission not granted, using inexact alarm (default reminder)")
                        alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            notificationTimeMillis,
                            pendingIntent
                        )
                    }
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    Log.d(TAG, "Using setExactAndAllowWhileIdle for Android M-R (default reminder)")
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        notificationTimeMillis,
                        pendingIntent
                    )
                } else {
                    Log.d(TAG, "Using setExact for pre-Android M (default reminder)")
                    alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        notificationTimeMillis,
                        pendingIntent
                    )
                }
                Log.d(TAG, "Successfully scheduled default alarm with request code: $requestCode")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to schedule default alarm", e)
                // Fallback to inexact alarms if exact alarms failed
                try {
                    Log.d(TAG, "Falling back to inexact alarm after error (default reminder)")
                    alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        notificationTimeMillis,
                        pendingIntent
                    )
                    Log.d(TAG, "Successfully scheduled inexact fallback alarm (default reminder)")
                } catch (e2: Exception) {
                    Log.e(TAG, "Failed to schedule even inexact alarm (default reminder)", e2)
                }
            }
            
            val notificationDate = Date(notificationTimeMillis)
            Log.d(TAG, "Scheduled default notification for event '${event.title}' at $notificationDate")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling default notification for event: ${event.title}", e)
        }
    }
    
    /**
     * Reschedule notifications for an event (used when event is updated)
     */
    fun rescheduleEventNotifications(event: TimelineEvent) {
        // Cancel existing notifications
        cancelEventNotifications(event.id)
        // Schedule new notifications
        scheduleEventNotification(event)
    }
}
