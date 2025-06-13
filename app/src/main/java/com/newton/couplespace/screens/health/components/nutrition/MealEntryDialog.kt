package com.newton.couplespace.screens.health.components.nutrition

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.newton.couplespace.screens.health.data.models.FoodItem
import com.newton.couplespace.screens.health.data.models.MealEntry
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*

/**
 * Dialog for adding or editing a meal entry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealEntryDialog(
    onDismiss: () -> Unit,
    onSave: (MealEntry) -> Unit,
    initialMeal: MealEntry? = null,
    userId: String = "current-user" // In a real app, this would be injected
) {
    var mealName by remember { mutableStateOf(initialMeal?.name ?: "") }
    val foodItems = remember { mutableStateListOf<FoodItem>() }
    var newFoodName by remember { mutableStateOf("") }
    var newFoodCalories by remember { mutableStateOf("") }
    var newFoodCarbs by remember { mutableStateOf("") }
    var newFoodProtein by remember { mutableStateOf("") }
    var newFoodFat by remember { mutableStateOf("") }
    
    // foodItems is already declared above
    
    // Initialize food items if editing existing meal
    LaunchedEffect(initialMeal) {
        initialMeal?.let { meal ->
            if (meal.foods.isNotEmpty()) {
                foodItems.clear()
                foodItems.addAll(meal.foods)
            }
        }
    }
    
    // Calculate total nutrition
    val totalCalories = foodItems.sumOf { it.calories }
    val totalCarbs = foodItems.sumOf { it.carbs.toDouble() }.toFloat()
    val totalProtein = foodItems.sumOf { it.protein.toDouble() }.toFloat()
    val totalFat = foodItems.sumOf { it.fat.toDouble() }.toFloat()
    
    // Time selector
    val currentTime = remember {
        if (initialMeal != null) {
            // Convert Instant to LocalTime without using ofInstant which requires API 31
            val localDateTime = initialMeal.timestamp.atZone(ZoneId.systemDefault()).toLocalDateTime()
            LocalTime.of(localDateTime.hour, localDateTime.minute)
        } else {
            LocalTime.now()
        }
    }
    
    var selectedHour by remember { mutableStateOf(currentTime.hour) }
    var selectedMinute by remember { mutableStateOf(currentTime.minute) }
    var showTimePicker by remember { mutableStateOf(false) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 8.dp
            )
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Dialog title
                Text(
                    text = initialMeal?.let { "Edit Meal" } ?: "Add Meal",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Meal name
                OutlinedTextField(
                    value = mealName,
                    onValueChange = { mealName = it },
                    label = { Text("Meal Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Time picker
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Time: ",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    
                    val timeFormatter = DateTimeFormatter.ofPattern("hh:mm a")
                    val displayTime = LocalTime.of(selectedHour, selectedMinute)
                        .format(timeFormatter)
                    
                    TextButton(onClick = { showTimePicker = true }) {
                        Text(
                            text = displayTime,
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Food items section
                Text(
                    text = "Food Items",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // List existing food items
                foodItems.forEachIndexed { index, foodItem ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = foodItem.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                
                                Text(
                                    text = "${foodItem.calories} cal, " +
                                            "C: ${foodItem.carbs}g, " +
                                            "P: ${foodItem.protein}g, " +
                                            "F: ${foodItem.fat}g",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            
                            IconButton(
                                onClick = { foodItems.removeAt(index) }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Food Item"
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Add new food item
                Text(
                    text = "Add Food Item",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Food name
                OutlinedTextField(
                    value = newFoodName,
                    onValueChange = { newFoodName = it },
                    label = { Text("Food Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Nutritional info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newFoodCalories,
                        onValueChange = { newFoodCalories = it.filter { it.isDigit() } },
                        label = { Text("Calories") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        )
                    )
                    
                    OutlinedTextField(
                        value = newFoodCarbs,
                        onValueChange = { newFoodCarbs = it.filter { it.isDigit() || it == '.' } },
                        label = { Text("Carbs (g)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newFoodProtein,
                        onValueChange = { newFoodProtein = it.filter { it.isDigit() || it == '.' } },
                        label = { Text("Protein (g)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )
                    
                    OutlinedTextField(
                        value = newFoodFat,
                        onValueChange = { newFoodFat = it.filter { it.isDigit() || it == '.' } },
                        label = { Text("Fat (g)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal
                        )
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Add food button
                Button(
                    onClick = {
                        // Validate and add food item
                        if (newFoodName.isNotBlank() && newFoodCalories.isNotBlank()) {
                            val newItem = FoodItem(
                                fdcId = UUID.randomUUID().toString(),
                                description = newFoodName,
                                calories = newFoodCalories.toIntOrNull() ?: 0,
                                carbs = newFoodCarbs.toFloatOrNull() ?: 0f,
                                protein = newFoodProtein.toFloatOrNull() ?: 0f,
                                fat = newFoodFat.toFloatOrNull() ?: 0f,
                                servingSizeUnit = "g"
                            )
                            foodItems.add(newItem)
                            
                            // Clear fields
                            newFoodName = ""
                            newFoodCalories = ""
                            newFoodCarbs = ""
                            newFoodProtein = ""
                            newFoodFat = ""
                        }
                    },
                    modifier = Modifier.align(Alignment.End),
                    enabled = newFoodName.isNotBlank() && newFoodCalories.isNotBlank()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Food")
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Nutritional summary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Meal Summary",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = "Total Calories: $totalCalories cal",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Text(
                            text = "Total Carbs: $totalCarbs g",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Text(
                            text = "Total Protein: $totalProtein g",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Text(
                            text = "Total Fat: $totalFat g",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onDismiss
                    ) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            // Create meal entry and save
                            if (mealName.isNotBlank() && foodItems.isNotEmpty()) {
                                val timestamp = LocalTime.of(selectedHour, selectedMinute)
                                    .atDate(java.time.LocalDate.now())
                                    .atZone(ZoneId.systemDefault())
                                    .toInstant()
                                
                                val meal = MealEntry(
                                    id = initialMeal?.id ?: UUID.randomUUID().toString(),
                                    userId = userId,
                                    name = mealName,
                                    timestamp = timestamp,
                                    calories = totalCalories,
                                    carbs = totalCarbs,
                                    protein = totalProtein,
                                    fat = totalFat,
                                    foods = foodItems.toList(),
                                    isShared = initialMeal?.isShared ?: false
                                )
                                
                                onSave(meal)
                            }
                        },
                        enabled = mealName.isNotBlank() && foodItems.isNotEmpty()
                    ) {
                        Text("Save Meal")
                    }
                }
            }
        }
    }
    
    // Time picker dialog
    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedHour,
            initialMinute = selectedMinute
        )
        
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedHour = timePickerState.hour
                        selectedMinute = timePickerState.minute
                        showTimePicker = false
                    }
                ) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showTimePicker = false }
                ) {
                    Text("Cancel")
                }
            },
            title = { Text("Select Time") },
            text = {
                TimePicker(state = timePickerState)
            }
        )
    }
}
