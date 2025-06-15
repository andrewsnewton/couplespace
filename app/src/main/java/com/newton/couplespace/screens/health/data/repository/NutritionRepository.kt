package com.newton.couplespace.screens.health.data.repository

import com.newton.couplespace.screens.health.data.models.DailyNutritionSummary
import com.newton.couplespace.screens.health.data.models.FoodItem
import com.newton.couplespace.screens.health.data.models.MealEntry
import com.newton.couplespace.screens.health.data.models.WaterIntakeMetric
import com.newton.couplespace.screens.health.data.remote.FoodSearchResponse
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/**
 * Repository for nutrition-related data
 */
interface NutritionRepository {
    /**
     * Search for foods using the USDA FoodData Central API
     */
    suspend fun searchFoods(query: String): Result<FoodSearchResponse>
    
    /**
     * Search for food with UI state management
     */
    suspend fun searchFood(query: String): Flow<List<FoodItem>>
    
    /**
     * Get food details by ID
     */
    suspend fun getFoodDetails(fdcId: String): Result<FoodItem>
    
    /**
     * Get meal history for a specific date
     */
    suspend fun getMealsForDate(date: LocalDate): Flow<List<MealEntry>>
    
    /**
     * Get meal history for all dates
     */
    suspend fun getMealHistory(): Flow<List<MealEntry>>
    
    /**
     * Log a meal
     */
    suspend fun logMeal(mealEntry: MealEntry): String
    
    /**
     * Save a meal
     */
    suspend fun saveMeal(mealEntry: MealEntry): String
    
    /**
     * Update meal shared status
     */
    suspend fun updateMealSharedStatus(mealId: String, isShared: Boolean)
    
    /**
     * Delete a meal
     */
    suspend fun deleteMeal(mealId: String)
    
    /**
     * Get water intake for a specific date
     */
    suspend fun getWaterIntakeForDate(date: LocalDate): Flow<List<WaterIntakeMetric>>
    
    /**
     * Log water intake
     */
    suspend fun logWaterIntake(amount: Int): String
    
    /**
     * Record water intake
     */
    suspend fun recordWaterIntake(amount: Int): String
    
    /**
     * Get nutrition summary for a specific date
     */
    suspend fun getNutritionSummaryForDate(date: LocalDate): Flow<DailyNutritionSummary>
    
    /**
     * Get the user's water intake goal
     */
    suspend fun getWaterGoal(): Flow<Int>
    
    /**
     * Update the user's water intake goal
     */
    suspend fun updateWaterGoal(goalMl: Int): Boolean
    
    /**
     * Set water intake reminder schedule
     * @param intervalMinutes Interval between reminders in minutes
     * @param startTime Start time for reminders (hour of day, 0-23)
     * @param endTime End time for reminders (hour of day, 0-23)
     * @param enabled Whether reminders are enabled
     */
    suspend fun setWaterReminderSchedule(intervalMinutes: Int, startTime: Int, endTime: Int, enabled: Boolean): Boolean
    
    /**
     * Get the current water reminder schedule
     * @return A map containing reminder settings: intervalMinutes, startTime, endTime, enabled
     */
    suspend fun getWaterReminderSchedule(): Flow<Map<String, Any>>
}
