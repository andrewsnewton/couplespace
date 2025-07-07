package com.newton.couplespace.screens.health.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newton.couplespace.screens.health.data.models.DailyNutritionSummary
import com.newton.couplespace.screens.health.data.models.FoodItem
import com.newton.couplespace.screens.health.data.models.MealEntry
import com.newton.couplespace.screens.health.data.models.WaterIntakeMetric
import com.newton.couplespace.screens.health.data.repository.NutritionRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject

/**
 * ViewModel for nutrition-related features (meals and water intake)
 */
@HiltViewModel
class NutritionViewModel @Inject constructor(
    private val nutritionRepository: NutritionRepository,
    private val auth: FirebaseAuth,
    private val waterReminderManager: com.newton.couplespace.screens.health.service.WaterReminderManager
) : ViewModel() {
    
    // Selected date for data display
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()
    
    // Daily meals
    private val _meals = MutableStateFlow<List<MealEntry>>(emptyList())
    val meals: StateFlow<List<MealEntry>> = _meals.asStateFlow()
    
    // Water intake for the day
    private val _waterIntake = MutableStateFlow<List<WaterIntakeMetric>>(emptyList())
    val waterIntake: StateFlow<List<WaterIntakeMetric>> = _waterIntake.asStateFlow()
    
    // Total water intake for the day
    private val _totalWaterIntake = MutableStateFlow(0)
    val totalWaterIntake: StateFlow<Int> = _totalWaterIntake.asStateFlow()
    
    // Water intake goal (default 2000ml)
    private val _waterGoal = MutableStateFlow(2000)
    val waterGoal: StateFlow<Int> = _waterGoal.asStateFlow()
    
    // Daily nutrition summary
    private val _nutritionSummary = MutableStateFlow<DailyNutritionSummary?>(null)
    val nutritionSummary: StateFlow<DailyNutritionSummary?> = _nutritionSummary.asStateFlow()
    
    // Food search results
    private val _searchResults = MutableStateFlow<List<FoodItem>>(emptyList())
    val searchResults: StateFlow<List<FoodItem>> = _searchResults.asStateFlow()
    
    // Loading state
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    // New meal being created/edited
    private val _currentMeal = MutableStateFlow<MealEntry?>(null)
    val currentMeal: StateFlow<MealEntry?> = _currentMeal.asStateFlow()
    
    // Water reminder settings
    private val _waterReminderSettings = MutableStateFlow<Map<String, Any>>(emptyMap())
    val waterReminderSettings: StateFlow<Map<String, Any>> = _waterReminderSettings.asStateFlow()
    
    // Water reminder enabled state
    private val _waterReminderEnabled = MutableStateFlow(false)
    val waterReminderEnabled: StateFlow<Boolean> = _waterReminderEnabled.asStateFlow()
    
    // Error state
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadNutritionData()
        loadWaterGoal()
        loadWaterReminderSettings()
    }
    
    /**
     * Sets the selected date and refreshes nutrition data
     */
    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        loadNutritionData()
    }
    
    /**
     * Loads nutrition data for the selected date
     */
    private fun loadNutritionData() {
        viewModelScope.launch {
            _isLoading.value = true
            
            val date = _selectedDate.value
            println("DEBUG: ViewModel - Loading nutrition data for date: $date")
            
            // Load meals
            nutritionRepository.getMealsForDate(date).collect { mealsList ->
                _meals.value = mealsList
                println("DEBUG: ViewModel - Loaded ${mealsList.size} meals")
            }
            
            // Load water intake
            println("DEBUG: ViewModel - Fetching water intake data")
            nutritionRepository.getWaterIntakeForDate(date).collect { waterIntakeList ->
                println("DEBUG: ViewModel - Received ${waterIntakeList.size} water intake records")
                _waterIntake.value = waterIntakeList
                val totalWater = waterIntakeList.sumOf { it.amount }
                println("DEBUG: ViewModel - Setting total water intake to: $totalWater ml")
                _totalWaterIntake.value = totalWater
            }
            
            // Create nutrition summary
            updateNutritionSummary()
            
            _isLoading.value = false
            println("DEBUG: ViewModel - Nutrition data loading complete")
        }
    }
    
    /**
     * Updates the nutrition summary for the current date
     */
    private fun updateNutritionSummary() {
        val date = _selectedDate.value
        createNutritionSummary(date)
    }
    
    /**
     * Create a nutrition summary for the current date
     */
    private fun createNutritionSummary(date: LocalDate) {
        viewModelScope.launch {
            try {
                nutritionRepository.getNutritionSummaryForDate(date).collect { summary ->
                    _nutritionSummary.value = summary
                }
            } catch (e: Exception) {
                // Fallback to creating a summary from local data
                val summary = DailyNutritionSummary(
                    date = date,
                    totalCalories = _meals.value.sumOf { it.calories },
                    totalProtein = _meals.value.sumOf { it.protein.toInt() },
                    totalCarbs = _meals.value.sumOf { it.carbs.toInt() },
                    totalFat = _meals.value.sumOf { it.fat.toInt() },
                    totalWaterIntake = _totalWaterIntake.value
                )
                
                _nutritionSummary.value = summary
            }
        }
    }
    
    /**
     * Searches for food items
     */
    fun searchFood(query: String) {
        if (query.length < 2) {
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            nutritionRepository.searchFood(query).collect { results ->
                _searchResults.value = results
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Starts creating a new meal
     */
    fun startNewMeal() {
        _currentMeal.value = MealEntry(
            id = UUID.randomUUID().toString(),
            userId = auth.currentUser?.uid ?: "currentUserId",
            name = "",
            category = "breakfast",
            timestamp = Instant.now(),
            calories = 0,
            carbs = 0f,
            protein = 0f,
            fat = 0f,
            foods = emptyList(),
            notes = "",
            imageUri = null,
            isShared = false,
            isFavorite = false,
            tags = emptyList()
        )
    }
    
    /**
     * Starts editing an existing meal
     */
    fun editMeal(mealId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val meal = nutritionRepository.getMealById(mealId)
                _currentMeal.value = meal
            } catch (e: Exception) {
                // Handle error - meal not found
                println("Error fetching meal: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Adds a food item to the current meal
     */
    fun addFoodToMeal(foodItem: FoodItem) {
        val currentMealValue = _currentMeal.value ?: return
        
        // Update the meal with the new food item
        val updatedMeal = currentMealValue.copy(
            calories = currentMealValue.calories + foodItem.calories,
            protein = currentMealValue.protein + foodItem.protein,
            carbs = currentMealValue.carbs + foodItem.carbs,
            fat = currentMealValue.fat + foodItem.fat
        )
        
        _currentMeal.value = updatedMeal
    }
    
    /**
     * Saves the current meal with just a name update
     */
    fun saveMeal(name: String) {
        val meal = _currentMeal.value ?: return
        
        if (name.isBlank()) {
            // Handle error - meal name cannot be blank
            return
        }
        
        val updatedMeal = meal.copy(name = name)
        
        viewModelScope.launch {
            _isLoading.value = true
            val mealId = nutritionRepository.saveMeal(updatedMeal)
            _currentMeal.value = null
            loadNutritionData() // Refresh data to include the new meal
        }
    }

    /**
     * Saves a complete meal entry
     * @param mealEntry The complete meal entry to save
     */
    fun saveMeal(mealEntry: MealEntry) {
        if (mealEntry.name.isBlank()) {
            // Handle error - meal name cannot be blank
            return
        }
        
        viewModelScope.launch {
            _isLoading.value = true
            val mealId = nutritionRepository.saveMeal(mealEntry)
            _currentMeal.value = null
            loadNutritionData() // Refresh data to include the new meal
        }
    }
    
    /**
     * Cancels the current meal creation/editing operation
     */
    fun cancelMeal() {
        _currentMeal.value = null
    }
    
    /**
     * Deletes a meal
     */
    fun deleteMeal(mealId: String) {
        viewModelScope.launch {
            nutritionRepository.deleteMeal(mealId)
            loadNutritionData() // Refresh data to reflect the deletion
        }
    }
    
    /**
     * Updates a meal's shared status
     */
    fun updateMealSharedStatus(mealId: String, isShared: Boolean) {
        viewModelScope.launch {
            nutritionRepository.updateMealSharedStatus(mealId, isShared)
            loadNutritionData() // Refresh data to reflect the update
        }
    }
    
    /**
     * Records water intake
     */
    fun recordWaterIntake(amountMl: Int) {
        if (amountMl <= 0) {
            println("DEBUG: ViewModel - Invalid water amount: $amountMl")
            return
        }
        
        println("DEBUG: ViewModel - Recording water intake: $amountMl ml")
        
        // Immediately update the UI with the new water intake
        val currentTotal = _totalWaterIntake.value
        val newTotal = currentTotal + amountMl
        println("DEBUG: ViewModel - Updating UI immediately: $currentTotal + $amountMl = $newTotal ml")
        _totalWaterIntake.value = newTotal
        
        // Create a permanent water intake record (not temporary)
        val waterIntakeId = UUID.randomUUID().toString()
        val waterIntake = WaterIntakeMetric(
            id = waterIntakeId,
            userId = auth.currentUser?.uid ?: "current-user",
            timestamp = Instant.now(),
            amount = amountMl,
            source = "User Input",
            isShared = false
        )
        
        // Add to the current list
        val currentList = _waterIntake.value.toMutableList()
        currentList.add(waterIntake)
        _waterIntake.value = currentList
        
        // Then save to repository WITHOUT refreshing data
        viewModelScope.launch {
            try {
                val result = nutritionRepository.recordWaterIntake(amountMl)
                println("DEBUG: ViewModel - Water intake recorded with result: $result")
                
                // Don't refresh the data from Firebase immediately
                // This prevents the UI from resetting
                
                // Schedule a delayed refresh to ensure data consistency
                delay(2000) // Wait 2 seconds before refreshing
                println("DEBUG: ViewModel - Performing delayed refresh of nutrition data")
                loadNutritionData()
            } catch (e: Exception) {
                println("DEBUG: ViewModel - Error recording water intake: ${e.message}")
                // Keep the UI update even if the repository call fails
            }
        }
    }
    
    /**
     * Loads the user's water goal from the repository
     */
    private fun loadWaterGoal() {
        viewModelScope.launch {
            try {
                nutritionRepository.getWaterGoal().collect { goal ->
                    println("DEBUG: ViewModel - Loaded water goal: $goal ml")
                    _waterGoal.value = goal
                }
            } catch (e: Exception) {
                println("DEBUG: ViewModel - Error loading water goal: ${e.message}")
                // Keep the default water goal
            }
        }
    }
    
    /**
     * Updates the user's water goal
     */
    fun updateWaterGoal(goalMl: Int) {
        if (goalMl < 500 || goalMl > 5000) {
            println("DEBUG: ViewModel - Invalid water goal: $goalMl ml (must be between 500-5000)")
            return
        }
        
        // Update UI immediately
        _waterGoal.value = goalMl
        
        // Save to repository
        viewModelScope.launch {
            try {
                val result = nutritionRepository.updateWaterGoal(goalMl)
                println("DEBUG: ViewModel - Water goal updated: $result")
            } catch (e: Exception) {
                println("DEBUG: ViewModel - Error updating water goal: ${e.message}")
                // Keep the UI update even if the repository call fails
            }
        }
    }
    
    /**
     * Loads water reminder settings from the repository
     */
    private fun loadWaterReminderSettings() {
        viewModelScope.launch {
            try {
                nutritionRepository.getWaterReminderSchedule().collect { settings ->
                    println("DEBUG: ViewModel - Loaded water reminder settings: $settings")
                    // Make sure we have all required fields with proper types
                    val validatedSettings = mapOf<String, Any>(
                        "intervalMinutes" to ((settings["intervalMinutes"] as? Number)?.toInt() ?: 60),
                        "startHour" to ((settings["startHour"] as? Number)?.toInt() ?: 8),
                        "endHour" to ((settings["endHour"] as? Number)?.toInt() ?: 20),
                        "enabled" to (settings["enabled"] as? Boolean ?: false),
                        "lastUpdated" to (settings["lastUpdated"] ?: System.currentTimeMillis())
                    )
                    _waterReminderSettings.value = validatedSettings
                    _waterReminderEnabled.value = validatedSettings["enabled"] as Boolean
                    println("DEBUG: ViewModel - Validated water reminder settings: $validatedSettings")
                }
            } catch (e: Exception) {
                println("DEBUG: ViewModel - Error loading water reminder settings: ${e.message}")
                // Set default values on error
                _waterReminderSettings.value = mapOf(
                    "intervalMinutes" to 60,
                    "startHour" to 8,
                    "endHour" to 20,
                    "enabled" to false,
                    "lastUpdated" to System.currentTimeMillis()
                )
                _waterReminderEnabled.value = false
            }
        }
    }
    
    /**
     * Update water reminder settings
     */
    fun updateWaterReminderSettings(intervalMinutes: Int, startTime: Int, endTime: Int, enabled: Boolean) {
        viewModelScope.launch {
            try {
                println("DEBUG: ViewModel - Updating water reminder settings: interval=$intervalMinutes, start=$startTime, end=$endTime, enabled=$enabled")
                
                // Update settings in repository
                val result = nutritionRepository.setWaterReminderSchedule(
                    intervalMinutes = intervalMinutes,
                    startTime = startTime,
                    endTime = endTime,
                    enabled = enabled
                )
                
                if (result) {
                    println("DEBUG: ViewModel - Water reminder settings updated successfully")
                    // Reload settings to reflect changes
                    loadWaterReminderSettings()
                    
                    // Update reminder schedule - only pass the enabled parameter
                    waterReminderManager.updateReminderSchedule(enabled)
                } else {
                    println("DEBUG: ViewModel - Failed to update water reminder settings")
                }
            } catch (e: Exception) {
                println("DEBUG: ViewModel - Error updating water reminder settings: ${e.message}")
            }
        }
    }
    
    /**
     * Toggles water reminder enabled state
     */
    fun toggleWaterReminders(enabled: Boolean) {
        val currentSettings = _waterReminderSettings.value
        val intervalMinutes = currentSettings["intervalMinutes"] as? Int ?: 60
        val startTime = currentSettings["startHour"] as? Int ?: 8
        val endTime = currentSettings["endHour"] as? Int ?: 20
        
        updateWaterReminderSettings(intervalMinutes, startTime, endTime, enabled)
    }
}
