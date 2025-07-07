package com.newton.couplespace.screens.main.timeline.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import android.util.Log
import com.google.firebase.Timestamp
import com.newton.couplespace.models.TimelineEvent
import com.newton.couplespace.screens.main.timeline.TimelineUtils
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.TimeZone

/**
 * A split timeline view that displays events for user and partner side by side
 * with a central timeline axis.
 */
@Composable
fun SplitTimelineView(
    date: LocalDate,
    events: List<TimelineEvent>,
    partnerEvents: List<TimelineEvent> = emptyList(),
    onEventClick: (String) -> Unit,
    onDateChange: (LocalDate) -> Unit,
    onAddEvent: () -> Unit,
    onAddEventWithTime: (Boolean, LocalTime) -> Unit = { _, _ -> },
    isPaired: Boolean = false,
    userTimeZone: TimeZone = TimeZone.getDefault(),
    partnerTimeZone: TimeZone? = null,
    userProfilePic: String? = null,
    partnerProfilePic: String? = null,
    modifier: Modifier = Modifier
) {
    // Scroll state for vertical scrolling timeline
    val scrollState = rememberScrollState()

    // Current time in user's timezone for scrolling to current hour and indicator
    val userZoneId = ZoneId.of(userTimeZone.id)
    val currentTime = LocalTime.now(userZoneId)
    val currentHour = currentTime.hour

    // Current user id from Firebase auth (to distinguish user vs partner events)
    val currentUserId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Partner ZoneId fallback
    val partnerZoneId = partnerTimeZone?.let { ZoneId.of(it.id) }

    // Filter events for the selected date and separate user vs partner events
    val userEventsForDate = events.filter { event ->
        if (event.startTime == null) {
            Log.d("SplitTimelineView", "User Event filtered: Missing start time")
            return@filter false
        }

        val sourceTimezoneId = when {
            event.sourceTimezone.isNotEmpty() -> event.sourceTimezone
            event.metadata["sourceTimezone"] is String -> event.metadata["sourceTimezone"] as String
            else -> userTimeZone.id
        }

        // Verify userTimeZone matches event timezone
        Log.d("SplitTimelineView", "User Event timezone check: ${sourceTimezoneId} vs ${userTimeZone.id}")
        
        // Use centralized TimelineUtils for date matching
        val shouldInclude = TimelineUtils.shouldEventAppearOnDate(
            event = event,
            selectedDate = date,
            userZoneId = userZoneId,
            eventSourceTimezoneId = sourceTimezoneId,
            currentUserId = currentUserId
        )
        
        // Verify currentUserId == event.userId
        val userIdCheck = event.userId == currentUserId
        Log.d("SplitTimelineView", "User Event ID check: $userIdCheck")
        
        Log.d("SplitTimelineView", "User Event final decision: $shouldInclude")
        
        shouldInclude
    }


    val partnerEventsForDate = partnerEvents.filter { event ->
        if (event.startTime == null) {
            Log.d("SplitTimelineView", "Partner Event ${event.title} FILTERED: Missing start time")
            return@filter false
        }

        // Determine if this is a partner event
        // Either it belongs to the partner (userId != currentUserId)
        // Or it was created by the current user but marked for the partner (isForPartner = true)
        val isPartnerEvent = event.userId != currentUserId
        val isForPartner = event.metadata["isForPartner"] as? Boolean == true
        
        // Get the correct timezone for the event
        val sourceTimezoneId = when {
            event.sourceTimezone.isNotEmpty() -> event.sourceTimezone
            event.metadata["sourceTimezone"] is String -> event.metadata["sourceTimezone"] as String
            else -> partnerTimeZone?.id ?: userTimeZone.id
        }

        // Log timezone information for debugging
        val partnerZoneId = partnerTimeZone?.let { ZoneId.of(it.id) }
        Log.d("SplitTimelineView", "Partner Event ${event.title}: sourceTimezone=${sourceTimezoneId}, partnerTimezone=${partnerZoneId?.id}")
        Log.d("SplitTimelineView", "Partner Event ${event.title}: userId=${event.userId}, currentUserId=$currentUserId, isPartnerEvent=$isPartnerEvent, isForPartner=$isForPartner")
        
        // Use the partner's timezone for date matching
        val shouldAppear = TimelineUtils.shouldEventAppearOnDate(
            event = event,
            selectedDate = date,
            userZoneId = partnerZoneId ?: userZoneId, // Use partner timezone if available
            eventSourceTimezoneId = sourceTimezoneId,
            currentUserId = currentUserId,
            isUserEvent = false, // This is a partner event
            skipUserIdCheck = true // Skip the built-in user ID check since we're handling it here
        )
        
        Log.d("SplitTimelineView", "Partner Event ${event.title} should appear on date: $shouldAppear")
        
        // Include if it's a partner event or marked for partner and should appear on this date
        val shouldInclude = (isPartnerEvent || isForPartner) && shouldAppear
        Log.d("SplitTimelineView", "Partner Event ${event.title} final decision: $shouldInclude")
        
        shouldInclude
}

    // Convert 120dp per hour to pixels for scrolling
    val density = LocalDensity.current
    val pixelsPerHour = remember(density) { with(density) { 120.dp.toPx() } }

    // Scroll to approx 2 hours before current time for context on initial load
    LaunchedEffect(Unit) {
        val scrollToHour = (currentHour - 2).coerceAtLeast(0)
        val scrollPosition = (scrollToHour * pixelsPerHour).toInt()
        scrollState.animateScrollTo(scrollPosition)
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Header with date, timezones, profile pics, add button, etc
        TimelineHeader(
            date = date,
            onDateChange = onDateChange,
            userTimeZone = userTimeZone,
            partnerTimeZone = partnerTimeZone,
            partnerProfilePic = partnerProfilePic,
            userProfilePic = userProfilePic,
            onAddEvent = onAddEvent,
            modifier = Modifier.fillMaxWidth()
        )

        // Main scrollable timeline area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(scrollState)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // For each hour 0..23 render a TimeSlot
                for (hour in 0..23) {
                    TimeSlot(
                        hour = hour,
                        userEvents = userEventsForDate.filter { event ->
                            isEventOverlappingHour(event, date, hour, userZoneId, userTimeZone)
                        },
                        partnerEvents = partnerEventsForDate.filter { event ->
                            // CRITICAL FIX: For partner events in the right side of the timeline,
                            // we need to convert the hour from the user's timezone to the partner's timezone
                            // and then check if the event overlaps with that hour in the partner's timezone
                            val partnerZoneIdSafe = partnerZoneId ?: userZoneId
                            val partnerTimeZoneSafe = partnerTimeZone ?: userTimeZone
                            
                            // Convert the user hour to the equivalent hour in partner timezone
                            val userDateTime = date.atTime(hour, 0).atZone(userZoneId)
                            val partnerDateTime = userDateTime.withZoneSameInstant(partnerZoneIdSafe)
                            val partnerHour = partnerDateTime.hour
                            val partnerDate = partnerDateTime.toLocalDate()
                            
                            Log.d("SplitTimelineView", "Partner event ${event.title} check: User hour=$hour -> Partner hour=$partnerHour, Partner date=$partnerDate")
                            
                            // Check if the event overlaps with the converted hour in partner timezone
                            isEventOverlappingHour(event, partnerDate, partnerHour, partnerZoneIdSafe, partnerTimeZoneSafe)
                        },
                        onEventClick = onEventClick,
                        onAddEventWithTime = { isForPartner, time ->
                            onAddEventWithTime(isForPartner, time)
                        },
                        isPaired = isPaired,
                        isCurrentHour = hour == currentHour,
                        modifier = Modifier.fillMaxWidth(),
                        userTimeZone = userTimeZone,
                        partnerTimeZone = partnerTimeZone,
                        date = date
                    )
                }
                Spacer(modifier = Modifier.height(120.dp))
            }
        }
    }
}

