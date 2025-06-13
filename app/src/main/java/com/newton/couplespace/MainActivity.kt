package com.newton.couplespace

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.newton.couplespace.navigation.AppNavigation
import com.newton.couplespace.navigation.Screen
import com.newton.couplespace.ui.theme.BondedTheme
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.newton.couplespace.services.NotificationService
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.d("MainActivity", "Firebase initialized")
        
        // Save FCM token for the current user
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            NotificationService.saveFcmToken(currentUser.uid)
        } else {
            Log.w("MainActivity", "No user logged in, cannot save FCM token")
        }
        
        // Handle notification intents
        handleNotificationIntent(intent)
        
        enableEdgeToEdge()
        setContent {
            BondedApp()
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }
    
    private fun handleNotificationIntent(intent: Intent?) {
        when (intent?.getStringExtra("navigate_to")) {
            "nudge" -> {
                // Navigate to the timeline when a nudge notification is tapped
                // The actual navigation will be handled in the Composable
                Log.d("MainActivity", "Handling nudge notification")
            }
        }
    }
}

@Composable
fun BondedApp() {
    BondedTheme {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            val navController = rememberNavController()
            val context = LocalContext.current
            
            // Use the Login screen as the default starting point
            // The auth flow will handle redirects based on user state
            val startDestination = Screen.Login.route
            Log.d("MainActivity", "Starting app with destination: $startDestination")
            
            // Handle nudge notification intent
            val activity = LocalContext.current as? MainActivity
            val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
            
            // Check if we have a nudge intent
            val nudgeIntent = remember { activity?.intent?.getStringExtra("navigate_to") == "nudge" }
            
            LaunchedEffect(nudgeIntent, currentRoute) {
                if (nudgeIntent && currentRoute != null) {
                    // If we're not on the timeline, navigate there
                    if (currentRoute != Screen.Timeline.route) {
                        navController.navigate(Screen.Timeline.route) {
                            popUpTo(Screen.Timeline.route) {
                                inclusive = true
                            }
                        }
                    }
                    // Clear the intent so we don't process it again
                    activity?.intent?.removeExtra("navigate_to")
                }
            }
            
            AppNavigation(
                navController = navController,
                startDestination = startDestination
            )
        }
    }
}