package com.newton.couplespace.screens.health.components.nutrition.meal.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * Data class representing a meal category
 */
data class MealCategory(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color
)

/**
 * Predefined meal categories
 */
val mealCategories = listOf(
    MealCategory(
        id = "breakfast",
        name = "Breakfast",
        icon = Icons.Default.WbSunny,
        color = Color(0xFFFFA726) // Orange
    ),
    MealCategory(
        id = "lunch",
        name = "Lunch",
        icon = Icons.Default.Restaurant,
        color = Color(0xFF66BB6A) // Green
    ),
    MealCategory(
        id = "dinner",
        name = "Dinner",
        icon = Icons.Default.DinnerDining,
        color = Color(0xFF5C6BC0) // Indigo
    ),
    MealCategory(
        id = "snack",
        name = "Snack",
        icon = Icons.Default.Cookie,
        color = Color(0xFFEF5350) // Red
    ),
    MealCategory(
        id = "dessert",
        name = "Dessert",
        icon = Icons.Default.Cake,
        color = Color(0xFFEC407A) // Pink
    ),
    MealCategory(
        id = "drink",
        name = "Drink",
        icon = Icons.Default.LocalCafe,
        color = Color(0xFF26C6DA) // Cyan
    )
)

/**
 * A visually appealing meal category selector with animations
 */
@Composable
fun MealCategorySelector(
    selectedCategory: String,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(mealCategories) { category ->
            CategoryItem(
                category = category,
                isSelected = selectedCategory == category.id,
                onSelected = { onCategorySelected(category.id) }
            )
        }
    }
}

@Composable
private fun CategoryItem(
    category: MealCategory,
    isSelected: Boolean,
    onSelected: () -> Unit
) {
    val backgroundColor by animateColorAsState(
        targetValue = if (isSelected) {
            category.color.copy(alpha = 0.15f)
        } else {
            MaterialTheme.colorScheme.surface
        },
        animationSpec = tween(300),
        label = "BackgroundColorAnimation"
    )
    
    val borderColor by animateColorAsState(
        targetValue = if (isSelected) {
            category.color
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        animationSpec = tween(300),
        label = "BorderColorAnimation"
    )
    
    val textColor by animateColorAsState(
        targetValue = if (isSelected) {
            category.color
        } else {
            MaterialTheme.colorScheme.onSurface
        },
        animationSpec = tween(300),
        label = "TextColorAnimation"
    )
    
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.05f else 1f,
        animationSpec = tween(300),
        label = "ScaleAnimation"
    )
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(backgroundColor)
            .border(
                width = 1.dp,
                color = borderColor,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onSelected)
            .padding(12.dp)
            .width(72.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(category.color.copy(alpha = 0.1f))
                .border(
                    width = 1.dp,
                    color = category.color.copy(alpha = 0.3f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = category.icon,
                contentDescription = category.name,
                tint = category.color,
                modifier = Modifier.size(24.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = category.name,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}
