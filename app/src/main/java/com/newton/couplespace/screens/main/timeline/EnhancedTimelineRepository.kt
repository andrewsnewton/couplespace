package com.newton.couplespace.screens.main.timeline

import android.content.Context
import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.newton.couplespace.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.*

/**
 * Repository class for handling enhanced timeline data operations
 */
class EnhancedTimelineRepository(private val context: Context) {
    companion object {
        private const val TAG = "EnhancedTimelineRepo"
        private const val COLLECTION_EVENTS = "timelineEvents"
        private const val COLLECTION_USERS = "users"
        private const val PREFS_NAME = "CoupleSpacePrefs"
    }
    
    private lateinit var firestore: FirebaseFirestore
    private lateinit var eventsCollection: com.google.firebase.firestore.CollectionReference
    private lateinit var usersCollection: com.google.firebase.firestore.CollectionReference
    private lateinit var sharedPrefs: android.content.SharedPreferences
    
    // State flows for reactive data
    private val _eventsFlow = MutableStateFlow<List<TimelineEvent>>(emptyList())
    val eventsFlow: StateFlow<List<TimelineEvent>> = _eventsFlow
    
    private val _loadingState = MutableStateFlow(false)
    val loadingState: StateFlow<Boolean> = _loadingState
    
    private val _errorState = MutableStateFlow<String?>(null)
    val errorState: StateFlow<String?> = _errorState
    
    init {
        try {
            // Initialize Firebase Firestore
            firestore = FirebaseFirestore.getInstance()
            eventsCollection = firestore.collection(COLLECTION_EVENTS)
            usersCollection = firestore.collection(COLLECTION_USERS)
            sharedPrefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            Log.d(TAG, "EnhancedTimelineRepository initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing EnhancedTimelineRepository", e)
            throw e
        }
    }
    
