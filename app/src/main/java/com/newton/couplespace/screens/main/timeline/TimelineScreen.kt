package com.newton.couplespace.screens.main.timeline

import android.app.Application
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material.icons.filled.ViewWeek
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.newton.couplespace.models.TimelineViewMode
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.WeekFields
import java.util.Locale
import java.util.TimeZone
import com.newton.couplespace.screens.main.timeline.components.SplitTimelineView
import com.newton.couplespace.screens.main.timeline.components.WeekTimelineView
import com.newton.couplespace.screens.main.timeline.components.MonthCalendarView
import com.newton.couplespace.screens.main.timeline.components.AgendaView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(
    onAddEvent: () -> Unit,
    onEventClick: (String) -> Unit,
    modifier: Modifier = Modifier
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
    
    // Collect the view state from the ViewModel
    val viewState by viewModel.viewState.collectAsState()
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var viewMode by remember { mutableStateOf(TimelineViewMode.DAY) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = when (viewMode) {
                            TimelineViewMode.DAY -> "${selectedDate.dayOfMonth} ${selectedDate.month}"
                            TimelineViewMode.WEEK -> "Week ${selectedDate.get(WeekFields.of(Locale.getDefault()).weekOfYear())}"
                            TimelineViewMode.MONTH -> "${selectedDate.month} ${selectedDate.year}"
                            TimelineViewMode.AGENDA -> "Agenda"
                        }
                    )
                },
                actions = {
                    // View mode selector
                    Row(
                        modifier = Modifier.padding(end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = { viewMode = TimelineViewMode.DAY }) {
                            Icon(
                                Icons.Default.CalendarToday,
                                contentDescription = "Day view",
                                tint = if (viewMode == TimelineViewMode.DAY) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewMode = TimelineViewMode.WEEK }) {
                            Icon(
                                Icons.Default.ViewWeek,
                                contentDescription = "Week view",
                                tint = if (viewMode == TimelineViewMode.WEEK) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewMode = TimelineViewMode.MONTH }) {
                            Icon(
                                Icons.Default.CalendarMonth,
                                contentDescription = "Month view",
                                tint = if (viewMode == TimelineViewMode.MONTH) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { viewMode = TimelineViewMode.AGENDA }) {
                            Icon(
                                Icons.Default.ViewAgenda,
                                contentDescription = "Agenda view",
                                tint = if (viewMode == TimelineViewMode.AGENDA) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddEvent,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Event")
            }
        }
    ) { padding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (viewMode) {
                TimelineViewMode.DAY -> SplitTimelineView(
                    date = viewState.selectedDate,
                    events = viewState.events,
                    partnerEvents = viewState.partnerEvents ?: emptyList(),
                    onEventClick = onEventClick,
                    onDateChange = { viewModel.setSelectedDate(it) },
                    onAddEvent = onAddEvent,
                    isPaired = viewState.isPaired,
                    userTimeZone = TimeZone.getDefault(),
                    partnerTimeZone = viewState.partnerTimeZone,
                    userProfilePic = viewState.userProfilePicture,
                    partnerProfilePic = viewState.partnerProfilePicture,
                    modifier = Modifier.fillMaxSize()
                )
                TimelineViewMode.WEEK -> WeekTimelineView(
                    startDate = selectedDate,
                    onEventClick = onEventClick,
                    modifier = Modifier.fillMaxSize()
                )
                TimelineViewMode.MONTH -> MonthCalendarView(
                    selectedDate = selectedDate,
                    onDateSelected = { selectedDate = it },
                    onEventClick = onEventClick,
                    modifier = Modifier.fillMaxSize()
                )
                TimelineViewMode.AGENDA -> AgendaView(
                    startDate = selectedDate,
                    onEventClick = onEventClick,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            // Current time indicator (only for day and week views)
            if (viewMode == TimelineViewMode.DAY || viewMode == TimelineViewMode.WEEK) {
                CurrentTimeIndicator(
                    visible = viewMode == TimelineViewMode.DAY || viewMode == TimelineViewMode.WEEK
                )
            }
        }
    }
}

@Composable
private fun CurrentTimeIndicator(visible: Boolean) {
    if (!visible) return
    
    val currentTime = remember { LocalTime.now() }
    
    // This would be positioned based on the current time
    // For now, we'll just show a simple indicator at the top
    if (visible) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(MaterialTheme.colorScheme.primary)
        )
    }
}
