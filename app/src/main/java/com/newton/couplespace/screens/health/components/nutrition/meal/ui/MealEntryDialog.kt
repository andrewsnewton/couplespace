package com.newton.couplespace.screens.health.components.nutrition.meal.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.newton.couplespace.screens.health.components.nutrition.meal.animation.MealAnimations
import com.newton.couplespace.screens.health.components.nutrition.meal.functional.BarcodeScanner
import com.newton.couplespace.screens.health.components.nutrition.meal.functional.VoiceInputHandler
import com.newton.couplespace.screens.health.data.models.FoodItem
import com.newton.couplespace.screens.health.data.models.MealEntry
import com.newton.couplespace.ui.components.TimePickerWheel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Dialog for adding or editing a meal entry
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealEntryDialog(
    initialMealEntry: MealEntry? = null,
    onDismiss: () -> Unit,
    onSave: (MealEntry) -> Unit,
    selectedDate: LocalDate = LocalDate.now(),
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    
    // State for dialog screens
    var currentScreen by remember { mutableStateOf(MealEntryScreen.CATEGORY) }
    
    // State for meal entry
    var mealName by remember { mutableStateOf(initialMealEntry?.name ?: "") }
    var selectedCategory by remember { mutableStateOf(initialMealEntry?.category ?: "breakfast") }
    var selectedTime by remember { mutableStateOf(initialMealEntry?.timestamp?.atZone(ZoneId.systemDefault())?.toLocalTime() ?: LocalTime.now()) }
    var notes by remember { mutableStateOf(initialMealEntry?.notes ?: "") }
    var foodItems by remember { mutableStateOf(initialMealEntry?.foods ?: emptyList<FoodItem>()) }
    var isShared by remember { mutableStateOf(initialMealEntry?.isShared ?: false) }
    var isFavorite by remember { mutableStateOf(initialMealEntry?.isFavorite ?: false) }
    
    // State for search functionality
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<FoodItem>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    
    // State for UI components
    var showBarcodeScannerView by remember { mutableStateOf(false) }
    var showVoiceInputView by remember { mutableStateOf(false) }
    var showTimePickerDialog by remember { mutableStateOf(false) }
    
    // Animation state - start visible immediately
    var isDialogVisible by remember { mutableStateOf(true) }
    
    // Focus requester for search bar
    val searchFocusRequester = remember { FocusRequester() }
    
    // Mock search function (would be replaced with actual repository call)
    fun performSearch(query: String) {
        // This would call the repository's searchFoodByName function
        // For now, we'll use mock data
        searchResults = listOf(
            FoodItem(
                id = "1",
                name = "Grilled Chicken Breast",
                servingSize = "100g",
                calories = 165,
                protein = 31f,
                carbs = 0f,
                fat = 3.6f,
                category = "meat"
            ),
            FoodItem(
                id = "2",
                name = "Brown Rice",
                servingSize = "100g cooked",
                calories = 112,
                protein = 2.6f,
                carbs = 23f,
                fat = 0.9f,
                fiber = 1.8f,
                category = "grain"
            ),
            FoodItem(
                id = "3",
                name = "Avocado",
                servingSize = "1 medium",
                calories = 240,
                protein = 3f,
                carbs = 12f,
                fat = 22f,
                fiber = 10f,
                category = "fruit"
            )
        ).filter { it.name.contains(query, ignoreCase = true) }
    }
    
    // Function to add food item to meal
    fun addFoodItem(foodItem: FoodItem) {
        foodItems = foodItems + foodItem
    }
    
    // Function to remove food item from meal
    fun removeFoodItem(foodItem: FoodItem) {
        foodItems = foodItems.filter { it.id != foodItem.id }
    }
    
    // Function to create meal entry
    fun createMealEntry(): MealEntry {
        // Convert LocalDateTime to Instant for timestamp
        val dateTime = LocalDateTime.of(selectedDate, selectedTime)
        val timestamp = dateTime.atZone(ZoneId.systemDefault()).toInstant()
        
        return MealEntry(
            id = initialMealEntry?.id ?: UUID.randomUUID().toString(),
            userId = initialMealEntry?.userId ?: "currentUserId",
            name = mealName.ifEmpty { getMealNameFromCategory(selectedCategory) },
            timestamp = timestamp,
            calories = foodItems.sumOf { it.calories },
            protein = foodItems.sumOf { it.protein.toDouble() }.toFloat(),
            carbs = foodItems.sumOf { it.carbs.toDouble() }.toFloat(),
            fat = foodItems.sumOf { it.fat.toDouble() }.toFloat(),
            foods = foodItems,
            category = selectedCategory,
            notes = notes,
            imageUri = initialMealEntry?.imageUri,
            isShared = isShared,
            isFavorite = isFavorite,
            tags = initialMealEntry?.tags ?: emptyList()
        )
    }
    
    // Function to handle barcode scan result
    fun handleBarcodeResult(barcode: String) {
        // This would call the repository's getFoodByBarcode function
        // For now, we'll add a mock food item
        addFoodItem(
            FoodItem(
                id = UUID.randomUUID().toString(),
                name = "Scanned Item ($barcode)",
                servingSize = "1 serving",
                calories = 200,
                protein = 5f,
                carbs = 25f,
                fat = 10f,
                category = "packaged"
            )
        )
        showBarcodeScannerView = false
    }
    
    // Function to handle voice input result
    fun handleVoiceInputResult(text: String) {
        searchQuery = text
        performSearch(text)
    }
    
    // Dialog content
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        AnimatedVisibility(
            visible = isDialogVisible,
            enter = fadeIn(animationSpec = tween(50)) + 
                   slideInVertically(animationSpec = tween(100)) { it / 2 },
            exit = fadeOut(animationSpec = tween(50)) + 
                   slideOutVertically(animationSpec = tween(100)) { it / 2 }
        ) {
            Surface(
                modifier = modifier
                    .fillMaxSize()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Main content based on current screen
                    when (currentScreen) {
                        MealEntryScreen.CATEGORY -> CategorySelectionScreen(
                            selectedCategoryId = selectedCategory,
                            onCategorySelected = { 
                                selectedCategory = it
                                currentScreen = MealEntryScreen.FOOD_SEARCH
                            },
                            onDismiss = {
                                scope.launch {
                                    isDialogVisible = false
                                    delay(50) // Reduced delay for faster dismissal
                                    onDismiss()
                                }
                            }
                        )
                        
                        MealEntryScreen.FOOD_SEARCH -> FoodSearchScreen(
                            searchQuery = searchQuery,
                            onSearchQueryChange = { 
                                searchQuery = it
                                performSearch(it)
                            },
                            searchResults = searchResults,
                            isSearching = isSearching,
                            onFoodItemSelected = { addFoodItem(it) },
                            onBarcodeScanRequest = { showBarcodeScannerView = true },
                            onVoiceInputRequest = { showVoiceInputView = true },
                            onBack = { currentScreen = MealEntryScreen.CATEGORY },
                            onNext = { currentScreen = MealEntryScreen.REVIEW },
                            foodItems = foodItems,
                            onRemoveFoodItem = { removeFoodItem(it) },
                            focusRequester = searchFocusRequester
                        )
                        
                        MealEntryScreen.REVIEW -> ReviewScreen(
                            mealName = mealName,
                            onMealNameChange = { mealName = it },
                            selectedTime = selectedTime,
                            onTimeClick = { showTimePickerDialog = true },
                            notes = notes,
                            onNotesChange = { notes = it },
                            foodItems = foodItems,
                            onRemoveFoodItem = { removeFoodItem(it) },
                            onBack = { currentScreen = MealEntryScreen.FOOD_SEARCH },
                            onSave = {
                                scope.launch {
                                    val mealEntry = createMealEntry()
                                    isDialogVisible = false
                                    delay(300)
                                    onSave(mealEntry)
                                    onDismiss()
                                }
                            }
                        )
                    }
                    
                    // Overlay views
                    if (showBarcodeScannerView) {
                        BarcodeScanner(
                            onBarcodeDetected = { handleBarcodeResult(it) },
                            onClose = { showBarcodeScannerView = false }
                        )
                    }
                    
                    if (showVoiceInputView) {
                        VoiceInputHandler(
                            onTextRecognized = { handleVoiceInputResult(it) },
                            onDismiss = { showVoiceInputView = false }
                        )
                    }
                    
                    if (showTimePickerDialog) {
                        AlertDialog(
                            onDismissRequest = { showTimePickerDialog = false },
                            title = { Text("Select Time") },
                            text = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    TimePickerWheel(
                                        value = selectedTime.hour,
                                        onValueChange = { selectedTime = selectedTime.withHour(it) },
                                        range = 0..23
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = { showTimePickerDialog = false }) {
                                    Text("Confirm")
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

/**
 * Helper function to get meal name from category
 */
private fun getMealNameFromCategory(categoryId: String): String {
    return when (categoryId) {
        "breakfast" -> "Breakfast"
        "lunch" -> "Lunch"
        "dinner" -> "Dinner"
        "snack" -> "Snack"
        else -> "Meal"
    }
}

/**
 * Enum for meal entry dialog screens
 */
private enum class MealEntryScreen {
    CATEGORY,
    FOOD_SEARCH,
    REVIEW
}
