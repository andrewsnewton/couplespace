package com.newton.couplespace.screens.main.timeline

import android.os.Bundle
import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.newton.couplespace.models.TimelineEvent
import com.newton.couplespace.models.TimelineViewMode
import com.newton.couplespace.screens.main.timeline.components.*
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.*

private const val TAG = "EnhancedTimelineScreen"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnhancedTimelineScreen(
    modifier: Modifier = Modifier,
    navController: androidx.navigation.NavController? = null
) {
    val context = LocalContext.current
    val application = context.applicationContext as android.app.Application
    
    val viewModel: TimelineViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return TimelineViewModel(application) as T
            }
        }
    )
    LaunchedEffect(Unit) {
        try {
            // Initial load
            viewModel.loadEventsForCurrentView()
        } catch (e: Exception) {
            Log.e(TAG, "Error in LaunchedEffect", e)
        }
    }
    val viewState by viewModel.viewState.collectAsState()
    val selectedEvent by viewModel.selectedEvent.collectAsState()
    
    // Format the title based on the view mode
    val titleFormatter = remember { DateTimeFormatter.ofPattern("MMMM d, yyyy") }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    
    // Don't show date in title for DAY view to match reference image
    val title = when (viewState.viewMode) {
        TimelineViewMode.DAY -> ""
        TimelineViewMode.WEEK -> {
            val weekOfYear = viewState.selectedDate.get(java.time.temporal.WeekFields.of(Locale.getDefault()).weekOfYear())
            val month = viewState.selectedDate.month.getDisplayName(TextStyle.FULL, Locale.getDefault())
            "Week $weekOfYear, $month"
        }
        TimelineViewMode.MONTH -> viewState.selectedDate.format(monthFormatter)
        TimelineViewMode.AGENDA -> "Agenda"
    }
    
    // State for showing the add event dialog
    var showAddEventDialog by remember { mutableStateOf(false) }
    
    // State for the selected date when adding a new event
    var selectedDateForNewEvent by remember { mutableStateOf(viewState.selectedDate) }
    
    // State for the selected time when adding a new event
    var selectedTimeForNewEvent by remember { mutableStateOf(
        LocalTime.now().plusHours(1).withMinute(0).withSecond(0)
    ) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = title) },
                navigationIcon = {
                    // Back button to return to regular timeline
                    IconButton(onClick = { 
                        navController?.popBackStack() ?: viewModel.navigateToToday()
                    }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back to Regular Timeline"
                        )
                    }
                },
                actions = {
                    
                    // View mode selector
                    IconButton(onClick = { viewModel.setViewMode(TimelineViewMode.DAY) }) {
                        Icon(
                            Icons.Default.ViewDay,
                            contentDescription = "Day view",
                            tint = if (viewState.viewMode == TimelineViewMode.DAY) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.setViewMode(TimelineViewMode.WEEK) }) {
                        Icon(
                            Icons.Default.ViewWeek,
                            contentDescription = "Week view",
                            tint = if (viewState.viewMode == TimelineViewMode.WEEK) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.setViewMode(TimelineViewMode.MONTH) }) {
                        Icon(
                            Icons.Default.CalendarMonth,
                            contentDescription = "Month view",
                            tint = if (viewState.viewMode == TimelineViewMode.MONTH) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.setViewMode(TimelineViewMode.AGENDA) }) {
                        Icon(
                            Icons.Default.ViewAgenda,
                            contentDescription = "Agenda view",
                            tint = if (viewState.viewMode == TimelineViewMode.AGENDA) 
                                MaterialTheme.colorScheme.primary 
                            else 
                                MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        content = { paddingValues ->
            Box(modifier = Modifier.padding(paddingValues)) {
                when (viewState.viewMode) {
                    TimelineViewMode.DAY -> {
                        DayTimelineView(
                            date = viewState.selectedDate,
                            events = viewState.events,
                            onEventClick = { eventId -> viewModel.selectEvent(eventId) },
                            onDateChange = { date -> viewModel.setSelectedDate(date) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    TimelineViewMode.WEEK -> {
                        WeekTimelineView(
                            startDate = viewState.selectedDate,
                            events = viewState.events,
                            onEventClick = { eventId -> viewModel.selectEvent(eventId) },
                            onDateChange = { date -> viewModel.setSelectedDate(date) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    TimelineViewMode.MONTH -> {
                        MonthCalendarView(
                            selectedDate = viewState.selectedDate,
                            events = viewState.events,
                            onDateSelected = { date -> viewModel.setSelectedDate(date) },
                            onEventClick = { eventId -> viewModel.selectEvent(eventId) },
                            onMonthChange = { date -> viewModel.setSelectedDate(date) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    TimelineViewMode.AGENDA -> {
                        // Filter out events with null timestamps to prevent crashes
                        val safeEvents = viewState.events.filter { 
                            it.startTime != null && it.endTime != null
                        }
                        
                        AgendaView(
                            startDate = viewState.selectedDate,
                            events = safeEvents,
                            onEventClick = { eventId -> viewModel.selectEvent(eventId) },
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }
                
                // Floating action button
                FloatingActionButton(
                    onClick = {
                        // Set the selected date and time for the new event
                        selectedDateForNewEvent = viewState.selectedDate
                        selectedTimeForNewEvent = LocalTime.now().plusHours(1).withMinute(0).withSecond(0)
                        showAddEventDialog = true
                    },
                    modifier = Modifier
                        .padding(16.dp)
                        .align(Alignment.BottomEnd),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Event")
                }
                
                // Show event detail dialog when an event is selected
                selectedEvent?.let { event ->
                    EventDetailDialog(
                        event = event,
                        onDismiss = { viewModel.clearSelectedEvent() },
                        onEdit = {
                            // Set up for editing the event
                            selectedDateForNewEvent = event.startTime.toDate().toInstant()
                                .atZone(ZoneId.systemDefault()).toLocalDate()
                            selectedTimeForNewEvent = event.startTime.toDate().toInstant()
                                .atZone(ZoneId.systemDefault()).toLocalTime()
                            viewModel.clearSelectedEvent()
                            showAddEventDialog = true
                        },
                        onDelete = { viewModel.deleteEvent(event.id) },
                        onMarkCompleted = { completed -> 
                            viewModel.markEventCompleted(event.id, completed) 
                        }
                    )
                }
                
                // Add/Edit Event Dialog
                if (showAddEventDialog) {
                    AddEventDialog(
                        onDismiss = { showAddEventDialog = false },
                        onSave = { event, isForPartner ->
                            if (selectedEvent != null) {
                                // Editing existing event
                                viewModel.updateEvent(event)
                            } else {
                                // Creating new event
                                viewModel.createEvent(event, isForPartner)
                            }
                            showAddEventDialog = false
                        },
                        initialEvent = selectedEvent,
                        initialDate = selectedDateForNewEvent,
                        initialTime = selectedTimeForNewEvent
                    )
                }
            }
        }
    )
}

@Composable
private fun EventDetailDialog(
    event: TimelineEvent,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkCompleted: (Boolean) -> Unit
) {
    // Generate a stable color based on event ID
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
    
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(0.dp)) {
                // Header with event title and color
                Surface(
                    color = eventColor.copy(alpha = 0.8f),
                    shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                ) {
                    Box(modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                    ) {
                        Column {
                            Text(
                                text = event.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                            
                            // Format dates for header
                            val startTime = event.startTime.toDate().toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                            val endTime = event.endTime.toDate().toInstant()
                                .atZone(java.time.ZoneId.systemDefault())
                            
                            val dateFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy")
                            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
                            
                            Text(
                                text = startTime.format(dateFormatter),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
                
                // Content
                Column(modifier = Modifier.padding(24.dp)) {
                    // Format dates
                    val startTime = event.startTime.toDate().toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                    val endTime = event.endTime.toDate().toInstant()
                        .atZone(java.time.ZoneId.systemDefault())
                    
                    val timeFormatter = DateTimeFormatter.ofPattern("h:mm a")
                    
                    // Time section
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = eventColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "${startTime.format(timeFormatter)} - ${endTime.format(timeFormatter)}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Duration: ${event.durationMinutes} minutes",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Divider(modifier = Modifier.padding(vertical = 16.dp))
                    
                    // Location section (if available)
                    if (event.location.isNotBlank()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = eventColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = event.location,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                    }
                    
                    // Description section (if available)
                    if (event.description.isNotBlank()) {
                        Row(verticalAlignment = Alignment.Top) {
                            Icon(
                                imageVector = Icons.Default.Event,
                                contentDescription = null,
                                tint = eventColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = "Description",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = event.description,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                    }
                    
                    // Details section
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = eventColor,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column {
                            Text(
                                text = "Details",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Type",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = event.eventType.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Category",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = event.category.name.lowercase().replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }
                    
                    // Completion status (for tasks and reminders)
                    if (event.eventType == com.newton.couplespace.models.EventType.TASK || 
                        event.eventType == com.newton.couplespace.models.EventType.REMINDER) {
                        Divider(modifier = Modifier.padding(vertical = 16.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Mark as completed",
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            Switch(
                                checked = event.isCompleted,
                                onCheckedChange = onMarkCompleted,
                                colors = SwitchDefaults.colors(checkedThumbColor = eventColor)
                            )
                        }
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDelete) {
                        Text("Delete")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    TextButton(onClick = onEdit) {
                        Text("Edit")
                    }
                }
            }
        }
    }
}
