package com.newton.couplespace.screens.health.data.repository

import com.newton.couplespace.screens.health.data.models.*
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository to manage couple-focused health data like shared goals and challenges
 */
interface CoupleHealthRepository {
    // Highlights
    suspend fun getUnacknowledgedHighlights(): Flow<List<HealthHighlight>>
    suspend fun acknowledgeHighlight(highlightId: String)
    suspend fun createHealthHighlight(metricType: HealthMetricType, achievement: String, message: String)
    
    // Reminders
    suspend fun getPendingReminders(): Flow<List<HealthReminder>>
    suspend fun sendHealthReminder(type: HealthReminderType, message: String)
    suspend fun acknowledgeReminder(reminderId: String, status: ReminderStatus)
    
    // Shared Goals
    suspend fun getActiveSharedGoals(): Flow<List<SharedHealthGoal>>
    suspend fun createSharedGoal(type: SharedGoalType, target: Int, startDate: LocalDate, endDate: LocalDate): String
    suspend fun updateGoalProgress(goalId: String, progress: Int)
    
    // Challenges
    suspend fun getActiveChallenges(): Flow<List<CoupleChallenge>>
    suspend fun createChallenge(title: String, description: String, type: SharedGoalType, 
                              durationType: ChallengeDurationType, startDate: LocalDate, endDate: LocalDate): String
    suspend fun updateChallengeProgress(challengeId: String, progress: Int)
    
    // Partner Health Data
    suspend fun hasConnectedPartner(): Boolean
    suspend fun getPartnerHealthMetrics(date: LocalDate): Flow<List<HealthMetric>>
    suspend fun shareHealthMetric(metric: HealthMetric): Boolean
}
