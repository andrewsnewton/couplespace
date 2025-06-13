package com.newton.couplespace.screens.health

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.addCallback
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.newton.couplespace.ui.theme.BondedTheme

/**
 * Activity that displays Health Connect permission rationales to the user.
 * This is launched when users click the privacy policy link in Health Connect settings.
 * 
 * It handles both:
 * - ACTION_SHOW_PERMISSIONS_RATIONALE for Android 13 and below
 * - VIEW_PERMISSION_USAGE for Android 14+
 */
class PermissionsRationaleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set up back press handling using the new API
        onBackPressedDispatcher.addCallback(this) {
            finish()
        }
        
        // Get permission names from the intent if available
        val permissionNames = intent?.getStringArrayExtra("android.health.permission.names")
        
        setContent {
            BondedTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    PermissionsRationaleScreen(permissionNames)
                }
            }
        }
    }
}

@Composable
fun PermissionsRationaleScreen(permissionNames: Array<String>?) {
    val scrollState = rememberScrollState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Health Data Access",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )
        
        Text(
            text = "CoupleSpace uses Health Connect to help you track and share health metrics with your partner.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // If we have specific permissions to explain, show them
        if (!permissionNames.isNullOrEmpty()) {
            Text(
                text = "Why we need these permissions:",
                style = MaterialTheme.typography.titleMedium
            )
            
            permissionNames.forEach { permission ->
                val (permissionTitle, rationale) = getPermissionRationale(permission)
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = permissionTitle,
                            style = MaterialTheme.typography.titleSmall
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = rationale,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        } else {
            // Generic explanation if no specific permissions are provided
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Steps",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "To track your daily step count and help you meet your fitness goals"
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Heart Rate",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "To monitor your heart health and track cardiovascular activity"
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Sleep",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "To analyze your sleep patterns and help improve your rest quality"
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Calories",
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "To track your energy expenditure and help with nutrition planning"
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Your data remains private and secure. You can revoke permissions at any time through Health Connect settings.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { /* Close the activity */ },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Text("Close")
        }
    }
}

/**
 * Returns a user-friendly title and rationale message for a given Health Connect permission
 */
private fun getPermissionRationale(permission: String): Pair<String, String> {
    return when {
        permission.contains("steps", ignoreCase = true) -> 
            Pair("Steps", "To track your daily step count and help you meet your fitness goals")
            
        permission.contains("heart_rate", ignoreCase = true) -> 
            Pair("Heart Rate", "To monitor your heart health and track cardiovascular activity")
            
        permission.contains("sleep", ignoreCase = true) -> 
            Pair("Sleep", "To analyze your sleep patterns and help improve your rest quality")
            
        permission.contains("calories", ignoreCase = true) -> 
            Pair("Calories", "To track your energy expenditure and help with nutrition planning")
            
        permission.contains("activity", ignoreCase = true) -> 
            Pair("Activity", "To monitor your physical activities and exercise routines")
            
        permission.contains("nutrition", ignoreCase = true) -> 
            Pair("Nutrition", "To track your food intake and help with meal planning")
            
        permission.contains("weight", ignoreCase = true) -> 
            Pair("Weight", "To monitor weight changes and body composition")
            
        permission.contains("distance", ignoreCase = true) -> 
            Pair("Distance", "To track how far you've moved during activities")
            
        else -> Pair("Health Data", "To provide you with comprehensive health tracking features")
    }
}
