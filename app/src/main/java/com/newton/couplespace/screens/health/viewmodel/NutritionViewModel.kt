package com.newton.couplespace.screens.health.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newton.couplespace.screens.health.data.models.DailyNutritionSummary
import com.newton.couplespace.screens.health.data.models.FoodItem
import com.newton.couplespace.screens.health.data.models.MealEntry
import com.newton.couplespace.screens.health.data.models.WaterIntakeMetric
import com.newton.couplespace.screens.health.data.repository.NutritionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
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
    private val nutritionRepository: NutritionRepository
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
    
    init {
        loadNutritionData()
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
            
            // Load meals
            nutritionRepository.getMealsForDate(date).collect { mealsList ->
                _meals.value = mealsList
            }
            
            // Load water intake
            nutritionRepository.getWaterIntakeForDate(date).collect { waterIntakeList ->
                _waterIntake.value = waterIntakeList
                _totalWaterIntake.value = waterIntakeList.sumOf { it.amount }
            }
            
            // Create nutrition summary
            updateNutritionSummary()
            
            _isLoading.value = false
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
            id = "",
            userId = "currentUserId", // This would be retrieved from auth
            name = "",
            timestamp = Instant.now(),
            calories = 0,
            carbs = 0f,
            protein = 0f,
            fat = 0f,
            foods = emptyList()
        )
    }
    
    /**
     * Starts editing an existing meal
     */
    fun editMeal(mealId: String) {
        viewModelScope.launch {
            val meal = _meals.value.find { it.id == mealId }
            _currentMeal.value = meal
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
     * Saves the current meal
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
     * Cancels the current meal creation/editing
     */
    fun cancelMeal() {
        _currentMeal.value = null
    }
    
    /**
     * Saves a complete meal entry
     */
    fun saveMeal(meal: MealEntry) {
        viewModelScope.launch {
            _isLoading.value = true
            nutritionRepository.saveMeal(meal)
            loadNutritionData() // Refresh data to include the new meal
            _isLoading.value = false
        }
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
            // Handle error - amount must be positive
            return
        }
        
        viewModelScope.launch {
            nutritionRepository.recordWaterIntake(amountMl)
            loadNutritionData() // Refresh data to include the new water intake
        }
    }
    
    /**
     * Updates the water intake goal
     */
    fun updateWaterGoal(goalMl: Int) {
        if (goalMl <= 0) {
            // Handle error - goal must be positive
            return
        }
        
        _waterGoal.value = goalMl
        updateNutritionSummary() // Update summary with new goal
    }
}
