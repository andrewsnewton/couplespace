package com.newton.couplespace.screens.main.timeline

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.newton.couplespace.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
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
    
    // UI State
    private val _viewState = MutableStateFlow(TimelineViewState())
    val viewState: StateFlow<TimelineViewState> = _viewState
    
    // Selected event
    private val _selectedEvent = MutableStateFlow<TimelineEvent?>(null)
    val selectedEvent: StateFlow<TimelineEvent?> = _selectedEvent
    
    init {
        try {
            // Initialize repository with application context
            repository = EnhancedTimelineRepository(getApplication<Application>().applicationContext)
            Log.d(TAG, "TimelineViewModel initialized successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing TimelineViewModel", e)
            throw e
        }
    }
    
    init {
        loadEventsForCurrentView()
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
     * Load events for the current view
     */
    fun loadEventsForCurrentView() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                _viewState.update { it.copy(isLoading = true, error = null) }
                
                val currentState = _viewState.value
                val (startDate, endDate) = getDateRangeForView(
                    currentState.selectedDate, 
                    currentState.viewMode
                )
                
                val result = repository.loadTimelineEvents(startDate, endDate)
                
                result.fold(
                    onSuccess = { events ->
                        withContext(Dispatchers.Main) {
                            _viewState.update { 
                                it.copy(
                                    events = events,
                                    isLoading = false,
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
     * Create a new event
     */
    fun createEvent(event: TimelineEvent, isPartnerEvent: Boolean = false) {
        viewModelScope.launch {
            try {
                _viewState.update { it.copy(isLoading = true, error = null) }
                
                val result = repository.saveTimelineEvent(event, isPartnerEvent)
                
                result.fold(
                    onSuccess = { eventId ->
                        Log.d(TAG, "Event created with ID: $eventId")
                        loadEventsForCurrentView()
                    },
                    onFailure = { error ->
                        _viewState.update { 
                            it.copy(
                                isLoading = false,
                                error = "Failed to create event: ${error.message}"
                            )
                        }
                        Log.e(TAG, "Error creating event", error)
                    }
                )
            } catch (e: Exception) {
                _viewState.update { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to create event: ${e.message}"
                    )
                }
                Log.e(TAG, "Error creating event", e)
            }
        }
    }
    
    /**
     * Update an existing event
     */
    fun updateEvent(event: TimelineEvent) {
        viewModelScope.launch {
            try {
                _viewState.update { it.copy(isLoading = true, error = null) }
                
                val result = repository.updateTimelineEvent(event)
                
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Event updated: ${event.id}")
                        loadEventsForCurrentView()
                        
                        // If this is the selected event, update it
                        if (_selectedEvent.value?.id == event.id) {
                            _selectedEvent.value = event
                            _viewState.update { it.copy(selectedEvent = event) }
                        }
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
                
                val result = repository.deleteTimelineEvent(eventId)
                
                result.fold(
                    onSuccess = {
                        Log.d(TAG, "Event deleted: $eventId")
                        loadEventsForCurrentView()
                        
                        // If this is the selected event, clear it
                        if (_selectedEvent.value?.id == eventId) {
                            clearSelectedEvent()
                        }
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
        return _viewState.value.events.filter { event ->
            val eventDate = event.startTime.toDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate()
            eventDate.isEqual(date)
        }
    }
    
    /**
     * Get events for a specific time slot
     */
    fun getEventsForTimeSlot(date: LocalDate, time: LocalTime): List<TimelineEvent> {
        val startOfSlot = LocalDateTime.of(date, time)
        val endOfSlot = startOfSlot.plusMinutes(30)
        
        return _viewState.value.events.filter { event ->
            val eventStart = event.startTime.toDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime()
            val eventEnd = event.endTime.toDate().toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDateTime()
                
            // Check if event overlaps with this time slot
            (eventStart.isBefore(endOfSlot) || eventStart.isEqual(endOfSlot)) &&
            (eventEnd.isAfter(startOfSlot) || eventEnd.isEqual(startOfSlot))
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
