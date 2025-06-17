package com.newton.couplespace.screens.health.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.newton.couplespace.screens.health.components.nutrition.meal.data.FoodSearchRepository
import com.newton.couplespace.screens.health.data.models.FoodItem
import com.newton.couplespace.screens.health.data.models.MealEntry
import com.newton.couplespace.screens.health.data.models.NutritionSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * ViewModel for the Meal Tracking functionality
 */
@HiltViewModel
class MealTrackingViewModel @Inject constructor(
    private val foodSearchRepository: FoodSearchRepository
) : ViewModel() {

    // Selected date for meal tracking
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    // Meals for the selected date
    private val _meals = MutableStateFlow<List<MealEntry>>(emptyList())
    val meals: StateFlow<List<MealEntry>> = _meals.asStateFlow()

    // Nutrition summary for the selected date
    private val _nutritionSummary = MutableStateFlow(NutritionSummary())
    val nutritionSummary: StateFlow<NutritionSummary> = _nutritionSummary.asStateFlow()

    // Search state
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchResults = MutableStateFlow<List<FoodItem>>(emptyList())
    val searchResults: StateFlow<List<FoodItem>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    // Recent food items
    private val _recentFoodItems = MutableStateFlow<List<FoodItem>>(emptyList())
    val recentFoodItems: StateFlow<List<FoodItem>> = _recentFoodItems.asStateFlow()

    // Favorite food items
    private val _favoriteFoodItems = MutableStateFlow<List<FoodItem>>(emptyList())
    val favoriteFoodItems: StateFlow<List<FoodItem>> = _favoriteFoodItems.asStateFlow()

    // Dialog state
    private val _showMealEntryDialog = MutableStateFlow(false)
    val showMealEntryDialog: StateFlow<Boolean> = _showMealEntryDialog.asStateFlow()

    private val _editingMealEntry = MutableStateFlow<MealEntry?>(null)
    val editingMealEntry: StateFlow<MealEntry?> = _editingMealEntry.asStateFlow()

    // Error state
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        // Observe meals for the selected date
        viewModelScope.launch {
            _selectedDate
                .flatMapLatest { date ->
                    foodSearchRepository.getMealsForDate(date)
                }
                .catch { e ->
                    _errorMessage.value = "Error loading meals: ${e.message}"
                }
                .collect { meals ->
                    _meals.value = meals
                }
        }

        // Observe nutrition summary for the selected date
        viewModelScope.launch {
            _selectedDate
                .flatMapLatest { date ->
                    foodSearchRepository.getNutritionSummaryForDate(date)
                }
                .catch { e ->
                    _errorMessage.value = "Error loading nutrition summary: ${e.message}"
                }
                .collect { summary ->
                    _nutritionSummary.value = summary
                }
        }

        // Load recent food items
        viewModelScope.launch {
            foodSearchRepository.getRecentFoodItems()
                .catch { e ->
                    _errorMessage.value = "Error loading recent food items: ${e.message}"
                }
                .collect { items ->
                    _recentFoodItems.value = items
                }
        }

        // Load favorite food items
        viewModelScope.launch {
            foodSearchRepository.getFavoriteFoodItems()
                .catch { e ->
                    _errorMessage.value = "Error loading favorite food items: ${e.message}"
                }
                .collect { items ->
                    _favoriteFoodItems.value = items
                }
        }
    }

    /**
     * Set the selected date
     */
    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    /**
     * Search for food items by name
     */
    fun searchFoodByName(query: String) {
        _searchQuery.value = query
        
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch {
            try {
                _isSearching.value = true
                val results = foodSearchRepository.searchFoodByName(query)
                _searchResults.value = results
            } catch (e: Exception) {
                _errorMessage.value = "Error searching for food: ${e.message}"
            } finally {
                _isSearching.value = false
            }
        }
    }

    /**
     * Get food item by barcode
     */
    fun getFoodByBarcode(barcode: String): Flow<FoodItem?> = flow {
        try {
            val foodItem = foodSearchRepository.getFoodByBarcode(barcode)
            emit(foodItem)
        } catch (e: Exception) {
            _errorMessage.value = "Error scanning barcode: ${e.message}"
            emit(null)
        }
    }

    /**
     * Save a meal entry
     */
    fun saveMealEntry(mealEntry: MealEntry) {
        viewModelScope.launch {
            try {
                foodSearchRepository.saveMealEntry(mealEntry)
                _showMealEntryDialog.value = false
                _editingMealEntry.value = null
            } catch (e: Exception) {
                _errorMessage.value = "Error saving meal: ${e.message}"
            }
        }
    }

    /**
     * Delete a meal entry
     */
    fun deleteMealEntry(mealEntry: MealEntry) {
        viewModelScope.launch {
            try {
                foodSearchRepository.deleteMealEntry(mealEntry)
            } catch (e: Exception) {
                _errorMessage.value = "Error deleting meal: ${e.message}"
            }
        }
    }

    /**
     * Show the meal entry dialog for creating a new meal
     */
    fun showAddMealDialog() {
        _editingMealEntry.value = null
        _showMealEntryDialog.value = true
    }

    /**
     * Show the meal entry dialog for editing an existing meal
     */
    fun showEditMealDialog(mealId: String) {
        viewModelScope.launch {
            try {
                val mealEntry = foodSearchRepository.getMealEntryById(mealId)
                if (mealEntry != null) {
                    _editingMealEntry.value = mealEntry
                    _showMealEntryDialog.value = true
                } else {
                    _errorMessage.value = "Meal not found"
                }
            } catch (e: Exception) {
                _errorMessage.value = "Error loading meal: ${e.message}"
            }
        }
    }

    /**
     * Hide the meal entry dialog
     */
    fun hideMealEntryDialog() {
        _showMealEntryDialog.value = false
        _editingMealEntry.value = null
    }

    /**
     * Toggle favorite status for a food item
     */
    fun toggleFavoriteFoodItem(foodItem: FoodItem) {
        viewModelScope.launch {
            try {
                val updatedFoodItem = foodItem.copy(isFavorite = !foodItem.isFavorite)
                foodSearchRepository.updateFoodItem(updatedFoodItem)
            } catch (e: Exception) {
                _errorMessage.value = "Error updating food item: ${e.message}"
            }
        }
    }

    /**
     * Toggle favorite status for a meal entry
     */
    fun toggleFavoriteMeal(mealEntry: MealEntry) {
        viewModelScope.launch {
            try {
                // Get current favorite status and toggle it
                val currentFavorite = mealEntry.isFavorite
                val updatedMealEntry = mealEntry.copy(isFavorite = !currentFavorite)
                foodSearchRepository.saveMealEntry(updatedMealEntry)
            } catch (e: Exception) {
                _errorMessage.value = "Error updating meal: ${e.message}"
            }
        }
    }

    /**
     * Clear error message
     */
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    /**
     * Save a custom food item
     */
    fun saveCustomFoodItem(foodItem: FoodItem) {
        viewModelScope.launch {
            try {
                foodSearchRepository.saveCustomFoodItem(foodItem)
            } catch (e: Exception) {
                _errorMessage.value = "Error saving custom food item: ${e.message}"
            }
        }
    }
}
