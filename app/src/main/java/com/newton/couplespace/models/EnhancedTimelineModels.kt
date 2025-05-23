package com.newton.couplespace.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName
import java.io.Serializable
import java.time.*
import java.util.*
import kotlin.jvm.Transient

/**
 * Enhanced timeline event model with support for recurring events and better time management
 */
data class TimelineEvent(
    @DocumentId
    val id: String = "",
    val userId: String = "",
    val coupleId: String = "",
    val title: String = "",
    val description: String = "",
    val location: String = "",
    val startTime: Timestamp = Timestamp.now(),
    val endTime: Timestamp = Timestamp.now(),
    val allDay: Boolean = false,
    val eventType: EventType = EventType.EVENT,
    val category: EventCategory = EventCategory.PERSONAL,
    val priority: Priority = Priority.MEDIUM,
    @field:PropertyName("isRecurring")
    val isRecurring: Boolean = false,
    val recurrenceRule: RecurrenceRule? = null,
    @field:PropertyName("isCompleted")
    val isCompleted: Boolean = false,
    val notificationSettings: NotificationSettings = NotificationSettings(),
    val createdAt: Timestamp = Timestamp.now(),
    val updatedAt: Timestamp = Timestamp.now(),
    val createdBy: String = "",
    val lastModifiedBy: String = "",
    val color: String = "", // Hex color code for the event
    @get:Exclude
    @Transient
    val metadata: MutableMap<String, Any?> = mutableMapOf()
) {
    /**
     * Calculates the duration of the event in minutes
     */
    @get:Exclude
    val durationMinutes: Long
        get() = (endTime.seconds - startTime.seconds) / 60

    /**
     * Checks if the event is happening at the specified time
     */
    fun isHappeningAt(time: LocalDateTime): Boolean {
        val eventStart = startTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        val eventEnd = endTime.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
        return !time.isBefore(eventStart) && time.isBefore(eventEnd)
    }
}

/**
 * Types of timeline events
 */
enum class EventType {
    EVENT,       // Standard calendar event
    REMINDER,    // Time-specific reminder
    TASK,        // Task with optional due time
    ANNIVERSARY, // Special date (birthday, anniversary, etc.)
    GOAL,        // Long-term goal with milestones
    MEAL,        // Meal planning
    EXERCISE,    // Workout or exercise
    APPOINTMENT, // Doctor's appointment, meeting, etc.
    TRAVEL,      // Travel plans
    CUSTOM       // User-defined type
}

/**
 * Categories for organizing events
 */
enum class EventCategory {
    PERSONAL,    // Personal events
    COUPLE,      // Events involving both partners
    WORK,        // Work-related events
    FAMILY,      // Family events
    HEALTH,      // Health-related (doctor visits, etc.)
    SOCIAL,      // Social events
    FINANCIAL,   // Bill payments, etc.
    EDUCATION,   // Classes, courses, etc.
    HOBBY,       // Hobbies and personal interests
    OTHER        // Uncategorized
}

/**
 * Priority levels for events
 */
enum class Priority {
    LOW,     // Low priority (optional)
    MEDIUM,  // Normal priority
    HIGH,    // Important
    URGENT   // Time-sensitive and important
}

/**
 * Defines recurrence rules for events
 */
data class RecurrenceRule(
    @field:PropertyName("frequency")
    val frequency: RecurrenceFrequency = RecurrenceFrequency.DAILY,
    @field:PropertyName("interval")
    val interval: Int = 1, // Every X frequency units
    @field:PropertyName("endDate")
    val endDate: Timestamp? = null, // Optional end date for recurrence
    @field:PropertyName("count")
    val count: Int? = null, // Optional max number of occurrences
    @field:PropertyName("byDay")
    val byDay: List<DayOfWeek> = emptyList(), // For weekly/monthly recurrence
    @field:PropertyName("byMonthDay")
    val byMonthDay: List<Int> = emptyList(), // For monthly recurrence (e.g., 15th of each month)
    @field:PropertyName("byYearDay")
    val byYearDay: List<Int>? = null, // For yearly recurrence
    @field:PropertyName("byWeekNo")
    val byWeekNo: List<Int>? = null, // For yearly recurrence (week numbers)
    @field:PropertyName("byMonth")
    val byMonth: List<Int>? = null // For yearly recurrence (months)
) {
    // No-argument constructor required for Firestore
    constructor() : this(RecurrenceFrequency.DAILY)
}

/**
 * Recurrence frequency options
 */
enum class RecurrenceFrequency {
    DAILY,
    WEEKLY,
    MONTHLY,
    YEARLY,
    CUSTOM
}

/**
 * Notification settings for an event
 */
data class NotificationSettings(
    @field:PropertyName("reminders")
    val reminders: List<Reminder> = listOf(
        Reminder(15, ReminderUnit.MINUTES) // Default 15-minute reminder
    ),
    @field:PropertyName("emailNotification")
    val emailNotification: Boolean = false,
    @field:PropertyName("pushNotification")
    val pushNotification: Boolean = true,
    @field:PropertyName("soundEnabled")
    val soundEnabled: Boolean = true,
    @field:PropertyName("vibrationEnabled")
    val vibrationEnabled: Boolean = true
) {
    // No-argument constructor required for Firestore
    constructor() : this(
        listOf(Reminder()),
        false,
        true,
        true,
        true
    )
}

/**
 * Reminder before an event
 */
data class Reminder(
    @field:PropertyName("value")
    val value: Int = 15,
    @field:PropertyName("unit")
    val unit: ReminderUnit = ReminderUnit.MINUTES
) {
    // No-argument constructor required for Firestore
    constructor() : this(15, ReminderUnit.MINUTES)
}

/**
 * Units for reminder times
 */
enum class ReminderUnit {
    MINUTES,
    HOURS,
    DAYS,
    WEEKS
}

/**
 * Data class for displaying time slots in the timeline
 */
data class TimeSlot(
    val time: LocalTime,
    val events: List<TimelineEvent> = emptyList(),
    val isCurrentTime: Boolean = false,
    val isHourMark: Boolean = false
)

/**
 * Data class for grouping events by date
 */
data class DailyEvents(
    val date: LocalDate,
    val events: List<TimelineEvent>,
    val isToday: Boolean = false,
    val isSelected: Boolean = false
)

/**
 * Timeline view state for managing the UI state
 */
data class TimelineViewState(
    val selectedDate: LocalDate = LocalDate.now(),
    val viewMode: TimelineViewMode = TimelineViewMode.DAY,
    val events: List<TimelineEvent> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedEvent: TimelineEvent? = null,
    val nudgeSuccess: Boolean = false
)

/**
 * Available timeline view modes
 */
enum class TimelineViewMode {
    DAY,
    WEEK,
    MONTH,
    AGENDA
}

/**
 * Extension function to convert Firebase Timestamp to LocalDateTime
 */
fun Timestamp.toLocalDateTime(): LocalDateTime {
    return this.toDate().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime()
}

/**
 * Extension function to convert LocalDateTime to Firebase Timestamp
 */
fun LocalDateTime.toTimestamp(): Timestamp {
    return Timestamp(Date.from(this.atZone(ZoneId.systemDefault()).toInstant()))
}
