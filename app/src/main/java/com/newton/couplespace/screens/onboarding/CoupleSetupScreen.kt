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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CoupleSetupScreen(navController: NavController) {
    var selectedOption by remember { mutableStateOf<CoupleSetupOption?>(null) }
    var coupleCode by remember { mutableStateOf("") }
    var generatedCode by remember { mutableStateOf<String?>(null) }
    
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    
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
                                        if (currentUserId != null) {
                                            isLoading = true
                                            
                                            // Update user with the generated code
                                            FirebaseFirestore.getInstance().collection("users")
                                                .document(currentUserId)
                                                .update(
                                                    mapOf(
                                                        "coupleCode" to code,
                                                        "updatedAt" to Date()
                                                    )
                                                )
                                                .addOnSuccessListener {
                                                    isLoading = false
                                                    successMessage = "Code saved! Share it with your partner."
                                                    
                                                    // Navigate to main screen after a delay
                                                    navController.navigate(Screen.Timeline.route) {
                                                        popUpTo(Screen.Welcome.route) { inclusive = true }
                                                    }
                                                }
                                                .addOnFailureListener { e ->
                                                    isLoading = false
                                                    errorMessage = "Error: ${e.message}"
                                                }
                                        }
                                    },
                                    enabled = !isLoading
                                ) {
                                    Text(text = if (isLoading) "Saving..." else "Continue")
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
                                if (coupleCode.isNotBlank() && currentUserId != null) {
                                    isLoading = true
                                    errorMessage = null
                                    
                                    // Find partner with this code
                                    FirebaseFirestore.getInstance().collection("users")
                                        .whereEqualTo("coupleCode", coupleCode)
                                        .get()
                                        .addOnSuccessListener { documents ->
                                            if (documents.isEmpty) {
                                                isLoading = false
                                                errorMessage = "No user found with this code"
                                            } else {
                                                val partner = documents.documents.first()
                                                val partnerId = partner.id
                                                
                                                // Update current user with partner ID
                                                FirebaseFirestore.getInstance().collection("users")
                                                    .document(currentUserId)
                                                    .update(
                                                        mapOf(
                                                            "partnerId" to partnerId,
                                                            "updatedAt" to Date()
                                                        )
                                                    )
                                                    .addOnSuccessListener {
                                                        // Update partner with current user ID
                                                        FirebaseFirestore.getInstance().collection("users")
                                                            .document(partnerId)
                                                            .update(
                                                                mapOf(
                                                                    "partnerId" to currentUserId,
                                                                    "updatedAt" to Date()
                                                                )
                                                            )
                                                            .addOnSuccessListener {
                                                                isLoading = false
                                                                successMessage = "Successfully connected with your partner!"
                                                                
                                                                // Navigate to main screen after a delay
                                                                navController.navigate(Screen.Timeline.route) {
                                                                    popUpTo(Screen.Welcome.route) { inclusive = true }
                                                                }
                                                            }
                                                            .addOnFailureListener { e ->
                                                                isLoading = false
                                                                errorMessage = "Error updating partner: ${e.message}"
                                                            }
                                                    }
                                                    .addOnFailureListener { e ->
                                                        isLoading = false
                                                        errorMessage = "Error updating your profile: ${e.message}"
                                                    }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            isLoading = false
                                            errorMessage = "Error searching for partner: ${e.message}"
                                        }
                                } else {
                                    errorMessage = "Please enter a valid code"
                                }
                            },
                            enabled = !isLoading && coupleCode.isNotBlank()
                        ) {
                            Text(text = if (isLoading) "Connecting..." else "Connect")
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
