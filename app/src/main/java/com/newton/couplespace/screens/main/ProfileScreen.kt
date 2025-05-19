package com.newton.couplespace.screens.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.newton.couplespace.models.User
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    // Add a log statement when the ProfileScreen is composed
    Log.d("ProfileScreen", "ProfileScreen is being composed")
    
    var user by remember { mutableStateOf<User?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isEditMode by remember { mutableStateOf(false) }
    
    // Form fields for edit mode
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    
    // Fetch user data
    LaunchedEffect(key1 = Unit) {
        try {
            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
            if (currentUserId != null) {
                Log.d("ProfileScreen", "Fetching user data for ID: $currentUserId")
                
                // Check if running in emulator
                val isEmulator = isEmulator()
                Log.d("ProfileScreen", "Is emulator: $isEmulator")
                
                if (isEmulator) {
                    // In emulator, we might have App Check issues, so add a delay and retry mechanism
                    Log.d("ProfileScreen", "Running in emulator, using special handling")
                    
                    // First attempt
                    FirebaseFirestore.getInstance().collection("users").document(currentUserId).get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val userData = document.toObject(User::class.java)
                                if (userData != null) {
                                    user = userData
                                    name = userData.name ?: ""
                                    age = userData.age.toString()
                                    height = userData.height.toString()
                                    weight = userData.weight.toString()
                                    isLoading = false
                                }
                            } else {
                                errorMessage = "User data not found"
                                isLoading = false
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("ProfileScreen", "First attempt failed, checking if it's an App Check error", e)
                            
                            if (e.message?.contains("App attestation failed") == true ||
                                e.message?.contains("AppCheck") == true) {
                                
                                Log.d("ProfileScreen", "App Check error detected, using fallback data")
                                
                                // Create dummy user data for testing in emulator
                                val dummyUser = User(
                                    id = currentUserId,
                                    name = "Test User",
                                    age = 30,
                                    height = 175,
                                    weight = 70.0,
                                    coupleCode = "TEST123",
                                    partnerId = ""
                                )
                                
                                user = dummyUser
                                name = dummyUser.name
                                age = dummyUser.age.toString()
                                height = dummyUser.height.toString()
                                weight = dummyUser.weight.toString()
                                isLoading = false
                                
                                Log.d("ProfileScreen", "Using fallback data due to App Check error")
                            } else {
                                Log.e("ProfileScreen", "Error fetching user data", e)
                                errorMessage = "Error fetching user data: ${e.message}"
                                isLoading = false
                            }
                        }
                } else {
                    // On real device, normal flow
                    FirebaseFirestore.getInstance().collection("users").document(currentUserId).get()
                        .addOnSuccessListener { document ->
                            if (document != null && document.exists()) {
                                val userData = document.toObject(User::class.java)
                                if (userData != null) {
                                    user = userData
                                    name = userData.name ?: ""
                                    age = userData.age.toString()
                                    height = userData.height.toString()
                                    weight = userData.weight.toString()
                                    isLoading = false
                                    
                                    Log.d("ProfileScreen", "Successfully loaded user data")
                                }
                            } else {
                                errorMessage = "User data not found"
                                isLoading = false
                                Log.d("ProfileScreen", "User document doesn't exist")
                            }
                        }
                        .addOnFailureListener { e ->
                            Log.e("ProfileScreen", "Error fetching user data", e)
                            errorMessage = "Error fetching user data: ${e.message}"
                            isLoading = false
                        }
                }
            } else {
                errorMessage = "User not authenticated"
                isLoading = false
                Log.d("ProfileScreen", "User not authenticated")
            }
        } catch (e: Exception) {
            errorMessage = "Error loading profile: ${e.message}"
            isLoading = false
            Log.e("ProfileScreen", "Exception in profile loading", e)
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    if (navController != null) {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (!isEditMode && user != null) {
                        IconButton(onClick = { isEditMode = true }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Profile")
                        }
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMessage != null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text("Error: $errorMessage", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = {
                        // Sign out and navigate to welcome screen
                        FirebaseAuth.getInstance().signOut()
                        navController?.navigate("welcome") {
                            popUpTo("welcome") {
                                inclusive = true
                            }
                        }
                    }) {
                        Text("Sign Out")
                    }
                }
            } else {
                if (isEditMode) {
                    EditProfileContent(
                        name = name,
                        age = age,
                        height = height,
                        weight = weight,
                        onNameChange = { name = it },
                        onAgeChange = { age = it },
                        onHeightChange = { height = it },
                        onWeightChange = { weight = it },
                        onSave = {
                            isLoading = true
                            val updatedUser = user?.copy(
                                name = name,
                                age = age.toIntOrNull() ?: 0,
                                height = height.toIntOrNull() ?: 0,
                                weight = weight.toDoubleOrNull() ?: 0.0
                            )
                            
                            if (updatedUser != null) {
                                val userId = FirebaseAuth.getInstance().currentUser?.uid
                                if (userId != null) {
                                    FirebaseFirestore.getInstance().collection("users").document(userId)
                                        .set(updatedUser)
                                        .addOnSuccessListener {
                                            user = updatedUser
                                            isEditMode = false
                                            isLoading = false
                                        }
                                        .addOnFailureListener { e ->
                                            errorMessage = "Error updating profile: ${e.message}"
                                            isLoading = false
                                        }
                                }
                            }
                        },
                        onCancel = { isEditMode = false }
                    )
                } else {
                    ViewProfileContent(user = user)
                }
            }
        }
    }
}

@Composable
fun ViewProfileContent(user: User?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Personal Information",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        ProfileInfoCard(user = user)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        CoupleInfoCard(user = user)
    }
}

@Composable
fun ProfileInfoCard(user: User?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Profile Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ProfileInfoRow(label = "Name", value = user?.name ?: "Not set")
            ProfileInfoRow(label = "Age", value = "${user?.age ?: "N/A"} years")
            ProfileInfoRow(label = "Height", value = "${user?.height ?: "N/A"} cm")
            ProfileInfoRow(label = "Weight", value = "${user?.weight ?: "N/A"} kg")
            
            if (user?.createdAt != null) {
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
                ProfileInfoRow(label = "Joined", value = dateFormat.format(user.createdAt))
            }
        }
    }
}

@Composable
fun CoupleInfoCard(user: User?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Couple Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ProfileInfoRow(label = "Couple Code", value = user?.coupleCode ?: "Not linked")
            ProfileInfoRow(label = "Partner ID", value = if (user?.partnerId.isNullOrBlank()) "Not linked" else "Linked")
        }
    }
}

@Composable
fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium
        )
    }
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

@Composable
fun EditProfileContent(
    name: String,
    age: String,
    height: String,
    weight: String,
    onNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Edit Profile",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Name Field
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Age Field
        OutlinedTextField(
            value = age,
            onValueChange = onAgeChange,
            label = { Text("Age") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Height Field
        OutlinedTextField(
            value = height,
            onValueChange = onHeightChange,
            label = { Text("Height (cm)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Weight Field
        OutlinedTextField(
            value = weight,
            onValueChange = onWeightChange,
            label = { Text("Weight (kg)") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel")
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Button(onClick = onSave) {
                Text("Save")
            }
        }
    }
}