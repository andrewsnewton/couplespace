package com.newton.couplespace.screens.main.timeline

import android.util.Log
import androidx.compose.runtime.Stable
import com.newton.couplespace.models.TimelineEvent
import java.time.LocalDate
import java.time.ZoneId

/**
 * Utility object for Timeline-related helper functions
 */
@Stable
object TimelineUtils {
    private const val TAG = "TimelineUtils"

    /**
     * Determines if an event should appear on a specific date, taking timezones into account
     * 
     * @param event The timeline event to check
     * @param selectedDate The date to check against (in user timezone)
     * @param userZoneId The user's timezone
     * @param eventSourceTimezoneId The event's source timezone (or null to use the event's own timezone)
     * @param currentUserId The current user's ID (to determine if it's a user or partner event)
     * @param isUserEvent If true, checks that event.userId == currentUserId; if false, checks that event.userId != currentUserId
     * @param skipUserIdCheck If true, skips the user ID check entirely (useful for time slot filtering)
     * @return true if the event should appear on the selected date, false otherwise
     */
    @Stable
    fun shouldEventAppearOnDate(
        event: TimelineEvent,
        selectedDate: LocalDate,
        userZoneId: ZoneId,
        eventSourceTimezoneId: String? = null,
        currentUserId: String,
        isUserEvent: Boolean = true,
        skipUserIdCheck: Boolean = false
    ): Boolean {
        try {
            // Determine if this is a user or partner event based on the parameter
            val eventType = if (isUserEvent) "User" else "Partner"
            
            // Check user ID match if not skipping this check
            if (!skipUserIdCheck) {
                val userIdMatches = if (isUserEvent) {
                    event.userId == currentUserId
                } else {
                    event.userId != currentUserId
                }
                
                if (!userIdMatches) {
                    Log.d(TAG, eventType + " event " + event.title + " filtered: User ID check failed")
                    Log.d(TAG, "  event.userId=" + event.userId + ", currentUserId=" + currentUserId + ", isUserEvent=" + isUserEvent)
                    return false
                }
            }
            
            // Get the source timezone from the parameter, event field, or fallback to metadata
            val sourceTimezone = when {
                !eventSourceTimezoneId.isNullOrEmpty() -> eventSourceTimezoneId
                event.sourceTimezone.isNotEmpty() -> event.sourceTimezone
                event.metadata["sourceTimezone"] is String -> event.metadata["sourceTimezone"] as String
                else -> userZoneId.id
            }
            
            // Use the event's source timezone for proper time conversion
            val eventZoneId = try {
                ZoneId.of(sourceTimezone)
            } catch (e: Exception) {
                Log.w(TAG, "Invalid sourceTimezone: $sourceTimezone, using user timezone instead", e)
                userZoneId
            }
            
            // Log timezone information for debugging
            Log.d(TAG, eventType + " event: " + event.title + ", Source timezone: " + sourceTimezone)
            
            // Convert UTC timestamps to event's source timezone
            val eventStartInstant = event.startTime.toDate().toInstant()
            val eventEndInstant = event.endTime?.toDate()?.toInstant() ?: eventStartInstant.plusSeconds(3600) // Default to 1 hour if no end time
            
            val eventStartDateTime = eventStartInstant.atZone(eventZoneId).toLocalDateTime()
            val eventEndDateTime = eventEndInstant.atZone(eventZoneId).toLocalDateTime()
            
            // Get event date in event's timezone for comparison with selected date
            val eventStartDate = eventStartDateTime.toLocalDate()
            val eventEndDate = eventEndDateTime.toLocalDate()
            
            Log.d(TAG, "  Event dates in source timezone: Start=" + eventStartDate + ", End=" + eventEndDate)
            
            // Convert selected date from user timezone to event timezone
            val selectedDateInEventTz = selectedDate.atStartOfDay(userZoneId)
                .withZoneSameInstant(eventZoneId).toLocalDate()
            
            Log.d(TAG, "  Selected date (" + selectedDate + ") in event timezone: " + selectedDateInEventTz)
            
            // Also get the next day, previous day, and ±2 days in event timezone for boundary cases
            val nextDayInEventTz = selectedDate.plusDays(1).atStartOfDay(userZoneId)
                .withZoneSameInstant(eventZoneId).toLocalDate()
            val prevDayInEventTz = selectedDate.minusDays(1).atStartOfDay(userZoneId)
                .withZoneSameInstant(eventZoneId).toLocalDate()
            val twoDaysAheadInEventTz = selectedDate.plusDays(2).atStartOfDay(userZoneId)
                .withZoneSameInstant(eventZoneId).toLocalDate()
            val twoDaysBehindInEventTz = selectedDate.minusDays(2).atStartOfDay(userZoneId)
                .withZoneSameInstant(eventZoneId).toLocalDate()
            
            // Enhanced date comparison logic for timezone boundaries
            val dateMatches = eventStartDate == selectedDateInEventTz || 
                             eventEndDate == selectedDateInEventTz ||
                             (eventStartDate.isBefore(selectedDateInEventTz) && eventEndDate.isAfter(selectedDateInEventTz)) ||
                             // Handle timezone boundary cases - simplified to catch more edge cases
                             eventStartDate == nextDayInEventTz ||
                             eventStartDate == prevDayInEventTz ||
                             // Handle larger timezone differences (up to 2 days in either direction)
                             eventStartDate == twoDaysAheadInEventTz ||
                             eventStartDate == twoDaysBehindInEventTz
            
            // Log detailed information about the date matching
            Log.d(TAG, "  Date matching for " + event.title + ": " + dateMatches)
            Log.d(TAG, "    Event start date: " + eventStartDate + ", Event end date: " + eventEndDate)
            Log.d(TAG, "    Selected date in event timezone: " + selectedDateInEventTz)
            Log.d(TAG, "    Next day in event timezone: " + nextDayInEventTz)
            Log.d(TAG, "    Previous day in event timezone: " + prevDayInEventTz)
            Log.d(TAG, "    Two days ahead in event timezone: " + twoDaysAheadInEventTz)
            Log.d(TAG, "    Two days behind in event timezone: " + twoDaysBehindInEventTz)
            
            return dateMatches
        } catch (e: Exception) {
            Log.e(TAG, "Error in shouldEventAppearOnDate for event " + event.title, e)
            return false // Default to not showing the event if there's an error
        }
    }
}
