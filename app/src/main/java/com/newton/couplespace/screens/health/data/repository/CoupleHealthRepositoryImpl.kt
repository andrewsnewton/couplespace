package com.newton.couplespace.screens.health.data.repository

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.newton.couplespace.screens.health.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of CoupleHealthRepository that uses Firebase for data storage
 */
@Singleton
class CoupleHealthRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val healthConnectRepository: HealthConnectRepository
) : CoupleHealthRepository {
    
    companion object {
        private const val TAG = "CoupleHealthRepo"
    }
    
    private val goalsCollection = firestore.collection("sharedHealthGoals")
    private val challengesCollection = firestore.collection("coupleHealthChallenges")
    private val remindersCollection = firestore.collection("healthReminders")
    private val highlightsCollection = firestore.collection("healthHighlights")
    private val couplesCollection = firestore.collection("couples")
    
    /**
     * Gets the current user's partner ID
     */
    private suspend fun getPartnerId(): String {
        val userId = auth.currentUser?.uid ?: "mock-user-id"
        
        try {
            // Try to get the partner ID from Firestore
            val coupleDoc = couplesCollection
                .whereArrayContains("members", userId)
                .limit(1)
                .get()
                .await()
            
            if (!coupleDoc.isEmpty) {
                val members = coupleDoc.documents[0].get("members") as? List<String>
                if (members != null && members.size == 2) {
                    return members.first { it != userId }
                }
            }
        } catch (e: Exception) {
            // Log error in a real app
        }
        
        // Return a mock partner ID for testing if no real partner found
        return "mock-partner-id"
    }
    
    override suspend fun getUnacknowledgedHighlights(): Flow<List<HealthHighlight>> = flow {
        val userId = auth.currentUser?.uid ?: "mock-user-id"
        
        try {
            val highlightsSnapshot = highlightsCollection
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("acknowledged", false)
                .get()
                .await()
            
            val highlights = highlightsSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                
                HealthHighlight(
                    id = doc.id,
                    title = data["title"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    timestamp = (data["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.toInstant()?.toEpochMilli() 
                        ?: Instant.now().toEpochMilli(),
                    type = try {
                        HighlightType.valueOf(data["type"] as? String ?: "GOAL_ACHIEVED")
                    } catch (e: Exception) {
                        HighlightType.GOAL_ACHIEVED
                    }
                )
            }
            
            emit(highlights)
        } catch (e: Exception) {
            // In a real app, log the error
            // For now, emit mock data
            val mockHighlights = listOf(
                HealthHighlight(
                    id = UUID.randomUUID().toString(),
                    title = "Sleep Goal Achievement",
                    description = "Your partner had a great night's sleep!",
                    timestamp = Instant.now().minus(1, java.time.temporal.ChronoUnit.DAYS).toEpochMilli(),
                    type = HighlightType.PARTNER_ACHIEVEMENT
                ),
                HealthHighlight(
                    id = UUID.randomUUID().toString(),
                    title = "Step Goal Achievement",
                    description = "Great job on hitting your step goal!",
                    timestamp = Instant.now().toEpochMilli(),
                    type = HighlightType.GOAL_ACHIEVED
                ),
            )
            emit(mockHighlights)
        }
    }
    
    override suspend fun acknowledgeHighlight(highlightId: String) {
        try {
            highlightsCollection.document(highlightId)
                .update("acknowledged", true)
                .await()
        } catch (e: Exception) {
            // In a real app, log the error
        }
    }
    
    override suspend fun createHealthHighlight(
        metricType: HealthMetricType,
        achievement: String,
        message: String
    ) {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
        
        try {
            val highlightType = getHighlightTypeFromMetric(metricType)
            val title = achievement
            val description = message
            
            val highlightData = hashMapOf(
                "title" to title,
                "description" to description,
                "timestamp" to Instant.now().toEpochMilli(),
                "type" to highlightType.name,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            
            highlightsCollection.add(highlightData).await()
        } catch (e: Exception) {
            // In a real app, log the error
        }
    }
    
    private fun getHighlightTypeFromMetric(metricType: HealthMetricType): HighlightType {
        return when (metricType) {
            HealthMetricType.WATER_INTAKE -> HighlightType.GOAL_ACHIEVED
            HealthMetricType.STEPS -> HighlightType.GOAL_ACHIEVED
            HealthMetricType.SLEEP -> HighlightType.GOAL_ACHIEVED
            HealthMetricType.HEART_RATE -> HighlightType.PERSONAL_BEST
            HealthMetricType.WEIGHT -> HighlightType.PERSONAL_BEST
            HealthMetricType.CALORIES_BURNED -> HighlightType.STREAK_MILESTONE
            HealthMetricType.ACTIVE_MINUTES -> HighlightType.STREAK_MILESTONE
            HealthMetricType.DISTANCE -> HighlightType.PERSONAL_BEST
            HealthMetricType.MEAL -> HighlightType.STREAK_MILESTONE
            else -> HighlightType.PARTNER_ACHIEVEMENT
        }
    }
    
    override suspend fun getPendingReminders(): Flow<List<HealthReminder>> = flow {
        val userId = auth.currentUser?.uid ?: "mock-user-id"
        
        try {
            val remindersSnapshot = remindersCollection
                .whereEqualTo("recipientId", userId)
                .whereEqualTo("status", "PENDING")
                .get()
                .await()
            
            val reminders = remindersSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                
                HealthReminder(
                    id = doc.id,
                    title = data["title"] as? String ?: "",
                    message = data["message"] as? String ?: "",
                    timestamp = (data["timestamp"] as? com.google.firebase.Timestamp)?.toDate()?.toInstant()?.toEpochMilli() 
                        ?: Instant.now().toEpochMilli(),
                    type = try {
                        HealthReminderType.valueOf(data["type"] as? String ?: "OTHER")
                    } catch (e: Exception) {
                        HealthReminderType.OTHER
                    },
                    status = try {
                        ReminderStatus.valueOf(data["status"] as? String ?: "PENDING")
                    } catch (e: Exception) {
                        ReminderStatus.PENDING
                    },
                    senderId = data["senderId"] as? String ?: "",
                    recipientId = userId
                )
            }
            
            emit(reminders)
        } catch (e: Exception) {
            // In a real app, log the error
            // For now, emit mock data
            val mockReminders = listOf(
                HealthReminder(
                    id = UUID.randomUUID().toString(),
                    title = "Meal Reminder",
                    message = "Don't forget to log your meals for today.",
                    timestamp = Instant.now().toEpochMilli(),
                    type = HealthReminderType.MEAL,
                    status = ReminderStatus.PENDING,
                    senderId = userId,
                    recipientId = getPartnerId()
                ),
                HealthReminder(
                    id = UUID.randomUUID().toString(),
                    title = "Hydration Reminder",
                    message = "Don't forget to drink water today!",
                    timestamp = Instant.now().toEpochMilli(),
                    type = HealthReminderType.WATER_INTAKE,
                    status = ReminderStatus.PENDING,
                    senderId = userId,
                    recipientId = getPartnerId()
                ),
            )
            emit(mockReminders)
        }
    }
    
    override suspend fun sendHealthReminder(type: HealthReminderType, message: String) {
        val userId = auth.currentUser?.uid ?: return
        val partnerId = getPartnerId()
        
        try {
            val reminderData = hashMapOf(
                "senderId" to userId,
                "recipientId" to partnerId,
                "title" to getReminderTitle(type),
                "message" to message,
                "type" to type.name,
                "timestamp" to com.google.firebase.Timestamp.now(),
                "dueDate" to com.google.firebase.Timestamp.now(),
                "status" to "PENDING"
            )
            
            remindersCollection.add(reminderData).await()
        } catch (e: Exception) {
            // In a real app, log the error
        }
    }
    
    private fun getReminderTitle(type: HealthReminderType): String {
        return when (type) {
            HealthReminderType.WATER_INTAKE -> "Drink Water Reminder"
            HealthReminderType.MEAL -> "Meal Reminder"
            HealthReminderType.STEP_GOAL -> "Activity Reminder"
            HealthReminderType.SLEEP_SCHEDULE -> "Sleep Reminder"
            HealthReminderType.WORKOUT -> "Workout Reminder"
            HealthReminderType.MEDICATION -> "Medication Reminder"
            HealthReminderType.OTHER -> "Health Reminder"
        }
    }
    
    override suspend fun acknowledgeReminder(reminderId: String, status: ReminderStatus) {
        try {
            remindersCollection.document(reminderId)
                .update("status", status.name)
                .await()
        } catch (e: Exception) {
            // In a real app, log the error
        }
    }
    
    override suspend fun getActiveSharedGoals(): Flow<List<SharedHealthGoal>> = flow {
        val userId = auth.currentUser?.uid ?: "mock-user-id"
        
        try {
            val goalsSnapshot = goalsCollection
                .whereArrayContains("participants", userId)
                .whereGreaterThan("endDate", com.google.firebase.Timestamp.now())
                .get()
                .await()
            
            val goals = goalsSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                
                val participants = data["participants"] as? List<String> ?: listOf()
                val isCreator = participants.getOrNull(0) == userId
                
                SharedHealthGoal(
                    id = doc.id,
                    title = data["title"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    type = try {
                        SharedGoalType.valueOf(data["type"] as? String ?: "STEPS")
                    } catch (e: Exception) {
                        SharedGoalType.STEPS
                    },
                    target = (data["target"] as? Number)?.toInt() ?: 0,
                    progress = (data["progress"] as? Number)?.toInt() ?: 0,
                    startDate = (data["startDate"] as? com.google.firebase.Timestamp)?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate() 
                        ?: LocalDate.now(),
                    endDate = (data["endDate"] as? com.google.firebase.Timestamp)?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate() 
                        ?: LocalDate.now().plusDays(7),
                    isCompleted = (data["isCompleted"] as? Boolean) ?: false,
                    creatorId = participants.getOrNull(0) ?: userId,
                    partnerId = participants.getOrNull(1) ?: getPartnerId()
                )
            }
            
            emit(goals)
        } catch (e: Exception) {
            // In a real app, log the error
            // For now, emit mock data
            val mockGoals = listOf(
                SharedHealthGoal(
                    id = "1",
                    title = "10,000 Steps Challenge",
                    description = "Reach 10,000 steps every day this week",
                    type = SharedGoalType.STEPS,
                    target = 10000,
                    progress = 7500,
                    startDate = LocalDate.now().minusDays(2),
                    endDate = LocalDate.now().plusDays(5),
                    isCompleted = false,
                    creatorId = userId,
                    partnerId = getPartnerId()
                ),
                SharedHealthGoal(
                    id = "2",
                    title = "8 Hours Sleep Goal",
                    description = "Get 8 hours of sleep every night",
                    type = SharedGoalType.SLEEP_DURATION,
                    target = 8,
                    progress = 7,
                    startDate = LocalDate.now().minusDays(1),
                    endDate = LocalDate.now().plusDays(6),
                    isCompleted = false,
                    creatorId = getPartnerId(),
                    partnerId = userId
                )
            )
            emit(mockGoals)
        }
    }
    
    override suspend fun createSharedGoal(
        type: SharedGoalType,
        target: Int,
        startDate: LocalDate,
        endDate: LocalDate
    ): String {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
        val partnerId = getPartnerId()
        
        try {
            val title = getGoalTitle(type, target)
            val description = getGoalDescription(type, target)
            
            val goalData = hashMapOf(
                "title" to title,
                "description" to description,
                "type" to type.name,
                "target" to target,
                "progress" to 0,
                "startDate" to com.google.firebase.Timestamp(startDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0),
                "endDate" to com.google.firebase.Timestamp(endDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0),
                "isCompleted" to false,
                "creatorId" to userId,
                "partnerId" to partnerId,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            
            val docRef = goalsCollection.add(goalData).await()
            return docRef.id
        } catch (e: Exception) {
            // In a real app, log the error
            return "mock-goal-id-${UUID.randomUUID()}"
        }
    }
    
    private fun getGoalTitle(type: SharedGoalType, target: Int): String {
        return when (type) {
            SharedGoalType.STEPS -> "$target Steps Goal"
            SharedGoalType.DISTANCE -> "$target Meters Distance Goal"
            SharedGoalType.ACTIVE_MINUTES -> "$target Active Minutes Goal"
            SharedGoalType.WATER_INTAKE -> "$target ml Water Intake Goal"
            SharedGoalType.SLEEP_DURATION -> "$target Hours of Sleep Goal"
            SharedGoalType.WEIGHT_LOSS -> "$target kg Weight Loss Goal"
            SharedGoalType.CALORIES_BURNED -> "$target Calories Burned Goal"
        }
    }
    
    private fun getGoalDescription(type: SharedGoalType, target: Int): String {
        return when (type) {
            SharedGoalType.STEPS -> "Reach $target steps every day"
            SharedGoalType.DISTANCE -> "Cover $target meters distance daily"
            SharedGoalType.ACTIVE_MINUTES -> "Complete $target minutes of activity daily"
            SharedGoalType.WATER_INTAKE -> "Drink $target ml of water daily"
            SharedGoalType.SLEEP_DURATION -> "Get $target hours of sleep every night"
            SharedGoalType.WEIGHT_LOSS -> "Lose $target kg of weight"
            SharedGoalType.CALORIES_BURNED -> "Burn $target calories through exercise"
        }
    }
    
    override suspend fun updateGoalProgress(goalId: String, progress: Int) {
        val userId = auth.currentUser?.uid ?: return
        
        try {
            // Get the goal to determine if the user is the creator or partner
            val goalDoc = goalsCollection.document(goalId).get().await()
            val participants = goalDoc.get("participants") as? List<String> ?: return
            
            val field = if (participants.getOrNull(0) == userId) "creatorProgress" else "partnerProgress"
            
            goalsCollection.document(goalId)
                .update(field, progress)
                .await()
        } catch (e: Exception) {
            // In a real app, log the error
        }
    }
    
    override suspend fun getActiveChallenges(): Flow<List<CoupleChallenge>> = flow {
        val userId = auth.currentUser?.uid ?: "mock-user-id"
        
        try {
            val challengesSnapshot = challengesCollection
                .whereArrayContains("participants", userId)
                .whereGreaterThan("endDate", com.google.firebase.Timestamp.now())
                .get()
                .await()
            
            val challenges = challengesSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                
                val participants = data["participants"] as? List<String> ?: listOf()
                val isCreator = participants.getOrNull(0) == userId
                
                CoupleChallenge(
                    id = doc.id,
                    title = data["title"] as? String ?: "",
                    description = data["description"] as? String ?: "",
                    type = try {
                        SharedGoalType.valueOf(data["type"] as? String ?: "STEPS")
                    } catch (e: Exception) {
                        SharedGoalType.STEPS
                    },
                    durationType = try {
                        ChallengeDurationType.valueOf(data["durationType"] as? String ?: "DAILY")
                    } catch (e: Exception) {
                        ChallengeDurationType.DAILY
                    },
                    creatorProgress = (data["creatorProgress"] as? Number)?.toInt() ?: 0,
                    partnerProgress = (data["partnerProgress"] as? Number)?.toInt() ?: 0,
                    startDate = (data["startDate"] as? com.google.firebase.Timestamp)?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate() 
                        ?: LocalDate.now(),
                    endDate = (data["endDate"] as? com.google.firebase.Timestamp)?.toDate()?.toInstant()?.atZone(ZoneId.systemDefault())?.toLocalDate() 
                        ?: LocalDate.now().plusDays(7),
                    isCreator = (data["participants"] as? List<*>)?.getOrNull(0) == userId
                )
            }
            
            emit(challenges)
        } catch (e: Exception) {
            // In a real app, log the error
            // For now, emit mock data
            val mockChallenges = listOf(
                CoupleChallenge(
                    id = "1",
                    title = "Weekly Step Challenge",
                    description = "Who can get the most steps this week?",
                    type = SharedGoalType.STEPS,
                    durationType = ChallengeDurationType.WEEKLY,
                    creatorProgress = 25000,
                    partnerProgress = 27500,
                    startDate = LocalDate.now().minusDays(3),
                    endDate = LocalDate.now().plusDays(4),
                    isCreator = true
                )
            )
            emit(mockChallenges)
        }
    }
    
    override suspend fun createChallenge(
        title: String, 
        description: String, 
        type: SharedGoalType, 
        durationType: ChallengeDurationType, 
        startDate: LocalDate, 
        endDate: LocalDate
    ): String {
        val userId = auth.currentUser?.uid ?: throw IllegalStateException("User not authenticated")
        val partnerId = getPartnerId()
        
        try {
            val challengeData = hashMapOf(
                "title" to title,
                "description" to description,
                "type" to type.name,
                "durationType" to durationType.name,
                "creatorProgress" to 0,
                "partnerProgress" to 0,
                "startDate" to com.google.firebase.Timestamp(startDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0),
                "endDate" to com.google.firebase.Timestamp(endDate.atStartOfDay(ZoneId.systemDefault()).toEpochSecond(), 0),
                "participants" to listOf(userId, partnerId),
                "isCreator" to true,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            
            val docRef = challengesCollection.add(challengeData).await()
            return docRef.id
        } catch (e: Exception) {
            // In a real app, log the error
            return "mock-challenge-id-${UUID.randomUUID()}"
        }
    }
    
    override suspend fun updateChallengeProgress(challengeId: String, progress: Int) {
        val userId = auth.currentUser?.uid ?: return
        
        try {
            // Get the challenge to determine if the user is the creator or partner
            val challengeDoc = challengesCollection.document(challengeId).get().await()
            val participants = challengeDoc.get("participants") as? List<String> ?: return
            
            val field = if (participants.getOrNull(0) == userId) "creatorProgress" else "partnerProgress"
            
            challengesCollection.document(challengeId)
                .update(field, progress)
                .await()
        } catch (e: Exception) {
            // In a real app, log the error
        }
    }
    
    // Partner Health Data Methods
    
    /**
     * Checks if the user has a connected partner
     */
    override suspend fun hasConnectedPartner(): Boolean {
        val userId = auth.currentUser?.uid ?: return false
        
        try {
            val coupleDoc = couplesCollection
                .whereArrayContains("members", userId)
                .limit(1)
                .get()
                .await()
            
            return !coupleDoc.isEmpty
        } catch (e: Exception) {
            // In a real app, log the error
            return false
        }
    }
    
    /**
     * Gets partner health metrics for a specific date
     */
    override suspend fun getPartnerHealthMetrics(date: LocalDate): Flow<List<HealthMetric>> = flow {
        val partnerId = getPartnerId()
        
        try {
            // In a real app, this would fetch the partner's shared metrics from Firestore
            // For now, we'll return mock data
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault())
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault())
            
            val sharedMetricsCollection = firestore.collection("sharedHealthMetrics")
            val metricsSnapshot = sharedMetricsCollection
                .whereEqualTo("userId", partnerId)
                .whereEqualTo("isShared", true)
                .whereGreaterThanOrEqualTo("timestamp", com.google.firebase.Timestamp(startOfDay.toEpochSecond(), 0))
                .whereLessThan("timestamp", com.google.firebase.Timestamp(endOfDay.toEpochSecond(), 0))
                .get()
                .await()
                
            if (!metricsSnapshot.isEmpty) {
                // Parse real metrics from Firestore
                val metrics = mutableListOf<HealthMetric>()
                
                for (document in metricsSnapshot.documents) {
                    val metricType = document.getString("type") ?: continue
                    val metricId = document.id
                    val userId = document.getString("userId") ?: continue
                    val timestamp = document.getTimestamp("timestamp")?.toDate()?.toInstant() ?: continue
                    val isShared = document.getBoolean("isShared") ?: false
                    val source = document.getString("source") ?: "Unknown"
                    
                    when (metricType) {
                        "STEPS" -> {
                            val steps = document.getLong("steps")?.toInt() ?: 0
                            metrics.add(StepsMetric(
                                id = metricId,
                                userId = userId,
                                timestamp = timestamp,
                                count = steps,
                                source = source,
                                isShared = isShared
                            ))
                    }
                    "HEART_RATE" -> {
                        val heartRateData = document.data
                        val heartRateMetric = HeartRateMetric(
                            id = metricId,
                            userId = userId,
                            timestamp = timestamp,
                            beatsPerMinute = heartRateData?.get("beatsPerMinute") as? Int ?: 70,
                            source = heartRateData?.get("source") as? String ?: "Partner's Health Connect",
                            isShared = isShared
                        )
                        metrics.add(heartRateMetric)
                    }
                    "SLEEP" -> {
                        val sleepData = document.data
                        val sleepMetric = SleepMetric(
                            id = metricId,
                            userId = userId,
                            timestamp = timestamp,
                            durationHours = (sleepData?.get("durationHours") as? Float) ?: 7f,
                            startTime = (sleepData?.get("startTime") as? Long)?.let { Instant.ofEpochMilli(it) },
                            endTime = (sleepData?.get("endTime") as? Long)?.let { Instant.ofEpochMilli(it) },
                            quality = (sleepData?.get("quality") as? String)?.let { 
                                try { SleepMetric.SleepQuality.valueOf(it) } catch (e: Exception) { null }
                            },
                            source = sleepData?.get("source") as? String ?: "Partner's Health Connect",
                            isShared = isShared
                        )
                        metrics.add(sleepMetric)
                    }
                    "CALORIES_BURNED" -> {
                        val calories = document.getLong("calories")?.toInt() ?: 0
                        metrics.add(CaloriesBurnedMetric(
                            id = metricId,
                            userId = userId,
                            timestamp = timestamp,
                            calories = calories,
                            source = source,
                            isShared = isShared
                        ))
                    }
                    "ACTIVE_MINUTES" -> {
                        val activeData = document.data
                        val activeMinutesMetric = ActiveMinutesMetric(
                            id = metricId,
                            userId = userId,
                            timestamp = timestamp,
                            minutes = activeData?.get("minutes") as? Int ?: 30,
                            intensity = ActiveMinutesMetric.ActivityIntensity.MODERATE,
                            source = activeData?.get("source") as? String ?: "Partner's Health Connect",
                            isShared = isShared
                        )
                        metrics.add(activeMinutesMetric)
                    }
                }
            }
            
            Log.d(TAG, "Parsed ${metrics.size} partner metrics from Firestore")
            emit(metrics)
        } else {
            // Return mock data for demo purposes
            val mockMetrics = createMockPartnerMetrics(date)
            emit(mockMetrics)
        }
    } catch (e: Exception) {
        // In a real app, log the error
        val mockMetrics = createMockPartnerMetrics(date)
        emit(mockMetrics)
    }
}

// ...

private fun createMockPartnerMetrics(date: LocalDate): List<HealthMetric> {
    val metrics = mutableListOf<HealthMetric>()
    val midTime = date.atTime(12, 0).toInstant(java.time.ZoneOffset.UTC)
    
    // Mock steps
    metrics.add(
        StepsMetric(
            id = "partner-steps-${date}",
            userId = "partner-id",
            timestamp = midTime,
            count = 8500 + (Math.random() * 2000).toInt(),
            source = "Partner's Health Connect",
            isShared = true
        )
    )
    
    // Mock heart rate
    metrics.add(
        HeartRateMetric(
            id = "partner-heart-rate-${date}",
            userId = "partner-id",
            timestamp = midTime,
            beatsPerMinute = 65 + (Math.random() * 10).toInt(),
            source = "Partner's Health Connect",
            isShared = true
        )
    )
    
    // Mock sleep
    metrics.add(
        SleepMetric(
            id = "partner-sleep-${date}",
            userId = "partner-id",
            timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
            durationHours = (420 + (Math.random() * 60).toInt()) / 60f, // Convert minutes to hours
            source = "Partner's Health Connect",
            isShared = true
        )
    )
    
    // Mock calories burned
    metrics.add(
        CaloriesBurnedMetric(
            id = "partner-calories-${date}",
            userId = "partner-id",
            timestamp = midTime,
            calories = 1800 + (Math.random() * 400).toInt(),
            source = "Partner's Health Connect",
            isShared = true
        )
    )
    
    // Mock active minutes
    metrics.add(
        ActiveMinutesMetric(
            id = "partner-active-${date}",
            userId = "partner-id",
            timestamp = midTime,
            minutes = 45 + (Math.random() * 30).toInt(),
            intensity = ActiveMinutesMetric.ActivityIntensity.MODERATE,
            source = "Partner's Health Connect",
            isShared = true
        )
    )
    
    return metrics
}

    /**
     * Share a health metric with partner
     */
    override suspend fun shareHealthMetric(metric: HealthMetric): Boolean {
        try {
            val userId = auth.currentUser?.uid ?: return false
            val partnerId = getPartnerId()
            
            // Create a copy of the metric with sharing flag set to true
            val sharedMetric = when (metric) {
                is StepsMetric -> metric.copy(isShared = true)
                is HeartRateMetric -> metric.copy(isShared = true)
                is SleepMetric -> metric.copy(isShared = true)
                is CaloriesBurnedMetric -> metric.copy(isShared = true)
                is ActiveMinutesMetric -> metric.copy(isShared = true)
                else -> return false // Unsupported metric type
            }
            
            // Save to Firestore in the partner's collection
            val baseMetricData = mapOf(
                "id" to sharedMetric.id,
                "userId" to userId,
                "timestamp" to sharedMetric.timestamp.toEpochMilli(),
                "type" to sharedMetric.type.name,
                "isShared" to true
            )
            
            // Add source based on metric type
            val sourceData = when (sharedMetric) {
                is StepsMetric -> mapOf("source" to sharedMetric.source)
                is HeartRateMetric -> mapOf("source" to sharedMetric.source)
                is SleepMetric -> mapOf("source" to sharedMetric.source)
                is CaloriesBurnedMetric -> mapOf("source" to sharedMetric.source)
                is ActiveMinutesMetric -> mapOf("source" to sharedMetric.source)
                else -> mapOf("source" to "Health Connect")
            }
            
            val metricData = baseMetricData + sourceData
            
            // Add type-specific data
            val typeSpecificData = when (sharedMetric) {
                is StepsMetric -> mapOf("count" to sharedMetric.count)
                is HeartRateMetric -> mapOf("beatsPerMinute" to sharedMetric.beatsPerMinute)
                is SleepMetric -> mapOf(
                    "durationHours" to sharedMetric.durationHours,
                    "startTime" to (sharedMetric.startTime?.toEpochMilli() ?: 0L),
                    "endTime" to (sharedMetric.endTime?.toEpochMilli() ?: 0L),
                    "quality" to (sharedMetric.quality?.name ?: "")
                )
                is CaloriesBurnedMetric -> mapOf("calories" to sharedMetric.calories)
                is ActiveMinutesMetric -> mapOf(
                    "minutes" to sharedMetric.minutes,
                    "intensity" to sharedMetric.intensity.name
                )
                else -> mapOf<String, Any>()
            }
            
            // Combine the data
            val combinedData = metricData + typeSpecificData
            
            // Save to Firestore in the shared metrics collection
            firestore.collection("users")
                .document(partnerId)
                .collection("sharedHealthMetrics")
                .document(sharedMetric.id)
                .set(combinedData)
                .await()
                
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error sharing health metric", e)
            return false
        }
    }
}
