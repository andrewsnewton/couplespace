package com.newton.couplespace.screens.main.timeline

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.firebase.auth.FirebaseAuth
import com.newton.couplespace.MainActivity
import com.newton.couplespace.R
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.newton.couplespace.models.EntryType
import com.newton.couplespace.services.NotificationService
import com.newton.couplespace.models.TimelineEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Repository class for handling timeline data operations
 */
class TimelineRepository(private val context: Context) {
    companion object {
        private const val TAG = "TimelineRepository"
    }
    
    private val firestore = FirebaseFirestore.getInstance()
    private val timelineCollection = firestore.collection("timelineEntries")
    private val usersCollection = firestore.collection("users")
    private val sharedPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    /**
     * Save a new timeline entry to Firestore
     */
    fun saveTimelineEntry(
        newEntry: TimelineEntry,
        currentUserId: String,
        isMyCalendar: Boolean,
        isEmulator: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        // If adding to my calendar, use my user ID
        if (isMyCalendar) {
            val userId = if (currentUserId.isBlank()) {
                val fallbackId = "test_user_${System.currentTimeMillis()}"
                Log.w("TimelineRepository", "WARNING: Blank user ID provided to saveTimelineEntry, using fallback: $fallbackId")
                fallbackId
            } else {
                currentUserId
            }
            
            saveEntryWithUserId(newEntry, userId, isEmulator, onComplete)
        } else {
            // If adding to partner's calendar, get partner ID from Firebase
            getPartnerIdFromFirebase(currentUserId) { partnerId ->
                if (partnerId.isBlank()) {
                    Log.w("TimelineRepository", "WARNING: Partner ID is blank, using current user ID instead")
                    saveEntryWithUserId(newEntry, currentUserId, isEmulator, onComplete)
                } else {
                    Log.d("TimelineRepository", "Using partner ID for timeline entry: $partnerId")
                    saveEntryWithUserId(newEntry, partnerId, isEmulator, onComplete)
                }
            }
        }
    }
    
    /**
     * Helper method to save an entry with a specific user ID
     */
    private fun saveEntryWithUserId(
        newEntry: TimelineEntry,
        userId: String,
        isEmulator: Boolean,
        onComplete: (Boolean) -> Unit
    ) {
        
        val entryToSave = newEntry.copy(
            userId = userId,
            createdAt = Date(),
            updatedAt = Date()
        )
        
        Log.d("TimelineRepository", "Adding new timeline entry: ${entryToSave.title} for user: $userId")
        Log.d("TimelineRepository", "Entry details: userId=${entryToSave.userId}, date=${entryToSave.date}, type=${entryToSave.type}")
        
        timelineCollection
            .add(entryToSave)
            .addOnSuccessListener { docRef ->
                Log.d("TimelineRepository", "Successfully added timeline entry with ID: ${docRef.id}")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("TimelineRepository", "Failed to add timeline entry: ${e.message}")
                
                // If in emulator and failed, add the entry to local state anyway
                if (isEmulator) {
                    Log.d("TimelineRepository", "In emulator, adding entry to local state despite Firestore failure")
                    onComplete(true) // Pretend it succeeded for emulator testing
                } else {
                    onComplete(false)
                }
            }
    }
    
    /**
     * Load timeline entries for a user
     */
    fun loadTimelineEntries(
        currentUserId: String,
        isMyCalendar: Boolean,
        onEntriesLoaded: (List<TimelineEntry>) -> Unit
    ) {
        // Ensure we have a valid user ID
        val validCurrentUserId = if (currentUserId.isBlank()) {
            val fallbackId = "test_user_${System.currentTimeMillis()}"
            Log.w("TimelineRepository", "WARNING: Blank current user ID provided to loadTimelineEntries, using fallback: $fallbackId")
            fallbackId
        } else {
            currentUserId
        }
        
        if (isMyCalendar) {
            // If viewing my calendar, use my user ID
            loadEntriesForUserId(validCurrentUserId, onEntriesLoaded)
        } else {
            // If viewing partner's calendar, get partner ID from Firebase
            getPartnerIdFromFirebase(validCurrentUserId) { partnerId ->
                if (partnerId.isBlank()) {
                    Log.w("TimelineRepository", "Partner ID is blank, cannot load partner timeline entries")
                    onEntriesLoaded(emptyList())
                } else {
                    loadEntriesForUserId(partnerId, onEntriesLoaded)
                }
            }
        }
    }
    
