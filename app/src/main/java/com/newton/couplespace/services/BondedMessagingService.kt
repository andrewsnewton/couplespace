package com.newton.couplespace.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.newton.couplespace.BondedApplication
import com.newton.couplespace.MainActivity
import com.newton.couplespace.R

class BondedMessagingService : FirebaseMessagingService() {
    private val TAG = "BondedMsgService"
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG, "Message received: ${remoteMessage.messageId}")
        
        // Log message data
        Log.d(TAG, "Message data: ${remoteMessage.data}")
        Log.d(TAG, "Message notification: ${remoteMessage.notification}")
        
        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            Log.d(TAG, "Received notification: ${notification.title} - ${notification.body}")
            sendNotification(notification.title, notification.body)
        }
        
        // Check if message contains a data payload
        if (remoteMessage.data.isNotEmpty()) {
            val title = remoteMessage.data["title"] ?: "Bonded"
            val message = remoteMessage.data["message"] ?: "You have a new notification"
            val type = remoteMessage.data["type"]
            
            Log.d(TAG, "Processing data message. Type: $type, Title: $title, Message: $message")
            
            when (type) {
                "nudge" -> {
                    Log.d(TAG, "Handling nudge notification")
                    handleNudge(title, message, remoteMessage)
                }
                "question" -> {
                    Log.d(TAG, "Handling question notification")
                    handleQuestion(title, message)
                }
                "message" -> {
                    Log.d(TAG, "Handling message notification")
                    handleMessage(title, message)
                }
                else -> {
                    Log.d(TAG, "Unknown notification type: $type")
                    sendNotification(title, message)
                }
            }
        } else {
            Log.d(TAG, "No data payload found in message")
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to your server
        // This would be implemented to update the user's FCM token in Firestore
    }
    
    private fun handleNudge(title: String, message: String, remoteMessage: RemoteMessage? = null) {
        Log.d(TAG, "Creating nudge notification. Title: $title, Message: $message")
        
        try {
            // Get the sender's name from the data payload if available
            val senderName = remoteMessage?.data?.get("senderName") ?: "Your partner"
            val fromUserId = remoteMessage?.data?.get("fromUserId") ?: ""
            
            Log.d(TAG, "Nudge from: $senderName (User ID: $fromUserId)")
            
            // Create a high priority notification for nudges
            val notificationId = System.currentTimeMillis().toInt()
            
            // Create intent to open the app when notification is tapped
            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                putExtra("navigate_to", "nudge")
                putExtra("from_user_id", fromUserId)
                putExtra("sender_name", senderName)
                Log.d(TAG, "Created intent with navigate_to: nudge, from_user_id: $fromUserId")
            }
            
            val pendingIntent = PendingIntent.getActivity(
                this,
                notificationId, // Use notificationId as requestCode to create unique pending intents
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            Log.d(TAG, "Created pending intent for notification")
            
            // Create notification channel for Android O and above
            val channelId = "nudge_notifications"
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Log.d(TAG, "Creating notification channel for Android O+")
                val channel = NotificationChannel(
                    channelId,
                    "Nudge Notifications",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Notifications for nudges from your partner"
                    enableVibration(true)
                    vibrationPattern = longArrayOf(0, 250, 250, 250)
                    setShowBadge(true)
                    enableLights(true)
                    lightColor = android.graphics.Color.GREEN
                    lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                }
                val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                notificationManager.createNotificationChannel(channel)
                Log.d(TAG, "Created notification channel: $channelId")
            }
            
            Log.d(TAG, "Building notification with channel: $channelId")
            val notification = NotificationCompat.Builder(this, channelId)
                .setSmallIcon(R.drawable.app_logo)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_MESSAGE)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(longArrayOf(0, 250, 250, 250))
                .setFullScreenIntent(pendingIntent, true) // Show as heads-up notification
                .setDefaults(Notification.DEFAULT_ALL)
                .build()
                
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            Log.d(TAG, "Displaying notification with ID: $notificationId")
            notificationManager.notify(notificationId, notification)
            Log.d(TAG, "Notification displayed successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error creating or showing notification", e)
        }
    }
    
    private fun handleQuestion(title: String, message: String) {
        // Handle question notification
        sendNotification(title, message)
    }
    
    private fun handleMessage(title: String, message: String) {
        // Handle message notification
        sendNotification(title, message)
    }
    
    private fun sendNotification(title: String?, messageBody: String?) {
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE
        )
        
        val notificationBuilder = NotificationCompat.Builder(this, BondedApplication.CHANNEL_ID)
            .setSmallIcon(R.drawable.app_logo)
            .setContentTitle(title)
            .setContentText(messageBody)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(0, notificationBuilder.build())
    }
}
