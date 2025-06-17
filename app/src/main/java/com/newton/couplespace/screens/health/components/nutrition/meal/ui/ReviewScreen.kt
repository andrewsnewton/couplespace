package com.newton.couplespace.screens.health.components.nutrition.meal.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import com.newton.couplespace.screens.health.components.nutrition.meal.animation.MealAnimations
import com.newton.couplespace.screens.health.components.nutrition.meal.animation.staggeredReveal
import com.newton.couplespace.screens.health.data.models.FoodItem
import com.newton.couplespace.screens.health.data.models.NutritionSummary
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/**
 * This screen is used to review and edit meal details before saving
 */

/**
 * Screen for reviewing and finalizing a meal entry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(
    mealName: String,
    onMealNameChange: (String) -> Unit,
    selectedTime: LocalTime,
    onTimeClick: () -> Unit,
    notes: String,
    onNotesChange: (String) -> Unit,
    foodItems: List<FoodItem>,
    onRemoveFoodItem: (FoodItem) -> Unit,
    onBack: () -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Calculate nutrition summary
    val nutritionSummary = remember(foodItems) {
        calculateNutritionSummary(foodItems)
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top app bar
        TopAppBar(
            title = { Text("Review Meal") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            }
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Meal name
            OutlinedTextField(
                value = mealName,
                onValueChange = onMealNameChange,
                label = { Text("Meal Name") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .staggeredReveal(index = 0),
                singleLine = true
            )
            
            // Time selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .clickable { onTimeClick() }
                    .staggeredReveal(index = 1),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.width(16.dp))
                
                Column {
                    Text(
                        text = "Time",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Text(
                        text = selectedTime.format(DateTimeFormatter.ofPattern("h:mm a")),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )
            
            // Nutrition summary
            Text(
                text = "Nutrition Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .staggeredReveal(index = 2)
            )
            
            NutritionSummaryView(
                nutritionSummary = nutritionSummary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .staggeredReveal(index = 3)
            )
            
            // Food items
            Text(
                text = "Food Items",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .staggeredReveal(index = 4)
            )
            
            foodItems.forEachIndexed { index, foodItem ->
                FoodItemCard(
                    foodItem = foodItem,
                    onEdit = { /* Edit functionality would be added here */ },
                    onDelete = { onRemoveFoodItem(foodItem) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .staggeredReveal(index = 5 + index)
                )
            }
            
            // Notes
            Text(
                text = "Notes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(top = 16.dp, bottom = 8.dp)
                    .staggeredReveal(index = 10)
            )
            
            OutlinedTextField(
                value = notes,
                onValueChange = onNotesChange,
                label = { Text("Add notes (optional)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp)
                    .padding(bottom = 16.dp)
                    .staggeredReveal(index = 11),
                maxLines = 5
            )
            
            // Save button
            Button(
                onClick = onSave,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(vertical = 8.dp)
                    .staggeredReveal(index = 12),
                shape = MaterialTheme.shapes.medium
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null
                )
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Text(
                    text = "Save Meal",
                    style = MaterialTheme.typography.titleMedium
                )
            }
            
            // Bottom spacing
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

/**
 * Calculate nutrition summary from food items
 */
private fun calculateNutritionSummary(foodItems: List<FoodItem>): NutritionSummary {
    var totalCalories = 0
    var totalProtein = 0f
    var totalCarbs = 0f
    var totalFat = 0f
    var totalFiber = 0f
    var totalSugar = 0f
    var totalSodium = 0f
    
    foodItems.forEach { foodItem ->
        totalCalories += foodItem.calories
        totalProtein += foodItem.protein
        totalCarbs += foodItem.carbs
        totalFat += foodItem.fat
        totalFiber += foodItem.fiber
        totalSugar += foodItem.sugar
        totalSodium += foodItem.sodium
    }
    
    return NutritionSummary(
        calories = totalCalories,
        protein = totalProtein,
        carbs = totalCarbs,
        fat = totalFat,
        fiber = totalFiber,
        sugar = totalSugar,
        sodium = totalSodium
    )
}