    /**
     * Helper method to load entries for a specific user ID
     */
    private fun loadEntriesForUserId(userId: String, onEntriesLoaded: (List<TimelineEntry>) -> Unit) {
        Log.d("TimelineRepository", "Loading timeline entries for user: $userId")
        
        timelineCollection
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents -> 
                Log.d("TimelineRepository", "Retrieved ${documents.size()} timeline entries")
                
                val entries = mutableListOf<TimelineEntry>()
                
                for (document in documents) {
                    try {
                        val id = document.id
                        val userId = document.getString("userId") ?: ""
                        val title = document.getString("title") ?: ""
                        val description = document.getString("description") ?: ""
                        val date = document.getDate("date") ?: Date()
                        val typeStr = document.getString("type") ?: EntryType.EVENT.name
                        val type = try {
                            EntryType.valueOf(typeStr)
                        } catch (e: Exception) {
                            Log.w("TimelineRepository", "Invalid entry type: $typeStr, defaulting to EVENT")
                            EntryType.EVENT
                        }
                        val completed = document.getBoolean("completed")
                        
                        val entry = TimelineEntry(
                            id = id,
                            userId = userId,
                            title = title,
                            description = description,
                            date = date,
                            type = type,
                            completed = completed
                        )
                        
                        entries.add(entry)
                    } catch (e: Exception) {
                        Log.e("TimelineRepository", "Error converting document to TimelineEntry: ${e.message}")
                    }
                }
                
                // Sort entries by date, most recent first
                entries.sortByDescending { it.date }
                
                onEntriesLoaded(entries)
            }
            .addOnFailureListener { e -> 
                Log.e("TimelineRepository", "Error loading timeline entries: ${e.message}")
                onEntriesLoaded(emptyList())
            }
    }
    
    /**
     * Update the completion status of a reminder entry
     */
    fun updateReminderStatus(entryId: String, completed: Boolean, onComplete: (Boolean) -> Unit) {
        timelineCollection
            .document(entryId)
            .update("completed", completed)
            .addOnSuccessListener {
                Log.d("TimelineRepository", "Successfully updated reminder status for entry: $entryId")
                onComplete(true)
            }
            .addOnFailureListener { e ->
                Log.e("TimelineRepository", "Failed to update reminder status: ${e.message}")
                onComplete(false)
            }
    }
    
    /**
     * Send a nudge to the partner
     */
    fun sendNudgeToPartner(currentUserId: String, onComplete: (Boolean) -> Unit) {
        if (currentUserId.isBlank()) {
            Log.w("TimelineRepository", "Cannot send nudge: Current user ID is blank")
            onComplete(false)
            return
        }

        // Get the current user's display name first
        val currentUserDoc = FirebaseFirestore.getInstance().collection("users").document(currentUserId)
        currentUserDoc.get()
            .addOnSuccessListener { currentUserSnapshot ->
                if (!currentUserSnapshot.exists()) {
                    Log.e("TimelineRepository", "Current user document doesn't exist")
                    onComplete(false)
                    return@addOnSuccessListener
                }
                
                // Get the sender's display name, first name, or email as fallback
                val senderName = currentUserSnapshot.getString("displayName") 
                    ?: currentUserSnapshot.getString("name")
                    ?: currentUserSnapshot.getString("email")?.substringBefore('@')
                    ?: "Your partner"
                
                Log.d("TimelineRepository", "Sending nudge from user: $currentUserId, display name: $senderName")
                
                // Now get the partner's ID
                getPartnerIdFromFirebase(currentUserId) { partnerId ->
                    if (partnerId.isBlank()) {
                        Log.w("TimelineRepository", "Cannot send nudge: Partner ID is blank")
                        onComplete(false)
                        return@getPartnerIdFromFirebase
                    }
                    
                    Log.d("TimelineRepository", "Sending nudge from $currentUserId to partner: $partnerId")
                    
                    // Create nudge entry for partner's timeline only
                    val partnerNudgeEntry = TimelineEntry(
                        title = "Nudge",
                        description = "You received a nudge from $senderName!",
                        date = Date(),
                        type = EntryType.EVENT,
                        nudgeEnabled = true
                    )
                    
                    // Save to partner's timeline
                    saveEntryWithUserId(partnerNudgeEntry, partnerId, false) { partnerSuccess ->
                        if (!partnerSuccess) {
                            Log.e("TimelineRepository", "Failed to save nudge to partner's timeline")
                            onComplete(false)
                            return@saveEntryWithUserId
                        }
                        
                        // Send push notification to partner via FCM
                        Log.d("TimelineRepository", "Sending FCM notification to partner: $partnerId")
                        
                        // Use coroutine scope to call the suspend function
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                Log.d("TimelineRepository", "Preparing to send nudge notification...")
                                
                                // Get the FCM token for the partner
                                val partnerDoc = FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(partnerId)
                                    .get()
                                    .await()
                                
                                val partnerFcmToken = partnerDoc.getString("fcmToken")
                                
                                if (partnerFcmToken.isNullOrEmpty()) {
                                    Log.e("TimelineRepository", "Partner FCM token is null or empty")
                                    onComplete(false)
                                    return@launch
                                }
                                
                                Log.d("TimelineRepository", "Sending nudge to FCM token: ${partnerFcmToken.take(5)}...")
                                
                                Log.d("TimelineRepository", "Sending nudge to partner: $partnerId with sender name: $senderName")
                                
                                // Send the notification using the companion object
                                try {
                                    NotificationService.sendNudgeNotification(
                                        fromUserId = currentUserId,
                                        toUserId = partnerId,
                                        context = context,
                                        senderName = senderName
                                    )
                                    Log.d("TimelineRepository", "Nudge notification sent successfully")
                                } catch (e: Exception) {
                                    Log.e("TimelineRepository", "Error sending nudge notification", e)
                                    throw e
                                }
                                
                                Log.d("TimelineRepository", "Nudge notification sent successfully")
                                // Ensure we call onComplete on the main thread
                                android.os.Handler(Looper.getMainLooper()).post {
                                    onComplete(true)
                                }
                            } catch (e: Exception) {
                                Log.e("TimelineRepository", "Failed to send nudge notification", e)
                                // Ensure we call onComplete on the main thread
                                android.os.Handler(Looper.getMainLooper()).post {
                                    onComplete(false)
                                }
                            }
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Log.e("TimelineRepository", "Error getting current user data: ${e.message}")
                onComplete(false)
            }
    }

    /**
     * Get partner ID from Firebase for a user
     */
    private fun getPartnerIdFromFirebase(userId: String, onPartnerIdRetrieved: (String) -> Unit) {
        // First check if we have a cached partner ID in SharedPreferences
        val sharedPrefs = context.getSharedPreferences("CoupleSpacePrefs", Context.MODE_PRIVATE)
        val cachedPartnerId = sharedPrefs.getString("partner_id", "") ?: ""
        
        // If we have a cached partner ID and it's not blank, use it
        if (cachedPartnerId.isNotBlank()) {
            Log.d("TimelineRepository", "Using cached partner ID from SharedPreferences: $cachedPartnerId")
            onPartnerIdRetrieved(cachedPartnerId)
            
            // Still fetch from Firebase to update the cache in the background
            fetchPartnerIdFromFirebase(userId) { freshPartnerId ->
                if (freshPartnerId != cachedPartnerId) {
                    Log.d("TimelineRepository", "Updating cached partner ID from $cachedPartnerId to $freshPartnerId")
                    sharedPrefs.edit().putString("partner_id", freshPartnerId).apply()
                }
            }
        } else {
            // If we don't have a cached partner ID, fetch it from Firebase
            fetchPartnerIdFromFirebase(userId, onPartnerIdRetrieved)
        }
    }
    
    /**
     * Fetch partner ID from Firebase for a user
     */
    private fun fetchPartnerIdFromFirebase(userId: String, onPartnerIdRetrieved: (String) -> Unit) {
        FirebaseFirestore.getInstance().collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val partnerId = document.getString("partnerId") ?: ""
                    Log.d("TimelineRepository", "Retrieved partner ID from Firebase: $partnerId for user: $userId")
                    
                    // Cache the partner ID in SharedPreferences for faster access next time
                    context.getSharedPreferences("CoupleSpacePrefs", Context.MODE_PRIVATE)
                        .edit()
                        .putString("partner_id", partnerId)
                        .apply()
                    
                    onPartnerIdRetrieved(partnerId)
                } else {
                    Log.w("TimelineRepository", "No user document found for ID: $userId")
                    onPartnerIdRetrieved("")
                }
            }
            .addOnFailureListener { e ->
                Log.e("TimelineRepository", "Error getting partner ID from Firebase: ${e.message}")
                onPartnerIdRetrieved("")
            }
    }
}
