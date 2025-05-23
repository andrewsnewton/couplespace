package com.newton.couplespace.screens.main.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material3.*
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.newton.couplespace.models.TimelineEvent
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

@Composable
fun AgendaView(
    startDate: LocalDate = LocalDate.now(),
    daysToShow: Int = 30,
    events: List<TimelineEvent> = emptyList(),
    onEventClick: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val days = remember(startDate, daysToShow) {
        (0 until daysToShow).map { startDate.plusDays(it.toLong()) }
    }
    
    val eventsByDay = remember(events, days) {
        days.associateWith { day ->
            // First filter out events with valid dates
            val validEvents = events.filter { event ->
                try {
                    val eventDate = event.startTime.toDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate()
                    eventDate.isEqual(day)
                } catch (e: Exception) {
                    false // Skip events with invalid dates
                }
            }
            
            // Then sort the valid events by start time
            validEvents.sortedBy { event -> 
                try {
                    event.startTime.toDate().time // Use milliseconds for sorting
                } catch (e: Exception) {
                    // Fallback to current time if there's an issue
                    System.currentTimeMillis()
                }
            }
        }
    }
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            days.forEach { day ->
                val dayEvents = eventsByDay[day] ?: emptyList()
                
                // Only show day header if there are events or it's today
                if (dayEvents.isNotEmpty() || day.isEqual(LocalDate.now())) {
                    item {
                        DayHeader(day = day, eventCount = dayEvents.size)
                    }
                    
                    if (dayEvents.isNotEmpty()) {
                        items(dayEvents) { event ->
                            AgendaEventItem(
                                event = event,
                                onClick = { onEventClick(event.id) }
                            )
                        }
                    } else {
                        item {
                            NoEventsPlaceholder()
                        }
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun DayHeader(
    day: LocalDate,
    eventCount: Int,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val isToday = day.isEqual(today)
    val dayFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d") }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Day circle with number
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = if (isToday) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = MaterialTheme.shapes.small
                )
                .border(
                    width = if (isToday) 0.dp else 1.dp,
                    color = if (isToday) Color.Transparent else MaterialTheme.colorScheme.outline,
                    shape = MaterialTheme.shapes.small
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (isToday) MaterialTheme.colorScheme.onPrimary 
                       else MaterialTheme.colorScheme.onSurface
            )
        }
        
        // Day of week and date
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f)
        ) {
            Text(
                text = day.format(dayFormatter),
                style = MaterialTheme.typography.titleSmall,
                color = if (isToday) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.onSurface
            )
            
            if (eventCount > 0) {
                Text(
                    text = "$eventCount ${if (eventCount == 1) "event" else "events"}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AgendaEventItem(
    event: TimelineEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    
    // Safely get start and end times with error handling
    val (startTime, endTime, timeRange) = try {
        val start = event.startTime.toDate().toInstant()
            .atZone(java.time.ZoneId.systemDefault()).toLocalTime()
        val end = event.endTime.toDate().toInstant()
            .atZone(java.time.ZoneId.systemDefault()).toLocalTime()
        Triple(
            start,
            end,
            "${start.format(timeFormatter)} - ${end.format(timeFormatter)}"
        )
    } catch (e: Exception) {
        // Fallback to default times if there's an issue
        val now = LocalTime.now()
        val later = now.plusHours(1)
        Triple(
            now,
            later,
            "Time not available"
        )
    }
    
    val eventColor = remember(event.id) {
        // Generate a stable color based on event ID or use category color
        val colors = listOf(
            Color(0xFF4285F4), // Blue
            Color(0xFFEA4335), // Red
            Color(0xFFFBBC05), // Yellow
            Color(0xFF34A853), // Green
            Color(0xFF673AB7)  // Purple
        )
        // Ensure we get a positive index by using Math.abs and making sure it's within bounds
        val colorIndex = Math.abs(event.id.hashCode()) % colors.size
        colors[colorIndex]
    }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = eventColor.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Time indicator line
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(
                        color = eventColor,
                        shape = MaterialTheme.shapes.extraSmall
                    )
            )
            
            // Event details
            Column(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                if (event.description.isNotBlank()) {
                    Text(
                        text = event.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
                
                Text(
                    text = timeRange,
                    style = MaterialTheme.typography.labelSmall,
                    color = eventColor,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun NoEventsPlaceholder() {
    Text(
        text = "No events scheduled",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}
