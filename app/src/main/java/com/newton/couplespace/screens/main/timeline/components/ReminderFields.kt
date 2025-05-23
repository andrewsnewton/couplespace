package com.newton.couplespace.screens.main.timeline.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.newton.couplespace.models.NotificationSettings
import com.newton.couplespace.models.Reminder
import com.newton.couplespace.models.ReminderUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderFields(
    notificationSettings: NotificationSettings,
    onNotificationSettingsChange: (NotificationSettings) -> Unit,
    modifier: Modifier = Modifier
) {
    var reminders by remember { mutableStateOf(notificationSettings.reminders) }
    var emailNotification by remember { mutableStateOf(notificationSettings.emailNotification) }
    var pushNotification by remember { mutableStateOf(notificationSettings.pushNotification) }
    var soundEnabled by remember { mutableStateOf(notificationSettings.soundEnabled) }
    var vibrationEnabled by remember { mutableStateOf(notificationSettings.vibrationEnabled) }
    
    // Update the parent state when any of the local states change
    LaunchedEffect(reminders, emailNotification, pushNotification, soundEnabled, vibrationEnabled) {
        onNotificationSettingsChange(
            NotificationSettings(
                reminders = reminders,
                emailNotification = emailNotification,
                pushNotification = pushNotification,
                soundEnabled = soundEnabled,
                vibrationEnabled = vibrationEnabled
            )
        )
    }
    
    Column(modifier = modifier) {
        Text(
            text = "Reminder Settings",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Notification options
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Push Notifications",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = pushNotification,
                onCheckedChange = { pushNotification = it }
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Email Notifications",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = emailNotification,
                onCheckedChange = { emailNotification = it }
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Sound",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = soundEnabled,
                onCheckedChange = { soundEnabled = it }
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Vibration",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = vibrationEnabled,
                onCheckedChange = { vibrationEnabled = it }
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Reminder times
        Text(
            text = "Reminder Times",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        LazyColumn(
            modifier = Modifier.heightIn(max = 200.dp)
        ) {
            items(reminders) { reminder ->
                ReminderItem(
                    reminder = reminder,
                    onDelete = {
                        reminders = reminders.filter { it != reminder }
                    }
                )
            }
            
            item {
                OutlinedButton(
                    onClick = {
                        // Add a new reminder with default values
                        reminders = reminders + Reminder(15, ReminderUnit.MINUTES)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Reminder",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Reminder")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReminderItem(
    reminder: Reminder,
    onDelete: () -> Unit
) {
    var value by remember { mutableStateOf(reminder.value.toString()) }
    var unit by remember { mutableStateOf(reminder.unit) }
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = "Reminder",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Value input
            OutlinedTextField(
                value = value,
                onValueChange = { 
                    // Only allow numbers
                    if (it.isBlank() || it.all { char -> char.isDigit() }) {
                        value = it
                    }
                },
                label = { Text("Time") },
                singleLine = true,
                modifier = Modifier
                    .width(80.dp)
                    .padding(end = 8.dp)
            )
            
            // Unit selector
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it }
            ) {
                OutlinedTextField(
                    value = unit.name.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .width(120.dp)
                        .menuAnchor()
                )
                
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    ReminderUnit.values().forEach { reminderUnit ->
                        DropdownMenuItem(
                            text = { 
                                Text(reminderUnit.name.lowercase().replaceFirstChar { it.uppercase() }) 
                            },
                            onClick = {
                                unit = reminderUnit
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Delete button
            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete Reminder",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
