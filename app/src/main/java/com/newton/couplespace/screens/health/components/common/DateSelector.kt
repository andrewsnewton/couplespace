package com.newton.couplespace.screens.health.components.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * A date selector component that allows users to navigate between dates
 */
@Composable
fun DateSelector(
    selectedDate: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    onDateClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val today = LocalDate.now()
    val dayFormatter = DateTimeFormatter.ofPattern("d")
    val monthFormatter = DateTimeFormatter.ofPattern("MMMM yyyy")
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left arrow - previous day
        IconButton(
            onClick = { onDateChange(selectedDate.minusDays(1)) }
        ) {
            Icon(
                imageVector = Icons.Default.ChevronLeft,
                contentDescription = "Previous Day",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        
        // Center - current date display
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Month and year
            Text(
                text = selectedDate.format(monthFormatter),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Day of month in box with day of week
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(16.dp)
                    )
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onDateClick() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = selectedDate.format(dayFormatter),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Day of week
            Text(
                text = selectedDate.dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault()),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // Indicator if today
            if (selectedDate.isEqual(today)) {
                Text(
                    text = "Today",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
        
        // Right arrow - next day (disabled if it would be in the future)
        IconButton(
            onClick = { 
                // Only allow going to future dates up to today
                if (selectedDate.isBefore(today)) {
                    onDateChange(selectedDate.plusDays(1))
                }
            },
            enabled = selectedDate.isBefore(today)
        ) {
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Next Day",
                tint = if (selectedDate.isBefore(today)) 
                        MaterialTheme.colorScheme.primary 
                      else 
                        MaterialTheme.colorScheme.outlineVariant
            )
        }
    }
}
