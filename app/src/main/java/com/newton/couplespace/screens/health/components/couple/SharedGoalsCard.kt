package com.newton.couplespace.screens.health.components.couple

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.newton.couplespace.screens.health.data.models.SharedGoalType
import com.newton.couplespace.screens.health.data.models.SharedHealthGoal
import java.time.format.DateTimeFormatter

/**
 * A card for displaying and managing shared health goals between partners
 */
@Composable
fun SharedGoalsCard(
    goals: List<SharedHealthGoal>,
    onUpdateProgress: (String, Int) -> Unit,
    onCreateGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and add button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Shared Goals",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                
                IconButton(onClick = onCreateGoal) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Create Goal",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Goals horizontal list
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(goals) { goal ->
                    SharedGoalItem(
                        goal = goal,
                        onUpdateProgress = { progress -> onUpdateProgress(goal.id, progress) }
                    )
                }
                
                // Add goal item
                item {
                    NewGoalItem(onClick = onCreateGoal)
                }
            }
        }
    }
}

/**
 * A card displaying a shared goal
 */
@Composable
fun SharedGoalItem(
    goal: SharedHealthGoal,
    onUpdateProgress: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.width(200.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 1.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Goal icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(getColorForGoalType(goal.type)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForGoalType(goal.type),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Goal type
            Text(
                text = formatGoalType(goal.type),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            // Goal target
            Text(
                text = formatGoalTarget(goal.type, goal.target),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Progress section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "You",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "${goal.progress}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    LinearProgressIndicator(
                        progress = goal.progress.toFloat() / goal.target.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = getColorForGoalType(goal.type)
                    )
                }
                
                Box(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(24.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "VS",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Partner",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = "${goal.progress}",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                    
                    LinearProgressIndicator(
                        progress = goal.progress.toFloat() / goal.target.toFloat(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(top = 4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                        color = getColorForGoalType(goal.type)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Goal timeframe
            val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
            Text(
                text = "${goal.startDate.format(dateFormatter)} - ${goal.endDate.format(dateFormatter)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Update button
            var progress by remember { mutableStateOf("") }
            var showUpdateDialog by remember { mutableStateOf(false) }
            
            Button(
                onClick = { showUpdateDialog = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = getColorForGoalType(goal.type).copy(alpha = 0.8f)
                )
            ) {
                Text(text = "Update Progress")
            }
            
            // Update dialog
            if (showUpdateDialog) {
                AlertDialog(
                    onDismissRequest = { showUpdateDialog = false },
                    title = { Text("Update Your Progress") },
                    text = {
                        Column {
                            Text("Enter your current progress for ${formatGoalType(goal.type)}")
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            OutlinedTextField(
                                value = progress,
                                onValueChange = { newValue ->
                                    // Only allow numeric input
                                    if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                        progress = newValue
                                    }
                                },
                                label = { Text("Current Progress") },
                                singleLine = true,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                                )
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                progress.toIntOrNull()?.let { newProgress ->
                                    if (newProgress >= 0) {
                                        onUpdateProgress(newProgress)
                                    }
                                }
                                showUpdateDialog = false
                            }
                        ) {
                            Text("Update")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showUpdateDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

/**
 * A card for creating a new goal
 */
@Composable
fun NewGoalItem(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .width(160.dp)
            .height(280.dp), // Match the approximate height of goal items
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Create New Goal",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(36.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = "Create New Goal",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text("Start")
            }
        }
    }
}

/**
 * Gets the appropriate icon for a goal type
 */
@Composable
fun getIconForGoalType(type: SharedGoalType): ImageVector {
    return when (type) {
        SharedGoalType.STEPS -> Icons.Default.DirectionsWalk
        SharedGoalType.DISTANCE -> Icons.Default.Straighten
        SharedGoalType.WATER_INTAKE -> Icons.Default.WaterDrop
        SharedGoalType.ACTIVE_MINUTES -> Icons.Default.Timer
        SharedGoalType.SLEEP_DURATION -> Icons.Default.Bedtime
        SharedGoalType.WEIGHT_LOSS -> Icons.Default.ControlPoint
        SharedGoalType.CALORIES_BURNED -> Icons.Default.LocalFireDepartment
    }
}

/**
 * Gets the appropriate color for a goal type
 */
@Composable
fun getColorForGoalType(type: SharedGoalType): Color {
    return when (type) {
        SharedGoalType.STEPS -> Color(0xFF4CAF50)  // Green
        SharedGoalType.DISTANCE -> Color(0xFF2196F3)  // Blue
        SharedGoalType.WATER_INTAKE -> Color(0xFF03A9F4)  // Light Blue
        SharedGoalType.ACTIVE_MINUTES -> Color(0xFFFF9800)  // Orange
        SharedGoalType.SLEEP_DURATION -> Color(0xFF673AB7)  // Purple
        SharedGoalType.WEIGHT_LOSS -> Color(0xFF607D8B)  // Blue Grey
        SharedGoalType.CALORIES_BURNED -> Color(0xFFF44336)  // Red
    }
}

/**
 * Formats a goal type for display
 */
fun formatGoalType(type: SharedGoalType): String {
    return when (type) {
        SharedGoalType.STEPS -> "Daily Steps"
        SharedGoalType.DISTANCE -> "Distance Goal"
        SharedGoalType.WATER_INTAKE -> "Water Intake"
        SharedGoalType.ACTIVE_MINUTES -> "Active Minutes"
        SharedGoalType.SLEEP_DURATION -> "Sleep Duration"
        SharedGoalType.WEIGHT_LOSS -> "Weight Loss"
        SharedGoalType.CALORIES_BURNED -> "Calories Burned"
    }
}

/**
 * Formats a goal target for display
 */
fun formatGoalTarget(type: SharedGoalType, target: Int): String {
    return when (type) {
        SharedGoalType.STEPS -> "$target steps"
        SharedGoalType.DISTANCE -> "$target meters"
        SharedGoalType.WATER_INTAKE -> "$target ml"
        SharedGoalType.ACTIVE_MINUTES -> "$target minutes"
        SharedGoalType.SLEEP_DURATION -> "$target hours"
        SharedGoalType.WEIGHT_LOSS -> "$target kg"
        SharedGoalType.CALORIES_BURNED -> "$target calories"
    }
}
