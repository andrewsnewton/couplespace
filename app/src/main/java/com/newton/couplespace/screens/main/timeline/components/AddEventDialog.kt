package com.newton.couplespace.screens.main.timeline.components

import android.icu.text.SimpleDateFormat
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.Timestamp
import com.newton.couplespace.models.*
import com.newton.couplespace.screens.main.timeline.TimelineViewModel
import com.newton.couplespace.util.toZoneId
import java.time.*
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.TimeZone

/**
 * Dialog for adding or editing timeline events
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEventDialog(
    onDismiss: () -> Unit,
    onSave: (TimelineEvent, Boolean) -> Unit,
    initialEvent: TimelineEvent? = null,
    initialDate: LocalDate = LocalDate.now(),
    initialTime: LocalTime = LocalTime.now().plusHours(1).withMinute(0).withSecond(0),
    viewModel: TimelineViewModel,
    initialIsForPartner: Boolean = false
) {
    // Event state
    var title by remember { mutableStateOf(initialEvent?.title ?: "") }
    var description by remember { mutableStateOf(initialEvent?.description ?: "") }
    var location by remember { mutableStateOf(initialEvent?.location ?: "") }
    var eventType by remember { mutableStateOf(initialEvent?.eventType ?: EventType.EVENT) }
    var category by remember { mutableStateOf(initialEvent?.category ?: EventCategory.PERSONAL) }
    var priority by remember { mutableStateOf(initialEvent?.priority ?: Priority.MEDIUM) }
    var isAllDay by remember { mutableStateOf(initialEvent?.allDay ?: false) }
    var isRecurring by remember { mutableStateOf(initialEvent?.isRecurring ?: false) }
    
    // Get partner timezone from view model
    val partnerTimeZone = viewModel.viewState.value.partnerTimeZone
    val partnerZoneId = partnerTimeZone?.let { ZoneId.of(it.id) }
    
    // Determine if this is a new event for partner based on initial parameters
    var isForPartner by remember { mutableStateOf(initialIsForPartner) }
    
    // Date and time state - initialize with proper timezone awareness
    var startDate by remember { mutableStateOf(
        initialEvent?.startTime?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate() 
            ?: initialDate
    ) }
    var endDate by remember { mutableStateOf(
        initialEvent?.endTime?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate() 
            ?: initialDate
    ) }
    
    // For start time, use the initialTime which may have been converted to partner timezone already
    var startTime by remember { mutableStateOf(
        initialEvent?.startTime?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalTime() 
            ?: initialTime
    ) }
    
    // For end time, add one hour to start time
    var endTime by remember { mutableStateOf(
        initialEvent?.endTime?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalTime() 
            ?: initialTime.plusHours(1)
    ) }
    
    // Log the initial time values for debugging
    LaunchedEffect(Unit) {
        Log.d("AddEventDialog", "Dialog initialized with date: $initialDate, time: $initialTime")
        if (partnerZoneId != null) {
            Log.d("AddEventDialog", "Partner timezone available: ${partnerZoneId}")
        }
    }
    
    // Notification settings
    var notificationSettings by remember { mutableStateOf(
        initialEvent?.notificationSettings ?: NotificationSettings()
    ) }
    
    // Recurrence rule
    var recurrenceRule by remember { mutableStateOf(
        initialEvent?.recurrenceRule ?: RecurrenceRule(RecurrenceFrequency.DAILY)
    ) }
    
    // Validation state
    var titleError by remember { mutableStateOf(false) }
    var dateError by remember { mutableStateOf(false) }
    var timeError by remember { mutableStateOf(false) }
    
    // Dialog content
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.9f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // Dialog header
                TopAppBar(
                    title = { 
                        Text(
                            text = if (initialEvent == null) "Add Event" else "Edit Event",
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    },
                    actions = {
                        TextButton(
                            onClick = {
                                // Validate inputs
                                titleError = title.isBlank()
                                dateError = startDate.isAfter(endDate)
                                timeError = !isAllDay && startTime.isAfter(endTime) && startDate.isEqual(endDate)
                                
                                if (!titleError && !dateError && !timeError) {
                                    // Create event with local times (user's timezone)
                                    val startDateTime = startDate.atTime(startTime)
                                    val endDateTime = if (isAllDay) {
                                        startDate.atTime(23, 59, 59)
                                    } else {
                                        endDate.atTime(endTime)
                                    }
                                    
                                    Log.d("AddEventDialog", "Creating event with local time: $startDateTime to $endDateTime")
                                    Log.d("AddEventDialog", "System timezone: ${ZoneId.systemDefault()}")
                                    
                                    // Use different timezone handling based on whether it's for partner or not
                                    val (startTimestamp, endTimestamp, sourceTimezone) = if (isForPartner && partnerZoneId != null) {
                                        // For partner events, convert to partner timezone
                                        val partnerStartDateTime = startDateTime.atZone(ZoneId.systemDefault())
                                            .withZoneSameInstant(partnerZoneId)
                                        val partnerEndDateTime = endDateTime.atZone(ZoneId.systemDefault())
                                            .withZoneSameInstant(partnerZoneId)
                                            
                                        Log.d("AddEventDialog", "Converting to partner timezone: $partnerStartDateTime to $partnerEndDateTime")
                                        
                                        Triple(
                                            Timestamp(Date.from(partnerStartDateTime.toInstant())),
                                            Timestamp(Date.from(partnerEndDateTime.toInstant())),
                                            partnerZoneId.id
                                        )
                                    } else {
                                        // For user events, use system timezone
                                        Triple(
                                            Timestamp(Date.from(startDateTime.atZone(ZoneId.systemDefault()).toInstant())),
                                            Timestamp(Date.from(endDateTime.atZone(ZoneId.systemDefault()).toInstant())),
                                            ZoneId.systemDefault().id
                                        )
                                    }
                                    
                                    val event = (initialEvent ?: TimelineEvent()).copy(
                                        title = title,
                                        description = description,
                                        location = location,
                                        eventType = eventType,
                                        category = category,
                                        priority = priority,
                                        startTime = startTimestamp,
                                        endTime = endTimestamp,
                                        allDay = isAllDay,
                                        isRecurring = isRecurring,
                                        recurrenceRule = if (isRecurring) recurrenceRule else null,
                                        notificationSettings = notificationSettings,
                                        sourceTimezone = sourceTimezone,
                                        metadata = mutableMapOf<String, Any?>().apply {
                                            put("lastUpdated", Timestamp.now())
                                            put("sourceTimezone", sourceTimezone)
                                            put("isForPartner", isForPartner)
                                        }
                                    )
                                    
                                    // We've already handled the timezone conversion earlier when creating the event
                                    // Just log the final event details and save
                                    Log.d("AddEventDialog", "Saving event with title: ${event.title}, start: ${event.startTime}, end: ${event.endTime}")
                                    Log.d("AddEventDialog", "Event timezone: ${event.sourceTimezone}, isForPartner: $isForPartner")
                                    
                                    // Save the event
                                    onSave(event, isForPartner)
                                }
                            }
                        ) {
                            Text("Save")
                        }
                    }
                )
                
                // Dialog content
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    // Event type selector
                    Text(
                        text = "Event Type",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    EventTypeSelector(
                        selectedType = eventType,
                        onTypeSelected = { eventType = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Basic event details
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it; titleError = false },
                        label = { Text("Title") },
                        isError = titleError,
                        supportingText = if (titleError) {
                            { Text("Title is required") }
                        } else null,
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Date and time selectors
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "All Day",
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = isAllDay,
                            onCheckedChange = { isAllDay = it }
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    DateTimeSelectors(
                        startDate = startDate,
                        endDate = endDate,
                        startTime = startTime,
                        endTime = endTime,
                        isAllDay = isAllDay,
                        onStartDateChange = { startDate = it; dateError = false },
                        onEndDateChange = { endDate = it; dateError = false },
                        onStartTimeChange = { startTime = it; timeError = false },
                        onEndTimeChange = { endTime = it; timeError = false },
                        dateError = dateError,
                        timeError = timeError
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Location field
                    OutlinedTextField(
                        value = location,
                        onValueChange = { location = it },
                        label = { Text("Location") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Category selector
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    CategorySelector(
                        selectedCategory = category,
                        onCategorySelected = { category = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Priority selector
                    Text(
                        text = "Priority",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    PrioritySelector(
                        selectedPriority = priority,
                        onPrioritySelected = { priority = it },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Type-specific fields
                    when (eventType) {
                        EventType.REMINDER -> {
                            ReminderFields(
                                notificationSettings = notificationSettings,
                                onNotificationSettingsChange = { notificationSettings = it }
                            )
                        }
                        EventType.TASK -> {
                            TaskFields()
                        }
                        EventType.ANNIVERSARY -> {
                            AnniversaryFields(
                                isRecurring = isRecurring,
                                onIsRecurringChange = { isRecurring = it },
                                recurrenceRule = recurrenceRule,
                                onRecurrenceRuleChange = { recurrenceRule = it }
                            )
                        }
                        else -> {
                            // Standard event fields
                            StandardEventFields(
                                isRecurring = isRecurring,
                                onIsRecurringChange = { isRecurring = it },
                                recurrenceRule = recurrenceRule,
                                onRecurrenceRuleChange = { recurrenceRule = it }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // For partner switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Add to Partner's Calendar"
                            )
                            val partnerTimeZone = viewModel.viewState.value.partnerTimeZone
                            if (partnerTimeZone != null) {
                                Text(
                                    text = "Event will be saved in partner's timezone (${partnerTimeZone.id ?: "Unknown"})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Switch(
                            checked = isForPartner,
                            onCheckedChange = { isForPartner = it }
                        )
                    }
                }
            }
        }
    }
}
