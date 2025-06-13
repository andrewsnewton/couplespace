package com.newton.couplespace.screens.health.components.healthconnect

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Card that displays Health Connect permission status and allows the user to connect
 */
@Composable
fun HealthConnectPermissionCard(
    isHealthConnectAvailable: Boolean,
    hasPermissions: Boolean,
    isRequestingPermissions: Boolean = false,
    onConnectClick: () -> Unit,
    onLearnMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (hasPermissions) "Connected to Health Connect" 
                       else if (isRequestingPermissions) "Requesting Permissions..."
                       else if (isHealthConnectAvailable) "Health Connect Available" 
                       else "Health Connect Not Available",
                style = MaterialTheme.typography.titleMedium
            )
            
            Text(
                text = if (hasPermissions) {
                    "Your health data is being synced from Health Connect."
                } else if (isRequestingPermissions) {
                    "Please wait while we request Health Connect permissions..."
                } else if (isHealthConnectAvailable) {
                    "Connect to Health Connect to sync your health data."
                } else {
                    "Health Connect is not available on your device. Install it from the Google Play Store to sync your health data."
                },
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
            
            if (isRequestingPermissions) {
                Spacer(modifier = Modifier.height(8.dp))
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onConnectClick,
                    enabled = !hasPermissions && !isRequestingPermissions,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (hasPermissions) "Connected" 
                               else if (isRequestingPermissions) "Requesting..."
                               else if (isHealthConnectAvailable) "Connect" 
                               else "Install Health Connect"
                    )
                }
                
                OutlinedButton(
                    onClick = onLearnMoreClick,
                    enabled = !isRequestingPermissions,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Learn More")
                }
            }
        }
    }
}