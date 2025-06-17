package com.newton.couplespace.screens.health.data.models

import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.UUID

/**
 * Data class representing a meal entry with multiple food items
 */
data class MealEntry(
    val id: String = UUID.randomUUID().toString(),
    val userId: String = "current_user", // Default user ID
    val name: String,
    val timestamp: Instant = Instant.now(),
    val calories: Int = 0,
    val carbs: Float = 0f,
    val protein: Float = 0f,
    val fat: Float = 0f,
    val foods: List<FoodItem> = emptyList(),
    val category: String = "breakfast",
    val notes: String = "",
    val imageUri: String? = null,
    val isShared: Boolean = false,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList()
) {
    /**
     * Calculate total nutritional values for the meal
     */
    fun calculateTotalNutrition(): NutritionSummary {
        var totalCalories = 0
        var totalProtein = 0f
        var totalCarbs = 0f
        var totalFat = 0f
        var totalFiber = 0f
        var totalSugar = 0f
        var totalSodium = 0f
        
        foods.forEach { foodItem ->
            totalCalories += foodItem.calories
            totalProtein += foodItem.protein
            totalCarbs += foodItem.carbs
            totalFat += foodItem.fat
            totalFiber += foodItem.fiber ?: 0f
            totalSugar += foodItem.sugar ?: 0f
            totalSodium += foodItem.sodium ?: 0f
        }
        
        return NutritionSummary(
            calories = totalCalories,
            protein = totalProtein,
            carbs = totalCarbs,
            fat = totalFat,
            fiber = totalFiber,
            sugar = totalSugar,
            sodium = totalSodium
        )
    }
    
    /**
     * Convert timestamp to LocalDateTime
     */
    fun getDateTime(): LocalDateTime {
        return LocalDateTime.ofInstant(timestamp, ZoneId.systemDefault())
    }
    
    companion object {
        /**
         * Create an empty meal entry
         */
        fun empty(): MealEntry {
            return MealEntry(
                id = UUID.randomUUID().toString(),
                userId = "current_user",
                name = "",
                timestamp = Instant.now(),
                calories = 0,
                carbs = 0f,
                protein = 0f,
                fat = 0f,
                foods = emptyList(),
                category = "breakfast",
                notes = "",
                imageUri = null,
                isShared = false,
                isFavorite = false,
                tags = emptyList()
            )
        }
        
        /**
         * Create a sample meal entry for preview
         */
        fun sample(): MealEntry {
            val sampleFoods = listOf(
                FoodItem.sample(0),
                FoodItem.sample(3),
                FoodItem.sample(2)
            )
            
            // Calculate total nutrition from sample foods
            var totalCalories = 0
            var totalProtein = 0f
            var totalCarbs = 0f
            var totalFat = 0f
            
            sampleFoods.forEach { food ->
                totalCalories += food.calories
                totalProtein += food.protein
                totalCarbs += food.carbs
                totalFat += food.fat
            }
            
            return MealEntry(
                id = UUID.randomUUID().toString(),
                userId = "current_user",
                name = "Healthy Breakfast",
                timestamp = Instant.now(),
                calories = totalCalories,
                carbs = totalCarbs,
                protein = totalProtein,
                fat = totalFat,
                foods = sampleFoods,
                category = "breakfast",
                notes = "High protein breakfast",
                imageUri = null,
                isShared = false,
                isFavorite = false,
                tags = listOf("healthy", "breakfast")
            )
        }
    }
}

/**
 * Data class representing a summary of nutritional values
 */
data class NutritionSummary(
    val calories: Int = 0,
    val protein: Float = 0f,
    val carbs: Float = 0f,
    val fat: Float = 0f,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val sodium: Float = 0f
) {
    /**
     * Calculate the percentage of calories from each macronutrient
     */
    fun calculateMacroPercentages(): Triple<Float, Float, Float> {
        val totalCaloriesFromMacros = 
            (protein * 4) + (carbs * 4) + (fat * 9)
        
        return if (totalCaloriesFromMacros > 0) {
            Triple(
                (protein * 4) / totalCaloriesFromMacros,
                (carbs * 4) / totalCaloriesFromMacros,
                (fat * 9) / totalCaloriesFromMacros
            )
        } else {
            Triple(0f, 0f, 0f)
        }
    }
}
