package com.newton.couplespace.screens.health.components.nutrition

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/**
 * A card for tracking water intake with a visual representation
 */
@Composable
fun WaterTrackerCard(
    totalWaterIntake: Int,
    waterGoal: Int,
    onAddWater: (Int) -> Unit,
    onUpdateGoal: (Int) -> Unit,
    onSendReminder: () -> Unit,
    reminderEnabled: Boolean = false,
    reminderSettings: Map<String, Any> = emptyMap(),
    onUpdateReminderSettings: (Int, Int, Int, Boolean) -> Unit = { _, _, _, _ -> },
    modifier: Modifier = Modifier
) {
    var showGoalDialog by remember { mutableStateOf(false) }
    var showReminderDialog by remember { mutableStateOf(false) }
    
    // Reminder settings state - initialize from saved settings and preserve across recompositions
    var reminderInterval by remember(reminderSettings) { mutableStateOf(reminderSettings["intervalMinutes"] as? Int ?: 60) }
    var reminderStartTime by remember(reminderSettings) { mutableStateOf(reminderSettings["startHour"] as? Int ?: 8) }
    var reminderEndTime by remember(reminderSettings) { mutableStateOf(reminderSettings["endHour"] as? Int ?: 20) }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Water Intake",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                Row {
                    // Reminder button
                    IconButton(
                        onClick = { showReminderDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Water Reminders",
                            tint = if (reminderEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                    }
                    
                    // Spacer to maintain layout after removing test button
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    // Settings button
                    IconButton(
                        onClick = { showGoalDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Change Goal",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Water progress visualization
            WaterBottleVisualization(
                progress = totalWaterIntake.toFloat() / waterGoal,
                totalWaterIntake = totalWaterIntake,
                waterGoal = waterGoal
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Quick add buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QuickAddButton(
                    amount = 100,
                    onClick = { 
                        println("DEBUG: WaterTrackerCard - Add 100ml button clicked")
                        onAddWater(100) 
                    }
                )
                
                QuickAddButton(
                    amount = 250,
                    onClick = { onAddWater(250) }
                )
                
                QuickAddButton(
                    amount = 500,
                    onClick = { 
                        println("DEBUG: WaterTrackerCard - Add 500ml button clicked")
                        onAddWater(500) 
                    }
                )
            }
        }
    }
    
    // Water reminder dialog
    if (showReminderDialog) {
        WaterReminderDialog(
            enabled = reminderEnabled,
            intervalMinutes = reminderInterval,
            startHour = reminderStartTime,
            endHour = reminderEndTime,
            onDismiss = { showReminderDialog = false },
            onConfirm = { interval, start, end, enabled ->
                onUpdateReminderSettings(interval, start, end, enabled)
                showReminderDialog = false
            }
        )
    }
    
    // Goal setting dialog
    if (showGoalDialog) {
        var goalInputValue by remember { mutableStateOf(waterGoal.toString()) }
        
        AlertDialog(
            onDismissRequest = { showGoalDialog = false },
            title = { Text("Set Water Goal") },
            text = {
                Column {
                    Text("Set your daily water intake goal (in ml)")
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = goalInputValue,
                        onValueChange = { newValue ->
                            // Only allow numeric input
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                goalInputValue = newValue
                            }
                        },
                        label = { Text("Water Goal (ml)") },
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
                        // Convert input to integer and update goal
                        goalInputValue.toIntOrNull()?.let { newGoal ->
                            if (newGoal > 0) {
                                onUpdateGoal(newGoal)
                            }
                        }
                        showGoalDialog = false
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoalDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

/**
 * A visual representation of water intake as a filling bottle
 */
@Composable
fun WaterBottleVisualization(
    progress: Float,
    totalWaterIntake: Int,
    waterGoal: Int,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        label = "WaterProgress"
    )
    
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        // Bottle outline
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(Color(0xFFE3F2FD))
        )
        
        // Water fill level
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(animatedProgress)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp))
                .background(Color(0xFF2196F3))
        )
        
        // Water drop icon - moved to the side
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.WaterDrop,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(24.dp)
            )
        }
        
        // Text overlay with improved visibility
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$totalWaterIntake ml",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = if (animatedProgress > 0.5f) Color.White else Color(0xFF2196F3),
                modifier = Modifier.padding(bottom = 4.dp)
            )
            
            Text(
                text = "of $waterGoal ml",
                style = MaterialTheme.typography.bodyMedium,
                color = if (animatedProgress > 0.5f) Color.White.copy(alpha = 0.9f) else Color(0xFF2196F3).copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Percentage with background for better visibility
            Box(
                modifier = Modifier
                    .background(
                        color = if (animatedProgress > 0.5f) Color.White.copy(alpha = 0.2f) else Color(0xFF2196F3).copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "${(progress * 100).roundToInt()}%",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (animatedProgress > 0.5f) Color.White else Color(0xFF2196F3)
                )
            }
        }
    }
}

