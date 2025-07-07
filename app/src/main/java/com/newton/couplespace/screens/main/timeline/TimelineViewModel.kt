package com.newton.couplespace.screens.main.timeline

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.newton.couplespace.auth.AuthService
import com.newton.couplespace.models.*
import com.newton.couplespace.services.EventNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.*

/**
 * ViewModel for the Timeline screen
 */
class TimelineViewModel(application: Application) : AndroidViewModel(application) {
    companion object {
        private const val TAG = "TimelineViewModel"
    }
    
    // Repository
    private val repository: EnhancedTimelineRepository
    
    // Auth Service for partner data
    private val authService: AuthService
    
    // Notification manager for event notifications
    private val notificationManager: EventNotificationManager
    
    // Firestore reference
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")
    
    // UI State
    private val _viewState = MutableStateFlow(TimelineViewState())
    val viewState: StateFlow<TimelineViewState> = _viewState
    
    // Selected event
    private val _selectedEvent = MutableStateFlow<TimelineEvent?>(null)
    val selectedEvent: StateFlow<TimelineEvent?> = _selectedEvent
    
    // User timezone
    private val _userTimezone = MutableStateFlow(TimeZone.getDefault())
    val userTimezone: StateFlow<TimeZone> = _userTimezone
    
    // Partner timezone
    private val _partnerTimezone = MutableStateFlow<TimeZone?>(null)
    val partnerTimezone: StateFlow<TimeZone?> = _partnerTimezone
    
    init {
        try {
            val appContext = getApplication<Application>().applicationContext
            // Initialize repository with application context
            repository = EnhancedTimelineRepository(appContext)
            // Initialize auth service with application context
            authService = AuthService(appContext)
            // Initialize notification manager
            notificationManager = EventNotificationManager(appContext)
            Log.d(TAG, "TimelineViewModel initialized successfully")
            
            // Load user timezone first
            loadUserTimezone()
            // Load partner data to ensure we have partner information available
            loadPartnerData()
            // Then load the events
            loadEventsForCurrentView()
            
            // Schedule notifications for today's events
            scheduleNotificationsForToday()
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TimelineViewModel", e)
            throw e
        }
    }
    
    /**
     * Change the timeline view mode
     */
    fun setViewMode(mode: TimelineViewMode) {
        _viewState.update { it.copy(viewMode = mode) }
        loadEventsForCurrentView()
    }
    
    /**
     * Change the selected date
     */
    fun setSelectedDate(date: LocalDate) {
        _viewState.update { it.copy(selectedDate = date) }
        loadEventsForCurrentView()
    }
    
    /**
     * Navigate to the previous time period (day, week, or month)
     */
    fun navigateToPrevious() {
        val currentState = _viewState.value
        val newDate = when (currentState.viewMode) {
            TimelineViewMode.DAY -> currentState.selectedDate.minusDays(1)
            TimelineViewMode.WEEK -> currentState.selectedDate.minusWeeks(1)
            TimelineViewMode.MONTH -> currentState.selectedDate.minusMonths(1)
            TimelineViewMode.AGENDA -> currentState.selectedDate.minusWeeks(2)
        }
        _viewState.update { it.copy(selectedDate = newDate) }
        loadEventsForCurrentView()
    }
    
    /**
     * Navigate to the next time period (day, week, or month)
     */
    fun navigateToNext() {
        val currentState = _viewState.value
        val newDate = when (currentState.viewMode) {
            TimelineViewMode.DAY -> currentState.selectedDate.plusDays(1)
            TimelineViewMode.WEEK -> currentState.selectedDate.plusWeeks(1)
            TimelineViewMode.MONTH -> currentState.selectedDate.plusMonths(1)
            TimelineViewMode.AGENDA -> currentState.selectedDate.plusWeeks(2)
        }
        _viewState.update { it.copy(selectedDate = newDate) }
        loadEventsForCurrentView()
    }
    
