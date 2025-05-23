package com.newton.couplespace.screens.main.timeline.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.google.firebase.Timestamp
import com.newton.couplespace.models.*
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

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
    initialTime: LocalTime = LocalTime.now().plusHours(1).withMinute(0).withSecond(0)
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
    var isForPartner by remember { mutableStateOf(false) }
    
    // Date and time state
    var startDate by remember { mutableStateOf(
        initialEvent?.startTime?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate() 
            ?: initialDate
    ) }
    var endDate by remember { mutableStateOf(
        initialEvent?.endTime?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate() 
            ?: initialDate
    ) }
    var startTime by remember { mutableStateOf(
        initialEvent?.startTime?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalTime() 
            ?: initialTime
    ) }
    var endTime by remember { mutableStateOf(
        initialEvent?.endTime?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalTime() 
            ?: initialTime.plusHours(1)
    ) }
    
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
                                    // Create event
                                    val startDateTime = LocalDateTime.of(startDate, if (isAllDay) LocalTime.MIN else startTime)
                                    val endDateTime = LocalDateTime.of(endDate, if (isAllDay) LocalTime.MAX else endTime)
                                    
                                    val event = (initialEvent ?: TimelineEvent()).copy(
                                        title = title,
                                        description = description,
                                        location = location,
                                        startTime = startDateTime.toTimestamp(),
                                        endTime = endDateTime.toTimestamp(),
                                        allDay = isAllDay,
                                        eventType = eventType,
                                        category = category,
                                        priority = priority,
                                        isRecurring = isRecurring,
                                        recurrenceRule = if (isRecurring) recurrenceRule else null,
                                        notificationSettings = notificationSettings
                                    )
                                    
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
                        Text(
                            text = "Add to Partner's Calendar",
                            modifier = Modifier.weight(1f)
                        )
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
