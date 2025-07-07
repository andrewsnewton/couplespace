package com.newton.couplespace.screens.main.timeline.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newton.couplespace.models.TimelineEvent
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.TimeZone

// Pixels per minute for scaling (2dp per minute = 120dp per hour)
const val PIXELS_PER_MINUTE = 2f

/**
 * Contains utility functions and composables for working with timeline events
 * with Material 3 expressive design elements
 */
object TimelineUtils {
    
    /**
     * Formats an hour for display in the timeline with AM/PM
     */
    fun formatHour(hour: Int): String {
        return when {
            hour == 0 -> "12\nAM"
            hour == 12 -> "12\nPM"
            hour < 12 -> "$hour\nAM"
            else -> "${hour - 12}\nPM"
        }
    }
    
    /**
     * Finds sets of overlapping events in a list of events
     */
    fun findOverlappingEvents(events: List<TimelineEvent>): List<List<TimelineEvent>> {
        if (events.isEmpty()) return emptyList()

        val result = mutableListOf<MutableList<TimelineEvent>>()
        var currentGroup = mutableListOf<TimelineEvent>()

        // Sort events by start time
        val sortedEvents = events.sortedBy {
            it.startTime?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalTime()
        }

        // Initialize with the first event
        currentGroup.add(sortedEvents.first())

        // Group overlapping events
        for (i in 1 until sortedEvents.size) {
            val currentEvent = sortedEvents[i]
            val previousEvent = sortedEvents[i - 1]

            val currentStartTime =
                currentEvent.startTime?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())
            val previousEndTime =
                previousEvent.endTime?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())

            if (currentStartTime != null && previousEndTime != null &&
                !currentStartTime.isAfter(previousEndTime)
            ) {
                // Events overlap, add to current group
                currentGroup.add(currentEvent)
            } else {
                // No overlap, start a new group
                result.add(currentGroup)
                currentGroup = mutableListOf(currentEvent)
            }
        }

        // Add the last group
        if (currentGroup.isNotEmpty()) {
            result.add(currentGroup)
        }

        return result
    }

    /**
     * Calculates the minutes since midnight for a given event
     */
    fun getEventStartMinutes(event: TimelineEvent): Int? {
        val startDateTime = event.startTime?.toDate()?.toInstant()
            ?.atZone(ZoneId.systemDefault()) ?: return null
        
        val startTime = startDateTime.toLocalTime()
        return startTime.hour * 60 + startTime.minute
    }
    
    /**
     * Calculates the duration in minutes for a given event
     */
    fun getEventDurationMinutes(event: TimelineEvent): Int? {
        val startDateTime = event.startTime?.toDate()?.toInstant()
            ?.atZone(ZoneId.systemDefault()) ?: return null
        val endDateTime = event.endTime?.toDate()?.toInstant()
            ?.atZone(ZoneId.systemDefault()) ?: return null
            
        return ChronoUnit.MINUTES.between(startDateTime, endDateTime).toInt()
    }
    
    /**
     * Returns a human-readable formatted time range string
     */
    fun formatTimeRange(startTime: LocalTime, endTime: LocalTime): String {
        val formatter = DateTimeFormatter.ofPattern("h:mm a")
        return "${formatter.format(startTime)} - ${formatter.format(endTime)}"
    }
    
    /**
     * Creates a color gradient between two tone values based on Material 3 color system
     */
    fun createTonalPalette(baseColor: Color, count: Int): List<Color> {
        // Create a palette by adjusting alpha values
        return List(count) { index ->
            val fraction = index.toFloat() / (count - 1)
            // Blend from 20% to 90% opacity based on index
            val alpha = 0.2f + (fraction * 0.7f)
            baseColor.copy(alpha = alpha)
        }
    }
    
    /**
     * A Material 3 styled time marker for the timeline
     */
    @Composable
    fun TimeMarker(
        hour: Int,
        isCurrentHour: Boolean = false,
        modifier: Modifier = Modifier
    ) {
        val markerColor = if (isCurrentHour) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.outlineVariant
        }
        
        val textStyle = if (isCurrentHour) {
            MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.primary)
        } else {
            MaterialTheme.typography.labelMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        
        val animatedWeight by animateFloatAsState(
            targetValue = if (isCurrentHour) 1.2f else 1f,
            label = "marker weight"
        )
        
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
        ) {
            Box(
                modifier = Modifier
                    .size(width = (1.dp * animatedWeight), height = 4.dp)
                    .clip(MaterialTheme.shapes.extraSmall)
                    .background(markerColor)
            )
            
            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = formatHour(hour),
                style = textStyle,
                textAlign = TextAlign.Center,
                fontSize = (11.sp * animatedWeight)
            )
        }
    }
}
