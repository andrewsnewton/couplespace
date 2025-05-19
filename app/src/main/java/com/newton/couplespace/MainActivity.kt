package com.newton.couplespace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.newton.couplespace.navigation.AppNavigation
import com.newton.couplespace.navigation.Screen
import com.newton.couplespace.ui.theme.BondedTheme
import android.util.Log

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        Log.d("MainActivity", "Firebase initialized")
        
        enableEdgeToEdge()
        setContent {
            BondedApp()
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
            
            AppNavigation(
                navController = navController,
                startDestination = startDestination
            )
        }
    }
}