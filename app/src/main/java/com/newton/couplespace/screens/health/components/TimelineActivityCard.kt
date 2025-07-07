package com.newton.couplespace.screens.health.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.newton.couplespace.models.TimelineEvent
import com.newton.couplespace.screens.main.timeline.components.SplitTimelineView
import java.time.LocalDate
import java.time.ZoneId
import java.util.TimeZone

/**
 * A Material 3 card that displays the activity timeline for the Health screen.
 * Integrates the split timeline view for user and partner activities.
 */
@Composable
fun TimelineActivityCard(
    date: LocalDate,
    events: List<TimelineEvent>,
    partnerEvents: List<TimelineEvent>,
    onEventClick: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    userTimeZone: TimeZone = TimeZone.getDefault(),
    partnerTimeZone: TimeZone? = null,
    expanded: Boolean = false,
    onExpandChange: (Boolean) -> Unit = {}
) {
    var isExpanded by remember { mutableStateOf(expanded) }
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Card Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Text(
                    text = "Activity Timeline",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.weight(1f))
                
                IconButton(onClick = { 
                    isExpanded = !isExpanded
                    onExpandChange(isExpanded)
                }) {
                    Icon(
                        imageVector = if (isExpanded) 
                            Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                        contentDescription = if (isExpanded) 
                            "Show less" else "Show more"
                    )
                }
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(500.dp) // Fixed height for the timeline
                ) {
                    SplitTimelineView(
                        date = date,
                        events = events,
                        partnerEvents = partnerEvents,
                        onEventClick = onEventClick,
                        onDateChange = onDateChange,
                        onAddEvent = {},
                        onAddEventWithTime = { _, _ -> },
                        isPaired = partnerTimeZone != null,
                        userTimeZone = userTimeZone,
                        partnerTimeZone = partnerTimeZone,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            if (!isExpanded) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Tap to view timeline",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
