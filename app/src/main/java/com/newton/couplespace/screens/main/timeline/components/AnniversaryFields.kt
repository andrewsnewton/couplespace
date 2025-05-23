package com.newton.couplespace.screens.main.timeline.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.newton.couplespace.models.RecurrenceFrequency
import com.newton.couplespace.models.RecurrenceRule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnniversaryFields(
    isRecurring: Boolean,
    onIsRecurringChange: (Boolean) -> Unit,
    recurrenceRule: RecurrenceRule,
    onRecurrenceRuleChange: (RecurrenceRule) -> Unit,
    modifier: Modifier = Modifier
) {
    var frequency by remember { mutableStateOf(recurrenceRule.frequency) }
    var interval by remember { mutableStateOf(recurrenceRule.interval.toString()) }
    var expanded by remember { mutableStateOf(false) }
    
    // Update parent state when local state changes
    LaunchedEffect(frequency, interval) {
        val intervalValue = interval.toIntOrNull() ?: 1
        onRecurrenceRuleChange(
            recurrenceRule.copy(
                frequency = frequency,
                interval = intervalValue
            )
        )
    }
    
    Column(modifier = modifier) {
        Text(
            text = "Anniversary Settings",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Anniversary type information
        Text(
            text = "Anniversaries are special events that can recur yearly.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Recurring toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recurring Anniversary",
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = isRecurring,
                onCheckedChange = onIsRecurringChange
            )
        }
        
        // Recurrence settings (only if recurring)
        if (isRecurring) {
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                text = "Recurrence",
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Interval input
                OutlinedTextField(
                    value = interval,
                    onValueChange = { 
                        // Only allow numbers
                        if (it.isBlank() || it.all { char -> char.isDigit() }) {
                            interval = it
                        }
                    },
                    label = { Text("Every") },
                    singleLine = true,
                    modifier = Modifier
                        .width(80.dp)
                        .padding(end = 8.dp)
                )
                
                // Frequency selector
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = when (frequency) {
                            RecurrenceFrequency.YEARLY -> "Year(s)"
                            RecurrenceFrequency.MONTHLY -> "Month(s)"
                            RecurrenceFrequency.WEEKLY -> "Week(s)"
                            RecurrenceFrequency.DAILY -> "Day(s)"
                            RecurrenceFrequency.CUSTOM -> "Custom"
                        },
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
                        // For anniversaries, typically yearly is the most common
                        DropdownMenuItem(
                            text = { Text("Year(s)") },
                            onClick = {
                                frequency = RecurrenceFrequency.YEARLY
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                        DropdownMenuItem(
                            text = { Text("Month(s)") },
                            onClick = {
                                frequency = RecurrenceFrequency.MONTHLY
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Anniversary reminder info
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Anniversary Reminders",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Text(
                        text = "You'll receive reminders for this anniversary based on your notification settings. For important anniversaries, consider adding multiple reminders (e.g., 1 week before, 1 day before).",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
