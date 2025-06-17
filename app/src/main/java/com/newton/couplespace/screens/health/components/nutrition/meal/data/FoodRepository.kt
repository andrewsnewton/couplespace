package com.newton.couplespace.screens.health.components.nutrition.meal.data

import com.newton.couplespace.screens.health.data.models.FoodItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for food item data
 */
@Singleton
class FoodRepository @Inject constructor(
    private val foodItemDao: FoodItemDao,
    private val nutritionApiService: NutritionApiService
) {
    /**
     * Search for food items by name
     */
    suspend fun searchFoodByName(query: String, limit: Int = 20): List<FoodItem> {
        // First search local database
        val localResults = foodItemDao.searchFoodItems(query, limit)
            .map { it.toDomainModel() }
        
        // If we have enough results from local database, return them
        if (localResults.size >= limit) {
            return localResults
        }
        
        // Otherwise, search remote API and cache results
        try {
            val remoteResults = nutritionApiService.searchFoodByName(
                query = query,
                pageSize = limit - localResults.size
            )
            
            // Cache remote results in local database
            foodItemDao.insertFoodItems(remoteResults.map { it.toEntity() })
            
            // Combine local and remote results, removing duplicates
            return (localResults + remoteResults)
                .distinctBy { it.id }
                .take(limit)
        } catch (e: Exception) {
            // If remote API fails, return local results only
            return localResults
        }
    }
    
    /**
     * Get food item by barcode, checking local database first then remote API
     */
    suspend fun getFoodByBarcode(barcode: String): FoodItem? {
        // First check local database
        val localResult = foodItemDao.getFoodItemByBarcode(barcode)?.toDomainModel()
        
        if (localResult != null) {
            return localResult
        }
        
        // If not found locally, check remote API and cache result
        try {
            val remoteResult = nutritionApiService.getFoodByBarcode(barcode)
            
            if (remoteResult != null) {
                foodItemDao.insertFoodItem(remoteResult.toEntity())
            }
            
            return remoteResult
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Get food item details by ID, checking local database first then remote API
     */
    suspend fun getFoodDetails(foodId: String): FoodItem? {
        // First check local database
        val localResult = foodItemDao.getFoodItemById(foodId)?.toDomainModel()
        
        if (localResult != null) {
            return localResult
        }
        
        // If not found locally, check remote API and cache result
        try {
            val remoteResult = nutritionApiService.getFoodDetails(foodId)
            
            if (remoteResult != null) {
                foodItemDao.insertFoodItem(remoteResult.toEntity())
            }
            
            return remoteResult
        } catch (e: Exception) {
            return null
        }
    }
    
    /**
     * Save a custom food item to the local database
     */
    suspend fun saveCustomFoodItem(foodItem: FoodItem) {
        foodItemDao.insertFoodItem(foodItem.toEntity(isCustom = true))
    }
    
    /**
     * Update a food item in the local database
     */
    suspend fun updateFoodItem(foodItem: FoodItem) {
        foodItemDao.updateFoodItem(foodItem.toEntity())
    }
    
    /**
     * Delete a food item from the local database
     */
    suspend fun deleteFoodItem(foodItem: FoodItem) {
        foodItemDao.deleteFoodItem(foodItem.toEntity())
    }
    
    /**
     * Get favorite food items from the local database
     */
    fun getFavoriteFoodItems(): Flow<List<FoodItem>> {
        return foodItemDao.getFavoriteFoodItems()
            .map { entities -> entities.map { it.toDomainModel() } }
    }
    
    /**
     * Get custom food items from the local database
     */
    fun getCustomFoodItems(): Flow<List<FoodItem>> {
        return foodItemDao.getCustomFoodItems()
            .map { entities -> entities.map { it.toDomainModel() } }
    }
    
    /**
     * Get recent food items from the local database
     */
    fun getRecentFoodItems(limit: Int = 20): Flow<List<FoodItem>> {
        return foodItemDao.getRecentFoodItems(limit)
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    // Extension functions for entity conversion
    private fun FoodItem.toEntity(isCustom: Boolean = false): FoodItemEntity {
        return FoodItemEntity(
            id = this.id,
            name = this.name,
            servingSize = this.servingSize,
            calories = this.calories,
            protein = this.protein,
            carbs = this.carbs,
            fat = this.fat,
            imageUrl = null,
            category = this.category ?: "",
            isFavorite = false,
            barcode = this.barcode,
            isCustom = isCustom,
            lastUsed = System.currentTimeMillis()
        )
    }

    private fun FoodItemEntity.toDomainModel(): FoodItem {
        return FoodItem(
            id = this.id,
            name = this.name,
            servingSize = this.servingSize,
            calories = this.calories,
            protein = this.protein,
            carbs = this.carbs,
            fat = this.fat,
            category = this.category
        )
    }
}
