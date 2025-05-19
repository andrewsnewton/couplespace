package com.newton.couplespace.utils

import android.content.Context
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.newton.couplespace.models.EntryType
import com.newton.couplespace.models.TimelineEntry
import java.util.Date

/**
 * Utility class to test partner ID functionality
 * This can be used to verify that partner IDs are correctly stored and used
 */
object PartnerIdTest {
    private const val TAG = "PartnerIdTest"
    
    /**
     * Test that partner IDs are correctly stored in SharedPreferences
     */
    fun testPartnerIdStorage(context: Context) {
        val sharedPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        
        // Get current user ID and partner ID
        val userId = sharedPrefs.getString("user_id", "") ?: ""
        val partnerId = sharedPrefs.getString("partner_id", "") ?: ""
        
        Log.d(TAG, "Current user ID: $userId")
        Log.d(TAG, "Partner ID: $partnerId")
        
        if (userId.isBlank()) {
            Log.e(TAG, "User ID is blank! This will cause issues with timeline entries.")
        }
        
        if (partnerId.isBlank()) {
            Log.w(TAG, "Partner ID is blank. This is expected if the user hasn't connected with a partner yet.")
        } else {
            Log.d(TAG, "Partner ID is set. User has connected with a partner.")
        }
    }
    
    /**
     * Test adding a timeline entry to the partner's calendar
     */
    fun testAddPartnerEntry(context: Context) {
        val sharedPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPrefs.getString("user_id", "") ?: ""
        val partnerId = sharedPrefs.getString("partner_id", "") ?: ""
        
        if (partnerId.isBlank()) {
            Log.e(TAG, "Cannot add partner entry: Partner ID is blank!")
            return
        }
        
        // Create a test entry
        val testEntry = TimelineEntry(
            id = "",
            userId = partnerId, // This should be set by the repository based on isMyCalendar
            title = "Test Partner Entry",
            description = "This is a test entry for the partner's timeline",
            date = Date(),
            type = EntryType.EVENT,
            completed = null
        )
        
        Log.d(TAG, "Creating test entry for partner with ID: $partnerId")
        
        // Save the entry directly to Firestore
        FirebaseFirestore.getInstance().collection("timelineEntries")
            .add(testEntry)
            .addOnSuccessListener { docRef ->
                Log.d(TAG, "Successfully added test entry to partner's timeline with ID: ${docRef.id}")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to add test entry to partner's timeline: ${e.message}")
            }
    }
    
    /**
     * Verify that entries are being saved with the correct user ID
     */
    fun verifyTimelineEntries(context: Context) {
        val sharedPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val userId = sharedPrefs.getString("user_id", "") ?: ""
        val partnerId = sharedPrefs.getString("partner_id", "") ?: ""
        
        Log.d(TAG, "Verifying timeline entries for user ID: $userId and partner ID: $partnerId")
        
        // Query for entries with the user's ID
        FirebaseFirestore.getInstance().collection("timelineEntries")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { documents ->
                Log.d(TAG, "Found ${documents.size()} entries for user ID: $userId")
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Error querying user entries: ${e.message}")
            }
        
        // Query for entries with the partner's ID
        if (partnerId.isNotBlank()) {
            FirebaseFirestore.getInstance().collection("timelineEntries")
                .whereEqualTo("userId", partnerId)
                .get()
                .addOnSuccessListener { documents ->
                    Log.d(TAG, "Found ${documents.size()} entries for partner ID: $partnerId")
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error querying partner entries: ${e.message}")
                }
        }
    }
}
