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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.newton.couplespace.models.User
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController? = null) {
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
                FirebaseFirestore.getInstance()
                    .collection("users")
                    .document(currentUserId)
                    .get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            val userData = document.toObject(User::class.java)
                            user = userData
                            
                            // Initialize form fields
                            name = userData?.name ?: ""
                            age = userData?.age?.toString() ?: ""
                            height = userData?.height?.toString() ?: ""
                            weight = userData?.weight?.toString() ?: ""
                        } else {
                            errorMessage = "User profile not found"
                        }
                        isLoading = false
                    }
                    .addOnFailureListener { e ->
                        errorMessage = "Error loading profile: ${e.message}"
                        isLoading = false
                    }
            } else {
                errorMessage = "Not authenticated"
                isLoading = false
            }
        } catch (e: Exception) {
            errorMessage = "Error loading profile: ${e.message}"
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    if (navController != null) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    }
                },
                actions = {
                    if (!isEditMode) {
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
                    Text(
                        text = errorMessage ?: "Unknown error",
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = {
                            // Sign out and navigate to welcome screen
                            FirebaseAuth.getInstance().signOut()
                            navController?.navigate("welcome") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    ) {
                        Text("Sign Out and Restart")
                    }
                }
            } else {
                if (isEditMode) {
                    // Edit Mode
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
                            // Update user profile
                            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                            if (currentUserId != null && user != null) {
                                isLoading = true
                                
                                val updatedUser = user!!.copy(
                                    name = name,
                                    age = age.toIntOrNull() ?: 0,
                                    height = height.toIntOrNull() ?: 0,
                                    weight = weight.toDoubleOrNull() ?: 0.0,
                                    updatedAt = Date()
                                )
                                
                                FirebaseFirestore.getInstance()
                                    .collection("users")
                                    .document(currentUserId)
                                    .set(updatedUser)
                                    .addOnSuccessListener {
                                        user = updatedUser
                                        isEditMode = false
                                        isLoading = false
                                    }
                                    .addOnFailureListener { e ->
                                        errorMessage = "Failed to update profile: ${e.message}"
                                        isLoading = false
                                    }
                            }
                        },
                        onCancel = { isEditMode = false }
                    )
                } else {
                    // View Mode
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
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        
        // Profile Avatar (placeholder)
        Surface(
            modifier = Modifier.size(120.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.primaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = user?.name?.firstOrNull()?.toString() ?: "?",
                    style = MaterialTheme.typography.headlineLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // User Name
        Text(
            text = user?.name ?: "Unknown",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // User Details
        ProfileInfoCard(user = user)
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Couple Information
        CoupleInfoCard(user = user)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Sign Out Button
        OutlinedButton(
            onClick = {
                FirebaseAuth.getInstance().signOut()
            },
            modifier = Modifier.fillMaxWidth(0.8f)
        ) {
            Text("Sign Out")
        }
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
                text = "Personal Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
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
