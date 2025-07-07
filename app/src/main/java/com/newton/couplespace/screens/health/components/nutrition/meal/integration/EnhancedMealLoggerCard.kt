package com.newton.couplespace.screens.health.components.nutrition.meal.integration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.newton.couplespace.screens.health.components.nutrition.meal.ui.MealEntryDialog
import com.newton.couplespace.screens.health.components.nutrition.meal.ui.MealLoggerCardNew
import com.newton.couplespace.screens.health.data.models.MealEntry
import com.newton.couplespace.screens.health.viewmodel.MealTrackingViewModel
import com.newton.couplespace.screens.health.viewmodel.NutritionViewModel
import java.time.Instant
import java.util.UUID

/**
 * Enhanced MealLoggerCard that integrates with the NutritionViewModel and MealEntryDialog
 * This component serves as the main meal tracking interface in the Health Screen
 * 
 * @param showMealEntryDialog Boolean flag to control the visibility of the meal entry dialog externally
 * @param onShowMealEntryDialogChange Callback to update the showMealEntryDialog state
 */
@Composable
fun EnhancedMealLoggerCard(
    modifier: Modifier = Modifier,
    nutritionViewModel: NutritionViewModel = hiltViewModel(),
    mealTrackingViewModel: MealTrackingViewModel = hiltViewModel(),
    showMealEntryDialog: Boolean = false,
    onShowMealEntryDialogChange: (Boolean) -> Unit = {}
) {
    // Get meals and nutrition data from the NutritionViewModel
    val meals by nutritionViewModel.meals.collectAsState()
    val nutritionSummary by nutritionViewModel.nutritionSummary.collectAsState()
    val selectedDate by nutritionViewModel.selectedDate.collectAsState()
    
    // State for tracking which meal is being edited
    var editingMealId by remember { mutableStateOf<String?>(null) }
    
    // Show the MealLoggerCardNew component
    MealLoggerCardNew(
        modifier = modifier,
        meals = meals,
        nutritionSummary = nutritionSummary,
        onAddMealClick = {
            // Start creating a new meal
            nutritionViewModel.startNewMeal()
            editingMealId = null
            onShowMealEntryDialogChange(true)
        },
        onMealClick = { mealId ->
            // Start editing an existing meal
            nutritionViewModel.editMeal(mealId)
            editingMealId = mealId
            onShowMealEntryDialogChange(true)
        }
    )
    
    // Show the MealEntryDialog when needed
    if (showMealEntryDialog) {
        val currentMeal by nutritionViewModel.currentMeal.collectAsState()
        
        // If we have a current meal, show the dialog
        currentMeal?.let { meal ->
            MealEntryDialog(
                initialMealEntry = meal,
                onDismiss = {
                    onShowMealEntryDialogChange(false)
                    // Reset the current meal state
                    nutritionViewModel.cancelMeal()
                },
                onSave = { updatedMeal ->
                    // Create a properly formatted meal entry with all required fields
                    val completeUpdatedMeal = updatedMeal.copy(
                        id = updatedMeal.id.ifEmpty { UUID.randomUUID().toString() },
                        userId = updatedMeal.userId.ifEmpty { "currentUserId" },
                        timestamp = updatedMeal.timestamp ?: Instant.now(),
                        category = updatedMeal.category.ifEmpty { "meal" },
                        foods = updatedMeal.foods ?: emptyList(),
                        notes = updatedMeal.notes ?: "",
                        imageUri = updatedMeal.imageUri,
                        isShared = updatedMeal.isShared ?: false,
                        isFavorite = updatedMeal.isFavorite ?: false,
                        tags = updatedMeal.tags ?: emptyList()
                    )
                    
                    // Save the complete meal
                    nutritionViewModel.saveMeal(completeUpdatedMeal)
                    onShowMealEntryDialogChange(false)
                },
                selectedDate = selectedDate
            )
        } ?: run {
            // If currentMeal is null, create a default meal
            val defaultMeal = MealEntry(
                id = "",
                userId = "currentUserId",
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
            
            MealEntryDialog(
                initialMealEntry = defaultMeal,
                onDismiss = {
                    onShowMealEntryDialogChange(false)
                },
                onSave = { newMeal ->
                    // Save the new meal
                    nutritionViewModel.saveMeal(newMeal)
                    onShowMealEntryDialogChange(false)
                },
                selectedDate = selectedDate
            )
        }
    }
}
