package com.newton.couplespace.screens.health.data.models

import androidx.compose.ui.graphics.Color
import java.util.UUID

/**
 * Data class representing a food item with nutritional information
 */
data class FoodItem(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val servingSize: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val sodium: Float = 0f,
    val imageUrl: String? = null,
    val category: String = "",
    val isFavorite: Boolean = false,
    val barcode: String? = null,
    val customColor: Color? = null,
    val lastUsed: Long = System.currentTimeMillis()
) {
    /**
     * Calculate the nutritional value based on the given portion size
     */
    fun calculateForPortion(portionMultiplier: Double): FoodItem {
        return copy(
            calories = (calories * portionMultiplier).toInt(),
            protein = (protein * portionMultiplier).toFloat(),
            carbs = (carbs * portionMultiplier).toFloat(),
            fat = (fat * portionMultiplier).toFloat(),
            fiber = (fiber * portionMultiplier).toFloat(),
            sugar = (sugar * portionMultiplier).toFloat(),
            sodium = (sodium * portionMultiplier).toFloat()
        )
    }
    
    /**
     * Get a color based on the food category
     */
    fun getCategoryColor(): Color {
        return customColor ?: when {
            category.equals("fruit", ignoreCase = true) || 
            name.contains("fruit", ignoreCase = true) -> Color(0xFF4CAF50) // Green
            
            category.equals("vegetable", ignoreCase = true) || 
            name.contains("vegetable", ignoreCase = true) -> Color(0xFF8BC34A) // Light Green
            
            category.equals("meat", ignoreCase = true) || 
            name.contains("meat", ignoreCase = true) ||
            name.contains("chicken", ignoreCase = true) ||
            name.contains("beef", ignoreCase = true) ||
            name.contains("pork", ignoreCase = true) -> Color(0xFFF44336) // Red
            
            category.equals("dairy", ignoreCase = true) || 
            name.contains("milk", ignoreCase = true) ||
            name.contains("cheese", ignoreCase = true) ||
            name.contains("yogurt", ignoreCase = true) -> Color(0xFF2196F3) // Blue
            
            category.equals("grain", ignoreCase = true) || 
            name.contains("bread", ignoreCase = true) ||
            name.contains("rice", ignoreCase = true) ||
            name.contains("pasta", ignoreCase = true) -> Color(0xFFFFEB3B) // Yellow
            
            category.equals("dessert", ignoreCase = true) || 
            name.contains("dessert", ignoreCase = true) ||
            name.contains("cake", ignoreCase = true) ||
            name.contains("cookie", ignoreCase = true) ||
            name.contains("ice cream", ignoreCase = true) -> Color(0xFFE91E63) // Pink
            
            else -> Color(0xFF9C27B0) // Purple (default)
        }
    }
    
    companion object {
        /**
         * Create an empty food item
         */
        fun empty(): FoodItem {
            return FoodItem(
                name = "",
                servingSize = "100g",
                calories = 0,
                protein = 0f,
                carbs = 0f,
                fat = 0f
            )
        }
        
        /**
         * Create a sample food item for preview
         */
        fun sample(index: Int = 0): FoodItem {
            val samples = listOf(
                FoodItem(
                    name = "Grilled Chicken Breast",
                    servingSize = "100g",
                    calories = 165,
                    protein = 31f,
                    carbs = 0f,
                    fat = 3.6f,
                    category = "meat"
                ),
                FoodItem(
                    name = "Brown Rice",
                    servingSize = "100g cooked",
                    calories = 112,
                    protein = 2.6f,
                    carbs = 23f,
                    fat = 0.9f,
                    fiber = 1.8f,
                    category = "grain"
                ),
                FoodItem(
                    name = "Avocado",
                    servingSize = "1 medium",
                    calories = 240,
                    protein = 3f,
                    carbs = 12f,
                    fat = 22f,
                    fiber = 10f,
                    category = "fruit"
                ),
                FoodItem(
                    name = "Greek Yogurt",
                    servingSize = "170g",
                    calories = 100,
                    protein = 17f,
                    carbs = 6f,
                    fat = 0.7f,
                    category = "dairy"
                ),
                FoodItem(
                    name = "Spinach",
                    servingSize = "100g",
                    calories = 23,
                    protein = 2.9f,
                    carbs = 3.6f,
                    fat = 0.4f,
                    fiber = 2.2f,
                    category = "vegetable"
                )
            )
            
            return samples[index % samples.size]
        }
    }
}
