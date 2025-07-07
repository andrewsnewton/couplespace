package com.newton.couplespace.screens.main.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.util.Log
import com.newton.couplespace.models.TimelineEvent
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.TimeZone

/**
 * Data class to represent a time of day with events for both user and partner
 * Note: We store the original hour/minute as entered, not converted between timezones
 */
data class TimeOfDay(
    val hour: Int = 0,
    val minute: Int = 0,
    val userEvent: String = "",
    val partnerEvent: String = ""
)

/**
 * A row in the timeline displaying events for both user and partner
 */
@Composable
fun TimelineRow(
    time: String,
    userEventName: String,
    partnerEventName: String,
    onUserEventClick: () -> Unit = {},
    onPartnerEventClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // User's event
        if (userEventName.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFFFF9C4) // Light yellow
                ),
                onClick = onUserEventClick
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userEventName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
        
        // Time in center
        Box(
            modifier = Modifier.width(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFC8389))
            )
            
            // Display time below the dot
            Text(
                text = time,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        
        // Partner's event
        if (partnerEventName.isNotEmpty()) {
            Card(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFE1F5FE) // Light blue
                ),
                onClick = onPartnerEventClick
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = partnerEventName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Black
                    )
                }
            }
        } else {
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}

/**
 * Main timeline view component that displays events for both the user and their partner side by side.
 * Events are displayed at their original input times without timezone conversion for display.
 */
@Composable
fun CoupleTimelineView(
    date: LocalDate = LocalDate.now(),
    userEvents: List<TimelineEvent> = emptyList(),
    partnerEvents: List<TimelineEvent> = emptyList(),
    userTimeZone: TimeZone = TimeZone.getDefault(),
    partnerTimeZone: TimeZone? = null, // Null if partner is not paired
    onEventClick: (String) -> Unit = {},
    isPaired: Boolean = false,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    
    // Current time for both timezones
    val currentUserTime = LocalTime.now(ZoneId.of(userTimeZone.id))
    val currentPartnerTime = if (partnerTimeZone != null) {
        LocalTime.now(ZoneId.of(partnerTimeZone.id))
    } else {
        null
    }
    
    // Get timezone abbreviations
    val userTimeZoneAbbr = ZoneId.of(userTimeZone.id).getDisplayName(TextStyle.SHORT, Locale.getDefault())
    val partnerTimeZoneAbbr = partnerTimeZone?.let { 
        ZoneId.of(it.id).getDisplayName(TextStyle.SHORT, Locale.getDefault())
    } ?: ""
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(8.dp)
    ) {
        // Header with app name and date
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(Color(0xFFFAE5EC), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "CS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF88AA)
                    )
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Column {
                    Text(
                        text = "Timeline",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("EEEE, MMM d")),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Add button would go here (not implemented yet)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFFAE5EC), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF88AA)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Time display section
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // User's time column
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Your Time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = currentUserTime.format(DateTimeFormatter.ofPattern("h:mm a")),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = userTimeZoneAbbr,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Connected indicator
            Box(
                modifier = Modifier.padding(top = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isPaired) Color.Green else Color.Gray)
                    )
                    
                    Spacer(modifier = Modifier.width(4.dp))
                    
                    Text(
                        text = if (isPaired) "Connected" else "Not Connected",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            // Partner's time column
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "Partner's Time",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (isPaired && currentPartnerTime != null) 
                        currentPartnerTime.format(DateTimeFormatter.ofPattern("h:mm a")) 
                    else 
                        "",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = if (isPaired) partnerTimeZoneAbbr else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Timeline section with events on both sides
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            // Center divider with time dots
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
                    .background(Color(0xFFFC8389))
                    .align(Alignment.Center)
            )

            // Process user and partner events to TimeOfDay objects
            val timelineEvents = remember(userEvents, partnerEvents) {
                // Map of hour to TimeOfDay objects for consistent hourly slots
                val eventMap = mutableMapOf<Int, TimeOfDay>()
                
                Log.d("CoupleTimelineView", "Processing ${userEvents.size} user events and ${partnerEvents.size} partner events")
                Log.d("CoupleTimelineView", "User timezone: ${userTimeZone.id}, Partner timezone: ${partnerTimeZone?.id ?: "null"}")
                
                // Process user events - use the original input time
                userEvents.forEach { event ->
                    if (event.startTime != null) {
                        // Get the time as entered (in user's timezone)
                        val startTime = event.startTime.toDate().toInstant()
                            .atZone(ZoneId.of(userTimeZone.id)).toLocalTime()
                        val hour = startTime.hour
                        val minute = startTime.minute
                        
                        Log.d("CoupleTimelineView", "User event '${event.title}' at $hour:$minute")
                        
                        // Create or update TimeOfDay
                        val existing = eventMap[hour] ?: TimeOfDay(
                            hour = hour,
                            minute = minute
                        )
                        eventMap[hour] = existing.copy(userEvent = event.title)
                    }
                }
                
                // Process partner events if paired
                if (isPaired) {
                    partnerEvents.forEach { event ->
                        if (event.startTime != null) {
                            // Get the time as entered (in partner's timezone)
                            val partnerTime = event.startTime.toDate().toInstant()
                                .atZone(ZoneId.of(partnerTimeZone?.id ?: userTimeZone.id)).toLocalTime()
                            val hour = partnerTime.hour
                            val minute = partnerTime.minute
                            
                            Log.d("CoupleTimelineView", "Partner event '${event.title}' at $hour:$minute")
                            
                            // Create or update TimeOfDay - use the same hour for organizing the timeline
                            val existing = eventMap[hour] ?: TimeOfDay(
                                hour = hour,
                                minute = minute
                            )
                            
                            // This keeps the existing user event if any and adds the partner event
                            eventMap[hour] = existing.copy(
                                partnerEvent = event.title
                            )
                        }
                    }
                } else {
                    Log.d("CoupleTimelineView", "Skipping partner events processing: isPaired=$isPaired")
                }
                
                // Return the sorted map values as a list
                eventMap.entries.sortedBy { it.key }.map { it.value }
            }
            
            // Format time strings for display
            val formatter = DateTimeFormatter.ofPattern("h:mm a")
            val userTimeStr = currentUserTime.format(formatter)
            val partnerTimeStr = currentPartnerTime?.format(formatter) ?: ""
            
            // User's side shows time in user timezone
            val userTimeDisplay = "$userTimeStr $userTimeZoneAbbr"
            
            // Partner's side shows time in partner timezone
            val partnerTimeDisplay = if (isPaired && partnerTimeZone != null) {
                "$partnerTimeStr $partnerTimeZoneAbbr"
            } else {
                ""
            }

            // Display each timeline row
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                timelineEvents.forEach { timeOfDay ->
                    // Format the time for display
                    val timeStr = String.format("%02d:%02d", timeOfDay.hour, timeOfDay.minute)
                    
                    // Display the timeline row
                    TimelineRow(
                        time = timeStr,
                        userEventName = timeOfDay.userEvent,
                        partnerEventName = timeOfDay.partnerEvent,
                        onUserEventClick = { if (timeOfDay.userEvent.isNotEmpty()) onEventClick(timeOfDay.userEvent) },
                        onPartnerEventClick = { if (timeOfDay.partnerEvent.isNotEmpty()) onEventClick(timeOfDay.partnerEvent) }
                    )
                }
            }
        }
    }
}