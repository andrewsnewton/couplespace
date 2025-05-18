package com.newton.couplespace.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.os.Build
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.newton.couplespace.models.User
import com.newton.couplespace.navigation.Screen
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserSetupScreen(navController: NavController) {
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
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
            verticalArrangement = Arrangement.Top
        ) {
            Text(
                text = "Tell us about yourself",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 24.dp)
            )
            
            // Name Field
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Age Field
            OutlinedTextField(
                value = age,
                onValueChange = { age = it },
                label = { Text("Age") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Height Field
            OutlinedTextField(
                value = height,
                onValueChange = { height = it },
                label = { Text("Height (cm)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Weight Field
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Weight (kg)") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Error message if any
            errorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
            
            // Continue Button
            Button(
                onClick = {
                    if (validateInputs(name, age, height, weight)) {
                        isLoading = true
                        errorMessage = null
                        
                        try {
                            android.util.Log.d("UserSetupScreen", "Starting anonymous sign-in")
                            
                            // Check if we're running in an emulator
                            val isEmulator = isEmulator()
                            android.util.Log.d("UserSetupScreen", "Is emulator: $isEmulator")
                            
                            if (isEmulator) {
                                // In emulator, we might have authentication issues, so use a fallback mechanism
                                android.util.Log.d("UserSetupScreen", "Running in emulator, using fallback mechanism")
                                
                                // Generate a random user ID for testing
                                val userId = UUID.randomUUID().toString()
                                android.util.Log.d("UserSetupScreen", "Generated test user ID: $userId")
                                
                                // Create user in Firestore
                                val user = User(
                                    id = userId,
                                    name = name,
                                    age = age.toIntOrNull() ?: 0,
                                    height = height.toIntOrNull() ?: 0,
                                    weight = weight.toDoubleOrNull() ?: 0.0,
                                    coupleCode = "TEST-" + UUID.randomUUID().toString().substring(0, 6),
                                    partnerId = "",
                                    createdAt = Date(),
                                    updatedAt = Date()
                                )
                                
                                android.util.Log.d("UserSetupScreen", "Created test user: $user")
                                
                                // Skip Firestore in emulator and proceed to next screen
                                isLoading = false
                                android.util.Log.d("UserSetupScreen", "Navigating to CoupleSetup screen")
                                navController.navigate(Screen.CoupleSetup.route)
                            } else {
                                // On real device, use normal authentication flow
                                android.util.Log.d("UserSetupScreen", "Running on real device, using normal authentication flow")
                                
                                // Create user in Firebase Auth with anonymous sign-in
                                FirebaseAuth.getInstance().signInAnonymously()
                                    .addOnSuccessListener { authResult ->
                                        val userId = authResult.user?.uid ?: UUID.randomUUID().toString()
                                        android.util.Log.d("UserSetupScreen", "Authentication successful, user ID: $userId")
                                        
                                        // Create user in Firestore
                                        val user = User(
                                            id = userId,
                                            name = name,
                                            age = age.toIntOrNull() ?: 0,
                                            height = height.toIntOrNull() ?: 0,
                                            weight = weight.toDoubleOrNull() ?: 0.0,
                                            coupleCode = "",
                                            partnerId = "",
                                            createdAt = Date(),
                                            updatedAt = Date()
                                        )
                                        
                                        FirebaseFirestore.getInstance().collection("users")
                                            .document(userId)
                                            .set(user)
                                            .addOnSuccessListener {
                                                isLoading = false
                                                android.util.Log.d("UserSetupScreen", "User data saved to Firestore, navigating to CoupleSetup")
                                                navController.navigate(Screen.CoupleSetup.route)
                                            }
                                            .addOnFailureListener { e ->
                                                isLoading = false
                                                android.util.Log.e("UserSetupScreen", "Error saving user data to Firestore", e)
                                                errorMessage = "Error: ${e.message}"
                                            }
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        android.util.Log.e("UserSetupScreen", "Authentication failed", e)
                                        errorMessage = "Authentication failed: ${e.message}"
                                        
                                        // If authentication fails with CONFIGURATION_NOT_FOUND, use fallback
                                        if (e.message?.contains("CONFIGURATION_NOT_FOUND") == true) {
                                            android.util.Log.d("UserSetupScreen", "Detected CONFIGURATION_NOT_FOUND error, using fallback")
                                            
                                            // Generate a random user ID for testing
                                            val userId = UUID.randomUUID().toString()
                                            
                                            // Skip Firestore and proceed to next screen
                                            isLoading = false
                                            android.util.Log.d("UserSetupScreen", "Navigating to CoupleSetup screen using fallback")
                                            navController.navigate(Screen.CoupleSetup.route)
                                        }
                                    }
                            }
                        } catch (e: Exception) {
                            isLoading = false
                            android.util.Log.e("UserSetupScreen", "Unexpected error in authentication flow", e)
                            errorMessage = "An unexpected error occurred: ${e.message}"
                        }
                    } else {
                        errorMessage = "Please fill all fields with valid values"
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading
            ) {
                Text(text = if (isLoading) "Creating Profile..." else "Continue")
            }
        }
    }
}

private fun validateInputs(name: String, age: String, height: String, weight: String): Boolean {
    if (name.isBlank()) return false
    
    val ageInt = age.toIntOrNull()
    if (ageInt == null || ageInt <= 0 || ageInt > 120) return false
    
    val heightInt = height.toIntOrNull()
    if (heightInt == null || heightInt <= 0 || heightInt > 300) return false
    
    val weightDouble = weight.toDoubleOrNull()
    if (weightDouble == null || weightDouble <= 0 || weightDouble > 500) return false
    
    return true
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
            || Build.PRODUCT.contains("simulator"))
}
