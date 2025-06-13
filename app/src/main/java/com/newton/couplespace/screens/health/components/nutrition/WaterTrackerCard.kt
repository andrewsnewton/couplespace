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
    modifier: Modifier = Modifier
) {
    var showGoalDialog by remember { mutableStateOf(false) }
    
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
                        onClick = onSendReminder,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Remind Partner",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    
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
                    onClick = { onAddWater(100) }
                )
                
                QuickAddButton(
                    amount = 200,
                    onClick = { onAddWater(200) }
                )
                
                QuickAddButton(
                    amount = 300,
                    onClick = { onAddWater(300) }
                )
                
                QuickAddButton(
                    amount = 500,
                    onClick = { onAddWater(500) }
                )
            }
        }
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
