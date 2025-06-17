package com.newton.couplespace.screens.health.components.nutrition.meal.integration

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.newton.couplespace.screens.health.components.nutrition.meal.ui.MealEntryDialog
import com.newton.couplespace.screens.health.components.nutrition.meal.ui.MealLoggerCardNew
import com.newton.couplespace.screens.health.data.models.MealEntry
import com.newton.couplespace.screens.health.viewmodel.NutritionViewModel
import com.newton.couplespace.screens.health.viewmodel.MealTrackingViewModel

/**
 * Enhanced MealLoggerCard that integrates with the new MealEntryDialog
 * This component uses the new MealLoggerCardNew and adds the ability to open the MealEntryDialog
 */
@Composable
fun EnhancedMealLoggerCard(
    modifier: Modifier = Modifier,
    nutritionViewModel: NutritionViewModel = hiltViewModel(),
    mealTrackingViewModel: MealTrackingViewModel = hiltViewModel()
) {
    val meals by nutritionViewModel.meals.collectAsState()
    val nutritionSummary by nutritionViewModel.nutritionSummary.collectAsState()
    val selectedDate by nutritionViewModel.selectedDate.collectAsState()
    
    // State for showing the meal entry dialog
    var showMealEntryDialog by remember { mutableStateOf(false) }
    var editingMealId by remember { mutableStateOf<String?>(null) }
    
    // Show the new MealLoggerCardNew
    MealLoggerCardNew(
        modifier = modifier,
        meals = meals,
        nutritionSummary = nutritionSummary,
        onAddMealClick = {
            // Show dialog for adding a new meal
            editingMealId = null
            showMealEntryDialog = true
        },
        onMealClick = { mealId ->
            // Show dialog for editing an existing meal
            editingMealId = mealId
            showMealEntryDialog = true
        }
    )
    
    // Show the MealEntryDialog when needed
    MealEntryDialogIntegration(
        isVisible = showMealEntryDialog,
        onDismiss = { showMealEntryDialog = false },
        existingMealId = editingMealId,
        nutritionViewModel = nutritionViewModel,
        mealTrackingViewModel = mealTrackingViewModel
    )
}

/**
 * A preview version of MealLoggerCardNew that can be used during development
 * This is useful for testing the UI without needing to connect to real data
 */
@Composable
fun PreviewMealLoggerCard(
    modifier: Modifier = Modifier,
    meals: List<MealEntry> = emptyList(),
    onAddMealClick: () -> Unit = {},
    onMealClick: (String) -> Unit = {}
) {
    // Use our new MealLoggerCardNew for previews
    MealLoggerCardNew(
        modifier = modifier,
        meals = meals,
        nutritionSummary = null,
        onAddMealClick = onAddMealClick,
        onMealClick = onMealClick
    )
}
