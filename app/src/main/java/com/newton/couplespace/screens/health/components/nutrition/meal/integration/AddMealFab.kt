package com.newton.couplespace.screens.health.components.nutrition.meal.integration

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.newton.couplespace.screens.health.viewmodel.MealTrackingViewModel

/**
 * Floating action button for adding a new meal
 * This can be used in the Scaffold's floatingActionButton parameter
 */
@Composable
fun AddMealFab(
    modifier: Modifier = Modifier,
    mealTrackingViewModel: MealTrackingViewModel = hiltViewModel()
) {
    // Show the meal entry dialog when the FAB is clicked
    FloatingActionButton(
        onClick = { mealTrackingViewModel.showAddMealDialog() },
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Meal"
        )
    }
}
