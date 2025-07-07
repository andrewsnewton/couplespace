package com.newton.couplespace.screens.main.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.newton.couplespace.models.TimelineEvent
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A timeline event card with pastel styling to match the design shown in the image.
 */
@Composable
fun TimelineEventCard(
    event: TimelineEvent,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isUserEvent: Boolean = true,
    displayDate: LocalDate? = null,
    sourceTimezone: ZoneId? = null,
    displayTimezone: ZoneId? = null
) {
    // Event type-based styling
    val eventInfo = getEventTypeInfo(event.eventType?.name ?: "")
    
    // Create soft pastel color set based on event type
    val cardBackground = eventInfo.backgroundColor
    val borderColor = eventInfo.borderColor
    val iconTint = eventInfo.iconTint
    
    // Create a card with the pastel styling
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = cardBackground,
        modifier = modifier
            .padding(
                start = if (isUserEvent) 8.dp else 4.dp,
                end = if (isUserEvent) 4.dp else 8.dp,
                top = 4.dp,
                bottom = 4.dp
            )
            .fillMaxWidth(0.9f) // Slightly smaller than full width
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable(onClick = onClick)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            // Icon on the left
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .background(Color.White.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = eventInfo.icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }
            
            Spacer(modifier = Modifier.width(8.dp))
            
            // Event details in column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // Event title
                Text(
                    text = event.title ?: "Untitled Event",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.DarkGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Show timezone indicator if event crosses date boundaries
                if (displayDate != null && sourceTimezone != null && displayTimezone != null) {
                    val eventDate = event.startTime.toDate().toInstant().atZone(sourceTimezone).toLocalDate()
                    val showsOnDifferentDate = eventDate != displayDate
                    
                    if (showsOnDifferentDate) {
                        val eventDateFormatted = eventDate.format(DateTimeFormatter.ofPattern("MMM d"))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Start,
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            // Timezone icon
                            Icon(
                                imageVector = Icons.Outlined.AccessTime,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            // Date text
                            Text(
                                text = "From $eventDateFormatted",
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.primary,
                                maxLines = 1
                            )
                        }
                    }
                }
                
                // Format time range using the event's source timezone
                val sourceTimezoneId = when {
                    event.sourceTimezone.isNotEmpty() -> event.sourceTimezone
                    event.metadata["sourceTimezone"] is String -> event.metadata["sourceTimezone"] as String
                    else -> ZoneId.systemDefault().id
                }
                
                val eventZoneId = try {
                    ZoneId.of(sourceTimezoneId)
                } catch (e: Exception) {
                    ZoneId.systemDefault()
                }
                
                val startTimeString = event.startTime?.toDate()?.toInstant()
                    ?.atZone(eventZoneId)?.toLocalTime()
                    ?.format(DateTimeFormatter.ofPattern("h:mm")) ?: ""
                
                val endTimeString = event.endTime?.toDate()?.toInstant()
                    ?.atZone(eventZoneId)?.toLocalTime()
                    ?.format(DateTimeFormatter.ofPattern("h:mm a")) ?: ""
                
                Text(
                    text = "$startTimeString - $endTimeString",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                // Description if available (optional)
                event.description?.let {
                    if (it.isNotBlank()) {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Event styling information class
 */
data class EventTypeInfo(
    val backgroundColor: Color,
    val borderColor: Color,
    val iconTint: Color,
    val icon: ImageVector
)
/**
 * Returns styling information based on the event type.
 */
@Composable
fun getEventTypeInfo(eventType: String): EventTypeInfo {
    return when(eventType.lowercase()) {
        "work", "meeting" -> EventTypeInfo(
            backgroundColor = Color(0xFFF3E5F5), // Light purple
            borderColor = Color(0xFFCE93D8).copy(alpha = 0.5f),
            iconTint = Color(0xFF9C27B0),
            icon = Icons.Outlined.Work
        )
        "meal", "food", "coffee", "lunch", "breakfast", "dinner" -> EventTypeInfo(
            backgroundColor = Color(0xFFE3F2FD), // Light blue
            borderColor = Color(0xFF90CAF9).copy(alpha = 0.5f),
            iconTint = Color(0xFF2196F3),
            icon = Icons.Outlined.Restaurant
        )
        "exercise", "workout", "fitness" -> EventTypeInfo(
            backgroundColor = Color(0xFFE8F5E9), // Light green
            borderColor = Color(0xFFA5D6A7).copy(alpha = 0.5f),
            iconTint = Color(0xFF4CAF50),
            icon = Icons.Outlined.FitnessCenter
        )
        "travel" -> EventTypeInfo(
            backgroundColor = Color(0xFFFFF3E0), // Light orange
            borderColor = Color(0xFFFFCC80).copy(alpha = 0.5f),
            iconTint = Color(0xFFFF9800),
            icon = Icons.Outlined.Flight
        )
        "medical", "doctor", "health" -> EventTypeInfo(
            backgroundColor = Color(0xFFFFEBEE), // Light red
            borderColor = Color(0xFFEF9A9A).copy(alpha = 0.5f),
            iconTint = Color(0xFFE57373),
            icon = Icons.Outlined.MedicalServices
        )
        "sleep" -> EventTypeInfo(
            backgroundColor = Color(0xFFE0F7FA), // Light cyan
            borderColor = Color(0xFF80DEEA).copy(alpha = 0.5f),
            iconTint = Color(0xFF00BCD4),
            icon = Icons.Outlined.Bedtime
        )
        "entertainment", "fun", "movie" -> EventTypeInfo(
            backgroundColor = Color(0xFFFCE4EC), // Light pink
            borderColor = Color(0xFFF48FB1).copy(alpha = 0.5f),
            iconTint = Color(0xFFEC407A),
            icon = Icons.Outlined.Theaters
        )
        "study", "learning", "education" -> EventTypeInfo(
            backgroundColor = Color(0xFFF1F8E9), // Light lime
            borderColor = Color(0xFFDCE775).copy(alpha = 0.5f),
            iconTint = Color(0xFF8BC34A),
            icon = Icons.Outlined.School
        )
        "family", "kids" -> EventTypeInfo(
            backgroundColor = Color(0xFFFFECB3), // Light amber
            borderColor = Color(0xFFFFC107).copy(alpha = 0.3f),
            iconTint = Color(0xFFFFA000),
            icon = Icons.Outlined.People
        )
        "shopping" -> EventTypeInfo(
            backgroundColor = Color(0xFFEAE1F9), // Light deep purple
            borderColor = Color(0xFFB388FF).copy(alpha = 0.3f),
            iconTint = Color(0xFF673AB7),
            icon = Icons.Outlined.ShoppingCart
        )
        else -> EventTypeInfo(
            backgroundColor = Color(0xFFEEEEEE), // Light gray
            borderColor = Color(0xFFBDBDBD).copy(alpha = 0.5f),
            iconTint = Color(0xFF757575),
            icon = Icons.Outlined.Event
        )
    }
}

/**
 * Extension properties for TimelineEvent to provide UI-specific formatting
 */
val TimelineEvent.formattedTime: String?
    get() {
        if (startTime == null) return null
        
        // Step 1: Get the source timezone where the event was created
        val sourceTimezoneId = when {
            sourceTimezone.isNotEmpty() -> sourceTimezone
            metadata["sourceTimezone"] is String -> metadata["sourceTimezone"] as String
            else -> java.time.ZoneId.systemDefault().id
        }
        
        // Step 2: Parse the source timezone, falling back to system default if invalid
        val eventZoneId = try {
            java.time.ZoneId.of(sourceTimezoneId)
        } catch (e: Exception) {
            android.util.Log.w("TimelineEventCard", "Invalid sourceTimezone: $sourceTimezoneId, using system default", e)
            java.time.ZoneId.systemDefault()
        }
        
        // Step 3: Get the event's UTC instant
        val eventStartInstant = startTime.toDate().toInstant()
        
        // Step 4: Convert the UTC instant to the event's source timezone
        val eventLocalTime = eventStartInstant.atZone(eventZoneId).toLocalTime()
        
        // Step 5: Format the time in 12-hour format with AM/PM
        return java.time.format.DateTimeFormatter.ofPattern("h:mm a").format(eventLocalTime)
    }
    
val TimelineEvent.icon: ImageVector?
    get() = null // Override this to provide custom icons based on event type
