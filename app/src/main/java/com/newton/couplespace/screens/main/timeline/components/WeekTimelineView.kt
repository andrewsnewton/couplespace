package com.newton.couplespace.screens.main.timeline.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.newton.couplespace.models.TimelineEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters

@Composable
fun WeekTimelineView(
    startDate: LocalDate = LocalDate.now(),
    events: List<TimelineEvent> = emptyList(),
    onEventClick: (String) -> Unit = {},
    onDateChange: (LocalDate) -> Unit = {}, // Added callback for date changes
    modifier: Modifier = Modifier
) {
    // State for tracking drag gesture
    var isDragging by remember { mutableStateOf(false) }
    val daysOfWeek = remember(startDate) { getDaysOfWeek(startDate) }
    val timeSlots = remember { generateTimeSlots() }
    
    // Current time for the time needle
    val currentTime = LocalTime.now()
    
    // Calculate current time position for the time needle
    val currentTimePosition = remember(currentTime) {
        val minutesSinceMidnight = currentTime.hour * 60 + currentTime.minute
        val totalMinutesInDay = 24 * 60
        minutesSinceMidnight.toFloat() / totalMinutesInDay
    }
    
    // Process events for each day
    val processedEventsByDay = remember(events, daysOfWeek) {
        daysOfWeek.associateWith { day ->
            processEventsForDay(events, day)
        }
    }
    
    // Track which day has a dialog open
    var selectedTimeSlotDay by remember { mutableStateOf<Pair<LocalTime, LocalDate>?>(null) }
    
    Surface(
        modifier = modifier
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
                            // Swiped right with enough velocity - go to previous week
                            onDateChange(startDate.minusWeeks(1))
                        } else if (velocity < -500) {
                            // Swiped left with enough velocity - go to next week
                            onDateChange(startDate.plusWeeks(1))
                        }
                        isDragging = false
                    }
                }
            ),
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Days header
            DaysHeader(daysOfWeek, startDate)
            
            // Time slots and events
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                // Get colors outside of Canvas to avoid Composable invocation errors
                val outlineVariantColor = MaterialTheme.colorScheme.outlineVariant
                
                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(timeSlots) { index, timeSlot ->
                        // Add a continuous timeline feel with grid lines
                        Box {
                            // Draw grid line for this time slot
                            Canvas(modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                            ) {
                                // Hour line or half-hour line based on time
                                val lineColor = if (timeSlot.minute == 0) {
                                    outlineVariantColor.copy(alpha = 0.5f) // Hour line
                                } else {
                                    outlineVariantColor.copy(alpha = 0.3f) // Half-hour line
                                }
                                
                                drawLine(
                                    color = lineColor,
                                    start = Offset(-size.width, 0f),
                                    end = Offset(size.width * 2, 0f),
                                    strokeWidth = if (timeSlot.minute == 0) 1f else 0.5f
                                )
                            }
                            
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                                    .border(
                                        width = 0.5.dp,
                                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                                    )
                            ) {
                                // Time column
                                val isHourMark = timeSlot.minute == 0
                                Box(
                                    modifier = Modifier
                                        .width(60.dp)
                                        .fillMaxHeight()
                                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isHourMark) {
                                        val timeFormatter = DateTimeFormatter.ofPattern("h a")
                                        Text(
                                            text = timeSlot.format(timeFormatter),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                
                                // Day columns
                                daysOfWeek.forEach { day ->
                                    val isToday = day.isEqual(LocalDate.now())
                                    val slotEvents = processedEventsByDay[day]?.get(timeSlot) ?: emptyList()
                                    
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .fillMaxHeight()
                                            .let {
                                                if (isToday) it.background(MaterialTheme.colorScheme.primary.copy(alpha = 0.05f))
                                                else it
                                            }
                                    ) {
                                        // Render events for this time slot and day
                                        if (slotEvents.isNotEmpty()) {
                                            if (slotEvents.size == 1) {
                                                // Single event card
                                                val event = slotEvents.first()
                                                EventCard(
                                                    event = event,
                                                    onClick = { onEventClick(event.id) }
                                                )
                                            } else {
                                                // Multiple events indicator
                                                Surface(
                                                    onClick = { selectedTimeSlotDay = Pair(timeSlot, day) },
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(1.dp),
                                                    shape = RoundedCornerShape(4.dp),
                                                    color = Color(0xFFF8BBD0), // Light pink color
                                                    tonalElevation = 1.dp,
                                                    shadowElevation = 1.dp
                                                ) {
                                                    Row(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(8.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        Text(
                                                            text = "${slotEvents.size} events",
                                                            style = MaterialTheme.typography.bodyMedium,
                                                            color = MaterialTheme.colorScheme.onSurface,
                                                            modifier = Modifier.weight(1f)
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
                
                // Current time indicator (red needle)
                val currentTime = LocalTime.now()
                val currentMinutesSinceMidnight = currentTime.hour * 60 + currentTime.minute
                
                // Calculate position for the current time indicator
                val pixelsPerMinute = 2f
                val needlePosition = (currentMinutesSinceMidnight * pixelsPerMinute).dp
                
                // Draw the current time indicator (red needle) above everything else
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Draw the current time indicator line
                    drawLine(
                        color = Color.Red,
                        start = Offset(0f, needlePosition.toPx()),
                        end = Offset(size.width, needlePosition.toPx()),
                        strokeWidth = 2f
                    )
                }
                
                // Current time text label
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = needlePosition - 10.dp)
                ) {
                    Text(
                        text = currentTime.format(DateTimeFormatter.ofPattern("h:mm a")),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Red,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                }
            }
        }
    }
    
    // Show dialog for grouped events if needed
    selectedTimeSlotDay?.let { (timeSlot, day) ->
        val eventsForSelectedTime = processedEventsByDay[day]?.get(timeSlot) ?: emptyList()
        
        if (eventsForSelectedTime.isNotEmpty()) {
            Dialog(
                onDismissRequest = { selectedTimeSlotDay = null },
                properties = DialogProperties(dismissOnClickOutside = true, usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .padding(16.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 4.dp,
                    shadowElevation = 8.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // Dialog header
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${day.month.name} ${day.dayOfMonth}, ${timeSlot.format(DateTimeFormatter.ofPattern("h:mm a"))}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            
                            IconButton(onClick = { selectedTimeSlotDay = null }) {
                                Icon(Icons.Default.Close, contentDescription = "Close")
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Events list
                        LazyColumn {
                            items(eventsForSelectedTime) { event ->
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .clickable { onEventClick(event.id) },
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface,
                                    ),
                                    elevation = CardDefaults.cardElevation(
                                        defaultElevation = 2.dp
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    border = BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.3f))
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Text(
                                            text = event.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        
                                        Spacer(modifier = Modifier.height(4.dp))
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = "Time",
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            
                                            Spacer(modifier = Modifier.width(4.dp))
                                            
                                            val startTime = event.startTime.toDate().toInstant()
                                                .atZone(ZoneId.systemDefault()).toLocalTime()
                                            val endTime = event.endTime.toDate().toInstant()
                                                .atZone(ZoneId.systemDefault()).toLocalTime()
                                            
                                            Text(
                                                text = "${startTime.format(DateTimeFormatter.ofPattern("h:mm a"))} - ${endTime.format(DateTimeFormatter.ofPattern("h:mm a"))}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        
                                        if (event.location.isNotBlank()) {
                                            Spacer(modifier = Modifier.height(4.dp))
                                            
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = Icons.Default.LocationOn,
                                                    contentDescription = "Location",
                                                    tint = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                
                                                Spacer(modifier = Modifier.width(4.dp))
                                                
                                                Text(
                                                    text = event.location,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        val timeSlotStart = timeSlot
        val timeSlotEnd = timeSlot.plusMinutes(30)
        
        val timeSlotEvents = events.filter { event ->
            val eventDate = event.startTime.toDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate()
            val eventTime = event.startTime.toDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalTime()
            val eventEndTime = event.endTime.toDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalTime()
            
            eventDate.isEqual(day) && 
            ((eventTime.isBefore(timeSlotEnd) || eventTime.equals(timeSlotEnd)) && 
            (eventEndTime.isAfter(timeSlotStart) || eventEndTime.equals(timeSlotStart)))
        }
        
        if (timeSlotEvents.isNotEmpty()) {
            TimeSlotEventsDialog(
                timeSlot = timeSlot,
                day = day,
                events = timeSlotEvents,
                onDismiss = { selectedTimeSlotDay = null },
                onEventClick = { eventId ->
                    onEventClick(eventId)
                    selectedTimeSlotDay = null
                }
            )
        }
    }
}

@Composable
private fun DaysHeader(
    days: List<LocalDate>,
    selectedDate: LocalDate
) {
    val dayFormatter = remember { DateTimeFormatter.ofPattern("E") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d") }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val today = LocalDate.now()
    
    // Get first and last day of the displayed week
    val firstDay = days.firstOrNull() ?: today
    val lastDay = days.lastOrNull() ?: today
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        // Week range header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${firstDay.format(monthFormatter)} - ${lastDay.format(monthFormatter)}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        // Days row
        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            // Time column header
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .height(48.dp)
            )
            
            // Day headers
            days.forEach { date ->
                val isToday = date.isEqual(today)
                val isSelected = date.isEqual(selectedDate)
                
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .let { if (isSelected) it.background(MaterialTheme.colorScheme.primary) else it },
                    color = Color.Transparent,
                    onClick = { /* Handle day selection */ }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = date.format(dayFormatter),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                                   else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = date.format(dateFormatter),
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                   else if (isToday) MaterialTheme.colorScheme.primary
                                   else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeekTimeSlotRow(
    timeSlot: LocalTime,
    days: List<LocalDate>,
    eventsByDay: Map<LocalDate, Map<LocalTime, List<TimelineEvent>>>,
    onEventClick: (String) -> Unit,
    onGroupClick: (LocalDate, List<TimelineEvent>) -> Unit
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h") }
    val isHourMark = timeSlot.minute == 0
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)

    ) {
        // Time column
        Box(
            modifier = Modifier
                .width(60.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopEnd
        ) {
            if (isHourMark) {
                Text(
                    text = timeSlot.format(timeFormatter),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(end = 8.dp, top = 4.dp)
                )
            }
        }
        
        // Day columns
        days.forEach { day ->
            val slotEvents = eventsByDay[day]?.get(timeSlot) ?: emptyList()
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .border(
                        width = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                    .let { if (isCurrentTime(timeSlot, day)) it.background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ) else it },
                contentAlignment = Alignment.TopStart
            ) {
                if (slotEvents.isNotEmpty()) {
                    if (slotEvents.size == 1) {
                        // Single event card
                        val event = slotEvents.first()
                        EventCard(
                            event = event,
                            onClick = { onEventClick(event.id) }
                        )
                    } else {
                        // Multiple events - show grouped card
                        GroupedEventsCard(
                            events = slotEvents,
                            onClick = { onGroupClick(day, slotEvents) }
                        )
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
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 2.dp, vertical = 2.dp),
        shape = RoundedCornerShape(4.dp),
        color = if (isLightTheme) Color(0xFFE3F2FD) else Color(0xFF0D47A1), // Adapt color for dark mode
        contentColor = if (isLightTheme) Color.Black else Color.White
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Event details
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                if (event.location.isNotBlank()) {
                    Text(
                        text = event.location,
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

@Composable
private fun GroupedEventsCard(
    events: List<TimelineEvent>,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .padding(1.dp),
        shape = RoundedCornerShape(4.dp),
        color = Color(0xFFF8BBD0), // Light pink color like in reference
        tonalElevation = 1.dp,
        shadowElevation = 1.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(24.dp),
                shape = CircleShape,
                color = Color(0xFFE91E63) // Pink color like in reference
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "${events.size}",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = "${events.size} events at this time",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More options",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TimeSlotEventsDialog(
    timeSlot: LocalTime,
    day: LocalDate,
    events: List<TimelineEvent>,
    onDismiss: () -> Unit,
    onEventClick: (String) -> Unit
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d") }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnClickOutside = true, usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.8f),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = "${day.format(dateFormatter)} at ${timeSlot.format(timeFormatter)}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            
                            Text(
                                text = "${events.size} events",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                        
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.align(Alignment.TopEnd)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                // Events list
                if (events.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No events for this time slot",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(events.sortedBy { it.startTime.seconds }) { event ->
                            val startTimeStr = remember(event) {
                                event.startTime.toDate().toInstant()
                                    .atZone(ZoneId.systemDefault()).toLocalTime()
                                    .format(timeFormatter)
                            }
                            val endTimeStr = remember(event) {
                                event.endTime.toDate().toInstant()
                                    .atZone(ZoneId.systemDefault()).toLocalTime()
                                    .format(timeFormatter)
                            }
                            
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { onEventClick(event.id) },
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface,
                                ),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 2.dp
                                ),
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, Color(0xFF2196F3).copy(alpha = 0.3f))
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Time indicator
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(Color(0xFF2196F3).copy(alpha = 0.1f), CircleShape)
                                            .border(1.dp, Color(0xFF2196F3).copy(alpha = 0.3f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = startTimeStr.split(":").first(),
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2196F3)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = event.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.AccessTime,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            
                                            Spacer(modifier = Modifier.width(4.dp))
                                            
                                            Text(
                                                text = "$startTimeStr - $endTimeStr",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        
                                        if (event.location.isNotBlank()) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier.padding(top = 4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.LocationOn,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                
                                                Spacer(modifier = Modifier.width(4.dp))
                                                
                                                Text(
                                                    text = event.location,
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
}

private fun processEventsForDay(events: List<TimelineEvent>, day: LocalDate): Map<LocalTime, List<TimelineEvent>> {
    val timeSlotMap = mutableMapOf<LocalTime, MutableList<TimelineEvent>>()
    
    // Filter events for this day
    val dayEvents = events.filter { event ->
        // Handle potential null values safely
        if (event.startTime == null || event.endTime == null) return@filter false
        
        try {
            val eventDate = event.startTime.toDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate()
            eventDate.isEqual(day)
        } catch (e: Exception) {
            false // Skip events that cause exceptions
        }
    }
    
    // Group events by time slot
    dayEvents.forEach { event ->
        try {
            val startTime = event.startTime.toDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalTime()
            val endTime = event.endTime.toDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalTime()
            
            // Find all 30-minute slots this event spans
            val slots = generateTimeSlots().filter { slot ->
                val slotEnd = slot.plusMinutes(30)
                (startTime.isBefore(slotEnd) || startTime.equals(slotEnd)) && 
                (endTime.isAfter(slot) || endTime.equals(slot))
            }
            
            // Add event to each slot it spans
            slots.forEach { slot ->
                if (!timeSlotMap.containsKey(slot)) {
                    timeSlotMap[slot] = mutableListOf()
                }
                timeSlotMap[slot]?.add(event)
            }
        } catch (e: Exception) {
            // Skip events that cause exceptions
        }
    }
    
    return timeSlotMap
}

private fun getDaysOfWeek(selectedDate: LocalDate): List<LocalDate> {
    val firstDayOfWeek = selectedDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY))
    return (0 until 7).map { firstDayOfWeek.plusDays(it.toLong()) }
}

private fun generateTimeSlots(): List<LocalTime> {
    return (0 until 48).map { // 48 slots = 24 hours * 2 (30 min intervals)
        val hour = it / 2
        val minute = (it % 2) * 30
        LocalTime.of(hour, minute)
    }
}

private fun isCurrentTime(time: LocalTime, day: LocalDate): Boolean {
    val now = LocalDate.now()
    return day.isEqual(now) && 
           time.hour == LocalTime.now().hour && 
           time.minute / 30 == LocalTime.now().minute / 30
}
