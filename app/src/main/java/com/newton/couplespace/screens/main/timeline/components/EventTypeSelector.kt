package com.newton.couplespace.screens.main.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.newton.couplespace.models.EventType

@Composable
fun EventTypeSelector(
    selectedType: EventType,
    onTypeSelected: (EventType) -> Unit,
    modifier: Modifier = Modifier
) {
    val eventTypes = listOf(
        EventTypeOption(EventType.EVENT, Icons.Default.Event, "Event"),
        EventTypeOption(EventType.REMINDER, Icons.Default.Notifications, "Reminder"),
        EventTypeOption(EventType.TASK, Icons.Default.CheckCircle, "Task"),
        EventTypeOption(EventType.ANNIVERSARY, Icons.Default.Favorite, "Anniversary"),
        EventTypeOption(EventType.GOAL, Icons.Default.Flag, "Goal"),
        EventTypeOption(EventType.MEAL, Icons.Default.Restaurant, "Meal"),
        EventTypeOption(EventType.EXERCISE, Icons.Default.FitnessCenter, "Exercise"),
        EventTypeOption(EventType.APPOINTMENT, Icons.Default.Schedule, "Appointment"),
        EventTypeOption(EventType.TRAVEL, Icons.Default.Flight, "Travel"),
        EventTypeOption(EventType.CUSTOM, Icons.Default.Add, "Custom")
    )
    
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(eventTypes) { typeOption ->
            EventTypeItem(
                type = typeOption,
                isSelected = selectedType == typeOption.type,
                onClick = { onTypeSelected(typeOption.type) }
            )
        }
    }
}

@Composable
private fun EventTypeItem(
    type: EventTypeOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(72.dp)
            .padding(4.dp)
            .clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                )
                .border(
                    width = if (isSelected) 0.dp else 1.dp,
                    color = if (isSelected) Color.Transparent else MaterialTheme.colorScheme.outline,
                    shape = CircleShape
                )
        ) {
            Icon(
                imageVector = type.icon,
                contentDescription = type.label,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimary
                      else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = type.label,
            style = MaterialTheme.typography.labelSmall,
            color = if (isSelected) MaterialTheme.colorScheme.primary
                  else MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

data class EventTypeOption(
    val type: EventType,
    val icon: ImageVector,
    val label: String
)
