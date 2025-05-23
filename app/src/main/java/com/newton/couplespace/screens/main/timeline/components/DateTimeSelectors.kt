package com.newton.couplespace.screens.main.timeline.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateTimeSelectors(
    startDate: LocalDate,
    endDate: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    isAllDay: Boolean,
    onStartDateChange: (LocalDate) -> Unit,
    onEndDateChange: (LocalDate) -> Unit,
    onStartTimeChange: (LocalTime) -> Unit,
    onEndTimeChange: (LocalTime) -> Unit,
    dateError: Boolean = false,
    timeError: Boolean = false,
    modifier: Modifier = Modifier
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }
    
    Column(modifier = modifier) {
        // Start date and time
        Text(
            text = "Start",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start date
            OutlinedCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showStartDatePicker = true },
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (dateError) 2.dp else 1.dp,
                    color = if (dateError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Start Date",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = startDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (dateError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Start time (only if not all day)
            if (!isAllDay) {
                OutlinedCard(
                    modifier = Modifier
                        .width(120.dp)
                        .clickable { showStartTimePicker = true },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (timeError) 2.dp else 1.dp,
                        color = if (timeError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Start Time",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = startTime.format(timeFormatter),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (timeError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        
        if (dateError) {
            Text(
                text = "End date must be after start date",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // End date and time
        Text(
            text = "End",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // End date
            OutlinedCard(
                modifier = Modifier
                    .weight(1f)
                    .clickable { showEndDatePicker = true },
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (dateError) 2.dp else 1.dp,
                    color = if (dateError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "End Date",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = endDate.format(dateFormatter),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (dateError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // End time (only if not all day)
            if (!isAllDay) {
                OutlinedCard(
                    modifier = Modifier
                        .width(120.dp)
                        .clickable { showEndTimePicker = true },
                    colors = CardDefaults.outlinedCardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(
                        width = if (timeError) 2.dp else 1.dp,
                        color = if (timeError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "End Time",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = endTime.format(timeFormatter),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (timeError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
        
        if (timeError) {
            Text(
                text = "End time must be after start time",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp)
            )
        }
    }
    
    // Date pickers
    if (showStartDatePicker) {
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                Button(
                    onClick = { showStartDatePicker = false }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showStartDatePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            // Simple date selection dialog
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select Start Date",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Simple date selection with year, month, day pickers
                val currentDate = remember { startDate }
                var year by remember { mutableStateOf(currentDate.year) }
                var month by remember { mutableStateOf(currentDate.monthValue) }
                var day by remember { mutableStateOf(currentDate.dayOfMonth) }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Year picker
                    NumberSelector(
                        value = year,
                        onValueChange = { year = it },
                        range = 2020..2030,
                        label = "Year"
                    )
                    
                    // Month picker
                    NumberSelector(
                        value = month,
                        onValueChange = { month = it },
                        range = 1..12,
                        label = "Month"
                    )
                    
                    // Day picker
                    NumberSelector(
                        value = day,
                        onValueChange = { day = it },
                        range = 1..31, // Simplified, should check actual days in month
                        label = "Day"
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        try {
                            val newDate = LocalDate.of(year, month, day)
                            onStartDateChange(newDate)
                            // If end date is before start date, update end date
                            if (endDate.isBefore(newDate)) {
                                onEndDateChange(newDate)
                            }
                            showStartDatePicker = false
                        } catch (e: Exception) {
                            // Handle invalid date
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set Date")
                }
            }
        }
    }
    
    if (showEndDatePicker) {
        androidx.compose.material3.DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                Button(
                    onClick = { showEndDatePicker = false }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEndDatePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            // Simple date selection dialog
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Select End Date",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Simple date selection with year, month, day pickers
                val currentDate = remember { endDate }
                var year by remember { mutableStateOf(currentDate.year) }
                var month by remember { mutableStateOf(currentDate.monthValue) }
                var day by remember { mutableStateOf(currentDate.dayOfMonth) }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Year picker
                    NumberSelector(
                        value = year,
                        onValueChange = { year = it },
                        range = 2020..2030,
                        label = "Year"
                    )
                    
                    // Month picker
                    NumberSelector(
                        value = month,
                        onValueChange = { month = it },
                        range = 1..12,
                        label = "Month"
                    )
                    
                    // Day picker
                    NumberSelector(
                        value = day,
                        onValueChange = { day = it },
                        range = 1..31, // Simplified, should check actual days in month
                        label = "Day"
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Button(
                    onClick = {
                        try {
                            val newDate = LocalDate.of(year, month, day)
                            // Ensure end date is not before start date
                            if (!newDate.isBefore(startDate)) {
                                onEndDateChange(newDate)
                                showEndDatePicker = false
                            }
                        } catch (e: Exception) {
                            // Handle invalid date
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Set Date")
                }
            }
        }
    }
    
    // Time pickers
    if (showStartTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            onTimeSelected = { hour, minute ->
                val time = LocalTime.of(hour, minute)
                onStartTimeChange(time)
                
                // If end time is before start time on the same day, update end time
                if (startDate.isEqual(endDate) && endTime.isBefore(time)) {
                    onEndTimeChange(time.plusHours(1))
                }
                
                showStartTimePicker = false
            },
            initialHour = startTime.hour,
            initialMinute = startTime.minute
        )
    }
    
    if (showEndTimePicker) {
        TimePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            onTimeSelected = { hour, minute ->
                val time = LocalTime.of(hour, minute)
                onEndTimeChange(time)
                showEndTimePicker = false
            },
            initialHour = endTime.hour,
            initialMinute = endTime.minute
        )
    }
}

@Composable
fun TimePickerDialog(
    onDismissRequest: () -> Unit,
    onTimeSelected: (hour: Int, minute: Int) -> Unit,
    initialHour: Int,
    initialMinute: Int
) {
    var hour by remember { mutableStateOf(initialHour) }
    var minute by remember { mutableStateOf(initialMinute) }
    
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Select Time") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Simple time picker with hour and minute selectors
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Hour selector
                    NumberSelector(
                        value = hour,
                        onValueChange = { hour = it },
                        range = 0..23,
                        label = "Hour"
                    )
                    
                    Text(
                        text = ":",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    // Minute selector
                    NumberSelector(
                        value = minute,
                        onValueChange = { minute = it },
                        range = 0..59,
                        label = "Minute"
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onTimeSelected(hour, minute) }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun NumberSelector(
    value: Int,
    onValueChange: (Int) -> Unit,
    range: IntRange,
    label: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        IconButton(
            onClick = {
                val newValue = if (value >= range.last) range.first else value + 1
                onValueChange(newValue)
            }
        ) {
            Text("▲", style = MaterialTheme.typography.titleLarge)
        }
        
        Text(
            text = value.toString().padStart(2, '0'),
            style = MaterialTheme.typography.headlineMedium
        )
        
        IconButton(
            onClick = {
                val newValue = if (value <= range.first) range.last else value - 1
                onValueChange(newValue)
            }
        ) {
            Text("▼", style = MaterialTheme.typography.titleLarge)
        }
        
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
