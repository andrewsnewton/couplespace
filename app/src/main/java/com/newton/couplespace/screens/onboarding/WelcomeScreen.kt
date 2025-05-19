package com.newton.couplespace.screens.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.newton.couplespace.R
import com.newton.couplespace.auth.AuthService
import com.newton.couplespace.navigation.Screen
import kotlinx.coroutines.launch
import android.util.Log

@Composable
fun WelcomeScreen(navController: NavController) {
    val context = LocalContext.current
    val authService = AuthService(context)
    val coroutineScope = rememberCoroutineScope()
    
    // Check if user is already logged in
    LaunchedEffect(key1 = Unit) {
        Log.d("WelcomeScreen", "Checking authentication status")
        
        if (authService.isUserLoggedIn()) {
            Log.d("WelcomeScreen", "User is already logged in")
            
            // Check if profile is complete
            if (authService.isProfileComplete()) {
                Log.d("WelcomeScreen", "User profile is complete")
                
                if (authService.isConnectedWithPartner()) {
                    Log.d("WelcomeScreen", "User is connected with partner, navigating to Timeline")
                    // User has completed profile and is connected with partner
                    navController.navigate(Screen.Timeline.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                } else {
                    Log.d("WelcomeScreen", "User is not connected with partner, navigating to CoupleSetup")
                    // User has completed profile but is not connected with partner
                    navController.navigate(Screen.CoupleSetup.route) {
                        popUpTo(Screen.Welcome.route) { inclusive = true }
                    }
                }
            } else {
                Log.d("WelcomeScreen", "User profile is not complete, navigating to UserSetup")
                // User has not completed profile
                navController.navigate(Screen.UserSetup.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                }
            }
        } else {
            Log.d("WelcomeScreen", "User is not logged in")
            // For emulator testing, create a test user if needed
            val testUserId = authService.handleEmulatorTesting()
            if (testUserId.isNotBlank()) {
                Log.d("WelcomeScreen", "Created test user for emulator: $testUserId")
                // If we created a test user, navigate to the appropriate screen
                navController.navigate(Screen.UserSetup.route) {
                    popUpTo(Screen.Welcome.route) { inclusive = true }
                }
            }
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // App Logo
        Image(
            painter = painterResource(id = R.drawable.app_logo),
            contentDescription = "Bonded App Logo",
            modifier = Modifier.size(150.dp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // App Name
        Text(
            text = "Bonded",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // App Description
        Text(
            text = "Strengthen your relationship through shared wellness and communication",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        // Get Started Button
        Button(
            onClick = {
                Log.d("WelcomeScreen", "Get Started button clicked, navigating to Login screen")
                navController.navigate(Screen.Login.route)
            },
            modifier = Modifier.size(width = 200.dp, height = 48.dp)
        ) {
            Text(text = "Get Started")
        }
    }
}
