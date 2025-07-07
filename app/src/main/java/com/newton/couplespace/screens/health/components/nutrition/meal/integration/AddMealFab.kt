package com.newton.couplespace.screens.health.components.nutrition.meal.integration

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Floating action button for adding a new meal
 * This can be used in the Scaffold's floatingActionButton parameter
 * 
 * @param onAddMealClick Callback to be invoked when the FAB is clicked
 */
@Composable
fun AddMealFab(
    modifier: Modifier = Modifier,
    onAddMealClick: () -> Unit
) {
    // Show the meal entry dialog when the FAB is clicked
    FloatingActionButton(
        onClick = onAddMealClick,
        containerColor = MaterialTheme.colorScheme.primaryContainer,
        modifier = modifier
    ) {
        Icon(
            imageVector = Icons.Default.Add,
            contentDescription = "Add Meal"
        )
    }
}
