package com.newton.couplespace.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.newton.couplespace.navigation.Screen
import java.util.Date
import java.util.UUID
import android.os.Build
import android.util.Log

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoupleSetupScreen(navController: NavController) {
    var selectedOption by remember { mutableStateOf<CoupleSetupOption?>(null) }
    var coupleCode by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf<String?>(null) }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
    // Log when the screen is composed
    Log.d("CoupleSetupScreen", "CoupleSetupScreen is being composed")
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Connect with Your Partner") }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Link your account with your partner",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Option Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Generate Code Option
                OptionCard(
                    title = "Generate My Couple Code",
                    description = "Create a code to share with your partner",
                    isSelected = selectedOption == CoupleSetupOption.GENERATE,
                    onClick = {
                        Log.d("CoupleSetupScreen", "Generate Code option selected")
                        selectedOption = CoupleSetupOption.GENERATE
                        if (generatedCode == null) {
                            generatedCode = generateCoupleCode()
                        }
                    }
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Enter Code Option
                OptionCard(
                    title = "Enter Partner's Code",
                    description = "Connect using your partner's code",
                    isSelected = selectedOption == CoupleSetupOption.ENTER,
                    onClick = {
                        Log.d("CoupleSetupScreen", "Enter Code option selected")
                        selectedOption = CoupleSetupOption.ENTER
                        generatedCode = null
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Content based on selected option
            when (selectedOption) {
                CoupleSetupOption.GENERATE -> {
                    generatedCode?.let { code ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Your Couple Code",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    text = code,
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Text(
                                    text = "Share this code with your partner to connect your accounts",
                                    style = MaterialTheme.typography.bodyMedium,
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(16.dp))
                                
                                Button(
                                    onClick = {
                                        Log.d("CoupleSetupScreen", "Continue button clicked in Generate option")
                                        try {
                                            // Navigate to Timeline screen
                                            Log.d("CoupleSetupScreen", "Navigating to Timeline screen from Generate option")
                                            navController.navigate(Screen.Timeline.route) {
                                                popUpTo(Screen.Welcome.route) { inclusive = true }
                                            }
                                        } catch (e: Exception) {
                                            Log.e("CoupleSetupScreen", "Error navigating to Timeline: ${e.message}")
                                            errorMessage = "Navigation error: ${e.message}"
                                        }
                                    }
                                ) {
                                    Text(text = "Continue")
                                }
                            }
                        }
                    }
                }
                
                CoupleSetupOption.ENTER -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        OutlinedTextField(
                            value = coupleCode,
                            onValueChange = { coupleCode = it },
                            label = { Text("Enter Partner's Code") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Error or success message
                        errorMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        successMessage?.let {
                            Text(
                                text = it,
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        
                        Button(
                            onClick = {
                                Log.d("CoupleSetupScreen", "Connect button clicked in Enter Code option")
                                try {
                                    // Navigate to Timeline screen
                                    Log.d("CoupleSetupScreen", "Navigating to Timeline screen from Enter Code option")
                                    navController.navigate(Screen.Timeline.route) {
                                        popUpTo(Screen.Welcome.route) { inclusive = true }
                                    }
                                } catch (e: Exception) {
                                    Log.e("CoupleSetupScreen", "Error navigating to Timeline: ${e.message}")
                                    errorMessage = "Navigation error: ${e.message}"
                                }
                            },
                            enabled = coupleCode.isNotBlank()
                        ) {
                            Text(text = "Connect")
                        }
                    }
                }
                
                else -> {
                    // No option selected yet
                    Text(
                        text = "Select an option above to continue",
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OptionCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(150.dp)
            .height(150.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

enum class CoupleSetupOption {
    GENERATE, ENTER
}

private fun generateCoupleCode(): String {
    // Generate a random 6-character alphanumeric code
    val allowedChars = ('A'..'Z') + ('0'..'9')
    return (1..6)
        .map { allowedChars.random() }
        .joinToString("")
}

// Helper method to detect if we're running in an emulator
private fun isEmulator(): Boolean {
    return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")
            || Build.FINGERPRINT.startsWith("generic")
            || Build.FINGERPRINT.startsWith("unknown")
            || Build.HARDWARE.contains("goldfish")
            || Build.HARDWARE.contains("ranchu")
            || Build.MODEL.contains("google_sdk")
            || Build.MODEL.contains("Emulator")
            || Build.MODEL.contains("Android SDK built for x86")
            || Build.MANUFACTURER.contains("Genymotion")
            || Build.PRODUCT.contains("sdk_google")
            || Build.PRODUCT.contains("google_sdk")
            || Build.PRODUCT.contains("sdk")
            || Build.PRODUCT.contains("sdk_x86")
            || Build.PRODUCT.contains("vbox86p")
            || Build.PRODUCT.contains("emulator")
            || Build.PRODUCT.contains("simulator")
    )
}
