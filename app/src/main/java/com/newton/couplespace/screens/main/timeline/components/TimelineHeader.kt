package com.newton.couplespace.screens.main.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.TimeZone

/**
 * A header for the couple timeline display matching the design in the image
 * with time zone display and profile pictures
 */
@Composable
fun TimelineHeader(
    date: LocalDate,
    onDateChange: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
    userTimeZone: TimeZone = TimeZone.getDefault(),
    partnerTimeZone: TimeZone? = null,
    partnerProfilePic: String? = null,
    userProfilePic: String? = null,
    onAddEvent: () -> Unit = {}
) {
    val isPaired = partnerTimeZone != null
    val userZoneId = userTimeZone.toZoneId()
    val partnerZoneId = partnerTimeZone?.toZoneId() ?: userZoneId
    
    // Get current time in each timezone
    val userTime = LocalTime.now(userZoneId)
    val partnerTime = LocalTime.now(partnerZoneId)
    
    // Convert the selected date to each timezone
    // For the user timezone, we can use the date directly
    val userDate = date
    
    // For the partner timezone, we need to convert from user timezone to partner timezone
    // First, create a ZonedDateTime from the date in user timezone
    val userZonedDateTime = date.atStartOfDay(userZoneId)
    
    // Then convert to partner timezone while keeping the same instant
    val partnerZonedDateTime = userZonedDateTime.withZoneSameInstant(partnerZoneId)
    
    // Finally, extract the local date in partner timezone
    val partnerDate = partnerZonedDateTime.toLocalDate()
    
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Profile pictures and add event row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // User profile picture
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    if (userProfilePic != null) {
                        AsyncImage(
                            model = userProfilePic,
                            contentDescription = "Your Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        // Placeholder for user profile
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "You",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                
                // Date navigation
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.weight(1f)
                ) {
                    IconButton(onClick = { onDateChange(date.minusDays(1)) }) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "Previous Day")
                    }
                    
                    Text(
                        text = date.format(DateTimeFormatter.ofPattern("MMM d")),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    
                    IconButton(onClick = { onDateChange(date.plusDays(1)) }) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "Next Day")
                    }
                }
                
                // Partner profile picture or add button
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    if (partnerProfilePic != null) {
                        AsyncImage(
                            model = partnerProfilePic,
                            contentDescription = "Partner Profile",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        // Add event button using FAB style
                        FloatingActionButton(
                            onClick = onAddEvent,
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(40.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add Event",
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
            
            // Time zone display
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Top
            ) {
                // User time - left side
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.Start
                ) {
                    // Display the selected date in user timezone
                    Text(
                        text = userDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")), 
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                    
                    Text(
                        text = userTime.format(DateTimeFormatter.ofPattern("h:mm a")),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = Color.DarkGray
                    )
                    
                    Text(
                        text = userTimeZone.displayName.substringAfterLast('/').replace('_', ' '),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF9E9E9E)
                    )
                }
                
                // Connected status in middle
                if (isPaired) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
                        )
                        
                        Text(
                            text = "Connected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                
                // Partner time - right side
                if (isPaired) {
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Display the selected date in partner timezone
                        Text(
                            text = partnerDate.format(DateTimeFormatter.ofPattern("EEEE, MMM d")), 
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                        
                        Text(
                            text = partnerTime.format(DateTimeFormatter.ofPattern("h:mm a")),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.DarkGray
                        )
                        
                        Text(
                            text = partnerTimeZone?.displayName?.substringAfterLast('/')?.replace('_', ' ') ?: "Unknown",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }
            }
        }
    }
}