/**
 * Check if the event overlaps with a given hour slot on the selected date,
 * considering timezone conversions and event start/end times.
 */
private fun isEventOverlappingHour(
    event: TimelineEvent,
    date: LocalDate,
    hour: Int,
    userZoneId: ZoneId,
    timezone: TimeZone
): Boolean {
    if (event.startTime == null) return false

    // Get the source timezone of the event (where it was created)
    val sourceTimezoneId = when {
        event.sourceTimezone.isNotEmpty() -> event.sourceTimezone
        event.metadata["sourceTimezone"] is String -> event.metadata["sourceTimezone"] as String
        else -> timezone.id
    }

    // Get the display timezone (user or partner timezone)
    val displayZoneId = ZoneId.of(timezone.id)

    // Get the ZoneId from sourceTimezone (fallback to display timezone if needed)
    val eventZoneId = runCatching { ZoneId.of(sourceTimezoneId) }.getOrElse {
        Log.w("SplitTimelineView", "Invalid sourceTimezone: $sourceTimezoneId, fallback to ${timezone.id}", it)
        displayZoneId
    }

    Log.d("SplitTimelineView", "Event ${event.title} hour overlap check: sourceTimezone=$sourceTimezoneId, displayTimezone=${timezone.id}, hour=$hour")

    return isEventOverlappingHour(
        event = event,
        date = date,
        hour = hour,
        sourceTimezoneId = eventZoneId,
        displayZoneId = displayZoneId
    )
}

