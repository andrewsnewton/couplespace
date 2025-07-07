package com.newton.couplespace.services

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.newton.couplespace.models.TimelineEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * Manager class for handling event notifications
 * This class coordinates between the TimelineViewModel and the EventNotificationScheduler
 */
class EventNotificationManager(private val context: Context) {
    
    companion object {
        private const val TAG = "EventNotificationManager"
    }
    
    private val scheduler = EventNotificationScheduler(context)
    private val repository = com.newton.couplespace.screens.main.timeline.EnhancedTimelineRepository(context)
    
    /**
     * Schedule notifications for all of today's events for the current user
     */
    fun scheduleTodaysEventNotifications() {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Get today's date
                val today = LocalDate.now()
                
                // Get events for today from the repository
                // We need to get events for just today, so start and end date are the same
                val result = repository.loadTimelineEvents(today, today, true)
                
                result.fold(
                    onSuccess = { events ->
                        // All events in the result should be scheduled for notification
                        // The repository already filters events to show only those that should appear in the user's calendar
                        Log.d(TAG, "Scheduling notifications for ${events.size} events in user's calendar today")
                        
                        // Schedule notifications for each event
                        withContext(Dispatchers.Main) {
                            scheduler.scheduleEventsNotifications(events)
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error fetching today's events", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error scheduling today's event notifications", e)
            }
        }
    }
    
    /**
     * Handle event creation - schedule notifications for the new event
     */
    fun onEventCreated(event: TimelineEvent) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        // Schedule notifications for all events in the user's calendar
        // This includes events created by the partner that appear in the user's calendar
        
        // Check if the event is for today or in the future
        val eventDate = event.startTime.toDate().toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        
        if (!eventDate.isBefore(today)) {
            scheduler.scheduleEventNotification(event)
        }
    }
    
    /**
     * Handle event update - reschedule notifications for the updated event
     */
    fun onEventUpdated(event: TimelineEvent) {
        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return
        
        // Reschedule notifications for all events in the user's calendar
        // This includes events created by the partner that appear in the user's calendar
        
        // Check if the event is for today or in the future
        val eventDate = event.startTime.toDate().toInstant()
            .atZone(ZoneId.systemDefault()).toLocalDate()
        val today = LocalDate.now()
        
        if (!eventDate.isBefore(today)) {
            scheduler.rescheduleEventNotifications(event)
        }
    }
    
    /**
     * Handle event deletion - cancel notifications for the deleted event
     */
    fun onEventDeleted(eventId: String) {
        scheduler.cancelEventNotifications(eventId)
    }
}
