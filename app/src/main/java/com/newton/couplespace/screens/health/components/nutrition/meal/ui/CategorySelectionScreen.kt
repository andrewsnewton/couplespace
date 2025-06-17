package com.newton.couplespace.screens.health.components.nutrition.meal.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.newton.couplespace.screens.health.components.nutrition.meal.animation.staggeredReveal

/**
 * Screen for selecting meal category
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategorySelectionScreen(
    selectedCategoryId: String,
    onCategorySelected: (String) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top app bar
        TopAppBar(
            title = { Text("Add Meal") },
            navigationIcon = {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close"
                    )
                }
            }
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Title
            Text(
                text = "Select Meal Category",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(vertical = 16.dp)
                    // No animation for title for instant appearance
            )
            
            // Meal category selector
            MealCategorySelector(
                selectedCategory = selectedCategoryId,
                onCategorySelected = onCategorySelected,
                modifier = Modifier
                    .fillMaxWidth()
                    .staggeredReveal(5) // Minimal delay for near-instant appearance
            )
            
            Spacer(modifier = Modifier.weight(1f))
            
            // Continue button
            Button(
                onClick = { onCategorySelected(selectedCategoryId) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .staggeredReveal(10), // Minimal delay for near-instant appearance
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = "Continue",
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }
    }
}
