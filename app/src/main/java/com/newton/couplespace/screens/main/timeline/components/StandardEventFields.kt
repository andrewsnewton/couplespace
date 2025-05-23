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
fun StandardEventFields(
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
            text = "Event Settings",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        
        // Recurring toggle
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recurring Event",
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
                            RecurrenceFrequency.DAILY -> "Day(s)"
                            RecurrenceFrequency.WEEKLY -> "Week(s)"
                            RecurrenceFrequency.MONTHLY -> "Month(s)"
                            RecurrenceFrequency.YEARLY -> "Year(s)"
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
                        DropdownMenuItem(
                            text = { Text("Day(s)") },
                            onClick = {
                                frequency = RecurrenceFrequency.DAILY
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                        DropdownMenuItem(
                            text = { Text("Week(s)") },
                            onClick = {
                                frequency = RecurrenceFrequency.WEEKLY
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
                        DropdownMenuItem(
                            text = { Text("Year(s)") },
                            onClick = {
                                frequency = RecurrenceFrequency.YEARLY
                                expanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Recurrence explanation
            val explanationText = when (frequency) {
                RecurrenceFrequency.DAILY -> "This event will repeat every ${if (interval.isBlank() || interval == "1") "" else "$interval "} day(s)."
                RecurrenceFrequency.WEEKLY -> "This event will repeat every ${if (interval.isBlank() || interval == "1") "" else "$interval "} week(s) on the same day of the week."
                RecurrenceFrequency.MONTHLY -> "This event will repeat every ${if (interval.isBlank() || interval == "1") "" else "$interval "} month(s) on the same day of the month."
                RecurrenceFrequency.YEARLY -> "This event will repeat every ${if (interval.isBlank() || interval == "1") "" else "$interval "} year(s) on the same date."
                RecurrenceFrequency.CUSTOM -> "Custom recurrence pattern."
            }
            
            Text(
                text = explanationText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 4.dp, top = 4.dp, bottom = 8.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Event sharing info
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
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
                    text = "Sharing This Event",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = "You can add this event to your partner's calendar by toggling 'Add to Partner's Calendar' below. This will make the event visible to both of you.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
    }
}
