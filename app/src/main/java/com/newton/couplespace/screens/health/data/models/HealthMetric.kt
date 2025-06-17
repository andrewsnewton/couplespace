package com.newton.couplespace.screens.health.data.models

import java.time.Instant
import java.time.LocalDate

/**
 * Represents different types of health metrics
 */
enum class HealthMetricType {
    STEPS,
    DISTANCE,
    ACTIVE_MINUTES,
    HEART_RATE,
    SLEEP,
    WEIGHT,
    CALORIES_BURNED,
    WATER_INTAKE,
    MEAL
}

/**
 * Base class for all health metrics
 */
sealed class HealthMetric {
    abstract val id: String
    abstract val userId: String
    abstract val timestamp: Instant
    abstract val type: HealthMetricType
    abstract val isShared: Boolean
}

/**
 * Represents step count data
 */
data class StepsMetric(
    override val id: String,
    override val userId: String,
    override val timestamp: Instant,
    val count: Int,
    val source: String,
    override val isShared: Boolean = false
) : HealthMetric() {
    override val type = HealthMetricType.STEPS
}

/**
 * Represents distance data (walking/running)
 */
data class DistanceMetric(
    override val id: String,
    override val userId: String,
    override val timestamp: Instant,
    val distanceMeters: Double,
    val source: String,
    override val isShared: Boolean = false
) : HealthMetric() {
    override val type = HealthMetricType.DISTANCE
}

/**
 * Represents active minutes data
 */
data class ActiveMinutesMetric(
    override val id: String,
    override val userId: String,
    override val timestamp: Instant,
    val minutes: Int,
    val intensity: ActivityIntensity,
    val source: String,
    override val isShared: Boolean = false
) : HealthMetric() {
    override val type = HealthMetricType.ACTIVE_MINUTES
    
    enum class ActivityIntensity {
        LIGHT, MODERATE, VIGOROUS
    }
}

/**
 * Represents heart rate data
 */
data class HeartRateMetric(
    override val id: String,
    override val userId: String,
    override val timestamp: Instant,
    val beatsPerMinute: Int,
    val source: String,
    override val isShared: Boolean = false
) : HealthMetric() {
    override val type = HealthMetricType.HEART_RATE
}

/**
 * Represents sleep data
 */
data class SleepMetric(
    override val id: String,
    override val userId: String,
    override val timestamp: Instant,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val durationHours: Float,
    val quality: SleepQuality? = null,
    val source: String,
    override val isShared: Boolean = false
) : HealthMetric() {
    override val type = HealthMetricType.SLEEP
    
    enum class SleepQuality {
        POOR, FAIR, GOOD, EXCELLENT
    }
}

/**
 * Represents weight data
 */
data class WeightMetric(
    override val id: String,
    override val userId: String,
    override val timestamp: Instant,
    val weightKg: Double,
    val source: String,
    override val isShared: Boolean = false
) : HealthMetric() {
    override val type = HealthMetricType.WEIGHT
}

/**
 * Represents calories burned data
 */
data class CaloriesBurnedMetric(
    override val id: String,
    override val userId: String,
    override val timestamp: Instant,
    val calories: Int,
    val source: String,
    val activity: String? = null,
    override val isShared: Boolean = false
) : HealthMetric() {
    override val type = HealthMetricType.CALORIES_BURNED
}

/**
 * Represents water intake data
 */
data class WaterIntakeMetric(
    override val id: String,
    override val userId: String,
    override val timestamp: Instant,
    val amount: Int, // in milliliters
    val source: String = "Manual Entry",
    override val isShared: Boolean = false
) : HealthMetric() {
    override val type = HealthMetricType.WATER_INTAKE
}

/**
 * Represents a health highlight that can be shared with a partner
 */
data class HealthHighlight(
    val id: String,
    val title: String,
    val description: String,
    val timestamp: Long,
    val type: HighlightType
)

/**
 * Types of health highlights
 */
enum class HighlightType {
    GOAL_ACHIEVED,
    STREAK_MILESTONE,
    PERSONAL_BEST,
    PARTNER_ACHIEVEMENT,
    CHALLENGE_COMPLETED
}

/**
 * Represents a health reminder that can be sent to a partner
 */
data class HealthReminder(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long,
    val type: HealthReminderType,
    val status: ReminderStatus = ReminderStatus.PENDING,
    val senderId: String,
    val recipientId: String
)

/**
 * Types of health reminders
 */
enum class HealthReminderType {
    WATER_INTAKE,
    STEP_GOAL,
    SLEEP_SCHEDULE,
    WORKOUT,
    MEAL,
    MEDICATION,
    OTHER
}

/**
 * Status of a health reminder
 */
enum class ReminderStatus {
    PENDING,
    ACKNOWLEDGED,
    COMPLETED,
    DISMISSED
}

/**
 * Represents a shared health goal between partners
 */
data class SharedHealthGoal(
    val id: String,
    val title: String,
    val description: String,
    val type: SharedGoalType,
    val target: Int,
    val progress: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isCompleted: Boolean = false,
    val creatorId: String,
    val partnerId: String
)

/**
 * Types of shared goals
 */
enum class SharedGoalType {
    STEPS,
    DISTANCE,
    ACTIVE_MINUTES,
    WATER_INTAKE,
    SLEEP_DURATION,
    WEIGHT_LOSS,
    CALORIES_BURNED
}

/**
 * Represents a challenge between partners
 */
data class CoupleChallenge(
    val id: String,
    val title: String,
    val description: String,
    val type: SharedGoalType,
    val durationType: ChallengeDurationType,
    val creatorProgress: Int,
    val partnerProgress: Int,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isCreator: Boolean
)

/**
 * Duration types for challenges
 */
enum class ChallengeDurationType {
    DAILY,
    WEEKLY,
    MONTHLY,
    CUSTOM
}

/**
 * Represents a food item from the USDA FoodData Central API
 */
data class HealthMetricFoodItem(
    val fdcId: String,
    val description: String,
    val brandName: String = "",
    val ingredients: String = "",
    val servingSize: Double = 100.0,
    val servingSizeUnit: String = "g",
    val calories: Int = 0,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f
)

/**
 * Represents a legacy meal entry (deprecated, use MealEntry instead)
 * @deprecated Use MealEntry from com.newton.couplespace.screens.health.data.models.MealEntry instead
 */
@Deprecated("Use MealEntry instead", ReplaceWith("MealEntry", "com.newton.couplespace.screens.health.data.models.MealEntry"))
data class LegacyMealEntry(
    val id: String,
    val userId: String,
    val name: String,
    val timestamp: Instant,
    val calories: Int,
    val carbs: Float,
    val protein: Float,
    val fat: Float,
    val foods: List<HealthMetricFoodItem> = emptyList(),
    val category: String = "meal",
    val isShared: Boolean = false
)

/**
 * Represents a daily nutrition summary
 */
data class DailyNutritionSummary(
    val date: LocalDate,
    val totalCalories: Int,
    val totalProtein: Int,
    val totalCarbs: Int,
    val totalFat: Int,
    val totalWaterIntake: Int
)
