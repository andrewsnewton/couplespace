package com.newton.couplespace.screens.main.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.newton.couplespace.models.TimelineEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.*

@Composable
fun MonthCalendarView(
    selectedDate: LocalDate = LocalDate.now(),
    events: List<TimelineEvent> = emptyList(),
    onDateSelected: (LocalDate) -> Unit = {},
    onEventClick: (String) -> Unit = {},
    onMonthChange: (LocalDate) -> Unit = {}, // Added callback for month changes
    modifier: Modifier = Modifier
) {
    // State for tracking drag gesture
    var isDragging by remember { mutableStateOf(false) }
    // State for day detail dialog
    var showDayDetailDialog by remember { mutableStateOf(false) }
    var selectedDayForDialog by remember { mutableStateOf<LocalDate?>(null) }
    val yearMonth = YearMonth.from(selectedDate)
    val daysInMonth = yearMonth.lengthOfMonth()
    val firstDayOfMonth = yearMonth.atDay(1)
    val lastDayOfMonth = yearMonth.atEndOfMonth()
    
    // Get the first day to show (previous month's days to fill the first week)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek
    val daysFromPrevMonth = when (firstDayOfWeek) {
        DayOfWeek.SUNDAY -> 0
        else -> firstDayOfWeek.value - 1
    }
    
    // Get the last day to show (next month's days to fill the last week)
    val lastDayOfWeek = lastDayOfMonth.dayOfWeek
    val daysFromNextMonth = when (lastDayOfWeek) {
        DayOfWeek.SATURDAY -> 0
        else -> 6 - lastDayOfWeek.value
    }
    
    val days = remember(selectedDate) {
        val daysList = mutableListOf<CalendarDay>()
        
        // Previous month
        val prevMonth = yearMonth.minusMonths(1)
        val prevMonthDays = prevMonth.lengthOfMonth()
        // Fix: Ensure we're not trying to access negative indices
        if (daysFromPrevMonth > 0) {
            for (i in daysFromPrevMonth - 1 downTo 0) {
                val date = prevMonth.atDay(prevMonthDays - i)
                daysList.add(CalendarDay(date, isCurrentMonth = false))
            }
        }
        
        // Current month
        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val dayEvents = events.filter { event ->
                val eventDate = event.startTime.toDate().toInstant()
                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate()
                eventDate.isEqual(date)
            }
            daysList.add(CalendarDay(date, isCurrentMonth = true, events = dayEvents))
        }
        
        // Next month
        val nextMonth = yearMonth.plusMonths(1)
        for (day in 1..daysFromNextMonth) {
            val date = nextMonth.atDay(day)
            daysList.add(CalendarDay(date, isCurrentMonth = false))
        }
        
        daysList
    }
    
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
                            // Swiped right with enough velocity - go to previous month
                            onMonthChange(selectedDate.minusMonths(1))
                        } else if (velocity < -500) {
                            // Swiped left with enough velocity - go to next month
                            onMonthChange(selectedDate.plusMonths(1))
                        }
                        isDragging = false
                    }
                }
            ),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Month header
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(
                        text = yearMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " " + yearMonth.year,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Days of week header
                    Row(modifier = Modifier.fillMaxWidth()) {
                        val daysOfWeek = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
                        daysOfWeek.forEach { day ->
                            Text(
                                text = day,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(vertical = 4.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            // Calendar grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(7),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(4.dp)
            ) {
                items(days) { day ->
                    DayCell(
                        day = day,
                        isSelected = day.date.isEqual(selectedDate),
                        onDayClick = { 
                            onDateSelected(day.date)
                            selectedDayForDialog = day.date
                            showDayDetailDialog = true
                        },
                        onEventClick = onEventClick,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(2.dp)
                    )
                }
            }
        }
    }
    
    // Show day detail dialog if a day is selected
    if (showDayDetailDialog && selectedDayForDialog != null) {
        val dayEvents = events.filter { event ->
            val eventDate = event.startTime.toDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate()
            eventDate.isEqual(selectedDayForDialog)
        }
        
        DayDetailDialog(
            date = selectedDayForDialog!!,
            events = dayEvents,
            onDismiss = { showDayDetailDialog = false },
            onEventClick = { eventId ->
                onEventClick(eventId)
                showDayDetailDialog = false
            }
        )
    }
}

@Composable
private fun DayDetailDialog(
    date: LocalDate,
    events: List<TimelineEvent>,
    onDismiss: () -> Unit,
    onEventClick: (String) -> Unit
) {
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
                                text = date.format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy")),
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
                            text = "No events for this day",
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
                            DayEventItem(
                                event = event,
                                onClick = { onEventClick(event.id) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayEventItem(
    event: TimelineEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eventColor = remember(event.id) {
        val colors = listOf(
            Color(0xFF4285F4), // Blue
            Color(0xFFEA4335), // Red
            Color(0xFFFBBC05), // Yellow
            Color(0xFF34A853), // Green
            Color(0xFF673AB7)  // Purple
        )
        val safeId = if (event.id.isNullOrEmpty()) "default" else event.id
        val index = Math.abs(safeId.hashCode()) % colors.size
        colors[index]
    }
    
    val startTime = remember(event) {
        event.startTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
            .format(DateTimeFormatter.ofPattern("h:mm a"))
    }
    
    val endTime = remember(event) {
        event.endTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalTime()
            .format(DateTimeFormatter.ofPattern("h:mm a"))
    }
    
    Card(
        modifier = modifier
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        ),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, eventColor.copy(alpha = 0.3f))
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
                    .background(eventColor.copy(alpha = 0.1f), CircleShape)
                    .border(1.dp, eventColor.copy(alpha = 0.3f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = startTime.split(":").first(),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = eventColor
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
                        text = "$startTime - $endTime",
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

@Composable
private fun DayCell(
    day: CalendarDay,
    isSelected: Boolean,
    onDayClick: () -> Unit,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val isToday = day.date.isEqual(today)
    
    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onDayClick)
            .padding(4.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            // Day number
            Text(
                text = day.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                color = when {
                    !day.isCurrentMonth -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
            )
            
            // Event indicators
            day.events.take(2).forEach { event ->
                EventIndicator(
                    event = event,
                    onClick = { onEventClick(event.id) },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(vertical = 1.dp)
                        .clip(MaterialTheme.shapes.extraSmall)
                )
            }
            
            // More events indicator
            if (day.events.size > 2) {
                Text(
                    text = "+${day.events.size - 2} more",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun EventIndicator(
    event: TimelineEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val eventColor = remember(event.id) {
        // Generate a stable color based on event ID or use category color
        val colors = listOf(
            Color(0xFF4285F4), // Blue
            Color(0xFFEA4335), // Red
            Color(0xFFFBBC05), // Yellow
            Color(0xFF34A853), // Green
            Color(0xFF673AB7)  // Purple
        )
        val safeId = if (event.id.isNullOrEmpty()) "default" else event.id
        val index = Math.abs(safeId.hashCode()) % colors.size
        colors[index]
    }
    
    Surface(
        onClick = onClick,
        modifier = modifier.height(4.dp),
        color = eventColor,
        shape = MaterialTheme.shapes.small
    ) {}
}

private data class CalendarDay(
    val date: LocalDate,
    val isCurrentMonth: Boolean = true,
    val events: List<TimelineEvent> = emptyList()
)
