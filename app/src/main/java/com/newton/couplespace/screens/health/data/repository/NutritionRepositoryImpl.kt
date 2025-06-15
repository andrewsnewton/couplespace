package com.newton.couplespace.screens.health.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.newton.couplespace.screens.health.data.models.DailyNutritionSummary
import com.newton.couplespace.screens.health.data.models.FoodItem
import com.newton.couplespace.screens.health.data.models.MealEntry
import com.newton.couplespace.screens.health.data.models.WaterIntakeMetric
import com.newton.couplespace.screens.health.data.remote.FoodDataCentralApi
import com.newton.couplespace.screens.health.data.remote.FoodSearchResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of NutritionRepository that uses Firebase for data storage
 * and the USDA FoodData Central API for food information
 */
@Singleton
class NutritionRepositoryImpl @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    private val foodDataCentralApi: FoodDataCentralApi
) : NutritionRepository {
    
    private val mealsCollection = firestore.collection("meals")
    private val waterIntakeCollection = firestore.collection("water_intake") // Changed collection name to match potential existing structure
    
    /**
     * Search for foods using the USDA FoodData Central API
     */
    override suspend fun searchFoods(query: String): Result<FoodSearchResponse> {
        return try {
            val response = foodDataCentralApi.searchFoods(query)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Search for food with UI state management
     */
    override suspend fun searchFood(query: String): Flow<List<FoodItem>> = flow {
        try {
            val result = searchFoods(query)
            if (result.isSuccess) {
                val response = result.getOrNull()
                emit(response?.foods?.map { food ->
                    FoodItem(
                        fdcId = food.fdcId,
                        description = food.description,
                        brandName = food.brandName ?: "",
                        ingredients = food.ingredients ?: "",
                        servingSize = food.servingSize?.toDouble() ?: 100.0,
                        servingSizeUnit = food.servingSizeUnit ?: "g",
                        calories = food.foodNutrients.find { it.nutrientName == "Energy" }?.value?.toDouble()?.toInt() ?: 0,
                        protein = food.foodNutrients.find { it.nutrientName == "Protein" }?.value?.toDouble()?.toFloat() ?: 0f,
                        carbs = food.foodNutrients.find { it.nutrientName == "Carbohydrate, by difference" }?.value?.toDouble()?.toFloat() ?: 0f,
                        fat = food.foodNutrients.find { it.nutrientName == "Total lipid (fat)" }?.value?.toDouble()?.toFloat() ?: 0f
                    )
                } ?: emptyList())
            } else {
                // Emit mock data if API call fails
                emit(getMockFoodItems(query))
            }
        } catch (e: Exception) {
            // Emit mock data if API call fails
            emit(getMockFoodItems(query))
        }
    }
    
    /**
     * Get mock food items for fallback
     */
    private fun getMockFoodItems(query: String): List<FoodItem> {
        return listOf(
            FoodItem(
                fdcId = "mock-1",
                description = "$query (Mock)",
                brandName = "Mock Brand",
                ingredients = "Mock ingredients",
                servingSize = 100.0,
                servingSizeUnit = "g",
                calories = 250,
                protein = 10f,
                carbs = 30f,
                fat = 8f
            ),
            FoodItem(
                fdcId = "mock-2",
                description = "$query Deluxe (Mock)",
                brandName = "Premium Mock",
                ingredients = "Premium mock ingredients",
                servingSize = 100.0,
                servingSizeUnit = "g",
                calories = 350,
                protein = 15f,
                carbs = 40f,
                fat = 12f
            )
        )
    }
    
    /**
     * Get food details by ID from the USDA FoodData Central API
     */
    override suspend fun getFoodDetails(fdcId: String): Result<FoodItem> {
        return try {
            val response = foodDataCentralApi.getFoodDetails(fdcId)
            
            // Extract the relevant nutritional information
            val description = response.description ?: "Unknown Food"
            val servingSize = response.servingSize ?: 100.0
            val servingSizeUnit = response.servingSizeUnit ?: "g"
            
            var calories = 0
            var protein = 0f
            var carbs = 0f
            var fat = 0f
            
            // Parse nutrient data
            response.foodNutrients.forEach { nutrient ->
                when (nutrient.nutrient.name) {
                    "Energy" -> calories = nutrient.amount.toInt()
                    "Protein" -> protein = nutrient.amount.toFloat()
                    "Carbohydrate, by difference" -> carbs = nutrient.amount.toFloat()
                    "Total lipid (fat)" -> fat = nutrient.amount.toFloat()
                }
            }
            
            val foodItem = FoodItem(
                fdcId = fdcId,
                description = description,
                brandName = response.brandName ?: "",
                ingredients = response.ingredients ?: "",
                servingSize = servingSize,
                servingSizeUnit = servingSizeUnit,
                calories = calories,
                protein = protein,
                carbs = carbs,
                fat = fat
            )
            
            Result.success(foodItem)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get meals for a specific date from Firebase
     */
    override suspend fun getMealsForDate(date: LocalDate): Flow<List<MealEntry>> = flow {
        val userId = auth.currentUser?.uid ?: "mock-user-id"
        
        try {
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            
            val mealsSnapshot = mealsCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .whereLessThan("timestamp", endOfDay)
                .get()
                .await()
            
            val meals = mealsSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                
                MealEntry(
                    id = doc.id,
                    userId = data["userId"] as? String ?: userId,
                    name = data["name"] as? String ?: "",
                    timestamp = Instant.ofEpochMilli((data["timestamp"] as? Number)?.toLong() ?: 0L),
                    calories = (data["calories"] as? Number)?.toInt() ?: 0,
                    carbs = (data["carbs"] as? Number)?.toFloat() ?: 0f,
                    protein = (data["protein"] as? Number)?.toFloat() ?: 0f,
                    fat = (data["fat"] as? Number)?.toFloat() ?: 0f,
                    isShared = data["isShared"] as? Boolean ?: false
                )
            }
            
            emit(meals)
        } catch (e: Exception) {
            // In a real app, log the error
            // For now, emit mock data
            val mockMeals = listOf(
                MealEntry(
                    id = "1",
                    userId = userId,
                    name = "Breakfast",
                    timestamp = date.atStartOfDay(ZoneId.systemDefault()).plusHours(8).toInstant(),
                    calories = 450,
                    carbs = 60f,
                    protein = 20f,
                    fat = 15f,
                    isShared = false
                ),
                MealEntry(
                    id = "2",
                    userId = userId,
                    name = "Lunch",
                    timestamp = date.atStartOfDay(ZoneId.systemDefault()).plusHours(13).toInstant(),
                    calories = 650,
                    carbs = 70f,
                    protein = 35f,
                    fat = 25f,
                    isShared = true
                )
            )
            emit(mockMeals)
        }
    }
    
    /**
     * Get all meal history from Firebase
     */
    override suspend fun getMealHistory(): Flow<List<MealEntry>> = flow {
        val userId = auth.currentUser?.uid ?: "mock-user-id"
        
        try {
            val mealsSnapshot = mealsCollection
                .whereEqualTo("userId", userId)
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(30) // Limit to last 30 meals
                .get()
                .await()
            
            val meals = mealsSnapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                
                MealEntry(
                    id = doc.id,
                    userId = data["userId"] as? String ?: userId,
                    name = data["name"] as? String ?: "",
                    timestamp = Instant.ofEpochMilli((data["timestamp"] as? Number)?.toLong() ?: 0L),
                    calories = (data["calories"] as? Number)?.toInt() ?: 0,
                    carbs = (data["carbs"] as? Number)?.toFloat() ?: 0f,
                    protein = (data["protein"] as? Number)?.toFloat() ?: 0f,
                    fat = (data["fat"] as? Number)?.toFloat() ?: 0f,
                    isShared = data["isShared"] as? Boolean ?: false
                )
            }
            
            emit(meals)
        } catch (e: Exception) {
            // In a real app, log the error
            // For now, emit mock data
            val mockMeals = listOf(
                MealEntry(
                    id = "1",
                    userId = userId,
                    name = "Breakfast",
                    timestamp = Instant.now().minusSeconds(86400), // Yesterday
                    calories = 450,
                    carbs = 60f,
                    protein = 20f,
                    fat = 15f,
                    isShared = false
                ),
                MealEntry(
                    id = "2",
                    userId = userId,
                    name = "Lunch",
                    timestamp = Instant.now().minusSeconds(43200), // 12 hours ago
                    calories = 650,
                    carbs = 70f,
                    protein = 35f,
                    fat = 25f,
                    isShared = true
                )
            )
            emit(mockMeals)
        }
    }
    
    /**
     * Log a meal to Firebase
     */
    override suspend fun logMeal(mealEntry: MealEntry): String {
        val userId = auth.currentUser?.uid ?: return "error-no-user"
        
        try {
            val mealData = hashMapOf(
                "userId" to userId,
                "name" to mealEntry.name,
                "timestamp" to mealEntry.timestamp.toEpochMilli(),
                "calories" to mealEntry.calories,
                "carbs" to mealEntry.carbs,
                "protein" to mealEntry.protein,
                "fat" to mealEntry.fat,
                "isShared" to mealEntry.isShared,
                "createdAt" to com.google.firebase.Timestamp.now()
            )
            
            val docRef = mealsCollection.add(mealData).await()
            return docRef.id
        } catch (e: Exception) {
            // In a real app, log the error
            return "error-${UUID.randomUUID()}"
        }
    }
    
    /**
     * Save a meal - alternative method for logging a meal
     */
    override suspend fun saveMeal(mealEntry: MealEntry): String {
        return logMeal(mealEntry)
    }
    
    /**
     * Delete a meal from Firebase
     */
    override suspend fun deleteMeal(mealId: String) {
        try {
            mealsCollection.document(mealId).delete().await()
        } catch (e: Exception) {
            // In a real app, log the error
        }
    }
    
    /**
     * Update meal shared status
     */
    override suspend fun updateMealSharedStatus(mealId: String, isShared: Boolean) {
        try {
            val userId = auth.currentUser?.uid ?: return
            firestore.collection("users")
                .document(userId)
                .collection("meals")
                .document(mealId)
                .update("isShared", isShared)
        } catch (e: Exception) {
            // Log error
            android.util.Log.e("NutritionRepository", "Error updating meal shared status: ${e.message}")
        }
    }
    
    /**
     * Log water intake to Firebase
     */
    override suspend fun logWaterIntake(amount: Int): String {
        val userId = auth.currentUser?.uid ?: return "no-user-id"
        
        if (amount <= 0) {
            println("DEBUG: Invalid water amount: $amount")
            return "invalid-amount"
        }
        
        try {
            println("DEBUG: Attempting to save water intake to collection: water_intake")
            
            val waterIntakeId = UUID.randomUUID().toString()
            val timestamp = System.currentTimeMillis()
            val today = LocalDate.now().toString()
            
            // Create a document with a specific ID for better tracking
            val waterIntakeData = hashMapOf(
                "id" to waterIntakeId,
                "userId" to userId,
                "amount" to amount,
                "timestamp" to timestamp,
                "date" to today,
                "source" to "User Input",
                "isShared" to false
            )
            
            println("DEBUG: Saving water intake: $amount ml with ID: $waterIntakeId for date: $today")
            
            // Use document().set() with a specific ID for more reliable retrieval
            waterIntakeCollection.document(waterIntakeId).set(waterIntakeData).await()
            println("DEBUG: Successfully saved water intake with ID: $waterIntakeId")
            
            // Verify the document was saved by reading it back
            val savedDoc = waterIntakeCollection.document(waterIntakeId).get().await()
            if (savedDoc.exists()) {
                println("DEBUG: Verified document exists with data: ${savedDoc.data}")
            } else {
                println("DEBUG: WARNING: Document was not found after saving!")
            }
            
            return waterIntakeId
        } catch (e: Exception) {
            // Log the error
            println("DEBUG: Error saving water intake: ${e.message}")
            e.printStackTrace()
            return "error-${UUID.randomUUID()}"
        }
    }
    
    /**
     * Record water intake
     */
    override suspend fun recordWaterIntake(amount: Int): String {
        println("DEBUG: Recording water intake: $amount ml")
        val result = logWaterIntake(amount)
        println("DEBUG: Water intake record result: $result")
        // Add a small delay to ensure Firebase has time to process the write
        delay(500)
        return result
    }
    
    /**
     * Get water intake for a specific date from Firebase
     */
    override suspend fun getWaterIntakeForDate(date: LocalDate): Flow<List<WaterIntakeMetric>> = flow {
        val userId = auth.currentUser?.uid ?: run {
            println("DEBUG: No user ID available for water intake")
            return@flow emit(emptyList())
        }
        
        try {
            val startOfDay = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val endOfDay = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            val dateString = date.toString()
            
            println("DEBUG: Fetching water intake for date: $date, userId: $userId")
            
            // Try multiple query approaches to ensure we find the data
            // First try by timestamp range
            var waterIntakeSnapshot = waterIntakeCollection
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .whereLessThan("timestamp", endOfDay)
                .get()
                .await()
            
            // If no results, try by date string if available
            if (waterIntakeSnapshot.isEmpty) {
                println("DEBUG: No results with timestamp query, trying date string query")
                waterIntakeSnapshot = waterIntakeCollection
                    .whereEqualTo("userId", userId)
                    .whereEqualTo("date", dateString)
                    .get()
                    .await()
            }
            
            // If still no results, try getting all user's records and filter manually
            if (waterIntakeSnapshot.isEmpty) {
                println("DEBUG: No results with date string query, fetching all user records")
                waterIntakeSnapshot = waterIntakeCollection
                    .whereEqualTo("userId", userId)
                    .get()
                    .await()
                
                println("DEBUG: Found ${waterIntakeSnapshot.documents.size} total water records for user")
            }
            
            println("DEBUG: Found ${waterIntakeSnapshot.documents.size} water intake records for date: $date")
            
            // Dump all document data for debugging
            waterIntakeSnapshot.documents.forEach { doc ->
                println("DEBUG: Document ${doc.id} data: ${doc.data}")
            }
            
            // Create individual water intake metrics for each record
            val waterIntakeMetrics = waterIntakeSnapshot.documents.map { doc ->
                val amount = doc.getLong("amount")?.toInt() ?: 0
                val timestamp = doc.getLong("timestamp") ?: startOfDay
                val docId = doc.id
                
                println("DEBUG: Water record - ID: $docId, Amount: $amount ml, Timestamp: ${Instant.ofEpochMilli(timestamp)}")
                
                WaterIntakeMetric(
                    id = docId,
                    userId = userId,
                    timestamp = Instant.ofEpochMilli(timestamp),
                    amount = amount,
                    source = doc.getString("source") ?: "Firebase",
                    isShared = doc.getBoolean("isShared") ?: false
                )
            }
            
            val totalAmount = waterIntakeMetrics.sumOf { it.amount }
            println("DEBUG: Total water intake: $totalAmount ml from ${waterIntakeMetrics.size} records")
            
            // Always emit at least one record with the current total
            if (waterIntakeMetrics.isEmpty()) {
                println("DEBUG: No water intake records found, creating a default zero record")
                val defaultMetric = WaterIntakeMetric(
                    id = UUID.randomUUID().toString(),
                    userId = userId,
                    timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    amount = 0,
                    source = "Default",
                    isShared = false
                )
                emit(listOf(defaultMetric))
            } else {
                emit(waterIntakeMetrics)
            }
        } catch (e: Exception) {
            println("DEBUG: Error fetching water intake: ${e.message}")
            e.printStackTrace()
            
            // Create a default zero record on error
            val defaultMetric = WaterIntakeMetric(
                id = UUID.randomUUID().toString(),
                userId = userId,
                timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                amount = 0,
                source = "Error Fallback",
                isShared = false
            )
            emit(listOf(defaultMetric))
        }
    }
    
    /**
     * Get nutrition summary for a specific date
     */
    override suspend fun getNutritionSummaryForDate(date: LocalDate): Flow<DailyNutritionSummary> = flow {
        val userId = auth.currentUser?.uid ?: return@flow emit(DailyNutritionSummary(
            date = date,
            totalCalories = 0,
            totalProtein = 0,
            totalCarbs = 0,
            totalFat = 0,
            totalWaterIntake = 0
        ))
        
        try {
            // Get meals for the date
            val meals = getMealsForDate(date).first()
            
            // Get water intake for the date
            val waterIntakeMetrics = getWaterIntakeForDate(date).first()
            
            // Calculate totals
            var totalCalories = 0
            var totalProtein = 0
            var totalCarbs = 0
            var totalFat = 0
            var totalWaterIntake = 0
            
            // Process meals if any exist
            for (meal in meals) {
                totalCalories += meal.calories
                totalProtein += meal.protein.toInt()
                totalCarbs += meal.carbs.toInt()
                totalFat += meal.fat.toInt()
            }
            
            // Calculate total water intake from all water intake metrics
            totalWaterIntake = waterIntakeMetrics.sumOf { it.amount }
            println("DEBUG: Calculated total water intake in summary: $totalWaterIntake ml from ${waterIntakeMetrics.size} records")
            
            val nutritionSummary = DailyNutritionSummary(
                date = date,
                totalCalories = totalCalories,
                totalProtein = totalProtein,
                totalCarbs = totalCarbs,
                totalFat = totalFat,
                totalWaterIntake = totalWaterIntake
            )
            
            emit(nutritionSummary)
        } catch (e: Exception) {
            // Log the error
            println("Error getting nutrition summary: ${e.message}")
            e.printStackTrace()
            
            // Return empty summary
            emit(DailyNutritionSummary(
                date = date,
                totalCalories = 0,
                totalProtein = 0,
                totalCarbs = 0,
                totalFat = 0,
                totalWaterIntake = 0
            ))
        }
    }
}
