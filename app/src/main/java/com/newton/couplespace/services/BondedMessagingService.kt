package com.newton.couplespace.services

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.newton.couplespace.BondedApplication
import com.newton.couplespace.MainActivity
import com.newton.couplespace.R

class BondedMessagingService : FirebaseMessagingService() {
    
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        
        // Check if message contains a notification payload
        remoteMessage.notification?.let { notification ->
            sendNotification(notification.title, notification.body)
        }
        
        // Check if message contains a data payload
        remoteMessage.data.isNotEmpty().let {
            val title = remoteMessage.data["title"] ?: "Bonded"
            val message = remoteMessage.data["message"] ?: "You have a new notification"
            val type = remoteMessage.data["type"]
            
            when (type) {
                "nudge" -> handleNudge(title, message)
                "question" -> handleQuestion(title, message)
                "message" -> handleMessage(title, message)
                else -> sendNotification(title, message)
            }
        }
    }
    
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Send token to your server
        // This would be implemented to update the user's FCM token in Firestore
    }
    
    private fun handleNudge(title: String, message: String) {
        // Handle nudge notification - could add special sound or vibration pattern
        sendNotification(title, message)
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