/**
 * Check if the event overlaps with a given hour slot on the selected date,
 * considering timezone conversions and event start/end times.
 */
private fun isEventOverlappingHour(
    event: TimelineEvent,
    date: LocalDate,
    hour: Int,
    sourceTimezoneId: ZoneId,
    displayZoneId: ZoneId
): Boolean {
    Log.d("SplitTimelineView", "Event ${event.title} hour overlap check: sourceTimezone=$sourceTimezoneId, displayTimezone=$displayZoneId, hour=$hour")

    // Get event start and end as Instants (timezone-independent)
    val eventStartInstant = event.startTime.toDate().toInstant()
    val eventEndInstant = event.endTime?.toDate()?.toInstant() ?: eventStartInstant.plusSeconds(3600)

    // First, check if the event should appear on this date using the same logic as TimelineUtils.shouldEventAppearOnDate
    val selectedDateInEventTz = date.atStartOfDay(displayZoneId)
        .withZoneSameInstant(sourceTimezoneId).toLocalDate()

    val eventStartDateInSourceTz = event.startTime.toDate().toInstant().atZone(sourceTimezoneId).toLocalDate()
    val eventEndDateInSourceTz = event.endTime?.toDate()?.toInstant()?.atZone(sourceTimezoneId)?.toLocalDate() ?: eventStartDateInSourceTz

    // Check if the event should appear on this date based on timezone conversion
    val nextDayInEventTz = selectedDateInEventTz.plusDays(1)
    val prevDayInEventTz = selectedDateInEventTz.minusDays(1)
    val twoDaysAheadInEventTz = selectedDateInEventTz.plusDays(2)
    val twoDaysBehindInEventTz = selectedDateInEventTz.minusDays(2)

    val tzAwareDateMatches = eventStartDateInSourceTz == selectedDateInEventTz ||
        eventEndDateInSourceTz == selectedDateInEventTz ||
        (eventStartDateInSourceTz.isBefore(selectedDateInEventTz) && eventEndDateInSourceTz.isAfter(selectedDateInEventTz)) ||
        eventStartDateInSourceTz == nextDayInEventTz ||
        eventStartDateInSourceTz == prevDayInEventTz ||
        eventStartDateInSourceTz == twoDaysAheadInEventTz ||
        eventStartDateInSourceTz == twoDaysBehindInEventTz

    if (!tzAwareDateMatches) {
        Log.d("SplitTimelineView", "Event ${event.title} does not match date after timezone conversion")
        return false
    }

    // Now create hour slots for all possible dates the event might appear on
    val possibleDates = listOf(
        date, // The selected date
        date.plusDays(1), // Next day
        date.minusDays(1) // Previous day
    )

    // Check if the event overlaps with the given hour on any of the possible dates
    for (slotDate in possibleDates) {
        // Create the hour slot boundaries for this date
        val slotStartDateTime = LocalDateTime.of(slotDate, LocalTime.of(hour, 0))
        val slotEndDateTime = LocalDateTime.of(slotDate, LocalTime.of(hour, 59, 59))

        // Convert slot times to Instant for accurate timezone-aware comparison
        val slotStartInstant = slotStartDateTime.atZone(displayZoneId).toInstant()
        val slotEndInstant = slotEndDateTime.atZone(displayZoneId).toInstant()

        // Check if the event overlaps with this hour slot using timezone-aware Instants
        val overlaps = (eventStartInstant.isBefore(slotEndInstant) || eventStartInstant.equals(slotEndInstant)) &&
            (eventEndInstant.isAfter(slotStartInstant) || eventEndInstant.equals(slotStartInstant))

        // Add detailed logging for debugging
        Log.d("SplitTimelineView", "Event ${event.title} overlap check for date $slotDate, hour $hour:")
        Log.d("SplitTimelineView", "  Event start: ${eventStartInstant}, Event end: ${eventEndInstant}")
        Log.d("SplitTimelineView", "  Slot start: ${slotStartInstant}, Slot end: ${slotEndInstant}")
        Log.d("SplitTimelineView", "  Overlap result: $overlaps")

        if (overlaps) {
            Log.d("SplitTimelineView", "Event ${event.title} OVERLAPS with hour $hour on date $slotDate")
            return true
        }
    }
    
    Log.d("SplitTimelineView", "Event ${event.title} does NOT overlap with hour $hour on any relevant date")
    return false
}


