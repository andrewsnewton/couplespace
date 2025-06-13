package com.newton.couplespace.screens.health.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newton.couplespace.screens.health.data.models.*
import com.newton.couplespace.screens.health.data.repository.CoupleHealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for couple-focused health features
 */
@HiltViewModel
class CoupleHealthViewModel @Inject constructor(
    private val coupleHealthRepository: CoupleHealthRepository
) : ViewModel() {
    
    // Active shared goals
    private val _sharedGoals = MutableStateFlow<List<SharedHealthGoal>>(emptyList())
    val sharedGoals: StateFlow<List<SharedHealthGoal>> = _sharedGoals.asStateFlow()
    
    // Active challenges
    private val _challenges = MutableStateFlow<List<CoupleChallenge>>(emptyList())
    val challenges: StateFlow<List<CoupleChallenge>> = _challenges.asStateFlow()
    
    // Pending reminders to partner
    private val _pendingReminders = MutableStateFlow<List<HealthReminder>>(emptyList())
    val pendingReminders: StateFlow<List<HealthReminder>> = _pendingReminders.asStateFlow()
    
    // Partner health highlights
    private val _partnerHighlights = MutableStateFlow<List<HealthHighlight>>(emptyList())
    val partnerHighlights: StateFlow<List<HealthHighlight>> = _partnerHighlights.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // Goal creation form state
    private val _newGoalState = MutableStateFlow(NewGoalState())
    val newGoalState: StateFlow<NewGoalState> = _newGoalState.asStateFlow()
    
    // Challenge creation form state
    private val _newChallengeState = MutableStateFlow(NewChallengeState())
    val newChallengeState: StateFlow<NewChallengeState> = _newChallengeState.asStateFlow()
    
    init {
        loadCoupleHealthData()
    }
    
    /**
     * Loads all couple health data
     */
    private fun loadCoupleHealthData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            // Load shared goals
            coupleHealthRepository.getActiveSharedGoals().collect { goals ->
                _sharedGoals.value = goals
            }
            
            // Load challenges
            coupleHealthRepository.getActiveChallenges().collect { challengesList ->
                _challenges.value = challengesList
            }
            
            // Load partner highlights
            coupleHealthRepository.getUnacknowledgedHighlights().collect { highlights ->
                _partnerHighlights.value = highlights
            }
            
            // Load pending reminders
            coupleHealthRepository.getPendingReminders().collect { reminders ->
                _pendingReminders.value = reminders
            }
            
            _isLoading.value = false
        }
    }
    
    /**
     * Creates a new shared health goal
     */
    fun createSharedGoal() {
        val goalState = _newGoalState.value
        
        if (!goalState.isValid()) {
            // Handle validation errors
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            coupleHealthRepository.createSharedGoal(
                type = goalState.type,
                target = goalState.target,
                startDate = goalState.startDate,
                endDate = goalState.endDate
            )
            
            // Reset form state
            _newGoalState.value = NewGoalState()
            
            // Reload data to include the new goal
            loadCoupleHealthData()
        }
    }
    
    /**
     * Updates the progress of a shared goal
     */
    fun updateGoalProgress(goalId: String, progress: Int) {
        viewModelScope.launch {
            coupleHealthRepository.updateGoalProgress(goalId, progress)
            
            // Update local state to reflect the change
            _sharedGoals.update { goals ->
                goals.map { goal ->
                    if (goal.id == goalId) {
                        // Update the progress field
                        goal.copy(progress = progress)
                    } else {
                        goal
                    }
                }
            }
        }
    }
    
    /**
     * Creates a new challenge
     */
    fun createChallenge() {
        val challengeState = _newChallengeState.value
        
        if (!challengeState.isValid()) {
            // Handle validation errors
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            
            coupleHealthRepository.createChallenge(
                title = challengeState.title,
                description = challengeState.description,
                type = challengeState.type,
                durationType = challengeState.durationType,
                startDate = challengeState.startDate,
                endDate = challengeState.endDate
            )
            
            // Reset form state
            _newChallengeState.value = NewChallengeState()
            
            // Reload data to include the new challenge
            loadCoupleHealthData()
        }
    }
    
    /**
     * Updates the progress of a challenge
     */
    fun updateChallengeProgress(challengeId: String, progress: Int) {
        viewModelScope.launch {
            coupleHealthRepository.updateChallengeProgress(challengeId, progress)
            
            // Update local state to reflect the change
            _challenges.update { challenges ->
                challenges.map { challenge ->
                    if (challenge.id == challengeId) {
                        // Note: This is a simplification. In reality, we'd need to determine 
                        // if the current user is the creator or partner
                        challenge.copy(creatorProgress = progress)
                    } else {
                        challenge
                    }
                }
            }
        }
    }
    
    /**
     * Sends a health reminder to the partner
     */
    fun sendHealthReminder(type: HealthReminderType, message: String) {
        if (message.isBlank()) {
            // Handle error - message cannot be blank
            return
        }
        
        viewModelScope.launch {
            coupleHealthRepository.sendHealthReminder(type, message)
        }
    }
    
    /**
     * Acknowledges a health reminder
     */
    fun acknowledgeReminder(reminderId: String, status: ReminderStatus) {
        viewModelScope.launch {
            coupleHealthRepository.acknowledgeReminder(reminderId, status)
            
            // Remove the reminder from the pending list
            _pendingReminders.update { reminders ->
                reminders.filter { it.id != reminderId }
            }
        }
    }
    
    /**
     * Creates a health highlight for the partner
     */
    fun createHealthHighlight(metricType: HealthMetricType, achievement: String, message: String) {
        if (achievement.isBlank() || message.isBlank()) {
            // Handle error - fields cannot be blank
            return
        }
        
        viewModelScope.launch {
            coupleHealthRepository.createHealthHighlight(metricType, achievement, message)
        }
    }
    
    /**
     * Acknowledges a health highlight
     */
    fun acknowledgeHighlight(highlightId: String) {
        viewModelScope.launch {
            coupleHealthRepository.acknowledgeHighlight(highlightId)
            
            // Remove the highlight from the list
            _partnerHighlights.update { highlights ->
                highlights.filter { it.id != highlightId }
            }
        }
    }
    
    /**
     * Updates the goal creation form state
     */
    fun updateNewGoalState(update: (NewGoalState) -> NewGoalState) {
        _newGoalState.update(update)
    }
    
    /**
     * Updates the challenge creation form state
     */
    fun updateNewChallengeState(update: (NewChallengeState) -> NewChallengeState) {
        _newChallengeState.update(update)
    }
    
    /**
     * Represents the state of the goal creation form
     */
    data class NewGoalState(
        val type: SharedGoalType = SharedGoalType.STEPS,
        val target: Int = 0,
        val startDate: LocalDate = LocalDate.now(),
        val endDate: LocalDate = LocalDate.now().plusDays(7),
        val validationErrors: Map<String, String> = emptyMap()
    ) {
        fun isValid(): Boolean {
            return target > 0 && 
                   startDate.isBefore(endDate) &&
                   validationErrors.isEmpty()
        }
    }
    
    /**
     * Represents the state of the challenge creation form
     */
    data class NewChallengeState(
        val title: String = "",
        val description: String = "",
        val type: SharedGoalType = SharedGoalType.STEPS,
        val durationType: ChallengeDurationType = ChallengeDurationType.DAILY,
        val startDate: LocalDate = LocalDate.now(),
        val endDate: LocalDate = LocalDate.now().plusDays(1),
        val validationErrors: Map<String, String> = emptyMap()
    ) {
        fun isValid(): Boolean {
            return title.isNotBlank() && 
                   startDate.isBefore(endDate) && 
                   validationErrors.isEmpty()
        }
    }
}
