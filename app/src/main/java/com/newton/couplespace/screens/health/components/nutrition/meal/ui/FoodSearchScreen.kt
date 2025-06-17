package com.newton.couplespace.screens.health.components.nutrition.meal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import android.util.Log
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.newton.couplespace.screens.health.components.nutrition.meal.animation.MealAnimations
import com.newton.couplespace.screens.health.components.nutrition.meal.animation.bounceInEffect
import com.newton.couplespace.screens.health.components.nutrition.meal.animation.staggeredReveal
import androidx.compose.ui.composed
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import com.newton.couplespace.screens.health.data.models.FoodItem
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Screen for searching and selecting food items
 */

// Animation functions now imported from MealAnimations.kt

/**
 * Screen for searching and selecting food items
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodSearchScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    searchResults: List<FoodItem>,
    isSearching: Boolean,
    onFoodItemSelected: (FoodItem) -> Unit,
    onBarcodeScanRequest: () -> Unit,
    onVoiceInputRequest: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    foodItems: List<FoodItem>,
    onRemoveFoodItem: (FoodItem) -> Unit,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    
    // Effect to focus search bar when screen appears
    LaunchedEffect(Unit) {
        // Delay to ensure the UI is ready before requesting focus
        delay(300)
        try {
            focusRequester.requestFocus()
        } catch (e: IllegalStateException) {
            // Handle the case where the FocusRequester is not attached yet
            Log.e("FoodSearchScreen", "Failed to request focus: ${e.message}")
        }
    }
    
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // Top app bar
        TopAppBar(
            title = { Text("Add Food") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Back"
                    )
                }
            },
            actions = {
                if (foodItems.isNotEmpty()) {
                    IconButton(onClick = onNext) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Continue"
                        )
                    }
                }
            }
        )
        
        // Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            // Search bar
            FoodSearchBar(
                query = searchQuery,
                onQueryChange = onSearchQueryChange,
                onSearch = { query -> /* Handle search submission if needed */ },
                onBarcodeScan = onBarcodeScanRequest,
                onVoiceInput = onVoiceInputRequest,
                suggestions = searchResults.take(5),
                focusRequester = focusRequester,
                onSuggestionSelected = { foodItem ->
                    onFoodItemSelected(foodItem)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
                    .staggeredReveal()
            )
            
            // Selected food items
            if (foodItems.isNotEmpty()) {
                Text(
                    text = "Selected Items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .staggeredReveal(200)
                )
                
                LazyColumn(
                    modifier = Modifier
                        .weight(0.4f)
                        .fillMaxWidth()
                ) {
                    items(foodItems) { foodItem ->
                        key(foodItem.id) {
                            FoodItemCard(
                                foodItem = foodItem,
                                onDelete = { onRemoveFoodItem(foodItem) },
                                onEdit = { /* TODO: Implement edit */ },
                                onPortionChange = { portion -> /* TODO: Implement portion change */ },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .bounceInEffect()
                            )
                        }
                    }
                }
            }
            
            // Search results
            Box(
                modifier = Modifier
                    .weight(if (foodItems.isEmpty()) 1f else 0.6f)
                    .fillMaxWidth()
            ) {
                if (isSearching) {
                    // Loading indicator
                    CircularProgressIndicator(
                        modifier = Modifier
                            .size(48.dp)
                            .align(Alignment.Center)
                    )
                } else if (searchResults.isEmpty() && searchQuery.isNotEmpty()) {
                    // No results message
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "No food items found",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Button(
                            onClick = {
                                // This would open a dialog to add a custom food item
                                // For now, we'll add a mock item
                                onFoodItemSelected(
                                    FoodItem(
                                        id = java.util.UUID.randomUUID().toString(),
                                        name = searchQuery,
                                        servingSize = "1 serving",
                                        calories = 100,
                                        protein = 0f,
                                        carbs = 0f,
                                        fat = 0f,
                                        category = "custom"
                                    )
                                )
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Custom Food")
                        }
                    }
                } else if (searchQuery.isNotEmpty()) {
                    // Search results
                    LazyColumn(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(searchResults) { foodItem ->
                            key(foodItem.id) {
                                FoodItemCard(
                                    foodItem = foodItem,
                                    onDelete = { /* Not applicable for search results */ },
                                    onEdit = { /* Not applicable for search results */ },
                                    onPortionChange = { portion -> /* Not applicable for search results */ },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp)
                                        .bounceInEffect()
                                        .clickable { onFoodItemSelected(foodItem) }
                                )
                            }
                        }
                    }
                } else if (foodItems.isEmpty()) {
                    // Empty state
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Search for food items to add to your meal",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(onClick = onBarcodeScanRequest) {
                                Text("Scan Barcode")
                            }
                            
                            Button(onClick = onVoiceInputRequest) {
                                Text("Voice Input")
                            }
                        }
                    }
                }
            }
            
            // Continue button if items selected
            AnimatedVisibility(
                visible = foodItems.isNotEmpty(),
                enter = fadeIn(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300))
            ) {
                Button(
                    onClick = onNext,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(vertical = 8.dp),
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
}
