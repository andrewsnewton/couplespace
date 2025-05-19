package com.newton.couplespace.screens.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.util.Log
import com.newton.couplespace.auth.AuthService
import com.newton.couplespace.navigation.Screen
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(navController: NavController) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isNewUser by remember { mutableStateOf(false) }
    
    val context = LocalContext.current
    val authService = remember { AuthService(context) }
    val coroutineScope = rememberCoroutineScope()
    
    // Check if already logged in
    LaunchedEffect(Unit) {
        // Skip emulator testing to prevent infinite loops
        // Only check if user is already logged in
        
        // Check if user is already logged in
        if (authService.isUserLoggedIn()) {
            // Check if profile is complete
            if (authService.isProfileComplete()) {
                if (authService.isConnectedWithPartner()) {
                    // User has completed profile and is connected with partner
                    navController.navigate(Screen.Timeline.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                } else {
                    // User has completed profile but is not connected with partner
                    navController.navigate(Screen.CoupleSetup.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            } else {
                // User has not completed profile
                navController.navigate(Screen.UserSetup.route) {
                    popUpTo(Screen.Login.route) { inclusive = true }
                }
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isNewUser) "Create Account" else "Sign In") }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (isNewUser) "Create a new account" else "Sign in to your account",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
                
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    label = { Text("Email") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                errorMessage?.let {
                    Text(
                        text = it,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Please enter both email and password"
                            return@Button
                        }
                        
                        isLoading = true
                        errorMessage = null
                        
                        coroutineScope.launch {
                            val result = if (isNewUser) {
                                authService.createAccountWithEmailPassword(email, password)
                            } else {
                                authService.signInWithEmailPassword(email, password)
                            }
                            
                            result.fold(
                                onSuccess = {
                                    // Check if profile is complete
                                    if (authService.isProfileComplete()) {
                                        if (authService.isConnectedWithPartner()) {
                                            // User has completed profile and is connected with partner
                                            navController.navigate(Screen.Timeline.route) {
                                                popUpTo(Screen.Login.route) { inclusive = true }
                                            }
                                        } else {
                                            // User has completed profile but is not connected with partner
                                            navController.navigate(Screen.CoupleSetup.route) {
                                                popUpTo(Screen.Login.route) { inclusive = true }
                                            }
                                        }
                                    } else {
                                        // User has not completed profile
                                        navController.navigate(Screen.UserSetup.route) {
                                            popUpTo(Screen.Login.route) { inclusive = true }
                                        }
                                    }
                                },
                                onFailure = { e ->
                                    errorMessage = e.message ?: "Authentication failed"
                                    isLoading = false
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
                        Text(if (isNewUser) "Create Account" else "Sign In")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                TextButton(
                    onClick = { isNewUser = !isNewUser },
                    enabled = !isLoading
                ) {
                    Text(
                        if (isNewUser) "Already have an account? Sign In" 
                        else "Don't have an account? Create one"
                    )
                }
            }
        }
    }
}
