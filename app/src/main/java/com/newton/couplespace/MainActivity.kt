package com.newton.couplespace

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.google.firebase.FirebaseApp
import com.newton.couplespace.navigation.AppNavigation
import com.newton.couplespace.navigation.Screen
import com.newton.couplespace.ui.theme.BondedTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)
        
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
            
            // Check if user is logged in
            val startDestination = Screen.Welcome.route
            
            AppNavigation(
                navController = navController,
                startDestination = startDestination
            )
        }
    }
}