/**
 * Represents a single hour slot in the timeline with events on both sides.
 */
@Composable
private fun TimeSlot(
    hour: Int,
    userEvents: List<TimelineEvent>,
    partnerEvents: List<TimelineEvent>,
    onEventClick: (String) -> Unit,
    onAddEventWithTime: (Boolean, LocalTime) -> Unit,
    isPaired: Boolean,
    isCurrentHour: Boolean,
    modifier: Modifier = Modifier,
    userTimeZone: TimeZone,
    partnerTimeZone: TimeZone?,
    date: LocalDate
) {
    var selectedSide by remember { mutableStateOf<String?>(null) }

    val userZoneId = ZoneId.of(userTimeZone.id)
    val formattedUserTime = LocalTime.of(hour, 0).format(DateTimeFormatter.ofPattern("h a"))

    val formattedPartnerTime = partnerTimeZone?.let {
        val partnerZoneId = ZoneId.of(it.id)
        val userDateTime = date.atTime(hour, 0).atZone(userZoneId)
        val partnerDateTime = userDateTime.withZoneSameInstant(partnerZoneId)
        val timeFormatter = if (partnerDateTime.toLocalTime().minute != 0) {
            DateTimeFormatter.ofPattern("h:mm a")
        } else {
            DateTimeFormatter.ofPattern("h a")
        }
        partnerDateTime.toLocalTime().format(timeFormatter)
    } ?: ""

    val rowBackground = when {
        selectedSide == "user" || selectedSide == "partner" -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
        else -> Color.Transparent
    }

    val handleUserSlotClick = {
        if (selectedSide == "user") {
            onAddEventWithTime(false, LocalTime.of(hour, 0))
            selectedSide = null
        } else {
            selectedSide = "user"
        }
    }

    val handlePartnerSlotClick = {
        if (selectedSide == "partner") {
            // Convert the time from user timezone to partner timezone
            val partnerTime = if (partnerTimeZone != null) {
                val partnerZoneId = ZoneId.of(partnerTimeZone.id)
                val userDateTime = date.atTime(hour, 0).atZone(userZoneId)
                val partnerDateTime = userDateTime.withZoneSameInstant(partnerZoneId)
                partnerDateTime.toLocalTime()
            } else {
                LocalTime.of(hour, 0)
            }
            
            // Log the timezone conversion for debugging
            Log.d("SplitTimelineView", "Adding partner event: User time $hour:00 -> Partner time ${partnerTime.hour}:${partnerTime.minute}")
            
            onAddEventWithTime(true, partnerTime)
            selectedSide = null
        } else {
            selectedSide = "partner"
        }
    }

    Row(
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(vertical = 4.dp)
            .background(rowBackground)
    ) {
        // User events side
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(end = 8.dp)
                .fillMaxHeight()
                .clickable(enabled = userEvents.isEmpty()) {
                    handleUserSlotClick()
                },
            contentAlignment = Alignment.Center
        ) {
            if (userEvents.isEmpty()) {
                if (selectedSide == "user") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(40.dp)
                            .padding(4.dp)
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Tap again to add",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    userEvents.forEach { event ->
                        TimelineEventCard(
                            event = event,
                            onClick = { onEventClick(event.id) },
                            isUserEvent = true,
                            modifier = Modifier.fillMaxWidth(),
                            displayDate = date,
                            sourceTimezone = ZoneId.of(when {
                                event.sourceTimezone.isNotEmpty() -> event.sourceTimezone
                                event.metadata["sourceTimezone"] is String -> event.metadata["sourceTimezone"] as String
                                else -> userTimeZone.id
                            }),
                            displayTimezone = ZoneId.of(userTimeZone.id)
                        )
                    }
                }
            }
        }

        // Center timeline with time labels and marker
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.width(IntrinsicSize.Min)
        ) {
            if (isPaired) {
                Box(modifier = Modifier.width(60.dp)) {
                    Text(
                        text = formattedUserTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrentHour) MaterialTheme.colorScheme.primary else Color.Gray,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(IntrinsicSize.Min)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCurrentHour) MaterialTheme.colorScheme.primary
                                else Color.LightGray.copy(alpha = 0.5f)
                            )
                    )
                }

                if (!isPaired) {
                    Text(
                        text = formattedUserTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrentHour) MaterialTheme.colorScheme.primary else Color.Gray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }

                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(70.dp)
                        .background(
                            if (isCurrentHour) MaterialTheme.colorScheme.primary
                            else Color.LightGray.copy(alpha = 0.5f)
                        )
                )
            }

            if (isPaired && partnerTimeZone != null) {
                Box(modifier = Modifier.width(60.dp)) {
                    Text(
                        text = formattedPartnerTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isCurrentHour) MaterialTheme.colorScheme.primary else Color.Gray,
                        textAlign = TextAlign.Start,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp)
                    )
                }
            }
        }

        // Partner events side
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp)
                .fillMaxHeight()
                .clickable(enabled = isPaired && partnerEvents.isEmpty()) {
                    handlePartnerSlotClick()
                },
            contentAlignment = Alignment.Center
        ) {
            if (isPaired) {
                if (partnerEvents.isEmpty()) {
                    if (selectedSide == "partner") {
                        // Show "Tap to add to partner's timeline" hint when this side is selected
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .padding(4.dp)
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Tap again to add",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.Start) {
                        partnerEvents.forEach { event ->
                            TimelineEventCard(
                                event = event,
                                onClick = { onEventClick(event.id) },
                                isUserEvent = false,
                                modifier = Modifier.fillMaxWidth(),
                                displayDate = date,
                                sourceTimezone = ZoneId.of(when {
                                    event.sourceTimezone.isNotEmpty() -> event.sourceTimezone
                                    event.metadata["sourceTimezone"] is String -> event.metadata["sourceTimezone"] as String
                                    else -> (partnerTimeZone ?: userTimeZone).id
                                }),
                                displayTimezone = ZoneId.of((partnerTimeZone ?: userTimeZone).id)
                            )
                        }
                    }
                }
            }
        }
    }
}
