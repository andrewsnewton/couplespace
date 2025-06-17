package com.newton.couplespace.screens.health.components.nutrition.meal.data

import com.newton.couplespace.screens.health.data.models.FoodItem
import com.newton.couplespace.screens.health.data.models.MealEntry
import com.newton.couplespace.screens.health.data.models.NutritionSummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing meal and food data
 */
@Singleton
class FoodSearchRepository @Inject constructor(
    private val foodRepository: LocalFoodRepository,
    private val mealEntryDao: MealEntryDao
) {
    /**
     * Search for food items by name
     */
    suspend fun searchFoodByName(query: String, limit: Int = 20): List<FoodItem> {
        return foodRepository.searchFoodByName(query, limit)
    }

    /**
     * Get food item by barcode
     */
    suspend fun getFoodByBarcode(barcode: String): FoodItem? {
        return foodRepository.getFoodByBarcode(barcode)
    }

    /**
     * Get food item details by ID
     */
    suspend fun getFoodDetails(foodId: String): FoodItem? {
        return foodRepository.getFoodDetails(foodId)
    }

    /**
     * Save a custom food item
     */
    suspend fun saveCustomFoodItem(foodItem: FoodItem) {
        foodRepository.saveCustomFoodItem(foodItem)
    }

    /**
     * Update a food item
     */
    suspend fun updateFoodItem(foodItem: FoodItem) {
        foodRepository.updateFoodItem(foodItem)
    }

    /**
     * Delete a food item
     */
    suspend fun deleteFoodItem(foodItem: FoodItem) {
        foodRepository.deleteFoodItem(foodItem)
    }

    /**
     * Get favorite food items
     */
    fun getFavoriteFoodItems(): Flow<List<FoodItem>> {
        return foodRepository.getFavoriteFoodItems()
    }

    /**
     * Get custom food items
     */
    fun getCustomFoodItems(): Flow<List<FoodItem>> {
        return foodRepository.getCustomFoodItems()
    }

    /**
     * Get recent food items
     */
    fun getRecentFoodItems(limit: Int = 20): Flow<List<FoodItem>> {
        return foodRepository.getRecentFoodItems(limit)
    }

    /**
     * Save a meal entry
     */
    suspend fun saveMealEntry(mealEntry: MealEntry) {
        // Convert to entity
        val mealEntity = MealEntryEntity(
            id = mealEntry.id,
            name = mealEntry.name,
            categoryId = mealEntry.category,
            dateTime = mealEntry.timestamp.toEpochMilli(),
            notes = mealEntry.notes,
            imageUri = mealEntry.imageUri,
            isFavorite = mealEntry.isFavorite
        )

        // Insert meal entry
        mealEntryDao.insertMealEntry(mealEntity)

        // Delete existing cross references
        mealEntryDao.deleteMealFoodCrossRefsByMealId(mealEntry.id)

        // Insert food items and cross references
        val crossRefs = mealEntry.foods.map { foodItem ->
            // Save food item
            foodRepository.updateFoodItem(foodItem.copy(lastUsed = System.currentTimeMillis()))

            // Create cross reference
            MealFoodCrossRef(
                mealId = mealEntry.id,
                foodId = foodItem.id,
                portionMultiplier = 1.0 // Default portion multiplier
            )
        }

        mealEntryDao.insertMealFoodCrossRefs(crossRefs)
    }

    /**
     * Get meal entry by ID
     */
    suspend fun getMealEntryById(id: String): MealEntry? {
        val mealWithFoods = mealEntryDao.getMealWithFoodsById(id) ?: return null

        // Convert to domain model
        val foodItems = mealWithFoods.foods.map { foodEntity ->
            val foodItem = foodEntity.toDomainModel()
            val portionMultiplier = 1.0 // Default multiplier

            // Apply portion multiplier
            if (portionMultiplier != 1.0) {
                foodItem.calculateForPortion(portionMultiplier)
            } else {
                foodItem
            }
        }

        return MealEntry(
            id = mealWithFoods.meal.id,
            name = mealWithFoods.meal.name,
            category = mealWithFoods.meal.categoryId,
            foods = foodItems,
            timestamp = Instant.ofEpochMilli(
                mealWithFoods.meal.dateTime
            ),
            notes = mealWithFoods.meal.notes,
            imageUri = mealWithFoods.meal.imageUri,
            isFavorite = mealWithFoods.meal.isFavorite
        )
    }

    /**
     * Get meals for a specific date
     */
    fun getMealsForDate(date: LocalDate): Flow<List<MealEntry>> {
        val startOfDay = date.atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val endOfDay = date.plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli() - 1

        return mealEntryDao.getMealsForDateRange(startOfDay, endOfDay) // Assumes returns Flow<List<MealWithFoods>>
            .map { mealsWithFoodsList ->
                mealsWithFoodsList.map { mealWithFoodsItem -> // mealWithFoodsItem is MealWithFoods
                    val mealId = mealWithFoodsItem.meal.id
                    val foodItemsWithPortionForThisMeal = mealEntryDao.getFoodItemsWithPortionForMealList(mealId)

                    val portionMultiplierMap = foodItemsWithPortionForThisMeal.associate {
                        it.foodItem.id to it.portionMultiplier
                    }

                    val domainFoodItems = mealWithFoodsItem.foods.map { foodItemEntity -> // foodItemEntity is FoodItemEntity
                        val foodItem = foodItemEntity.toDomainModel()
                        val portionMultiplier = portionMultiplierMap[foodItemEntity.id] ?: 1.0

                        if (portionMultiplier != 1.0) {
                            foodItem.calculateForPortion(portionMultiplier)
                        } else {
                            foodItem
                        }
                    }

                    MealEntry(
                        id = mealWithFoodsItem.meal.id,
                        name = mealWithFoodsItem.meal.name,
                        category = mealWithFoodsItem.meal.categoryId,
                        foods = domainFoodItems,
                        timestamp = Instant.ofEpochMilli(mealWithFoodsItem.meal.dateTime),
                        notes = mealWithFoodsItem.meal.notes,
                        imageUri = mealWithFoodsItem.meal.imageUri,
                        isFavorite = mealWithFoodsItem.meal.isFavorite
                    )
                }
            }
    }

    /**
     * Delete a meal entry
     */
    suspend fun deleteMealEntry(mealEntry: MealEntry) {
        val mealEntity = MealEntryEntity(
            id = mealEntry.id,
            name = mealEntry.name,
            categoryId = mealEntry.category,
            dateTime = mealEntry.timestamp.toEpochMilli(),
            notes = mealEntry.notes,
            imageUri = mealEntry.imageUri,
            isFavorite = mealEntry.isFavorite
        )

        mealEntryDao.deleteMealEntry(mealEntity)
    }

    /**
     * Get favorite meals
     */
    fun getFavoriteMeals(): Flow<List<MealEntry>> {
        return mealEntryDao.getFavoriteMeals() // Assumes this returns Flow<List<MealWithFoods>>
            .map { mealsWithFoodsList ->      // mealsWithFoodsList is List<MealWithFoods>
                mealsWithFoodsList.map { mealWithFoodsItem -> // mealWithFoodsItem is MealWithFoods
                    val mealId = mealWithFoodsItem.meal.id

                    // Fetch FoodItemWithPortion for the current meal to get actual portion multipliers
                    val foodItemsWithPortionForThisMeal = mealEntryDao.getFoodItemsWithPortionForMealList(mealId)

                    // Create a map for quick lookup of portionMultiplier by foodId
                    // This uses the foodItem.id from the FoodItemEntity embedded in FoodItemWithPortion
                    val portionMultiplierMap = foodItemsWithPortionForThisMeal.associate { foodItemWithPortion ->
                        foodItemWithPortion.foodItem.id to foodItemWithPortion.portionMultiplier
                    }

                    // mealWithFoodsItem.foods is List<FoodItemEntity>
                    val domainFoodItems = mealWithFoodsItem.foods.map { foodItemEntity ->
                        val foodItemDomainModel = foodItemEntity.toDomainModel() // Convert entity to domain model
                        // Get the portion multiplier for this specific food in this specific meal
                        val portionMultiplier = portionMultiplierMap[foodItemEntity.id] ?: 1.0

                        // Apply portion multiplier to the domain model
                        if (portionMultiplier != 1.0) {
                            foodItemDomainModel.calculateForPortion(portionMultiplier)
                        } else {
                            foodItemDomainModel
                        }
                    }

                    MealEntry(
                        id = mealWithFoodsItem.meal.id,
                        name = mealWithFoodsItem.meal.name,
                        category = mealWithFoodsItem.meal.categoryId,
                        foods = domainFoodItems, // Use the correctly processed list
                        timestamp = Instant.ofEpochMilli(mealWithFoodsItem.meal.dateTime),
                        notes = mealWithFoodsItem.meal.notes,
                        imageUri = mealWithFoodsItem.meal.imageUri,
                        isFavorite = mealWithFoodsItem.meal.isFavorite
                    )
                }
            }
    }

    /**
     * Calculate nutrition summary for a specific date
     */
    fun getNutritionSummaryForDate(date: LocalDate): Flow<NutritionSummary> {
        return getMealsForDate(date)
            .map { meals ->
                var totalCalories = 0
                var totalProtein = 0f
                var totalCarbs = 0f
                var totalFat = 0f
                var totalFiber = 0f
                var totalSugar = 0f
                var totalSodium = 0f

                meals.forEach { meal ->
                    val mealNutrition = meal.calculateTotalNutrition()
                    totalCalories += mealNutrition.calories
                    totalProtein += mealNutrition.protein
                    totalCarbs += mealNutrition.carbs
                    totalFat += mealNutrition.fat
                    totalFiber += mealNutrition.fiber
                    totalSugar += mealNutrition.sugar
                    totalSodium += mealNutrition.sodium
                }

                NutritionSummary(
                    calories = totalCalories,
                    protein = totalProtein,
                    carbs = totalCarbs,
                    fat = totalFat,
                    fiber = totalFiber,
                    sugar = totalSugar,
                    sodium = totalSodium
                )
            }
    }
}