/**
 * A button for quickly adding a specific amount of water
 */
@Composable
fun QuickAddButton(
    amount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.primary
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            
            Spacer(modifier = Modifier.width(4.dp))
            
            Text(
                text = "$amount ml",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

/**
 * Dialog for setting water intake goal
 */
@Composable
fun WaterGoalDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var goalValue by remember { mutableStateOf(currentGoal.toString()) }
    var isError by remember { mutableStateOf(false) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set Water Goal") },
        text = {
            Column {
                Text(
                    "Set your daily water intake goal (500-5000 ml)",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                OutlinedTextField(
                    value = goalValue,
                    onValueChange = { 
                        goalValue = it.filter { char -> char.isDigit() }
                        isError = goalValue.toIntOrNull()?.let { it < 500 || it > 5000 } ?: true
                    },
                    label = { Text("Goal (ml)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = isError,
                    supportingText = if (isError) {
                        { Text("Please enter a value between 500 and 5000 ml") }
                    } else null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Preset buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf(1500, 2000, 2500, 3000).forEach { preset ->
                        SuggestionChip(
                            onClick = { 
                                goalValue = preset.toString()
                                isError = false
                            },
                            label = { Text("$preset ml") }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val goal = goalValue.toIntOrNull() ?: return@Button
                    if (goal in 500..5000) {
                        onConfirm(goal)
                    }
                },
                enabled = !isError && goalValue.isNotEmpty()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Dialog for configuring water intake reminders
 */
@Composable
fun WaterReminderDialog(
    enabled: Boolean,
    intervalMinutes: Int,
    startHour: Int,
    endHour: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int, Int, Boolean) -> Unit
) {
    // Use remember with the input parameters as keys to preserve state but update when inputs change
    var isEnabled by remember(enabled) { mutableStateOf(enabled) }
    var interval by remember(intervalMinutes) { mutableStateOf(intervalMinutes) }
    var start by remember(startHour) { mutableStateOf(startHour) }
    var end by remember(endHour) { mutableStateOf(endHour) }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Water Reminder Settings") },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Enable/disable switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Enable Reminders")
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = { isEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.primary,
                            checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Reminder interval
                Text("Reminder Interval")
                
                // Interval selection chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    FilterChip(
                        selected = interval == 30,
                        onClick = { interval = 30 },
                        label = { Text("30 min") },
                        enabled = isEnabled
                    )
                    
                    FilterChip(
                        selected = interval == 60,
                        onClick = { interval = 60 },
                        label = { Text("60 min") },
                        enabled = isEnabled
                    )
                    
                    FilterChip(
                        selected = interval == 90,
                        onClick = { interval = 90 },
                        label = { Text("90 min") },
                        enabled = isEnabled
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Active hours
                Text("Active Hours", style = MaterialTheme.typography.titleMedium)
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Start time selector
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("From", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        com.newton.couplespace.ui.components.TimePickerWheel(
                            value = start,
                            onValueChange = { start = it },
                            range = 0..23,
                            enabled = isEnabled,
                            format = { "%02d:00".format(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    // End time selector
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("To", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        com.newton.couplespace.ui.components.TimePickerWheel(
                            value = end,
                            onValueChange = { end = it },
                            range = 0..23,
                            enabled = isEnabled,
                            format = { "%02d:00".format(it) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(interval, start, end, isEnabled) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
