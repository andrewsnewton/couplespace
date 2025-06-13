package com.newton.couplespace.screens.health.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.newton.couplespace.screens.health.data.models.*
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A timeline view that displays health activities chronologically
 */
@Composable
fun ActivityTimeline(
    healthMetrics: List<HealthMetric>,
    onMetricClick: (HealthMetric) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Activity Timeline",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            if (healthMetrics.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No activities recorded for this day",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                // Sort metrics by timestamp
                val sortedMetrics = healthMetrics.sortedByDescending { it.timestamp }
                
                // Use a regular Column instead of LazyColumn to avoid nested scrolling issues
                Column(modifier = Modifier.heightIn(max = 300.dp)) {
                    sortedMetrics.forEach { metric ->
                        TimelineItem(
                            healthMetric = metric,
                            onClick = { onMetricClick(metric) }
                        )
                    }
                }
            }
        }
    }
}

/**
 * A single timeline item representing a health metric
 */
@Composable
fun TimelineItem(
    healthMetric: HealthMetric,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Format time
    val time = healthMetric.timestamp
        .atZone(ZoneId.systemDefault())
        .toLocalTime()
        .format(DateTimeFormatter.ofPattern("h:mm a"))
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Time
        Text(
            text = time,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(64.dp)
        )
        
        // Vertical line and icon
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(
                        color = getColorForMetricType(healthMetric.type),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getIconForMetricType(healthMetric.type),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        // Metric details
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
        ) {
            // Title
            Text(
                text = getTitleForMetric(healthMetric),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            // Description
            Text(
                text = getDescriptionForMetric(healthMetric),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Shared indicator if applicable
        if (healthMetric.isShared) {
            Icon(
                imageVector = Icons.Default.Share,
                contentDescription = "Shared with partner",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/**
 * Gets the appropriate icon for a metric type
 */
@Composable
fun getIconForMetricType(type: HealthMetricType): ImageVector {
    return when (type) {
        HealthMetricType.STEPS -> Icons.Default.DirectionsWalk
        HealthMetricType.DISTANCE -> Icons.Default.Timeline
        HealthMetricType.ACTIVE_MINUTES -> Icons.Default.Timer
        HealthMetricType.HEART_RATE -> Icons.Default.Favorite
        HealthMetricType.SLEEP -> Icons.Default.Bedtime
        HealthMetricType.WEIGHT -> Icons.Default.ControlPoint
        HealthMetricType.CALORIES_BURNED -> Icons.Default.LocalFireDepartment
        HealthMetricType.WATER_INTAKE -> Icons.Default.WaterDrop
        HealthMetricType.MEAL -> Icons.Default.Restaurant
    }
}

/**
 * Gets the appropriate color for a metric type
 */
@Composable
fun getColorForMetricType(type: HealthMetricType): Color {
    return when (type) {
        HealthMetricType.STEPS -> Color(0xFF4CAF50)  // Green
        HealthMetricType.DISTANCE -> Color(0xFF2196F3)  // Blue
        HealthMetricType.ACTIVE_MINUTES -> Color(0xFFFF9800)  // Orange
        HealthMetricType.HEART_RATE -> Color(0xFFE91E63)  // Pink
        HealthMetricType.SLEEP -> Color(0xFF673AB7)  // Purple
        HealthMetricType.WEIGHT -> Color(0xFF607D8B)  // Blue Grey
        HealthMetricType.CALORIES_BURNED -> Color(0xFFF44336)  // Red
        HealthMetricType.WATER_INTAKE -> Color(0xFF03A9F4)  // Light Blue
        HealthMetricType.MEAL -> Color(0xFF795548)  // Brown
    }
}

/**
 * Gets a title for a health metric
 */
fun getTitleForMetric(metric: HealthMetric): String {
    return when (metric) {
        is StepsMetric -> "Steps"
        is DistanceMetric -> "Distance"
        is ActiveMinutesMetric -> "Activity"
        is HeartRateMetric -> "Heart Rate"
        is SleepMetric -> "Sleep"
        is WeightMetric -> "Weight"
        is CaloriesBurnedMetric -> "Calories Burned"
        is WaterIntakeMetric -> "Water Intake"
        else -> "Health Activity"
    }
}

/**
 * Gets a description for a health metric
 */
fun getDescriptionForMetric(metric: HealthMetric): String {
    return when (metric) {
        is StepsMetric -> "${metric.count} steps"
        is DistanceMetric -> "${String.format("%.2f", metric.distanceMeters / 1000)} km"
        is ActiveMinutesMetric -> "${metric.minutes} active minutes (${metric.intensity.name.lowercase()})"
        is HeartRateMetric -> "${metric.beatsPerMinute} bpm"
        is SleepMetric -> "${metric.durationHours.toInt()} hrs ${((metric.durationHours - metric.durationHours.toInt()) * 60).toInt()} mins"
        is WeightMetric -> "${String.format("%.1f", metric.weightKg)} kg"
        is CaloriesBurnedMetric -> "${metric.calories} calories" + (metric.activity?.let { " from $it" } ?: "")
        is WaterIntakeMetric -> "${metric.amount} ml of water"
        else -> "Health activity recorded"
    }
}