    /**
     * Navigate to today
     */
    fun navigateToToday() {
        _viewState.update { it.copy(selectedDate = LocalDate.now()) }
        loadEventsForCurrentView()
    }
    
    /**
     * Show the add event dialog
     */
    fun showAddEventDialog() {
        // This will be handled by the UI state, we just need the function to exist
        // for the EnhancedTimelineScreen to call
    }
    
    /**
     * Load user timezone from their profile
     */
    private fun loadUserTimezone() {
        viewModelScope.launch {
            try {
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserId != null) {
                    Log.d(TAG, "Loading user timezone for ID: $currentUserId")
                    
                    val userDoc = withContext(Dispatchers.IO) {
                        usersCollection.document(currentUserId).get().await()
                    }
                    
                    if (userDoc.exists()) {
                        val userData = userDoc.toObject(User::class.java)
                        if (userData != null && userData.timezone.isNotBlank()) {
                            // Set the user timezone from profile
                            val userTz = TimeZone.getTimeZone(userData.timezone)
                            _userTimezone.value = userTz
                            Log.d(TAG, "User timezone loaded: ${userData.timezone}")
                        } else {
                            Log.d(TAG, "User timezone not found in profile, using system default")
                        }
                    } else {
                        Log.d(TAG, "User document not found, using system default timezone")
                    }
                } else {
                    Log.d(TAG, "No current user ID, using system default timezone")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading user timezone", e)
                // Keep using the default timezone
            }
        }
    }
    
