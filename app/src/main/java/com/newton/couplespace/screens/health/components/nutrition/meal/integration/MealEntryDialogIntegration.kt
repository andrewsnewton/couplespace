package com.newton.couplespace.screens.health.components.nutrition.meal.integration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.newton.couplespace.screens.health.components.nutrition.meal.data.FoodSearchRepository
import com.newton.couplespace.screens.health.components.nutrition.meal.ui.MealEntryDialog
import com.newton.couplespace.screens.health.data.models.FoodItem
import com.newton.couplespace.screens.health.data.models.MealEntry
import com.newton.couplespace.screens.health.viewmodel.MealTrackingViewModel
import com.newton.couplespace.screens.health.viewmodel.NutritionViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

/**
 * Integration component that connects the MealEntryDialog with the NutritionViewModel
 * This serves as a bridge between our modular meal tracking system and the app's main architecture
 */
@Composable
fun MealEntryDialogIntegration(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    nutritionViewModel: NutritionViewModel = hiltViewModel(),
    mealTrackingViewModel: MealTrackingViewModel = hiltViewModel(),
    existingMealId: String? = null
) {
    // Get current meal from NutritionViewModel if editing an existing meal
    val currentMeal by nutritionViewModel.currentMeal.collectAsState()
    val selectedDate by nutritionViewModel.selectedDate.collectAsState()
    
    // Local state for the dialog
    var showDialog by remember { mutableStateOf(isVisible) }
    
    // Handle visibility changes from parent
    if (isVisible != showDialog) {
        showDialog = isVisible
        
        // If dialog is being shown and we have an existing meal ID, start editing
        if (isVisible && existingMealId != null) {
            nutritionViewModel.editMeal(existingMealId)
        } else if (!isVisible) {
            nutritionViewModel.cancelMeal()
        } else if (isVisible) {
            nutritionViewModel.startNewMeal()
        }
    }
    
    // Don't show dialog if not visible
    if (!showDialog) {
        return
    }
    
    // Convert currentMeal to MealEntry if editing, or create a new one if adding
    val mealEntry = currentMeal?.let { meal ->
        MealEntry(
            id = meal.id,
            userId = "currentUserId", // This would be retrieved from auth
            name = meal.name,
            category = meal.category,
            timestamp = meal.timestamp,
            calories = meal.calories,
            protein = meal.protein,
            carbs = meal.carbs,
            fat = meal.fat,
            foods = meal.foods,
            notes = meal.notes,
            imageUri = meal.imageUri,
            isShared = meal.isShared,
            isFavorite = meal.isFavorite,
            tags = meal.tags
        )
    } ?: MealEntry(
        id = UUID.randomUUID().toString(),
        userId = "currentUserId", // This would be retrieved from auth
        name = "",
        category = "breakfast",
        timestamp = Instant.now(),
        calories = 0,
        protein = 0f,
        carbs = 0f,
        fat = 0f,
        foods = emptyList(),
        notes = "",
        imageUri = null,
        isShared = false,
        isFavorite = false,
        tags = emptyList()
    )
    
    // Show the MealEntryDialog
    MealEntryDialog(
        initialMealEntry = mealEntry,
        onDismiss = {
            onDismiss()
            nutritionViewModel.cancelMeal()
        },
        onSave = { updatedMealEntry ->
            // Save the meal using NutritionViewModel
            nutritionViewModel.saveMeal(updatedMealEntry)
            onDismiss()
        },
        selectedDate = selectedDate
    )
}

/**
 * Extension function to handle saving a MealEntry using NutritionViewModel
 */
private fun NutritionViewModel.saveMeal(mealEntry: MealEntry) {
    // Since we're now using the same MealEntry class, we can directly save it
    // Just call the existing saveMeal method with the meal name
    saveMeal(mealEntry.name)
}
