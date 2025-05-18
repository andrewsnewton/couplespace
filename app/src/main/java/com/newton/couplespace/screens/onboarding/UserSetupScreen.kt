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
                        
                        // Create user in Firebase Auth with anonymous sign-in
                        FirebaseAuth.getInstance().signInAnonymously()
                            .addOnSuccessListener { authResult ->
                                val userId = authResult.user?.uid ?: UUID.randomUUID().toString()
                                
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
                                        navController.navigate(Screen.CoupleSetup.route)
                                    }
                                    .addOnFailureListener { e ->
                                        isLoading = false
                                        errorMessage = "Error: ${e.message}"
                                    }
                            }
                            .addOnFailureListener { e ->
                                isLoading = false
                                errorMessage = "Authentication failed: ${e.message}"
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
