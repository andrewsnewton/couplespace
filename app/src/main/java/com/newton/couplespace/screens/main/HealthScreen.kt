package com.newton.couplespace.screens.main

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.background
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.newton.couplespace.models.HealthLog
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(navController: androidx.navigation.NavController) {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var partnerUserId by remember { mutableStateOf<String?>(null) }
    var healthLogs by remember { mutableStateOf<List<HealthLog>>(emptyList()) }
    var isMyHealth by remember { mutableStateOf(true) }
    var showAddMealDialog by remember { mutableStateOf(false) }
    var selectedTimeRange by remember { mutableStateOf(TimeRange.DAILY) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Get partner ID
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            FirebaseFirestore.getInstance().collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener { document ->
                    partnerUserId = document.getString("partnerId")
                    loadHealthLogs(currentUserId, isMyHealth, partnerUserId, selectedTimeRange) { logs ->
                        healthLogs = logs
                        isLoading = false
                    }
                }
        }
    }
    
    // Load health logs when user toggle or time range changes
    LaunchedEffect(isMyHealth, selectedTimeRange) {
        isLoading = true
        loadHealthLogs(currentUserId, isMyHealth, partnerUserId, selectedTimeRange) { logs ->
            healthLogs = logs
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Tracking") }
            )
        },
        floatingActionButton = {
            if (isMyHealth) {
                FloatingActionButton(
                    onClick = { showAddMealDialog = true }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Meal")
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // User Toggle
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SegmentedButton(
                    selected = isMyHealth,
                    onClick = { isMyHealth = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("My Health")
                }
                SegmentedButton(
                    selected = !isMyHealth,
                    onClick = { isMyHealth = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Partner's Health")
                }
            }
            
            // Time Range Selection
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                TimeRange.values().forEachIndexed { index, range ->
                    SegmentedButton(
                        selected = selectedTimeRange == range,
                        onClick = { selectedTimeRange = range },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = TimeRange.values().size
                        )
                    ) {
                        Text(range.displayName)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Calorie Summary Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Calorie Summary",
                        style = MaterialTheme.typography.titleMedium
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    if (isLoading) {
                        Text(
                            text = "Loading data...",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else if (healthLogs.isEmpty()) {
                        Text(
                            text = "No meal data available for this period",
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        // Calorie Stats
                        val totalCalories = healthLogs.sumOf { it.calories }
                        val avgCalories = if (healthLogs.isNotEmpty()) {
                            totalCalories / when (selectedTimeRange) {
                                TimeRange.DAILY -> 1
                                TimeRange.WEEKLY -> 7
                                TimeRange.MONTHLY -> 30
                                TimeRange.CUSTOM -> 1 // Would be calculated based on custom range
                            }
                        } else 0
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            CalorieStat(
                                label = "Total",
                                value = totalCalories
                            )
                            
                            CalorieStat(
                                label = "Average",
                                value = avgCalories
                            )
                            
                            CalorieStat(
                                label = "Goal",
                                value = 2000
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        // Simple Graph
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                        ) {
                            SimpleCalorieGraph(healthLogs)
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Meal Logs
            Text(
                text = "Meal Logs",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (isLoading) {
                Text(
                    text = "Loading meals...",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            } else if (healthLogs.isEmpty()) {
                Text(
                    text = if (isMyHealth) "No meals logged yet" else "Your partner hasn't logged any meals yet",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    textAlign = TextAlign.Center
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(healthLogs.sortedByDescending { it.time }) { log ->
                        MealLogItem(log)
                    }
                }
            }
        }
    }
    
    // Add Meal Dialog
    if (showAddMealDialog) {
        AddMealDialog(
            onDismiss = { showAddMealDialog = false },
            onMealAdded = { log ->
                // Add meal to Firestore
                val newLog = log.copy(
                    userId = currentUserId,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                
                FirebaseFirestore.getInstance().collection("healthLogs")
                    .add(newLog)
                    .addOnSuccessListener {
                        // Refresh health logs
                        loadHealthLogs(currentUserId, isMyHealth, partnerUserId, selectedTimeRange) { logs ->
                            healthLogs = logs
                        }
                    }
                
                showAddMealDialog = false
            }
        )
    }
}

@Composable
fun CalorieStat(label: String, value: Int) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SimpleCalorieGraph(logs: List<HealthLog>) {
    if (logs.isEmpty()) return
    
    // Group logs by day
    val caloriesByDay = logs.groupBy {
        SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(it.time)
    }.mapValues { entry ->
        entry.value.sumOf { it.calories }
    }
    
    // Sort by date
    val sortedEntries = caloriesByDay.entries.sortedBy { it.key }
    
    Column(modifier = Modifier
        .fillMaxWidth()
        .height(300.dp)
        .padding(16.dp)) {
        
        Text(
            text = "Calorie Intake by Day",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Display data in a row of columns
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            sortedEntries.take(7).forEach { entry ->
                val date = SimpleDateFormat("MM/dd", Locale.getDefault())
                    .format(SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(entry.key) ?: Date())
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Calculate height based on value
                    val maxValue = sortedEntries.maxByOrNull { it.value }?.value ?: 0.0
                    val maxCalories = (maxValue.toInt() / 500 + 1) * 500
                    val heightPercentage = if (maxCalories > 0) entry.value.toFloat() / maxCalories.toFloat() else 0f
                    
                    Text(
                        text = entry.value.toInt().toString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    // Bar
                    Box(
                        modifier = Modifier
                            .width(30.dp)
                            .height((150.dp * heightPercentage).coerceAtLeast(5.dp))
                            .background(MaterialTheme.colorScheme.primary)
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = date,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MealLogItem(log: HealthLog) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Meal icon
            Icon(
                imageVector = Icons.Default.RestaurantMenu,
                contentDescription = "Meal",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(end = 16.dp)
            )
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = log.foodName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "Quantity: ${log.quantity}",
                    style = MaterialTheme.typography.bodyMedium
                )
                
                if (log.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = log.notes,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${log.calories} cal",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = SimpleDateFormat("h:mm a", Locale.getDefault()).format(log.time),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMealDialog(
    onDismiss: () -> Unit,
    onMealAdded: (HealthLog) -> Unit
) {
    var foodName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(Date()) }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.time
    )
    
    val timePickerState = rememberTimePickerState(
        initialHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY),
        initialMinute = Calendar.getInstance().get(Calendar.MINUTE)
    )
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Log a Meal",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                
                // Food Name Field
                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = { Text("Food Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Quantity Field
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("Quantity") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Calories Field
                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it },
                    label = { Text("Calories") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Time Field
                OutlinedTextField(
                    value = SimpleDateFormat("EEE, MMM d, yyyy h:mm a", Locale.getDefault()).format(selectedDate),
                    onValueChange = { },
                    label = { Text("Time") },
                    modifier = Modifier.fillMaxWidth(),
                    readOnly = true,
                    trailingIcon = {
                        IconButton(onClick = { showDatePicker = true }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date and Time")
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Buttons
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
                            if (foodName.isNotBlank() && quantity.isNotBlank() && calories.isNotBlank()) {
                                onMealAdded(
                                    HealthLog(
                                        foodName = foodName,
                                        quantity = quantity,
                                        calories = calories.toIntOrNull() ?: 0,
                                        time = selectedDate,
                                        notes = notes
                                    )
                                )
                            }
                        },
                        enabled = foodName.isNotBlank() && quantity.isNotBlank() && calories.isNotBlank()
                    ) {
                        Text("Add Meal")
                    }
                }
            }
        }
    }
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            val calendar = Calendar.getInstance()
                            calendar.timeInMillis = it
                            calendar.set(Calendar.HOUR_OF_DAY, Calendar.getInstance().get(Calendar.HOUR_OF_DAY))
                            calendar.set(Calendar.MINUTE, Calendar.getInstance().get(Calendar.MINUTE))
                            selectedDate = calendar.time
                        }
                        showDatePicker = false
                        showTimePicker = true
                    }
                ) {
                    Text("Next")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showDatePicker = false }
                ) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    if (showTimePicker) {
        Dialog(onDismissRequest = { showTimePicker = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Select Time",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            timeSelectorSelectedContainerColor = MaterialTheme.colorScheme.primary,
                            timeSelectorSelectedContentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = { showTimePicker = false }
                        ) {
                            Text("Cancel")
                        }
                        
                        Spacer(modifier = Modifier.width(8.dp))
                        
                        Button(
                            onClick = {
                                val calendar = Calendar.getInstance()
                                calendar.time = selectedDate
                                calendar.set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                                calendar.set(Calendar.MINUTE, timePickerState.minute)
                                selectedDate = calendar.time
                                showTimePicker = false
                            }
                        ) {
                            Text("OK")
                        }
                    }
                }
            }
        }
    }
}

enum class TimeRange(val displayName: String) {
    DAILY("Daily"),
    WEEKLY("Weekly"),
    MONTHLY("Monthly"),
    CUSTOM("Custom")
}

private fun loadHealthLogs(
    currentUserId: String,
    isMyHealth: Boolean,
    partnerUserId: String?,
    timeRange: TimeRange,
    onLogsLoaded: (List<HealthLog>) -> Unit
) {
    val userId = if (isMyHealth) currentUserId else partnerUserId
    
    if (userId.isNullOrBlank()) {
        onLogsLoaded(emptyList())
        return
    }
    
    // Calculate date range based on selected time range
    val calendar = Calendar.getInstance()
    val endDate = calendar.time
    
    calendar.add(
        when (timeRange) {
            TimeRange.DAILY -> Calendar.DAY_OF_MONTH
            TimeRange.WEEKLY -> Calendar.WEEK_OF_YEAR
            TimeRange.MONTHLY -> Calendar.MONTH
            TimeRange.CUSTOM -> Calendar.DAY_OF_MONTH // Would be customized
        },
        -1
    )
    val startDate = calendar.time
    
    FirebaseFirestore.getInstance().collection("healthLogs")
        .whereEqualTo("userId", userId)
        .whereGreaterThanOrEqualTo("time", startDate)
        .whereLessThanOrEqualTo("time", endDate)
        .get()
        .addOnSuccessListener { documents ->
            val logs = documents.mapNotNull { doc ->
                doc.toObject(HealthLog::class.java)
            }
            onLogsLoaded(logs)
        }
        .addOnFailureListener {
            onLogsLoaded(emptyList())
        }
}
