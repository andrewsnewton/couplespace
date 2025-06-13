package com.newton.couplespace.screens.health.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A card that displays a summary of key health metrics
 */
@Composable
fun HealthSummaryCard(
    steps: Int,
    calories: Int,
    activeMinutes: Int,
    modifier: Modifier = Modifier,
    stepsGoal: Int = 10000,
    caloriesGoal: Int = 500,
    activeMinutesGoal: Int = 30
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Today's Activity",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Steps counter
                MetricCounter(
                    icon = Icons.Default.DirectionsWalk,
                    iconTint = Color(0xFF4CAF50),
                    value = steps,
                    goal = stepsGoal,
                    label = "Steps",
                    modifier = Modifier.weight(1f)
                )
                
                // Calories counter
                MetricCounter(
                    icon = Icons.Default.LocalFireDepartment,
                    iconTint = Color(0xFFF44336),
                    value = calories,
                    goal = caloriesGoal,
                    label = "Calories",
                    modifier = Modifier.weight(1f)
                )
                
                // Active minutes counter
                MetricCounter(
                    icon = Icons.Default.Timer,
                    iconTint = Color(0xFFFF9800),
                    value = activeMinutes,
                    goal = activeMinutesGoal,
                    label = "Active Mins",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

/**
 * A component that displays a metric with an icon, value, and progress
 */
@Composable
fun MetricCounter(
    icon: ImageVector,
    iconTint: Color,
    value: Int,
    goal: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(horizontal = 4.dp)
    ) {
        // Icon with background
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(iconTint.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Value
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        
        // Label
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Progress bar
        val progress = (value.toFloat() / goal).coerceIn(0f, 1f)
        LinearProgressIndicator(
            progress = progress,
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp)),
            color = iconTint,
            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.1f)
        )
        
        // Goal percentage
        Text(
            text = "${(progress * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
    }
}
