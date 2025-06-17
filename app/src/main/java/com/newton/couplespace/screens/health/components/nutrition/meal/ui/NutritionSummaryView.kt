package com.newton.couplespace.screens.health.components.nutrition.meal.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.newton.couplespace.screens.health.data.models.NutritionSummary
import kotlinx.coroutines.launch

/**
 * A visually appealing component to display nutrition summary with animated charts
 */
@Composable
fun NutritionSummaryView(
    nutritionSummary: NutritionSummary,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Nutrition Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Calories display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CaloriesDisplay(calories = nutritionSummary.calories)
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Macronutrient breakdown
            Text(
                text = "Macronutrient Breakdown",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Calculate percentages
            val (proteinPercentage, carbsPercentage, fatPercentage) = 
                nutritionSummary.calculateMacroPercentages()
            
            MacronutrientChart(
                proteinPercentage = proteinPercentage,
                carbsPercentage = carbsPercentage,
                fatPercentage = fatPercentage,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(24.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Macronutrient details
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                MacronutrientDetail(
                    name = "Protein",
                    value = nutritionSummary.protein,
                    color = Color(0xFF2196F3),
                    percentage = proteinPercentage,
                    modifier = Modifier.weight(1f)
                )
                
                MacronutrientDetail(
                    name = "Carbs",
                    value = nutritionSummary.carbs,
                    color = Color(0xFF4CAF50),
                    percentage = carbsPercentage,
                    modifier = Modifier.weight(1f)
                )
                
                MacronutrientDetail(
                    name = "Fat",
                    value = nutritionSummary.fat,
                    color = Color(0xFFFFC107),
                    percentage = fatPercentage,
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Additional nutrients
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                AdditionalNutrientDetail(
                    name = "Fiber",
                    value = nutritionSummary.fiber,
                    modifier = Modifier.weight(1f)
                )
                
                AdditionalNutrientDetail(
                    name = "Sugar",
                    value = nutritionSummary.sugar,
                    modifier = Modifier.weight(1f)
                )
                
                AdditionalNutrientDetail(
                    name = "Sodium",
                    value = nutritionSummary.sodium,
                    unit = "mg",
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun CaloriesDisplay(
    calories: Int,
    modifier: Modifier = Modifier
) {
    val animatedCalories = remember { Animatable(0f) }
    
    LaunchedEffect(calories) {
        animatedCalories.animateTo(
            targetValue = calories.toFloat(),
            animationSpec = tween(
                durationMillis = 1000,
                easing = LinearEasing
            )
        )
    }
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = animatedCalories.value.toInt().toString(),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "calories",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun MacronutrientChart(
    proteinPercentage: Float,
    carbsPercentage: Float,
    fatPercentage: Float,
    modifier: Modifier = Modifier
) {
    val animatedProtein = remember { Animatable(0f) }
    val animatedCarbs = remember { Animatable(0f) }
    val animatedFat = remember { Animatable(0f) }
    
    val scope = rememberCoroutineScope()
    
    LaunchedEffect(proteinPercentage, carbsPercentage, fatPercentage) {
        scope.launch {
            animatedProtein.animateTo(
                targetValue = proteinPercentage,
                animationSpec = tween(durationMillis = 1000)
            )
        }
        scope.launch {
            animatedCarbs.animateTo(
                targetValue = carbsPercentage,
                animationSpec = tween(durationMillis = 1000)
            )
        }
        scope.launch {
            animatedFat.animateTo(
                targetValue = fatPercentage,
                animationSpec = tween(durationMillis = 1000)
            )
        }
    }
    
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(animatedProtein.value.coerceAtLeast(0.01f))
                    .background(Color(0xFF2196F3))
            )
            
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(animatedCarbs.value.coerceAtLeast(0.01f))
                    .background(Color(0xFF4CAF50))
            )
            
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .weight(animatedFat.value.coerceAtLeast(0.01f))
                    .background(Color(0xFFFFC107))
            )
        }
    }
}

@Composable
private fun MacronutrientDetail(
    name: String,
    value: Float,
    color: Color,
    percentage: Float,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "${value.toInt()}g",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        
        Text(
            text = "${(percentage * 100).toInt()}%",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun AdditionalNutrientDetail(
    name: String,
    value: Float,
    unit: String = "g",
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Text(
            text = "${value.toInt()}$unit",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
    }
}
