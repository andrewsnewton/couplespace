package com.newton.couplespace.screens.health.components.nutrition.meal.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.newton.couplespace.screens.health.components.nutrition.meal.ui.clickable
import com.newton.couplespace.screens.health.data.models.DailyNutritionSummary
import com.newton.couplespace.screens.health.data.models.MealEntry
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * A card for logging and tracking meals and nutrition
 * This is a new implementation that replaces the old MealLoggerCard
 */
@Composable
fun MealLoggerCardNew(
    meals: List<MealEntry>,
    nutritionSummary: DailyNutritionSummary?,
    onAddMealClick: () -> Unit,
    onMealClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Header with title and add button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Meals & Nutrition",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                
                IconButton(onClick = onAddMealClick) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Meal",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Nutrition summary
            nutritionSummary?.let { summary ->
                NutritionSummary(summary = summary)
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Divider()
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Meals list
            if (meals.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No meals logged for today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Today's Meals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Use a regular Column instead of LazyColumn to avoid nested scrolling issues
                Column(
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    meals.forEach { meal ->
                        MealItem(
                            meal = meal,
                            onClick = { onMealClick(meal.id) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * A component displaying a nutrition summary
 */
@Composable
private fun NutritionSummary(
    summary: DailyNutritionSummary,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        // Calories
        Text(
            text = "Calories: ${summary.totalCalories} kcal",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Macronutrients
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            MacroNutrient(
                name = "Carbs",
                amount = summary.totalCarbs.toFloat(),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            
            MacroNutrient(
                name = "Protein",
                amount = summary.totalProtein.toFloat(),
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.weight(1f)
            )
            
            MacroNutrient(
                name = "Fat",
                amount = summary.totalFat.toFloat(),
                color = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

/**
 * A component displaying a macronutrient value
 */
@Composable
private fun MacroNutrient(
    name: String,
    amount: Float,
    color: Color,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color, CircleShape)
        )
        
        Spacer(modifier = Modifier.width(4.dp))
        
        Text(
            text = "$name: ${String.format("%.1f", amount)}g",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * A component displaying a meal item
 */
@Composable
private fun MealItem(
    meal: MealEntry,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formatter = remember { DateTimeFormatter.ofPattern("h:mm a") }
    val time = remember(meal) { 
        meal.getDateTime().format(formatter) 
    }
    
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Meal icon with colored background
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Restaurant,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.width(12.dp))
        
        // Meal details
        Column(
            modifier = Modifier.weight(1f)
        ) {
            // Name and time
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = meal.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                
                Text(
                    text = time,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Calories and macros
            val nutrition = meal.calculateTotalNutrition()
            Text(
                text = "${nutrition.calories} kcal • ${String.format("%.1f", nutrition.carbs)}g carbs • ${String.format("%.1f", nutrition.protein)}g protein • ${String.format("%.1f", nutrition.fat)}g fat",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        
        // Edit indicator
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = "Edit Meal",
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}
