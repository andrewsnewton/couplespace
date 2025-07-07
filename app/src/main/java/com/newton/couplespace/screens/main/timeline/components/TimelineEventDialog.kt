package com.newton.couplespace.screens.main.timeline.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.newton.couplespace.models.TimelineEvent
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A Material 3 dialog for displaying timeline events at a specific time.
 * Uses our pastel color scheme and visual styling to match the rest of the timeline UI.
 * Includes event details with type-specific styling using EventTypeInfo.
 */
@Composable
fun TimelineEventDialog(
    events: List<TimelineEvent>,
    selectedTime: LocalTime?,
    onEventClick: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 8.dp,
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Dialog header with selected time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Rounded.Schedule,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Events at ${
                            selectedTime?.format(
                                DateTimeFormatter.ofPattern("h:mm a")
                            ) ?: ""
                        }",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                
                // Divider with elegant styling
                HorizontalDivider(
                    thickness = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))

                // List of events
                Column(modifier = Modifier.fillMaxWidth()) {
                    events.forEach { event ->
                        var isPressed by remember { mutableStateOf(false) }
                        val backgroundColor by animateColorAsState(
                            targetValue = if (isPressed) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            },
                            animationSpec = tween(durationMillis = 200),
                            label = "background color"
                        )
                        
                        // Event item with hover effects and type-specific styling
                        val eventType = event.eventType?.name ?: ""
                        val eventTypeInfo = getEventTypeInfo(eventType)
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clickable(onClick = { 
                                    onEventClick(event.id)
                                    onDismissRequest() 
                                }),
                            color = if (isPressed) {
                                eventTypeInfo.backgroundColor.copy(alpha = 0.8f)
                            } else {
                                eventTypeInfo.backgroundColor.copy(alpha = 0.5f)
                            },
                            shape = MaterialTheme.shapes.medium
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Event icon with event-specific color
                                Icon(
                                    imageVector = eventTypeInfo.icon,
                                    contentDescription = null,
                                    tint = eventTypeInfo.iconTint,
                                    modifier = Modifier.size(28.dp)
                                )
                                
                                Spacer(modifier = Modifier.width(12.dp))
                                
                                Column(modifier = Modifier.weight(1f)) {
                                    // Event title
                                    Text(
                                        text = event.title ?: "Event",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    // Event details
                                    if (!event.description.isNullOrBlank()) {
                                        Text(
                                            text = event.description,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    
                                    // Event time range
                                    val startTime = event.startTime?.toDate()?.toInstant()
                                        ?.atZone(ZoneId.systemDefault())?.toLocalTime()
                                    val endTime = event.endTime?.toDate()?.toInstant()
                                        ?.atZone(ZoneId.systemDefault())?.toLocalTime()

                                    if (startTime != null && endTime != null) {
                                        Text(
                                            text = "${
                                                startTime.format(
                                                    DateTimeFormatter.ofPattern("h:mm a")
                                                )
                                            } - ${
                                                endTime.format(
                                                    DateTimeFormatter.ofPattern("h:mm a")
                                                )
                                            }",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    
                                    // Optional location
                                    if (event.location?.isNotBlank() == true) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(top = 4.dp)
                                        ) {
                                            Icon(
                                                Icons.Rounded.LocationOn,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp),
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = event.location,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = Color(0xFFF48FB1)
                        )
                    ) {
                        Text("Close")
                    }
                    
                    Button(
                        onClick = onDismissRequest,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF48FB1)
                        ),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("View All")
                    }
                }
            }
        }
    }
}
