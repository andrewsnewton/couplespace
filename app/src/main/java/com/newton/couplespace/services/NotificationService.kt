package com.newton.couplespace.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.ktx.functions
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.newton.couplespace.MainActivity
import kotlinx.coroutines.tasks.await
import java.util.UUID

class NotificationService : FirebaseMessagingService() {
    companion object {
        private const val TAG = "NotificationService"
        private const val CHANNEL_ID = "couplespace_notifications"
        private const val NOTIFICATION_ID = 1
        
        // Save FCM token to Firestore
        fun saveFcmToken(userId: String) {
            if (userId.isBlank()) return
            
            FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
                if (!task.isSuccessful) return@addOnCompleteListener
                
                val token = task.result
                if (token.isNullOrEmpty()) return@addOnCompleteListener
                
                // Save token to Firestore
                saveTokenToFirestore(userId, token)
            }
        }
        
        private fun saveTokenToFirestore(userId: String, token: String) {
            FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .update("fcmToken", token)
                .addOnSuccessListener {
                    verifyTokenSaved(userId)
                }
        }
        
        private fun verifyTokenSaved(userId: String) {
            FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { doc ->
                    val savedToken = doc.getString("fcmToken")
                    Log.d(TAG, "FCM token saved: ${savedToken?.take(5)}...")
                }
        }
        
        // Send a nudge notification to a user
        suspend fun sendNudgeNotification(
            fromUserId: String, 
            toUserId: String, 
            context: Context,
            senderName: String
        ): String {
            try {
                val db = FirebaseFirestore.getInstance()
                
                // Get the recipient's FCM token
                val recipientDoc = db.collection("users").document(toUserId).get().await()
                if (!recipientDoc.exists()) {
                    throw Exception("Recipient user not found: $toUserId")
                }
                
                val fcmToken = recipientDoc.getString("fcmToken")
                if (fcmToken.isNullOrEmpty()) {
                    throw Exception("No FCM token found for recipient: $toUserId")
                }
                
                // Use the provided sender name or fall back to "Your partner"
                val displayName = senderName.ifEmpty { "Your partner" }
                Log.d(TAG, "Using display name for notification: $displayName")
                
                // Create the notification data
                val notificationData = Bundle().apply {
                    // Create a nested notification object for better compatibility
                    val notification = Bundle().apply {
                        putString("title", "Nudge from $displayName!")
                        putString("body", "$displayName is thinking of you ❤️")
                    }
                    
                    // Main data payload
                    putString("to", fcmToken)
                    putString("title", "Nudge from $displayName!")
                    putString("message", "$displayName is thinking of you ❤️")
                    putString("type", "nudge")
                    putString("fromUserId", fromUserId)
                    putString("toUserId", toUserId)
                    putString("click_action", "FLUTTER_NOTIFICATION_CLICK")
                    // Add the sender's name to the data payload
                    putString("senderName", displayName)
                    
                    // Log the data being sent to the cloud function
                    Log.d(TAG, "Sending notification with data: " +
                        "to=$fcmToken, " +
                        "title=Nudge from $displayName!, " +
                        "message=$displayName is thinking of you ❤️, " +
                        "type=nudge, " +
                        "fromUserId=$fromUserId, " +
                        "toUserId=$toUserId, " +
                        "senderName=$displayName")
                }
                
                // Send the notification
                return send(notificationData, context)
                
            } catch (e: Exception) {
                Log.e(TAG, "Error sending nudge notification", e)
                throw e
            }
        }
        
        // Send a notification using Firebase Cloud Functions
        private suspend fun send(data: Bundle, context: Context): String {
            try {
                // Parse the data from the Bundle
                val title = data.getString("title") ?: "New Notification"
                val message = data.getString("message") ?: "You have a new notification"
                val toToken = data.getString("to") ?: throw IllegalArgumentException("Missing 'to' field in notification data")
                val type = data.getString("type") ?: "general"
                val fromUserId = data.getString("fromUserId") ?: ""
                val toUserId = data.getString("toUserId") ?: ""
                val senderName = data.getString("senderName") ?: ""
                
                // Create the notification data for Firebase Functions
                val notificationData = hashMapOf(
                    "to" to toToken,
                    "notification" to hashMapOf(
                        "title" to title,
                        "body" to message
                    ),
                    "data" to hashMapOf(
                        "type" to type,
                        "title" to title,
                        "message" to message,
                        "fromUserId" to fromUserId,
                        "toUserId" to toUserId,
                        "senderName" to senderName,
                        "click_action" to "FLUTTER_NOTIFICATION_CLICK"
                    )
                )
                
                // Get the current user
                val currentUser = FirebaseAuth.getInstance().currentUser
                    ?: throw Exception("No authenticated user found")
                
                // Get the ID token for the current user
                val idToken = currentUser.getIdToken(false).await()
                if (idToken.token.isNullOrEmpty()) {
                    throw Exception("Failed to get ID token")
                }
                
                // Create the data to send to the Cloud Function
                val functionData = hashMapOf(
                    "data" to notificationData,
                    "auth" to hashMapOf(
                        "uid" to currentUser.uid,
                        "token" to idToken.token
                    )
                )
                
                // Call the function with the prepared data
                val functions = Firebase.functions
                val callable = functions.getHttpsCallable("sendNotification")
                val result = callable.call(functionData).await()
                
                return (result.data as? Map<*, *>)?.get("messageId")?.toString() ?: generateMessageId()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error calling Firebase Function", e)
                throw e
            }
        }
        
        private fun generateMessageId(): String {
            return "msg_${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}"
        }
    }
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received: ${remoteMessage.messageId}")
        
        // Handle the received message
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Notification received - Title: ${notification.title}, Body: ${notification.body}")
            
            // Create and show the notification
            showNotification(
                notification.title ?: "New Notification",
                notification.body ?: "You have a new notification",
                remoteMessage.data
            )
        }
        
        // Also handle data payload if present
        if (remoteMessage.data.isNotEmpty()) {
            Log.d(TAG, "Message data payload: ${remoteMessage.data}")
            
            // Handle the data message here
            val title = remoteMessage.data["title"] ?: "New Notification"
            val message = remoteMessage.data["message"] ?: "You have a new notification"
            
            // Show the notification
            showNotification(title, message, remoteMessage.data)
        }
    }
    
    private fun showNotification(title: String, message: String, data: Map<String, String>) {
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        // Create notification channel for Android O and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "CoupleSpace Notifications",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "CoupleSpace Notifications Channel"
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        // Create an intent for the notification
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            // Add any data to the intent
            data.forEach { (key, value) ->
                putExtra(key, value)
            }
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        // Build the notification
        val notificationBuilder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info) // Replace with your notification icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
        
        // Show the notification
        notificationManager.notify(NOTIFICATION_ID, notificationBuilder.build())
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.d(TAG, "Refreshed FCM token: $token")
        
        // If you want to send messages to this application instance or
        // manage this apps subscriptions on the server side, you can
        // send the token to your app server.
        // sendRegistrationToServer(token)
    }
}