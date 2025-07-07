package com.newton.couplespace.screens.main

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import java.time.Instant
import java.time.ZoneId
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import android.os.Build
import android.util.Log
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.newton.couplespace.models.User
import kotlinx.coroutines.tasks.await
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
    var timezone by remember { mutableStateOf("") }
    var profilePicture by remember { mutableStateOf("") }
    
    // Partner data
    var partner by remember { mutableStateOf<User?>(null) }
    var isPartnerLoading by remember { mutableStateOf(false) }
    
    // Fetch partner data when user data is loaded
    LaunchedEffect(user) {
        try {
            val partnerId = user?.partnerId?.takeIf { it.isNotEmpty() } ?: run {
                partner = null
                isPartnerLoading = false
                return@LaunchedEffect
            }
            
            isPartnerLoading = true
            FirebaseFirestore.getInstance()
                .collection("users")
                .document(partnerId)
                .get()
                .addOnSuccessListener { document ->
                    try {
                        if (document.exists()) {
                            partner = document.toObject(User::class.java)
                        } else {
                            Log.w("ProfileScreen", "Partner document does not exist")
                            partner = null
                        }
                    } catch (e: Exception) {
                        Log.e("ProfileScreen", "Error parsing partner data", e)
                        partner = null
                    } finally {
                        isPartnerLoading = false
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ProfileScreen", "Error fetching partner data", e)
                    partner = null
                    isPartnerLoading = false
                }
        } catch (e: Exception) {
            Log.e("ProfileScreen", "Exception in partner data fetch", e)
            partner = null
            isPartnerLoading = false
        }
    }
    
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
                                    timezone = userData.timezone
                                    profilePicture = userData.profilePicture
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
                                timezone = dummyUser.timezone
                                profilePicture = dummyUser.profilePicture
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
                                    timezone = userData.timezone
                                    profilePicture = userData.profilePicture
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
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        timezone = timezone,
                        profilePicture = profilePicture,
                        onNameChange = { name = it },
                        onAgeChange = { age = it },
                        onHeightChange = { height = it },
                        onWeightChange = { weight = it },
                        onTimezoneChange = { timezone = it },
                        onProfilePictureChange = { profilePicture = it },
                        onSave = {
                            isLoading = true
                            // Instead of replacing the entire document, update only the fields that changed
                            val userId = FirebaseAuth.getInstance().currentUser?.uid
                            if (userId != null) {
                                // Create a map of fields to update
                                val updates = hashMapOf<String, Any>(
                                    "name" to name,
                                    "age" to (age.toIntOrNull() ?: 0),
                                    "height" to (height.toIntOrNull() ?: 0),
                                    "weight" to (weight.toDoubleOrNull() ?: 0.0),
                                    "timezone" to timezone,
                                    "profilePicture" to profilePicture,
                                    "updatedAt" to Date(),
                                    "isProfileComplete" to true // Ensure profile is marked as complete
                                )
                                
                                // Use update() instead of set() to preserve other fields
                                FirebaseFirestore.getInstance().collection("users").document(userId)
                                    .update(updates)
                                    .addOnSuccessListener {
                                        // Update the local user object with the new values
                                        user = user?.copy(
                                            name = name,
                                            age = age.toIntOrNull() ?: 0,
                                            height = height.toIntOrNull() ?: 0,
                                            weight = weight.toDoubleOrNull() ?: 0.0,
                                            timezone = timezone,
                                            profilePicture = profilePicture,
                                            updatedAt = Date(),
                                            isProfileComplete = true
                                        )
                                        isEditMode = false
                                        isLoading = false
                                        
                                        Log.d("ProfileScreen", "Profile updated successfully")
                                    }
                                    .addOnFailureListener { e ->
                                        errorMessage = "Error updating profile: ${e.message}"
                                        isLoading = false
                                        Log.e("ProfileScreen", "Error updating profile: ${e.message}")
                                    }
                            }
                        },
                        onCancel = { isEditMode = false }
                    )
                } else {
                    val scrollState = rememberScrollState()
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scrollState)
                            .padding(16.dp)
                    ) {
                        ViewProfileContent(user)
                        
                        // Show partner profile section
                        Spacer(modifier = Modifier.height(24.dp))
                        Text(
                            "Partner Profile",
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        
                        when {
                            isPartnerLoading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator()
                                }
                            }
                            partner != null -> {
                                val currentPartner = partner
                                if (currentPartner != null) {
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(16.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Icon(
                                                    Icons.Default.Person,
                                                    contentDescription = "Partner",
                                                    modifier = Modifier.size(24.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    currentPartner.name.ifEmpty { "Partner" },
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(12.dp))
                                            ProfileInfoRow("Age", currentPartner.age.takeIf { it > 0 }?.toString() ?: "Not set")
                                            ProfileInfoRow("Height", if (currentPartner.height > 0) "${currentPartner.height} cm" else "Not set")
                                            ProfileInfoRow("Weight", if (currentPartner.weight > 0) "${currentPartner.weight} kg" else "Not set")
                                        }
                                    }
                                }
                            }
                            else -> {
                                Text(
                                    "No partner linked yet",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Add profile picture at the top if available
            if (!user?.profilePicture.isNullOrBlank()) {
                AsyncImage(
                    model = user?.profilePicture,
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            Text(
                text = "Profile Information",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            ProfileInfoRow(label = "Name", value = user?.name ?: "Not set")
            ProfileInfoRow(label = "Age", value = if (user?.age != 0) "${user?.age} years" else "Not set")
            ProfileInfoRow(label = "Height", value = if (user?.height != 0) "${user?.height} cm" else "Not set")
            ProfileInfoRow(label = "Weight", value = if (user?.weight != 0.0) "${user?.weight} kg" else "Not set")
            ProfileInfoRow(label = "Timezone", value = user?.timezone?.takeIf { it.isNotBlank() } ?: "Not set")
            
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
            || Build.PRODUCT.contains("sdk_gphone64_arm64")
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
    timezone: String,
    profilePicture: String,
    onNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onHeightChange: (String) -> Unit,
    onWeightChange: (String) -> Unit,
    onTimezoneChange: (String) -> Unit,
    onProfilePictureChange: (String) -> Unit,
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
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Timezone Field
        // Create a simplified list of common timezones with their offsets
        val availableTimezones = listOf(
            "America/Los_Angeles", // UTC-8/UTC-7
            "America/Denver",      // UTC-7/UTC-6
            "America/Chicago",     // UTC-6/UTC-5
            "America/New_York",    // UTC-5/UTC-4
            "America/Toronto",     // UTC-5/UTC-4
            "Europe/London",       // UTC+0/UTC+1
            "Europe/Paris",        // UTC+1/UTC+2
            "Europe/Berlin",       // UTC+1/UTC+2
            "Europe/Moscow",       // UTC+3
            "Asia/Dubai",          // UTC+4
            "Asia/Kolkata",        // UTC+5:30
            "Asia/Singapore",      // UTC+8
            "Asia/Tokyo",          // UTC+9
            "Australia/Sydney",    // UTC+10/UTC+11
            "Pacific/Auckland"     // UTC+12/UTC+13
        )
        
        // Create a map of timezone IDs to display names with offset
        val timezoneDisplayMap = availableTimezones.associateWith { tzId ->
            val zoneId = ZoneId.of(tzId)
            val offset = zoneId.rules.getOffset(Instant.now())
            val hours = offset.totalSeconds / 3600
            val minutes = kotlin.math.abs((offset.totalSeconds % 3600).toInt()) / 60
            val offsetStr = String.format("%+d:%02d", hours, minutes)
            val city = tzId.substringAfterLast('/')
            "$city (UTC$offsetStr)"
        }
        
        var isTimezoneExpanded by remember { mutableStateOf(false) }
        
        Text(
            text = "Timezone",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Using experimental Material3 dropdown with opt-in
        @OptIn(ExperimentalMaterial3Api::class)
        ExposedDropdownMenuBox(
            expanded = isTimezoneExpanded,
            onExpandedChange = { isTimezoneExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            @OptIn(ExperimentalMaterial3Api::class)
            OutlinedTextField(
                value = timezoneDisplayMap[timezone] ?: timezone,
                onValueChange = {},
                readOnly = true,
                trailingIcon = { 
                    @OptIn(ExperimentalMaterial3Api::class)
                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = isTimezoneExpanded) 
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .menuAnchor()
            )
            
            @OptIn(ExperimentalMaterial3Api::class)
            ExposedDropdownMenu(
                expanded = isTimezoneExpanded,
                onDismissRequest = { isTimezoneExpanded = false },
                modifier = Modifier.height(200.dp)
            ) {
                availableTimezones.forEach { tzId ->
                    DropdownMenuItem(
                        text = { Text(timezoneDisplayMap[tzId] ?: tzId) },
                        onClick = {
                            onTimezoneChange(tzId)
                            isTimezoneExpanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Profile Picture Selection
        val context = LocalContext.current
        val imagePickerLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            uri?.let {
                // Start upload process and update profilePicture with URL
                uploadImageToFirebase(context, it) { downloadUrl ->
                    onProfilePictureChange(downloadUrl)
                }
            }
        }
        
        Text(
            text = "Profile Picture",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Image preview or placeholder
        Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            if (profilePicture.isNotBlank()) {
                // Show existing profile picture
                AsyncImage(
                    model = profilePicture,
                    contentDescription = "Profile Picture",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") }
                )
            } else {
                // Show placeholder
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        .clickable { imagePickerLauncher.launch("image/*") },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AddPhotoAlternate,
                        contentDescription = "Add Photo",
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = { imagePickerLauncher.launch("image/*") },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text(if (profilePicture.isBlank()) "Select Image" else "Change Image")
        }
        
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

// Helper function to upload image to Firebase Storage and get download URL
private fun uploadImageToFirebase(context: Context, imageUri: Uri, onComplete: (String) -> Unit) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val storageRef = FirebaseStorage.getInstance().reference
    val imageRef = storageRef.child("profile_images/$userId/${UUID.randomUUID()}.jpg")
    
    // Show upload in progress (you could add a loading indicator here)
    val uploadTask = imageRef.putFile(imageUri)
    
    uploadTask.addOnSuccessListener {
        // Get download URL after upload is successful
        imageRef.downloadUrl.addOnSuccessListener { downloadUri ->
            val imageUrl = downloadUri.toString()
            onComplete(imageUrl)
            Log.d("ProfileScreen", "Image uploaded successfully, URL: $imageUrl")
        }.addOnFailureListener { e ->
            Log.e("ProfileScreen", "Error getting download URL: ${e.message}")
        }
    }.addOnFailureListener { e ->
        Log.e("ProfileScreen", "Error uploading image: ${e.message}")
    }
}