    /**
     * Load partner data from AuthService and update the view state
     */
    private fun loadPartnerData() {
        viewModelScope.launch {
            try {
                // Get current user ID
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
                if (currentUserId == null) {
                    Log.w(TAG, "Cannot load partner data: User not authenticated")
                    return@launch
                }
                
                // Check if the user is connected with a partner
                val isPaired = authService.isConnectedWithPartner()
                Log.d(TAG, "User isPaired status: $isPaired")
                
                if (isPaired) {
                    // Get partner ID
                    val partnerId = authService.getPartnerIdFromFirestore()
                    Log.d(TAG, "Partner ID: $partnerId")
                    
                    if (partnerId.isNotBlank()) {
                        try {
                            // Get partner profile data directly from Firestore
                            val partnerDoc = usersCollection.document(partnerId).get().await()
                            
                            // Get partner timezone from profile
                            val timezoneString = partnerDoc.getString("timezone")
                            val partnerTimezone = if (!timezoneString.isNullOrBlank()) {
                                TimeZone.getTimeZone(timezoneString)
                            } else {
                                null
                            }
                            
                            // Update partner timezone StateFlow
                            _partnerTimezone.value = partnerTimezone
                            
                            // Get partner profile picture
                            val profilePicture = partnerDoc.getString("profilePicture")
                            
                            // Update view state with partner data
                            _viewState.update {
                                it.copy(
                                    isPaired = true,
                                    partnerTimeZone = partnerTimezone,
                                    partnerProfilePicture = profilePicture
                                )
                            }
                            
                            Log.d(TAG, "Partner data loaded successfully: ${partnerDoc.getString("name")}, timezone: $timezoneString")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error loading partner profile", e)
                            // Still mark as paired but with incomplete data
                            _viewState.update {
                                it.copy(isPaired = true)
                            }
                        }
                    } else {
                        // Partner ID not found, update with isPaired true but no partner data
                        _viewState.update {
                            it.copy(isPaired = true)
                        }
                    }
                } else {
                    // Not paired, update view state
                    _viewState.update {
                        it.copy(
                            isPaired = false,
                            partnerTimeZone = null,
                            partnerProfilePicture = null
                        )
                    }
                    Log.d(TAG, "User is not paired with a partner")
                }
                
                // Get current user profile picture
                try {
                    val currentUserDoc = usersCollection.document(currentUserId).get().await()
                    val userProfilePicture = currentUserDoc.getString("profilePicture")
                    
                    _viewState.update {
                        it.copy(userProfilePicture = userProfilePicture)
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading user profile", e)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading partner data", e)
            }
        }
    }

    /**
     * Load events for the current view
     */
    fun loadEventsForCurrentView() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                withContext(Dispatchers.Main) {
                    _viewState.update { it.copy(isLoading = true, error = null) }
                }
                
                val currentState = _viewState.value
                val (startDate, endDate) = getDateRangeForView(
                    currentState.selectedDate, 
                    currentState.viewMode
                )
                
                val result = repository.loadTimelineEvents(startDate, endDate)
                
                result.fold(
                    onSuccess = { events ->
                        withContext(Dispatchers.Main) {
                            // Separate user events from partner events
                            val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                            val userEvents = events.filter { it.userId == currentUserId }
                            val partnerEvents = events.filter { it.userId != currentUserId || (it.metadata["isForPartner"] as? Boolean == true) }
                            
                            Log.d(TAG, "Total events: ${events.size}, User events: ${userEvents.size}")
                            Log.d(TAG, "Total partner events: ${partnerEvents.size}")
                            
                            _viewState.update { 
                                it.copy(
                                    isLoading = false,
                                    events = userEvents,
                                    partnerEvents = partnerEvents,
                                    error = null
                                )
                            }
                        }
                    },
                    onFailure = { error ->
                        withContext(Dispatchers.Main) {
                            _viewState.update { 
                                it.copy(
                                    isLoading = false,
                                    error = "Failed to load events: ${error.message}"
                                )
                            }
                        }
                        Log.e(TAG, "Error loading events", error)
                    }
                )
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _viewState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Failed to load events: ${e.message}"
                        )
                    }
                }
                Log.e(TAG, "Error loading events", e)
            }
        }
    }
    
    /**
     * Get the date range for the current view
     */
    private fun getDateRangeForView(date: LocalDate, viewMode: TimelineViewMode): Pair<LocalDate, LocalDate> {
        return when (viewMode) {
            TimelineViewMode.DAY -> Pair(date, date)
            TimelineViewMode.WEEK -> {
                val firstDayOfWeek = date.with(WeekFields.of(Locale.getDefault()).dayOfWeek(), 1)
                Pair(firstDayOfWeek, firstDayOfWeek.plusDays(6))
            }
            TimelineViewMode.MONTH -> {
                val firstDayOfMonth = date.withDayOfMonth(1)
                val lastDayOfMonth = date.withDayOfMonth(date.lengthOfMonth())
                Pair(firstDayOfMonth, lastDayOfMonth)
            }
            TimelineViewMode.AGENDA -> {
                // For agenda view, show events for 4 weeks (2 weeks before and 2 weeks after)
                Pair(date.minusWeeks(2), date.plusWeeks(2))
            }
        }
    }
    
    /**
     * Select an event
     */
    fun selectEvent(eventId: String) {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                val result = withContext(Dispatchers.IO) {
                    repository.getEventById(eventId)
                }
                
                result.fold(
                    onSuccess = { event ->
                        withContext(Dispatchers.Main) {
                            _selectedEvent.value = event
                            _viewState.update { it.copy(selectedEvent = event) }
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error getting event", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error getting event", e)
            }
        }
    }
    
    /**
     * Clear selected event
     */
    fun clearSelectedEvent() {
        _selectedEvent.value = null
        _viewState.update { it.copy(selectedEvent = null) }
    }
    
    /**
     * Refresh partner data and timeline events
     */
    fun refreshAllData() {
        loadPartnerData()
        loadEventsForCurrentView()
    }

    /**
     * Convert local date and time to UTC timestamp
     */
    private fun localDateTimeToUtcTimestamp(
        date: LocalDate,
        time: LocalTime,
        zoneId: ZoneId
    ): Timestamp {
        val localDateTime = LocalDateTime.of(date, time)
        val zonedDateTime = ZonedDateTime.of(localDateTime, zoneId)
        val utcInstant = zonedDateTime.toInstant()
        return Timestamp(utcInstant.epochSecond, utcInstant.nano)
    }
    
    /**
     * Convert UTC timestamp to local date and time in a specific timezone
     */
    private fun utcTimestampToLocalDateTime(
        timestamp: Timestamp,
        zoneId: ZoneId
    ): Pair<LocalDate, LocalTime> {
        val instant = Instant.ofEpochSecond(timestamp.seconds, timestamp.nanoseconds.toLong())
        val zonedDateTime = ZonedDateTime.ofInstant(instant, zoneId)
        return Pair(zonedDateTime.toLocalDate(), zonedDateTime.toLocalTime())
    }
    
   fun createEvent(event: TimelineEvent, isPartnerEvent: Boolean = false) {
    viewModelScope.launch {
        try {
            _viewState.update { it.copy(isLoading = true, error = null) }

            val userZoneId = ZoneId.of(_userTimezone.value.id)
            val partnerZoneId = _partnerTimezone.value?.let { ZoneId.of(it.id) }
            val targetZoneId = if (isPartnerEvent && partnerZoneId != null) partnerZoneId else userZoneId

            Log.d(TAG, "Creating event for ${if (isPartnerEvent) "partner" else "user"} with timezone: ${targetZoneId.id}")

            // 1. Get source timezone from event (fallback to user timezone)
            val sourceZoneId = runCatching {
                ZoneId.of(event.sourceTimezone)
            }.getOrElse {
                userZoneId
            }

            // 2. Convert UTC timestamps to ZonedDateTime in source timezone
            val startZonedInSource = event.startTime.toDate().toInstant().atZone(sourceZoneId)
            val endZonedInSource = event.endTime?.toDate()?.toInstant()?.atZone(sourceZoneId) ?: startZonedInSource.plusHours(1)

            Log.d(TAG, "Event times in source timezone (${sourceZoneId.id}): $startZonedInSource to $endZonedInSource")

            // 3. Extract LocalDateTime from source timezone (user picked local date/time)
            val localStart = startZonedInSource.toLocalDateTime()
            val localEnd = endZonedInSource.toLocalDateTime()

            // 4. Now attach the target timezone (user or partner)
            val startInTargetZone = localStart.atZone(targetZoneId)
            val endInTargetZone = localEnd.atZone(targetZoneId)

            // 5. Convert these to UTC instants for storage
            val startInstantUtc = startInTargetZone.toInstant()
            val endInstantUtc = endInTargetZone.toInstant()

            val utcStartTimestamp = Timestamp(startInstantUtc.epochSecond, startInstantUtc.nano)
            val utcEndTimestamp = Timestamp(endInstantUtc.epochSecond, endInstantUtc.nano)

            Log.d(TAG, "Converted to UTC for storage: $startInstantUtc to $endInstantUtc")

            // 6. Save event with proper UTC time and sourceTimezone
            val utcEvent = event.copy(
                startTime = utcStartTimestamp,
                endTime = utcEndTimestamp,
                sourceTimezone = targetZoneId.id
            )

            val result = repository.saveTimelineEvent(utcEvent, isPartnerEvent)

            result.fold(
                onSuccess = { eventId ->
                    Log.d(TAG, "Event created with ID: $eventId")
                    
                    // Schedule notification for the event
                    // All events in the user's calendar should have notifications
                    notificationManager.onEventCreated(utcEvent)
                    
                    loadEventsForCurrentView()
                },
                onFailure = { error ->
                    _viewState.update {
                        it.copy(isLoading = false, error = "Failed to create event: ${error.message}")
                    }
                    Log.e(TAG, "Error creating event", error)
                }
            )
        } catch (e: Exception) {
            _viewState.update {
                it.copy(isLoading = false, error = "Failed to create event: ${e.message}")
            }
            Log.e(TAG, "Error creating event", e)
        }
    }
}



    
    /**
     * Update an existing event with proper timezone handling
     */
    fun updateEvent(event: TimelineEvent) {
        viewModelScope.launch {
            try {
                _viewState.update { it.copy(isLoading = true, error = null) }

                // Get source timezone from the field or fallback to user timezone
                val sourceTimezoneId = if (event.sourceTimezone.isNotEmpty()) {
                    event.sourceTimezone
                } else {
                    // For backward compatibility, check metadata
                    event.metadata["sourceTimezone"] as? String ?: _userTimezone.value.id
                }
                val sourceZoneId = ZoneId.of(sourceTimezoneId)

                Log.d(TAG, "Updating event with source timezone: $sourceTimezoneId")

                val startDate = event.startTime.toDate()
                val endDate = event.endTime.toDate()

                Log.d(TAG, "Original event times from dialog for update - Start: $startDate, End: $endDate")

                val systemZoneId = ZoneId.systemDefault()
                Log.d(TAG, "System timezone for update: $systemZoneId")

                // Use Calendar to extract year, month, day, hour, minute in system timezone (ignored)
                val startCal = Calendar.getInstance().apply { time = startDate }
                val endCal = Calendar.getInstance().apply { time = endDate }

                // Rebuild LocalDate and LocalTime from the extracted components
                val startPickedDate = LocalDate.of(
                    startCal.get(Calendar.YEAR),
                    startCal.get(Calendar.MONTH) + 1,
                    startCal.get(Calendar.DAY_OF_MONTH)
                )
                val endPickedDate = LocalDate.of(
                    endCal.get(Calendar.YEAR),
                    endCal.get(Calendar.MONTH) + 1,
                    endCal.get(Calendar.DAY_OF_MONTH)
                )

                val startPickedTime = LocalTime.of(
                    startCal.get(Calendar.HOUR_OF_DAY),
                    startCal.get(Calendar.MINUTE)
                )
                val endPickedTime = LocalTime.of(
                    endCal.get(Calendar.HOUR_OF_DAY),
                    endCal.get(Calendar.MINUTE)
                )

                // Combine into LocalDateTime in source timezone
                val startLocalDateTimeInSourceTZ = LocalDateTime.of(startPickedDate, startPickedTime)
                val endLocalDateTimeInSourceTZ = LocalDateTime.of(endPickedDate, endPickedTime)

                val startZonedDateTime = startLocalDateTimeInSourceTZ.atZone(sourceZoneId)
                val endZonedDateTime = endLocalDateTimeInSourceTZ.atZone(sourceZoneId)

                // Convert to UTC
                val startInstantUtc = startZonedDateTime.toInstant()
                val endInstantUtc = endZonedDateTime.toInstant()

                // Create UTC timestamps
                val utcStartTimestamp = Timestamp(startInstantUtc.epochSecond, startInstantUtc.nano)
                val utcEndTimestamp = Timestamp(endInstantUtc.epochSecond, endInstantUtc.nano)

                Log.d(TAG, "Created time in source timezone: $startZonedDateTime")
                Log.d(TAG, "Converted to UTC: $startInstantUtc")
                Log.d(TAG, "UTC timestamp: ${utcStartTimestamp.seconds} seconds, ${utcStartTimestamp.nanoseconds} nanos")

                // Update event with UTC times and set sourceTimezone field
                val utcEvent = event.copy(
                    startTime = utcStartTimestamp,
                    endTime = utcEndTimestamp,
                    updatedAt = Timestamp.now(),
                    sourceTimezone = sourceZoneId.id
                )

                Log.d(TAG, "Stored in UTC for update - Start: ${Date.from(startInstantUtc)}, End: ${Date.from(endInstantUtc)}")
                Log.d(TAG, "Original local time in $systemZoneId: $startPickedTime")
                Log.d(TAG, "When viewed in source timezone $sourceTimezoneId: ${startZonedDateTime.toLocalTime()}")
                Log.d(TAG, "When viewed from UTC: ${ZonedDateTime.ofInstant(startInstantUtc, ZoneId.of("UTC")).toLocalTime()}")

                val result = repository.updateTimelineEvent(utcEvent)

                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Event updated: ${event.id}")
                        loadEventsForCurrentView()

                        if (_selectedEvent.value?.id == event.id) {
                            _selectedEvent.value = utcEvent
                            _viewState.update { it.copy(selectedEvent = utcEvent) }
                        }
                        
                        // Update notification for the event
                        // All events in the user's calendar should have notifications
                        notificationManager.onEventUpdated(utcEvent)
                    },
                    onFailure = { error ->
                        _viewState.update {
                            it.copy(
                                isLoading = false,
                                error = "Failed to update event: ${error.message}"
                            )
                        }
                        Log.e(TAG, "Error updating event", error)
                    }
                )
            } catch (e: Exception) {
                _viewState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to update event: ${e.message}"
                    )
                }
                Log.e(TAG, "Error updating event", e)
            }
        }
    }

    
    /**
     * Delete an event
     */
    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            try {
                _viewState.update { it.copy(isLoading = true, error = null) }
                
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                if (currentUserId.isBlank()) {
                    _viewState.update { 
                        it.copy(
                            isLoading = false,
                            error = "You must be logged in to delete an event"
                        )
                    }
                    return@launch
                }
                
                // Get the event to check permissions
                val event = _viewState.value.events.find { it.id == eventId }
                    ?: _viewState.value.partnerEvents?.find { it.id == eventId }
                
                if (event == null) {
                    _viewState.update { 
                        it.copy(
                            isLoading = false,
                            error = "Event not found"
                        )
                    }
                    return@launch
                }
                
                // Check if the user has permission to delete this event
                val isUserEvent = event.userId == currentUserId
                val isPartnerEvent = event.userId != currentUserId && 
                                    event.coupleId.isNotEmpty() && 
                                    event.createdBy == currentUserId
                
                if (!isUserEvent && !isPartnerEvent) {
                    _viewState.update { 
                        it.copy(
                            isLoading = false,
                            error = "You don't have permission to delete this event"
                        )
                    }
                    return@launch
                }
                
                // Delete the event from the repository
                val result = repository.deleteTimelineEvent(eventId)
                
                result.fold(
                    onSuccess = { _ ->
                        // Clear selected event if it was the deleted one
                        if (_selectedEvent.value?.id == eventId) {
                            _selectedEvent.value = null
                        }
                        
                        // Cancel notifications for the deleted event
                        // We cancel notifications for any event that was in the user's calendar
                        notificationManager.onEventDeleted(eventId)
                        
                        // Refresh events
                        loadEventsForCurrentView()
                        _viewState.update { it.copy(isLoading = false) }
                    },
                    onFailure = { error ->
                        _viewState.update { 
                            it.copy(
                                isLoading = false,
                                error = "Failed to delete event: ${error.message}"
                            )
                        }
                        Log.e(TAG, "Error deleting event", error)
                    }
                )
            } catch (e: Exception) {
                _viewState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to delete event: ${e.message}"
                    )
                }
                Log.e(TAG, "Error deleting event", e)
            }
        }
    }
    
    /**
     * Mark an event as completed
     */
    fun markEventCompleted(eventId: String, completed: Boolean) {
        viewModelScope.launch {
            try {
                val result = repository.markEventCompleted(eventId, completed)
                
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Event $eventId marked as ${if (completed) "completed" else "incomplete"}")
                        loadEventsForCurrentView()
                        
                        // If this is the selected event, update its completion status
                        _selectedEvent.value?.let { event ->
                            if (event.id == eventId) {
                                _selectedEvent.value = event.copy(isCompleted = completed)
                                _viewState.update { it.copy(selectedEvent = _selectedEvent.value) }
                            }
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Error marking event as completed", error)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error marking event as completed", e)
            }
        }
    }
    
    /**
     * Get events for a specific date
     */
    fun getEventsForDate(date: LocalDate): List<TimelineEvent> {
        // Use the user's timezone instead of system default
        val userZoneId = ZoneId.of(_userTimezone.value.id)
        
        return _viewState.value.events.filter { event ->
            try {
                // Get source timezone from the field or fallback to user timezone
                val sourceTimezoneId = if (event.sourceTimezone.isNotEmpty()) {
                    event.sourceTimezone
                } else {
                    // For backward compatibility, check metadata
                    event.metadata["sourceTimezone"] as? String ?: _userTimezone.value.id
                }
                
                // Use source timezone if available, otherwise use user timezone
                val zoneId = try {
                    ZoneId.of(sourceTimezoneId)
                } catch (e: Exception) {
                    Log.w(TAG, "Invalid sourceTimezone: $sourceTimezoneId, using user timezone instead", e)
                    userZoneId
                }
                
                // Convert event time to the appropriate timezone
                val eventDate = event.startTime.toDate().toInstant()
                    .atZone(zoneId).toLocalDate()
                    
                Log.d(TAG, "Comparing event date ${eventDate} with requested date ${date} using timezone ${zoneId.id}")
                eventDate.isEqual(date)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing event ${event.id} for date filtering", e)
                false // Skip events that cause errors
            }
        }
    }
    
    /**
     * Get events for a specific time slot
     */
    fun getEventsForTimeSlot(date: LocalDate, time: LocalTime): List<TimelineEvent> {
        val startOfSlot = LocalDateTime.of(date, time)
        val endOfSlot = startOfSlot.plusMinutes(30)
        
        // Use the user's timezone instead of system default
        val userZoneId = ZoneId.of(_userTimezone.value.id)
        
        return _viewState.value.events.filter { event ->
            try {
                // Get source timezone from the field or fallback to user timezone
                val sourceTimezoneId = if (event.sourceTimezone.isNotEmpty()) {
                    event.sourceTimezone
                } else {
                    // For backward compatibility, check metadata
                    event.metadata["sourceTimezone"] as? String ?: _userTimezone.value.id
                }
                
                // Use source timezone if available, otherwise use user timezone
                val zoneId = try {
                    ZoneId.of(sourceTimezoneId)
                } catch (e: Exception) {
                    Log.w(TAG, "Invalid sourceTimezone: $sourceTimezoneId, using user timezone instead", e)
                    userZoneId
                }
                
                // Convert event times to the appropriate timezone
                val eventStart = event.startTime.toDate().toInstant()
                    .atZone(zoneId).toLocalDateTime()
                val eventEnd = event.endTime.toDate().toInstant()
                    .atZone(zoneId).toLocalDateTime()
                    
                Log.d(TAG, "Checking event ${event.id} at ${eventStart} - ${eventEnd} against slot ${startOfSlot} - ${endOfSlot} using timezone ${zoneId.id}")
                
                // Check if event overlaps with this time slot
                (eventStart.isBefore(endOfSlot) || eventStart.isEqual(endOfSlot)) &&
                (eventEnd.isAfter(startOfSlot) || eventEnd.isEqual(startOfSlot))
            } catch (e: Exception) {
                Log.e(TAG, "Error processing event ${event.id} for time slot filtering", e)
                false // Skip events that cause errors
            }
        }
    }
    
    /**
     * Create a sample event (for testing)
     */
    fun createSampleEvent() {
        val now = LocalDateTime.now()
        val startTime = now.plusHours(1)
        val endTime = startTime.plusHours(1)
        
        val event = TimelineEvent(
            title = "Sample Event",
            description = "This is a sample event created for testing",
            startTime = startTime.toTimestamp(),
            endTime = endTime.toTimestamp(),
            eventType = EventType.EVENT,
            category = EventCategory.PERSONAL,
            priority = Priority.MEDIUM,
            location = "Sample Location"
        )
        
        createEvent(event)
    }
    
    /**
     * Check if notification permissions are granted
     */
    fun checkNotificationPermissions() {
        viewModelScope.launch {
            Log.d(TAG, "Checking notification and exact alarm permissions")
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val context = getApplication<Application>().applicationContext
                val notificationPermission = ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                )
                
                val hasPermission = notificationPermission == PackageManager.PERMISSION_GRANTED
                Log.d(TAG, "POST_NOTIFICATIONS permission: $hasPermission (Android ${Build.VERSION.SDK_INT})")
                
                _viewState.update { 
                    it.copy(hasNotificationPermission = hasPermission) 
                }
            } else {
                // For Android 12 and below, notification permission is granted by default
                Log.d(TAG, "POST_NOTIFICATIONS permission: true (default for Android ${Build.VERSION.SDK_INT})")
                _viewState.update { it.copy(hasNotificationPermission = true) }
            }
            
            // Check for exact alarm permission
            val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val canScheduleExactAlarms = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val result = alarmManager.canScheduleExactAlarms()
                Log.d(TAG, "SCHEDULE_EXACT_ALARM permission: $result (Android ${Build.VERSION.SDK_INT})")
                result
            } else {
                Log.d(TAG, "SCHEDULE_EXACT_ALARM permission: true (default for Android ${Build.VERSION.SDK_INT})")
                true // For Android 11 and below, exact alarms are allowed by default
            }
            
            _viewState.update { 
                it.copy(hasExactAlarmPermission = canScheduleExactAlarms) 
            }
            
            // Log the updated state
            Log.d(TAG, "Updated permission state - Notification: ${_viewState.value.hasNotificationPermission}, Exact Alarm: ${_viewState.value.hasExactAlarmPermission}")
        }
    }
    
    /**
     * Create an intent to open the exact alarm permission settings
     * This should be called when the user needs to grant the SCHEDULE_EXACT_ALARM permission
     */
    fun getExactAlarmPermissionSettingsIntent(): Intent? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Log.d(TAG, "Creating intent to open exact alarm permission settings")
            val packageName = getApplication<Application>().packageName
            Intent().apply {
                action = android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                data = android.net.Uri.fromParts("package", packageName, null)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        } else {
            // Not needed for Android 11 and below
            Log.d(TAG, "Exact alarm permission settings not needed for this Android version")
            null
        }
    }
    
    /**
     * Schedule notifications for today's events
     */
    private fun scheduleNotificationsForToday() {
        // Check permissions first
        checkNotificationPermissions()
        
        // Only schedule if we have the necessary permissions
        if (_viewState.value.hasNotificationPermission && _viewState.value.hasExactAlarmPermission) {
            notificationManager.scheduleTodaysEventNotifications()
        } else {
            Log.d(TAG, "Cannot schedule notifications: missing permissions")
        }
    }
    
    /**
     * Send a nudge to the partner
     */
    fun sendNudge() {
        viewModelScope.launch {
            try {
                _viewState.update { it.copy(isLoading = true, error = null) }
                
                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
                if (currentUserId.isBlank()) {
                    _viewState.update { 
                        it.copy(
                            isLoading = false,
                            error = "You must be logged in to send a nudge"
                        )
                    }
                    return@launch
                }
                
                val result = repository.sendNudgeToPartner(currentUserId)
                
                result.fold(
                    onSuccess = {
                        _viewState.update { 
                            it.copy(
                                isLoading = false,
                                nudgeSuccess = true
                            )
                        }
                        
                        // Reset nudge success after a delay
                        viewModelScope.launch {
                            delay(3000) // 3 seconds
                            _viewState.update { it.copy(nudgeSuccess = false) }
                        }
                    },
                    onFailure = { error ->
                        _viewState.update { 
                            it.copy(
                                isLoading = false,
                                error = "Failed to send nudge: ${error.message}"
                            )
                        }
                        Log.e(TAG, "Error sending nudge", error)
                    }
                )
            } catch (e: Exception) {
                _viewState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to send nudge: ${e.message}"
                    )
                }
                Log.e(TAG, "Error sending nudge", e)
            }
        }
    }
}
