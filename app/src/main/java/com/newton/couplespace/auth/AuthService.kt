package com.newton.couplespace.auth

import android.content.Context
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

/**
 * Service to handle authentication and user management
 */
class AuthService(private val context: Context) {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    private val sharedPrefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
    
    private val TAG = "AuthService"
    
    /**
     * Check if user is currently logged in
     */
    fun isUserLoggedIn(): Boolean {
        val currentUser = auth.currentUser
        return currentUser != null
    }
    
    /**
     * Get current user ID
     */
    fun getCurrentUserId(): String {
        val currentUser = auth.currentUser
        return currentUser?.uid ?: sharedPrefs.getString("user_id", "") ?: ""
    }
    
    /**
     * Sign in with email and password
     */
    suspend fun signInWithEmailPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            
            if (user != null) {
                // Store user ID in SharedPreferences for offline access
                sharedPrefs.edit().putString("user_id", user.uid).apply()
                
                // Update last login timestamp
                usersCollection.document(user.uid)
                    .update("lastLogin", Date())
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error updating last login: ${e.message}")
                    }
                
                Result.success(user)
            } else {
                Result.failure(Exception("Authentication failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Sign in failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Create a new account with email and password
     */
    suspend fun createAccountWithEmailPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            Log.d(TAG, "Attempting to create account with email: $email")
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            
            if (user != null) {
                Log.d(TAG, "Account created successfully for user: ${user.uid}")
                // Store user ID in SharedPreferences for offline access
                sharedPrefs.edit().putString("user_id", user.uid).apply()
                
                // Create initial user document in Firestore
                val userData = hashMapOf(
                    "userId" to user.uid,
                    "email" to email,
                    "createdAt" to Date(),
                    "lastLogin" to Date(),
                    "isProfileComplete" to false
                )
                
                usersCollection.document(user.uid)
                    .set(userData)
                    .addOnSuccessListener {
                        Log.d(TAG, "User document created successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e(TAG, "Error creating user document: ${e.message}")
                    }
                
                Result.success(user)
            } else {
                Log.e(TAG, "Account creation returned null user")
                Result.failure(Exception("Account creation failed"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Create account failed: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Sign out current user
     */
    fun signOut() {
        auth.signOut()
    }
    
    /**
     * Check if user has completed profile setup
     */
    suspend fun isProfileComplete(): Boolean {
        val userId = getCurrentUserId()
        if (userId.isBlank()) return false
        
        return try {
            val document = usersCollection.document(userId).get().await()
            document.getBoolean("isProfileComplete") ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error checking profile status: ${e.message}")
            false
        }
    }
    
    /**
     * Update user profile information
     */
    suspend fun updateUserProfile(name: String, birthdate: Date?): Result<Boolean> {
        val userId = getCurrentUserId()
        if (userId.isBlank()) return Result.failure(Exception("User not logged in"))
        
        val userData = hashMapOf<String, Any>(
            "name" to name,
            "isProfileComplete" to true
        )
        
        birthdate?.let { userData["birthdate"] = it }
        
        return try {
            usersCollection.document(userId).update(userData).await()
            Result.success(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating profile: ${e.message}")
            Result.failure(e)
        }
    }
    
    /**
     * Get user's partner ID from Firestore
     */
    suspend fun getPartnerIdFromFirestore(): String {
        val userId = getCurrentUserId()
        if (userId.isBlank()) return ""
        
        return try {
            val document = usersCollection.document(userId).get().await()
            document.getString("partnerId") ?: ""
        } catch (e: Exception) {
            Log.e(TAG, "Error getting partner ID: ${e.message}")
            ""
        }
    }
    
    /**
     * Check if user is connected with a partner
     */
    suspend fun isConnectedWithPartner(): Boolean {
        val partnerId = getPartnerIdFromFirestore()
        return partnerId.isNotBlank()
    }
    
    /**
     * Handle emulator testing - create a test user if in emulator
     * Returns empty string if not in emulator or if emulator testing is disabled
     */
    suspend fun handleEmulatorTesting(): String {
        // Disable automatic emulator testing to prevent infinite loops
        val enableEmulatorTesting = false
        
        // Check if running in emulator
        val isEmulator = isEmulator()
        
        if (isEmulator && enableEmulatorTesting) {
            Log.d(TAG, "Running in emulator, creating test user if needed")
            
            // Check if we have a stored test user ID
            var testUserId = sharedPrefs.getString("test_user_id", "") ?: ""
            
            if (testUserId.isBlank()) {
                // Create a new test user ID
                testUserId = "test_user_${System.currentTimeMillis()}"
                sharedPrefs.edit().putString("test_user_id", testUserId).apply()
                sharedPrefs.edit().putString("user_id", testUserId).apply()
                
                // Create a test user document in Firestore
                val userData = hashMapOf(
                    "userId" to testUserId,
                    "email" to "test@example.com",
                    "name" to "Test User",
                    "createdAt" to Date(),
                    "lastLogin" to Date(),
                    "isProfileComplete" to true
                )
                
                try {
                    usersCollection.document(testUserId).set(userData).await()
                    Log.d(TAG, "Created test user document in Firestore")
                } catch (e: Exception) {
                    Log.e(TAG, "Error creating test user document: ${e.message}")
                }
            }
            
            return testUserId
        }
        
        return ""
    }
    
    /**
     * Check if running in an emulator
     */
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
}
