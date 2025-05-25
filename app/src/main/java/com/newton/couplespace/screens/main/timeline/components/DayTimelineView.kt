package com.newton.couplespace.screens.main.timeline.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import com.newton.couplespace.models.TimelineEvent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.max
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

// Pixels per minute for scaling (2dp per minute = 120dp per hour)
private const val PIXELS_PER_MINUTE = 2f

@Composable
fun DayTimelineView(
    date: LocalDate,
    events: List<TimelineEvent> = emptyList(),
    onEventClick: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // State for tracking drag gesture
    var isDragging by remember { mutableStateOf(false) }
    // State for tracking selected time and events
    var selectedTime by remember { mutableStateOf<LocalTime?>(null) }
    var showEventGroupDialog by remember { mutableStateOf(false) }
    var eventsForSelectedTime by remember { mutableStateOf<List<TimelineEvent>>(emptyList()) }
    
    // Process events for display
    val processedEvents = remember(events, date) {
        events.filter { event ->
            if (event.startTime == null || event.endTime == null) {
                return@filter false
            }
            
            try {
                // Convert to LocalDate for comparison
                val eventStartDate = event.startTime.toDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                val eventEndDate = event.endTime.toDate().toInstant()
                    .atZone(ZoneId.systemDefault()).toLocalDate()
                    
                // Check if event is on the selected date
                eventStartDate.isEqual(date) || 
                    (eventStartDate.isBefore(date) && eventEndDate.isAfter(date)) ||
                    eventEndDate.isEqual(date)
            } catch (e: Exception) {
                false
            }
        }
    }
    
    // Total minutes in a day for continuous timeline
    val totalMinutesInDay = 24 * 60
    
    // Use the constant for pixels per minute
    val pixelsPerMinute = PIXELS_PER_MINUTE
    
    // Total height of the timeline in dp
    val timelineHeight = (totalMinutesInDay * pixelsPerMinute).dp
    
    // Create a scrollState for the continuous timeline
    val scrollState = rememberScrollState()
    
    // Current time for the red needle
    val currentTime = LocalTime.now()
    val currentMinutesSinceMidnight = currentTime.hour * 60 + currentTime.minute
    
    // Scroll to current time on initial load (with offset to show context)
    LaunchedEffect(Unit) {
        val scrollPosition = ((currentMinutesSinceMidnight - 60) * pixelsPerMinute).toInt().coerceAtLeast(0)
        scrollState.scrollTo(scrollPosition)
    }
    
    // Main column for the timeline
    Column(modifier = modifier
        .fillMaxSize()
        .draggable(
            orientation = Orientation.Horizontal,
            state = rememberDraggableState { delta ->
                // Just track that we're dragging, actual navigation happens on drag end
                isDragging = true
            },
            onDragStarted = { isDragging = true },
            onDragStopped = { velocity ->
                if (isDragging) {
                    // Navigate based on velocity direction
                    if (velocity > 500) {
                        // Swiped right with enough velocity - go to previous day
                        onDateChange(date.minusDays(1))
                    } else if (velocity < -500) {
                        // Swiped left with enough velocity - go to next day
                        onDateChange(date.plusDays(1))
                    }
                    isDragging = false
                }
            }
        )
    ) {
        // Date display at the top
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Day number in square box
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(4.dp))
                    .border(0.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${date.dayOfMonth}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Day of week
            Text(
                text = date.dayOfWeek.toString().lowercase().replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        // Timeline content
        Box(modifier = Modifier.weight(1f)) {
            // Scrollable continuous timeline
            Row(modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
            ) {
                // Time markers column (left side)
                Box(
                    modifier = Modifier
                        .width(36.dp)  // Reduced from 40dp
                        .height(timelineHeight)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // Draw hour markers
                    for (hour in 0..23) {
                        val yPosition = (hour * 60 * PIXELS_PER_MINUTE).dp
                        
                        // Format hour display (show AM/PM only at specific hours)
                        val hourText = formatHour(hour)
                        
                        Text(
                            text = hourText,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .padding(end = 4.dp)
                                .align(Alignment.TopEnd)
                                .offset(y = yPosition)
                        )
                    }
                    
                    // Current time indicator in the hours column
                    val needlePosition = (currentMinutesSinceMidnight * pixelsPerMinute).dp
                    
                    // Only show the time indicator if it's for today
                    if (date.isEqual(LocalDate.now())) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .offset(y = needlePosition - 12.dp)
                                .zIndex(10f)
                                .background(MaterialTheme.colorScheme.surface)
                                .padding(end = 4.dp),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Text(
                                text = currentTime.format(DateTimeFormatter.ofPattern("h:mm")),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Red,
                                fontSize = 10.sp,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 2.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
                
                // Events column with continuous timeline (right side)
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(timelineHeight)
                        .background(MaterialTheme.colorScheme.surface)
                ) {
                    // Get colors outside of Canvas to avoid Composable invocation errors
                    val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
                    val density = LocalDensity.current.density
                    // Draw the timeline grid first (background)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val hourLineColor = Color.DarkGray  // Solid black for hour lines
                        val halfHourLineColor = outlineVariantColor.copy(alpha = 0.3f)
                        
                        // Draw hour separator lines
                        for (hour in 0..23) {
                            val yPositionDp = (hour * 60 * PIXELS_PER_MINUTE).dp
                            // Convert to pixels
                            val yPositionPx = yPositionDp.value * density
                            
                            // Hour line - solid black line across full width
                            drawLine(
                                color = hourLineColor,
                                start = Offset(0f, yPositionPx),
                                end = Offset(size.width, yPositionPx),
                                strokeWidth = 1.0f  // Slightly thicker for better visibility
                            )
                            
                            // Half-hour line - dashed line
                            val halfHourY = yPositionPx + (30 * PIXELS_PER_MINUTE * density)
                            drawLine(
                                color = halfHourLineColor.copy(alpha = 0.6f),  // Increased alpha for better visibility
                                start = Offset(0f, halfHourY),
                                end = Offset(size.width, halfHourY),
                                strokeWidth = 1.0f,  // Slightly thicker
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 5f), 0f)  // Add dash effect
                            )
                            
                            // We're removing the quarter-hour lines as they're causing visual clutter
                        }
                    }
                    
                    // Content Box for events
                    Box(modifier = Modifier.fillMaxSize().padding(horizontal = 2.dp))
                    
                    // Group events by time slot to handle overlapping events
                    val eventsByTimeSlot = mutableMapOf<Int, MutableList<TimelineEvent>>()
                    
                    // First pass: group events by their start time (rounded to nearest hour)
                    processedEvents.forEach { event ->
                        if (event.startTime == null || event.endTime == null) return@forEach
                        
                        val startDateTime = event.startTime.toDate().toInstant()
                            .atZone(ZoneId.systemDefault())
                        val startTime = startDateTime.toLocalTime()
                        val startHour = startTime.hour
                        
                        // Create or get the list for this hour
                        val eventsInSlot = eventsByTimeSlot.getOrPut(startHour) { mutableListOf() }
                        eventsInSlot.add(event)
                    }
                    
                    // Second pass: render events with proper positioning and width
                    eventsByTimeSlot.forEach { (_, eventsInSlot) ->
                        // Sort events by start time
                        val sortedEvents = eventsInSlot.sortedBy { 
                            it.startTime?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalTime() 
                        }
                        
                        // Find overlapping events
                        val overlappingSets = findOverlappingEvents(sortedEvents)
                        
                        // Render each set of overlapping events
                        overlappingSets.forEach { overlappingEvents ->
                            if (overlappingEvents.size == 1) {
                                // Single event - render with full width
                                renderSingleEvent(overlappingEvents.first(), onEventClick)
                            } else {
                                // Multiple overlapping events - render side by side
                                val maxEventsToShow = 3
                                val eventsToRender = overlappingEvents.take(maxEventsToShow)
                                val remainingCount = overlappingEvents.size - maxEventsToShow
                                
                                // Calculate the available width for each event
                                val screenWidth = LocalConfiguration.current.screenWidthDp - 40 // Subtract left sidebar width
                                val availableWidth = screenWidth - 8 // Subtract padding
                                
                                // Render visible events side by side
                                eventsToRender.forEachIndexed { index, event ->
                                    val eventWidth = availableWidth / eventsToRender.size
                                    val xOffset = index * eventWidth
                                    
                                    renderOverlappingEvent(event, eventWidth, xOffset, onEventClick)
                                }
                                
                                // If there are more events than we can show, display a +N indicator
                                if (remainingCount > 0) {
                                    val lastEvent = eventsToRender.last()
                                    val startDateTime = lastEvent.startTime?.toDate()?.toInstant()
                                        ?.atZone(ZoneId.systemDefault())
                                    val endDateTime = lastEvent.endTime?.toDate()?.toInstant()
                                        ?.atZone(ZoneId.systemDefault())
                                    
                                    if (startDateTime != null && endDateTime != null) {
                                        val startTime = startDateTime.toLocalTime()
                                        val startMinutesSinceMidnight = startTime.hour * 60 + startTime.minute
                                        val durationMinutes = ChronoUnit.MINUTES.between(
                                            startDateTime.toLocalDateTime(),
                                            endDateTime.toLocalDateTime()
                                        ).toInt()
                                        
                                        val topOffset = (startMinutesSinceMidnight * PIXELS_PER_MINUTE).dp
                                        val eventHeight = (durationMinutes * PIXELS_PER_MINUTE).dp.coerceAtLeast(30.dp)
                                        
                                        // Calculate position for +N indicator
                                        val eventWidth = availableWidth / eventsToRender.size
                                        val xOffset = eventsToRender.size * eventWidth
                                        
                                        // Render the +N indicator
                                        Box(
                                            modifier = Modifier
                                                .width(30.dp)
                                                .height(eventHeight)
                                                .offset(y = topOffset, x = xOffset.dp)
                                                .background(
                                                    color = Color.DarkGray.copy(alpha = 0.7f),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .padding(4.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "+$remainingCount",
                                                color = Color.White,
                                                style = MaterialTheme.typography.bodySmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    
                    // Current time indicator (red needle) - only show if it's today
                    if (date.isEqual(LocalDate.now())) {
                        val needlePosition = (currentMinutesSinceMidnight * pixelsPerMinute).dp
                        
                        // Red line with arrow (right side)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(24.dp)
                                .offset(y = needlePosition - 12.dp)
                                .zIndex(10f)
                          
                        ) {
                            // Right-pointing arrow that's part of the line
                            Box(
                                modifier = Modifier
                                    .size(8.dp, 8.dp)
                                    .offset(x = 0.dp, y = 8.dp)  // Position to align with the line and connect to time
                            ) {
                                Canvas(
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    val path = Path().apply {
                                        moveTo(0f, 0f)  // Top-left
                                        lineTo(size.width, size.height / 2)  // Middle-right
                                        lineTo(0f, size.height)  // Bottom-left
                                        close()
                                    }
                                    drawPath(
                                        path = path,
                                        color = Color.Red
                                    )
                                }
                            }
                            
                            // Red line that connects to the arrow
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(2.dp)
                                    .offset(y = 11.dp)  // Center vertically with the arrow
                                    .padding(start = 4.dp)  // Start after the arrow
                                    .background(Color.Red)
                            )
                        }
                    }
                }
            }
        }
        
        // Event group dialog
        if (showEventGroupDialog && selectedTime != null) {
            Dialog(onDismissRequest = { showEventGroupDialog = false }) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Events at ${selectedTime?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: ""}",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )
                        
                        eventsForSelectedTime.forEach { event ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { 
                                        onEventClick(event.id)
                                        showEventGroupDialog = false 
                                    },
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, Color(0xFF4285F4).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = event.title,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        
                                        if (event.location.isNotBlank()) {
                                            Text(
                                                text = event.location,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        
                                        // Show time range
                                        val startTime = event.startTime?.toDate()?.toInstant()
                                            ?.atZone(ZoneId.systemDefault())?.toLocalTime()
                                        val endTime = event.endTime?.toDate()?.toInstant()
                                            ?.atZone(ZoneId.systemDefault())?.toLocalTime()
                                        
                                        if (startTime != null && endTime != null) {
                                            Text(
                                                text = "${startTime.format(DateTimeFormatter.ofPattern("h:mm a"))} - ${endTime.format(DateTimeFormatter.ofPattern("h:mm a"))}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(
    event: TimelineEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    Surface(
        modifier = modifier
            .clickable { onClick() }
            .padding(horizontal = 2.dp, vertical = 2.dp),
        shape = RoundedCornerShape(4.dp),
        color = if (isLightTheme) Color(0xFFE3F2FD) else Color(0xFF0D47A1), // Adapt color for dark mode
        contentColor = if (isLightTheme) Color.Black else Color.White,
        border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(8.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            
            if (event.location.isNotBlank()) {
                Text(
                    text = event.location,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Helper function to format hour display
private fun formatHour(hour: Int): String {
    return when {
        hour == 1 -> "1\nAM"
        hour == 12 -> "12\nPM"
        hour == 0 -> "12"
        hour > 12 -> (hour - 12).toString()
        else -> hour.toString()
    }
}



// Helper function to render a single event with full width
@Composable
private fun BoxScope.renderSingleEvent(
    event: TimelineEvent,
    onEventClick: (String) -> Unit
) {
    if (event.startTime == null || event.endTime == null) return
    
    val startDateTime = event.startTime.toDate().toInstant()
        .atZone(ZoneId.systemDefault())
    val endDateTime = event.endTime.toDate().toInstant()
        .atZone(ZoneId.systemDefault())
    
    val startTime = startDateTime.toLocalTime()
    
    // Calculate minutes since midnight for positioning
    val startMinutesSinceMidnight = startTime.hour * 60 + startTime.minute
    
    // Calculate duration in minutes
    val durationMinutes = ChronoUnit.MINUTES.between(
        startDateTime.toLocalDateTime(),
        endDateTime.toLocalDateTime()
    ).toInt()
    
    // Calculate exact position and height
    val topOffset = (startMinutesSinceMidnight * PIXELS_PER_MINUTE).dp
    val eventHeight = (durationMinutes * PIXELS_PER_MINUTE).dp.coerceAtLeast(30.dp)
    
    // Display the event card with full width
    EventCard(
        event = event,
        onClick = { onEventClick(event.id) },
        modifier = Modifier
            .fillMaxWidth()
            .height(eventHeight)
            .offset(y = topOffset)
    )
}

// Helper function to render an overlapping event with specific width and position
@Composable
private fun BoxScope.renderOverlappingEvent(
    event: TimelineEvent,
    widthDp: Int,
    xOffsetDp: Int,
    onEventClick: (String) -> Unit
) {
    if (event.startTime == null || event.endTime == null) return
    
    val startDateTime = event.startTime.toDate().toInstant()
        .atZone(ZoneId.systemDefault())
    val endDateTime = event.endTime.toDate().toInstant()
        .atZone(ZoneId.systemDefault())
    
    val startTime = startDateTime.toLocalTime()
    
    // Calculate minutes since midnight for positioning
    val startMinutesSinceMidnight = startTime.hour * 60 + startTime.minute
    
    // Calculate duration in minutes
    val durationMinutes = ChronoUnit.MINUTES.between(
        startDateTime.toLocalDateTime(),
        endDateTime.toLocalDateTime()
    ).toInt()
    
    // Calculate exact position and height
    val topOffset = (startMinutesSinceMidnight * PIXELS_PER_MINUTE).dp
    val eventHeight = (durationMinutes * PIXELS_PER_MINUTE).dp.coerceAtLeast(30.dp)
    
    // Display the event card with specific width and position
    EventCard(
        event = event,
        onClick = { onEventClick(event.id) },
        modifier = Modifier
            .width(widthDp.dp)
            .height(eventHeight)
            .offset(y = topOffset, x = xOffsetDp.dp)
    )
}

// Helper function to find sets of overlapping events
private fun findOverlappingEvents(events: List<TimelineEvent>): List<List<TimelineEvent>> {
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
        val previousEvent = sortedEvents[i-1]
        
        val currentStartTime = currentEvent.startTime?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())
        val previousEndTime = previousEvent.endTime?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())
        
        if (currentStartTime != null && previousEndTime != null && 
            !currentStartTime.isAfter(previousEndTime)) {
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