    /**
     * Save a new timeline event to Firestore
     */
    suspend fun saveTimelineEvent(
        event: TimelineEvent,
        isPartnerEvent: Boolean = false
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            _loadingState.value = true
            
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext Result.failure(
                IllegalStateException("User not authenticated")
            )
            
            // If it's a partner event, get the partner ID
            val targetUserId = if (isPartnerEvent) {
                getPartnerIdSuspend(currentUserId) ?: return@withContext Result.failure(
                    IllegalStateException("Partner not found")
                )
            } else {
                currentUserId
            }
            
            // Get couple ID (if available)
            val coupleId = getCoupleIdSuspend(currentUserId) ?: ""
            
            // Create event with updated fields
            val eventToSave = event.copy(
                userId = targetUserId,
                coupleId = coupleId,
                createdBy = currentUserId,
                lastModifiedBy = currentUserId,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            
            Log.d(TAG, "Saving event: ${eventToSave.title} for user: $targetUserId")
            
            // Add to Firestore
            val docRef = eventsCollection.add(eventToSave).await()
            Log.d(TAG, "Event saved with ID: ${docRef.id}")
            
            // If it's a recurring event, handle recurrence
            if (eventToSave.isRecurring && eventToSave.recurrenceRule != null) {
                handleRecurringEvent(eventToSave, docRef.id)
            }
            
            // Schedule notifications
            scheduleEventNotifications(eventToSave, docRef.id)
            
            _loadingState.value = false
            return@withContext Result.success(docRef.id)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving event", e)
            _errorState.value = "Failed to save event: ${e.message}"
            _loadingState.value = false
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Update an existing timeline event
     */
    suspend fun updateTimelineEvent(event: TimelineEvent): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _loadingState.value = true
            
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext Result.failure(
                IllegalStateException("User not authenticated")
            )
            
            // Check if user has permission to update this event
            if (event.userId != currentUserId && event.createdBy != currentUserId) {
                return@withContext Result.failure(IllegalStateException("Not authorized to update this event"))
            }
            
            // Update event
            val eventToUpdate = event.copy(
                lastModifiedBy = currentUserId,
                updatedAt = Timestamp.now()
            )
            
            eventsCollection.document(event.id).set(eventToUpdate).await()
            Log.d(TAG, "Event updated: ${event.id}")
            
            // Update notifications
            scheduleEventNotifications(eventToUpdate, event.id)
            
            _loadingState.value = false
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating event", e)
            _errorState.value = "Failed to update event: ${e.message}"
            _loadingState.value = false
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Delete a timeline event
     */
    suspend fun deleteTimelineEvent(eventId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _loadingState.value = true
            
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext Result.failure(
                IllegalStateException("User not authenticated")
            )
            
            // Check if user has permission to delete this event
            val eventDoc = eventsCollection.document(eventId).get().await()
            val event = eventDoc.toObject(TimelineEvent::class.java)
            
            if (event == null) {
                return@withContext Result.failure(IllegalStateException("Event not found"))
            }
            
            if (event.userId != currentUserId && event.createdBy != currentUserId) {
                return@withContext Result.failure(IllegalStateException("Not authorized to delete this event"))
            }
            
            // Delete event
            eventsCollection.document(eventId).delete().await()
            Log.d(TAG, "Event deleted: $eventId")
            
            // Cancel notifications
            cancelEventNotifications(eventId)
            
            _loadingState.value = false
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting event", e)
            _errorState.value = "Failed to delete event: ${e.message}"
            _loadingState.value = false
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Load timeline events for a specific date range
     */
    suspend fun loadTimelineEvents(
        startDate: LocalDate,
        endDate: LocalDate,
        includePartnerEvents: Boolean = true
    ): Result<List<TimelineEvent>> = withContext(Dispatchers.IO) {
        try {
            // Update loading state on main thread
            withContext(Dispatchers.Main) {
                _loadingState.value = true
                _errorState.value = null
            }
            
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext Result.failure(
                IllegalStateException("User not authenticated")
            )
            
            // Convert LocalDate to Timestamp for query
            val startTimestamp = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().let { Timestamp(it.epochSecond, 0) }
            val endTimestamp = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().let { Timestamp(it.epochSecond, 0) }
            
            // Query for user's events
            val userEvents = eventsCollection
                .whereEqualTo("userId", currentUserId)
                .whereGreaterThanOrEqualTo("startTime", startTimestamp)
                .whereLessThan("startTime", endTimestamp)
                .orderBy("startTime", Query.Direction.ASCENDING)
                .get()
                .await()
                .toObjects(TimelineEvent::class.java)
            
            val events = userEvents.toMutableList()
            
            // If including partner events, get partner ID and query their events
            if (includePartnerEvents) {
                val partnerId = getPartnerIdSuspend(currentUserId)
                if (!partnerId.isNullOrBlank()) {
                    // Get events created by the partner
                    val partnerEvents = eventsCollection
                        .whereEqualTo("userId", partnerId)
                        .whereGreaterThanOrEqualTo("startTime", startTimestamp)
                        .whereLessThan("startTime", endTimestamp)
                        .orderBy("startTime", Query.Direction.ASCENDING)
                        .get()
                        .await()
                        .toObjects(TimelineEvent::class.java)
                    
                    // Also get events created by the current user but marked for the partner
                    // We can't query directly on metadata fields, so we'll filter after fetching
                    val userCreatedPartnerEvents = eventsCollection
                        .whereEqualTo("createdBy", currentUserId)
                        .whereGreaterThanOrEqualTo("startTime", startTimestamp)
                        .whereLessThan("startTime", endTimestamp)
                        .orderBy("startTime", Query.Direction.ASCENDING)
                        .get()
                        .await()
                        .toObjects(TimelineEvent::class.java)
                        .filter { event ->
                            // Keep only events marked as partner events
                            event.metadata["isForPartner"] as? Boolean == true
                        }
                    
                    Log.d(TAG, "Partner events: ${partnerEvents.size}, User-created partner events: ${userCreatedPartnerEvents.size}")
                    
                    events.addAll(partnerEvents)
                    events.addAll(userCreatedPartnerEvents)
                    events.sortBy { it.startTime.seconds }
                }
            }
            
            // Update state flow on main thread
            withContext(Dispatchers.Main) {
                _eventsFlow.value = events
                _loadingState.value = false
            }
            
            return@withContext Result.success(events)
        } catch (e: Exception) {
            Log.e(TAG, "Error loading events", e)
            withContext(Dispatchers.Main) {
                _errorState.value = "Failed to load events: ${e.message}"
                _loadingState.value = false
            }
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Mark an event as completed (for tasks and reminders)
     */
    suspend fun markEventCompleted(eventId: String, completed: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _loadingState.value = true
            
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: return@withContext Result.failure(
                IllegalStateException("User not authenticated")
            )
            
            // Update the completion status
            eventsCollection.document(eventId)
                .update(
                    mapOf(
                        "isCompleted" to completed,
                        "lastModifiedBy" to currentUserId,
                        "updatedAt" to Timestamp.now()
                    )
                )
                .await()
            
            Log.d(TAG, "Event $eventId marked as ${if (completed) "completed" else "incomplete"}")
            _loadingState.value = false
            
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error marking event as completed", e)
            _errorState.value = "Failed to update event: ${e.message}"
            _loadingState.value = false
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Get a specific event by ID
     */
    suspend fun getEventById(eventId: String): Result<TimelineEvent> = withContext(Dispatchers.IO) {
        try {
            val eventDoc = eventsCollection.document(eventId).get().await()
            val event = eventDoc.toObject(TimelineEvent::class.java)
            
            return@withContext if (event != null) {
                Result.success(event)
            } else {
                Result.failure(NoSuchElementException("Event not found"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting event", e)
            return@withContext Result.failure(e)
        }
    }
    
    /**
     * Get partner ID for a user
     */
    private suspend fun getPartnerIdSuspend(userId: String): String? = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            val cachedPartnerId = sharedPrefs.getString("partner_id", null)
            if (!cachedPartnerId.isNullOrBlank()) {
                return@withContext cachedPartnerId
            }
            
            // Fetch from Firestore
            val userDoc = usersCollection.document(userId).get().await()
            val partnerId = userDoc.getString("partnerId")
            
            // Cache the result
            if (!partnerId.isNullOrBlank()) {
                sharedPrefs.edit().putString("partner_id", partnerId).apply()
            }
            
            return@withContext partnerId
        } catch (e: Exception) {
            Log.e(TAG, "Error getting partner ID", e)
            return@withContext null
        }
    }
    
    /**
     * Get couple ID for a user
     */
    private suspend fun getCoupleIdSuspend(userId: String): String? = withContext(Dispatchers.IO) {
        try {
            // Check cache first
            val cachedCoupleId = sharedPrefs.getString("couple_id", null)
            if (!cachedCoupleId.isNullOrBlank()) {
                return@withContext cachedCoupleId
            }
            
            // Fetch from Firestore
            val userDoc = usersCollection.document(userId).get().await()
            val coupleId = userDoc.getString("coupleCode")
            
            // Cache the result
            if (!coupleId.isNullOrBlank()) {
                sharedPrefs.edit().putString("couple_id", coupleId).apply()
            }
            
            return@withContext coupleId
        } catch (e: Exception) {
            Log.e(TAG, "Error getting couple ID", e)
            return@withContext null
        }
    }
    
    /**
     * Handle recurring events
     */
    private suspend fun handleRecurringEvent(event: TimelineEvent, parentEventId: String) {
        // Implementation for recurring events will go here
        // This is a placeholder for future implementation
    }
    
    /**
     * Schedule notifications for an event
     */
    private fun scheduleEventNotifications(event: TimelineEvent, eventId: String) {
        // Implementation for scheduling notifications will go here
        // This is a placeholder for future implementation
    }
    
    /**
     * Cancel notifications for an event
     */
    private fun cancelEventNotifications(eventId: String) {
        // Implementation for canceling notifications will go here
        // This is a placeholder for future implementation
    }
    
    /**
     * Send a nudge to the partner
     */
    suspend fun sendNudgeToPartner(currentUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            _loadingState.value = true
            
            if (currentUserId.isBlank()) {
                Log.w(TAG, "Cannot send nudge: Current user ID is blank")
                return@withContext Result.failure(IllegalArgumentException("Current user ID is blank"))
            }
            
            // Get the current user's display name first
            val currentUserDoc = usersCollection.document(currentUserId).get().await()
            val currentUserName = currentUserDoc.getString("name") ?: "Your partner"
            
            // Get partner ID
            val partnerId = getPartnerIdSuspend(currentUserId)
            if (partnerId.isNullOrBlank()) {
                Log.w(TAG, "Cannot send nudge: Partner ID not found")
                _loadingState.value = false
                return@withContext Result.failure(IllegalStateException("Partner not found"))
            }
            
            // Get partner's FCM token
            val partnerDoc = usersCollection.document(partnerId).get().await()
            val partnerFcmToken = partnerDoc.getString("fcmToken")
            
            if (partnerFcmToken.isNullOrBlank()) {
                Log.w(TAG, "Cannot send nudge: Partner FCM token not found")
                _loadingState.value = false
                return@withContext Result.failure(IllegalStateException("Partner's notification token not found"))
            }
            
            // Send the notification using FCM
            val message = mapOf(
                "to" to partnerFcmToken,
                "notification" to mapOf(
                    "title" to "Nudge from $currentUserName",
                    "body" to "$currentUserName is nudging you to check your timeline!"
                ),
                "data" to mapOf(
                    "type" to "nudge",
                    "navigate_to" to "timeline",
                    "sender_id" to currentUserId,
                    "sender_name" to currentUserName
                )
            )
            
            // Use Firebase Cloud Messaging API to send the notification
            // Use the NotificationService helper class to send the nudge notification
            val messageId = com.newton.couplespace.services.NotificationService.sendNudgeNotification(
                fromUserId = currentUserId,
                toUserId = partnerId,
                context = context,
                senderName = currentUserName
            )
            
            // If we got here without an exception, the notification was sent successfully
            Log.d(TAG, "Nudge notification sent with messageId: $messageId")
            
            // Additional data we might want to include with the notification
            val notificationData = mapOf(
                "type" to "nudge",
                "navigate_to" to "timeline",
                "sender_id" to currentUserId,
                "sender_name" to currentUserName
            )
            
            _loadingState.value = false
            
            // If we got here without exceptions, consider it a success
            Log.d(TAG, "Nudge sent successfully to partner: $partnerId")
            return@withContext Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error sending nudge", e)
            _loadingState.value = false
            return@withContext Result.failure(e)
        }
    }
}
