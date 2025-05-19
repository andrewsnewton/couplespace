package com.newton.couplespace.screens.main.timeline

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun CalendarToggle(
    isMyCalendar: Boolean,
    onToggleCalendar: (Boolean) -> Unit
) {
    SingleChoiceSegmentedButtonRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        SegmentedButton(
            selected = isMyCalendar,
            onClick = { onToggleCalendar(true) },
            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
        ) {
            Text("My Calendar")
        }
        SegmentedButton(
            selected = !isMyCalendar,
            onClick = { onToggleCalendar(false) },
            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
        ) {
            Text("Partner's Calendar")
        }
    }
}

@Composable
fun CalendarView(
    selectedDate: Date = Date(),
    onDateSelected: (Date) -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(selectedDate),
                    style = MaterialTheme.typography.titleMedium
                )
                
                IconButton(onClick = { /* Open full calendar */ }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Open Calendar")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Create a calendar instance for the selected date
            val calendar = Calendar.getInstance().apply {
                time = selectedDate
            }
            
            // Get the current month and year
            val currentMonth = calendar.get(Calendar.MONTH)
            val currentYear = calendar.get(Calendar.YEAR)
            val today = Calendar.getInstance()
            
            // Get the first day of the month
            val firstDayOfMonth = Calendar.getInstance().apply {
                set(currentYear, currentMonth, 1)
            }
            
            // Get the day of week for the first day of month (0 = Sunday, 1 = Monday, etc.)
            val firstDayOfWeek = firstDayOfMonth.get(Calendar.DAY_OF_WEEK) - 1
            
            // Get the number of days in the month
            val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            
            // Days of week header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                val daysOfWeek = listOf("S", "M", "T", "W", "T", "F", "S")
                daysOfWeek.forEach { day ->
                    Text(
                        text = day,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(4.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Calendar grid - weeks
            val weeks = (daysInMonth + firstDayOfWeek + 6) / 7 // Calculate number of weeks
            
            for (week in 0 until weeks) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Days in a week
                    for (dayOfWeek in 0..6) {
                        val day = week * 7 + dayOfWeek - firstDayOfWeek + 1
                        
                        if (day in 1..daysInMonth) {
                            // Create a calendar for this specific day
                            val dayCalendar = Calendar.getInstance().apply {
                                set(currentYear, currentMonth, day)
                            }
                            
                            // Check if this is the selected day
                            val isSelectedDay = calendar.get(Calendar.DAY_OF_MONTH) == day
                            
                            // Check if this is today
                            val isToday = today.get(Calendar.YEAR) == currentYear &&
                                          today.get(Calendar.MONTH) == currentMonth &&
                                          today.get(Calendar.DAY_OF_MONTH) == day
                            
                            // Simulate having events on some days
                            val hasEvent = day % 3 == 0
                            
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            isSelectedDay -> MaterialTheme.colorScheme.primary
                                            isToday -> MaterialTheme.colorScheme.primaryContainer
                                            else -> Color.Transparent
                                        }
                                    )
                                    .clickable {
                                        // Create a date for the selected day and notify
                                        val newDate = Calendar.getInstance().apply {
                                            set(currentYear, currentMonth, day)
                                        }.time
                                        onDateSelected(newDate)
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.toString(),
                                    color = when {
                                        isSelectedDay -> MaterialTheme.colorScheme.onPrimary
                                        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                                        else -> MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                            
                            if (hasEvent) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 40.dp)
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary)
                                )
                            }
                        } else {
                            // Empty space for days not in this month
                            Box(modifier = Modifier.size(36.dp))
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}
