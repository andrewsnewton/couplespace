package com.newton.couplespace.screens.main.health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    onBackClick: () -> Unit = {}
) {
    val viewModel: HealthViewModel = viewModel()
    val healthLogs by viewModel.healthLogs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val selectedDate by viewModel.selectedDate
    val showAddDialog by viewModel.showAddDialog
    val editingLog by viewModel.editingLog
    
    val dateFormat = remember { SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()) }
    
    // Load logs when the screen is first displayed
    LaunchedEffect(Unit) {
        viewModel.loadHealthLogs()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Health Tracker") 
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showAddDialog(true) },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Meal")
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Date selector
            DateSelector(
                selectedDate = selectedDate,
                onPreviousDay = {
                    val newDate = (selectedDate.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_MONTH, -1)
                    }
                    viewModel.setSelectedDate(newDate)
                },
                onNextDay = {
                    val newDate = (selectedDate.clone() as Calendar).apply {
                        add(Calendar.DAY_OF_MONTH, 1)
                    }
                    // Don't allow selecting future dates
                    val today = Calendar.getInstance()
                    if (newDate.before(today) || isSameDay(newDate, today)) {
                        viewModel.setSelectedDate(newDate)
                    }
                },
                onCalendarClick = {
                    // TODO: Implement date picker dialog
                }
            )
            
            // Stats summary
            val totalCalories = viewModel.getTotalCalories()
            val calorieGoal = 2000 // Default goal, could be customizable in settings
            val progress = (totalCalories.toFloat() / calorieGoal).coerceAtMost(1f)
            
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Daily Calories",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$totalCalories",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        
                        Text(
                            text = "/ $calorieGoal cal",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(CircleShape),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }
            }
            
            // Meal logs
            Text(
                text = "Meal Logs",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            )
            
            if (isLoading && healthLogs.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                MealLogList(
                    logs = healthLogs,
                    onEditClick = { log ->
                        viewModel.showAddDialog(true, log)
                    },
                    onDeleteClick = { log ->
                        viewModel.deleteHealthLog(log)
                    }
                )
            }
        }
    }
    
    // Add/Edit Meal Dialog
    if (showAddDialog) {
        AddMealDialog(
            onDismiss = { viewModel.showAddDialog(false) },
            onConfirm = { log ->
                // Move the coroutine to the ViewModel
                viewModel.saveHealthLog(log) { success ->
                    if (success) {
                        viewModel.showAddDialog(false)
                    }
                }
            },
            initialLog = editingLog,
            onDelete = editingLog?.let { log ->
                {
                    viewModel.deleteHealthLog(log) {
                        viewModel.showAddDialog(false)
                    }
                }
            }
        )
    }
}

@Composable
private fun DateSelector(
    selectedDate: Calendar,
    onPreviousDay: () -> Unit,
    onNextDay: () -> Unit,
    onCalendarClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("EEEE, MMM d, yyyy", Locale.getDefault()) }
    val today = Calendar.getInstance()
    val isToday = isSameDay(selectedDate, today)
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(
            onClick = onPreviousDay,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous day"
            )
        }
        
        Text(
            text = dateFormat.format(selectedDate.time),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.weight(1f)
        )
        
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isToday) {
                IconButton(
                    onClick = onNextDay,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Next day"
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(48.dp))
            }
            
            IconButton(
                onClick = onCalendarClick,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.DateRange,
                    contentDescription = "Select date"
                )
            }
        }
    }
}

private fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
            cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}
