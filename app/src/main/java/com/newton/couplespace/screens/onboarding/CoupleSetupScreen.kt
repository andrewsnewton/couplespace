package com.newton.couplespace.screens.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.Card
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.newton.couplespace.auth.AuthService
import com.newton.couplespace.navigation.Screen
import kotlinx.coroutines.launch
import android.util.Log
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoupleSetupScreen(navController: NavController) {
    var selectedOption by remember { mutableStateOf<CoupleSetupOption?>(null) }
    var coupleCode by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf<String?>(null) }
    
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val authService = remember { AuthService(context) }
    val coroutineScope = rememberCoroutineScope()
    
    // Check if user is logged in
    LaunchedEffect(Unit) {
        if (!authService.isUserLoggedIn()) {
            // User is not logged in, redirect to login screen
            Log.d("CoupleSetupScreen", "User not logged in, redirecting to login screen")
            navController.navigate(Screen.Login.route) {
                popUpTo(Screen.CoupleSetup.route) { inclusive = true }
            }
            return@LaunchedEffect
        }
        
        // Check if user is already connected with a partner
        if (authService.isConnectedWithPartner()) {
            // User is already connected, redirect to timeline
            Log.d("CoupleSetupScreen", "User already connected with partner, redirecting to timeline")
            navController.navigate(Screen.Timeline.route) {
                popUpTo(Screen.CoupleSetup.route) { inclusive = true }
            }
        }
    }
    
    // Get the current user ID
    val currentUserId = authService.getCurrentUserId()
    
    // Log when the screen is composed
    Log.d("CoupleSetupScreen", "CoupleSetupScreen is being composed with user ID: $currentUserId")
    
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
                    }
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Display UI based on selected option
            when (selectedOption) {
                CoupleSetupOption.GENERATE -> {
                    // Generate Code UI
                    generatedCode?.let { code ->
                        Text(
                            text = "Your Couple Code",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        Text(
                            text = code,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        Text(
                            text = "Share this code with your partner to connect your accounts",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(bottom = 24.dp)
                        )
                        
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
                                Log.d("CoupleSetupScreen", "Continue button clicked in Generate option")
                                
                                // Save the user data with the generated code
                                isLoading = true
                                errorMessage = null
                                successMessage = "Saving your couple code..."
                                
                                // Save to Firestore
                                val userData = hashMapOf<String, Any>(
                                    "coupleCode" to code,
                                    "updatedAt" to Date()
                                )
                                
                                FirebaseFirestore.getInstance().collection("users")
                                    .document(currentUserId)
                                    .update(userData)
                                    .addOnSuccessListener {
                                        Log.d("CoupleSetupScreen", "User data saved successfully with code: $code")
                                        successMessage = "Your couple code has been created! Share it with your partner."
                                        isLoading = false
                                        
                                        // Navigate to Timeline screen
                                        Log.d("CoupleSetupScreen", "Navigating to Timeline screen from Generate option")
                                        navController.navigate(Screen.Timeline.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("CoupleSetupScreen", "Error saving user data: ${e.message}")
                                        errorMessage = "Failed to save your couple code: ${e.message}"
                                        successMessage = null
                                        isLoading = false
                                        
                                        // If in emulator, navigate anyway for testing
                                        if (isEmulator()) {
                                            Log.d("CoupleSetupScreen", "In emulator, navigating despite error")
                                            navController.navigate(Screen.Timeline.route) {
                                                popUpTo(Screen.Login.route) { inclusive = true }
                                            }
                                        }
                                    }
                            },
                            enabled = !isLoading
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
                
                CoupleSetupOption.ENTER -> {
                    // Enter Code UI
                    Text(
                        text = "Enter your partner's couple code",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    OutlinedTextField(
                        value = coupleCode,
                        onValueChange = { 
                            // Convert to uppercase and limit to 6 characters
                            coupleCode = it.uppercase().take(6)
                        },
                        label = { Text("Couple Code") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 24.dp)
                    )
                    
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
                            errorMessage = null
                            successMessage = null
                            
                            // Check if the code is valid format
                            if (coupleCode.length != 6) {
                                errorMessage = "Couple code must be 6 characters long."
                                return@Button
                            }
                            
                            isLoading = true
                            successMessage = "Looking for your partner..."
                            
                            // First, find the partner with this code
                            FirebaseFirestore.getInstance().collection("users")
                                .whereEqualTo("coupleCode", coupleCode)
                                .get()
                                .addOnSuccessListener { documents ->
                                    if (documents.isEmpty) {
                                        Log.d("CoupleSetupScreen", "No partner found with code: $coupleCode")
                                        errorMessage = "Invalid couple code. Please check and try again."
                                        successMessage = null
                                        isLoading = false
                                        return@addOnSuccessListener
                                    }
                                    
                                    val partnerDocument = documents.documents[0]
                                    val partnerId = partnerDocument.id
                                    
                                    if (partnerId == currentUserId) {
                                        Log.d("CoupleSetupScreen", "User tried to connect with their own code")
                                        errorMessage = "You cannot connect with your own code."
                                        successMessage = null
                                        isLoading = false
                                        return@addOnSuccessListener
                                    }
                                    
                                    Log.d("CoupleSetupScreen", "Found partner with ID: $partnerId")
                                    successMessage = "Partner found! Connecting..."
                                    
                                    // Now update both user records to connect them
                                    // Update current user's record
                                    val currentUserData: Map<String, Any> = hashMapOf(
                                        "partnerId" to partnerId,
                                        "coupleCode" to "", // No need for a code anymore
                                        "isConnected" to true,
                                        "connectedAt" to Date(),
                                        "updatedAt" to Date()
                                    )
                                    
                                    // Update partner's record
                                    val partnerUpdateData: Map<String, Any> = hashMapOf(
                                        "partnerId" to currentUserId,
                                        "isConnected" to true,
                                        "connectedAt" to Date(),
                                        "coupleCode" to "", // Clear the partner's code too
                                        "updatedAt" to Date()
                                    )
                                    
                                    // Use a batch write to update both records atomically
                                    val batch = FirebaseFirestore.getInstance().batch()
                                    
                                    Log.d("CoupleSetupScreen", "Updating user records to connect partners")
                                    
                                    batch.update(FirebaseFirestore.getInstance().collection("users").document(currentUserId), 
                                              currentUserData)
                                    batch.update(FirebaseFirestore.getInstance().collection("users").document(partnerId), 
                                                partnerUpdateData)
                                    
                                    batch.commit()
                                        .addOnSuccessListener {
                                            Log.d("CoupleSetupScreen", "Successfully connected with partner: $partnerId")
                                            successMessage = "Successfully connected with your partner! Redirecting..."
                                            isLoading = false
                                            
                                            // Navigate to Timeline screen
                                            navController.navigate(Screen.Timeline.route) {
                                                popUpTo(Screen.Login.route) { inclusive = true }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("CoupleSetupScreen", "Error connecting with partner: ${e.message}")
                                            errorMessage = "Failed to connect with your partner: ${e.message}"
                                            successMessage = null
                                            isLoading = false
                                            
                                            // If in emulator, navigate anyway for testing
                                            if (isEmulator()) {
                                                Log.d("CoupleSetupScreen", "In emulator, navigating despite error")
                                                navController.navigate(Screen.Timeline.route) {
                                                    popUpTo(Screen.Login.route) { inclusive = true }
                                                }
                                            }
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("CoupleSetupScreen", "Error finding partner: ${e.message}")
                                    errorMessage = "Failed to find partner: ${e.message}"
                                    successMessage = null
                                    isLoading = false
                                }
                        },
                        enabled = coupleCode.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Connect")
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

@OptIn(ExperimentalMaterial3Api::class)
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
