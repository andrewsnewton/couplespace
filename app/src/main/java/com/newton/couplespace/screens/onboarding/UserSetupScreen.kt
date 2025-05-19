package com.newton.couplespace.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.newton.couplespace.auth.AuthService
import com.newton.couplespace.navigation.Screen
import kotlinx.coroutines.launch
import android.util.Log
import java.util.Calendar
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSetupScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var birthYear by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = LocalContext.current
    val authService = remember { AuthService(context) }
    val coroutineScope = rememberCoroutineScope()
    
    // Check if user is logged in
    LaunchedEffect(Unit) {
        if (!authService.isUserLoggedIn()) {
            // User is not logged in, redirect to login screen
            Log.d("UserSetupScreen", "User not logged in, redirecting to login screen")
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.UserSetup.route) { inclusive = true }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Your Profile") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "Tell us about yourself",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Your Name") },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = birthYear,
                onValueChange = { 
                    // Only allow numeric input
                    if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                        birthYear = it
                    }
                },
                label = { Text("Birth Year (Optional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Button(
                onClick = {
                    if (name.isBlank()) {
                        errorMessage = "Please enter your name"
                        return@Button
                    }
                    
                    isLoading = true
                    errorMessage = null
                    
                    // Calculate birthdate if birth year is provided
                    val birthdate = if (birthYear.isNotBlank()) {
                        try {
                            val calendar = Calendar.getInstance()
                            calendar.set(birthYear.toInt(), 0, 1) // January 1st of birth year
                            calendar.time
                        } catch (e: Exception) {
                            Log.e("UserSetupScreen", "Error parsing birth year: ${e.message}")
                            null
                        }
                    } else {
                        null
                    }
                    
                    coroutineScope.launch {
                        Log.d("UserSetupScreen", "Updating user profile with name: $name")
                        val result = authService.updateUserProfile(name, birthdate)
                        
                        result.fold(
                            onSuccess = {
                                Log.d("UserSetupScreen", "Profile updated successfully, navigating to CoupleSetup")
                                // Navigate to couple setup screen
                                navController.navigate(Screen.CoupleSetup.route) {
                                    popUpTo(Screen.UserSetup.route) { inclusive = true }
                                }
                            },
                            onFailure = { e ->
                                Log.e("UserSetupScreen", "Failed to update profile: ${e.message}")
                                errorMessage = "Failed to update profile: ${e.message}"
                                isLoading = false
                                
                                // For emulator testing, navigate anyway
                                if (isEmulator()) {
                                    Log.d("UserSetupScreen", "In emulator, navigating despite error")
                                    navController.navigate(Screen.CoupleSetup.route) {
                                        popUpTo(Screen.UserSetup.route) { inclusive = true }
                                    }
                                }
                            }
                        )
                    }
                },
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Continue")
                }
            }
        }
    }
}

// Helper method to detect if we're running in an emulator
private fun isEmulator(): Boolean {
    return (android.os.Build.BRAND.startsWith("generic") && android.os.Build.DEVICE.startsWith("generic")
            || android.os.Build.FINGERPRINT.startsWith("generic")
            || android.os.Build.FINGERPRINT.startsWith("unknown")
            || android.os.Build.HARDWARE.contains("goldfish")
            || android.os.Build.HARDWARE.contains("ranchu")
            || android.os.Build.MODEL.contains("google_sdk")
            || android.os.Build.MODEL.contains("Emulator")
            || android.os.Build.MODEL.contains("Android SDK built for x86")
            || android.os.Build.MANUFACTURER.contains("Genymotion")
            || android.os.Build.PRODUCT.contains("sdk_google")
            || android.os.Build.PRODUCT.contains("google_sdk")
            || android.os.Build.PRODUCT.contains("sdk")
            || android.os.Build.PRODUCT.contains("sdk_x86")
            || android.os.Build.PRODUCT.contains("vbox86p")
            || android.os.Build.PRODUCT.contains("emulator")
            || android.os.Build.PRODUCT.contains("simulator"))
}
