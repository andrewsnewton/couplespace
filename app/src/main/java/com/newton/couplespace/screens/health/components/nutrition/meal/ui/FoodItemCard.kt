package com.newton.couplespace.screens.health.components.nutrition.meal.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.newton.couplespace.screens.health.data.models.FoodItem
import com.newton.couplespace.screens.health.components.nutrition.meal.ui.NutrientBar

/**
 * A modern, interactive card for displaying food items with nutritional information
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodItemCard(
    foodItem: FoodItem,
    onEdit: () -> Unit = {},
    onDelete: () -> Unit = {},
    onPortionChange: (Double) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    // Determine color based on food category (simplified example)
    val categoryColor = when {
        foodItem.name.contains("fruit", ignoreCase = true) -> Color(0xFF4CAF50) // Green
        foodItem.name.contains("vegetable", ignoreCase = true) -> Color(0xFF8BC34A) // Light Green
        foodItem.name.contains("meat", ignoreCase = true) -> Color(0xFFF44336) // Red
        foodItem.name.contains("dairy", ignoreCase = true) -> Color(0xFF2196F3) // Blue
        foodItem.name.contains("grain", ignoreCase = true) -> Color(0xFFFFEB3B) // Yellow
        else -> Color(0xFF6200EE) // Default primary color
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category indicator and food name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(categoryColor.copy(alpha = 0.2f))
                            .border(2.dp, categoryColor.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Restaurant,
                            contentDescription = null,
                            tint = categoryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column {
                        Text(
                            text = foodItem.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = "${foodItem.calories} kcal · ${foodItem.servingSize}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                // Calories and expand button
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                            contentDescription = if (expanded) "Show less" else "Show more"
                        )
                    }
                }
            }
            
            // Expanded content with nutritional details
            if (expanded) {
                Spacer(modifier = Modifier.height(16.dp))
                
                // Nutrient bars
                NutrientBar(
                    name = "Carbs",
                    value = foodItem.carbs,
                    maxValue = 100f, // This should be based on daily recommended values
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                NutrientBar(
                    name = "Protein",
                    value = foodItem.protein,
                    maxValue = 50f, // This should be based on daily recommended values
                    color = Color(0xFF2196F3),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                NutrientBar(
                    name = "Fat",
                    value = foodItem.fat,
                    maxValue = 65f, // This should be based on daily recommended values
                    color = Color(0xFFFFC107),
                    modifier = Modifier.padding(vertical = 4.dp)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Portion slider
                Text(
                    text = "Portion Size",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                var sliderPosition by remember { mutableFloatStateOf(1f) }
                
                Slider(
                    value = sliderPosition,
                    onValueChange = { 
                        sliderPosition = it
                        onPortionChange(it.toDouble())
                    },
                    valueRange = 0.25f..2f,
                    steps = 7,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
                
                Text(
                    text = when {
                        sliderPosition < 0.5f -> "Small portion"
                        sliderPosition < 1f -> "Medium portion"
                        sliderPosition < 1.5f -> "Large portion"
                        else -> "Extra large portion"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedIconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit food item"
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    OutlinedIconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(40.dp),
                        colors = IconButtonDefaults.outlinedIconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove food item"
                        )
                    }
                }
            }
        }
    }
}
