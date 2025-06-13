package com.newton.couplespace.screens.health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.livedata.observeAsState
import kotlinx.coroutines.flow.StateFlow
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
// import androidx.hilt.navigation.compose.hiltViewModel
import com.newton.couplespace.screens.health.components.common.ActivityTimeline
import com.newton.couplespace.screens.health.components.common.DateSelector
import com.newton.couplespace.screens.health.components.common.HealthSummaryCard
import com.newton.couplespace.screens.health.components.couple.PartnerHealthSummaryCard
import com.newton.couplespace.screens.health.components.couple.PartnerHighlightsCard
import com.newton.couplespace.screens.health.components.couple.SharedGoalsCard
import com.newton.couplespace.screens.health.components.healthconnect.HealthConnectPermissionCard
import com.newton.couplespace.screens.health.components.nutrition.MealLoggerCard
import com.newton.couplespace.screens.health.components.nutrition.MealEntryDialog
import com.newton.couplespace.screens.health.components.common.HealthDatePickerDialog
import com.newton.couplespace.screens.health.components.nutrition.WaterTrackerCard
import com.newton.couplespace.screens.health.data.models.*
import com.newton.couplespace.screens.health.viewmodel.CoupleHealthViewModel
import com.newton.couplespace.screens.health.viewmodel.HealthViewModel
import com.newton.couplespace.screens.health.viewmodel.NutritionViewModel
import java.time.format.DateTimeFormatter

/**
 * Main Health Screen that brings together all health-related components
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    healthViewModel: HealthViewModel,
    nutritionViewModel: NutritionViewModel,
    coupleHealthViewModel: CoupleHealthViewModel
) {
    val selectedDate by healthViewModel.selectedDate.collectAsState()
    val isLoading by healthViewModel.isLoading.collectAsState()
    val partnerHighlights by coupleHealthViewModel.partnerHighlights.collectAsState()
    val sharedGoals by coupleHealthViewModel.sharedGoals.collectAsState()
    val pendingReminders by coupleHealthViewModel.pendingReminders.collectAsState()
    val healthMetrics by healthViewModel.healthMetrics.collectAsState()
    val partnerHealthMetrics by healthViewModel.partnerHealthMetrics.collectAsState()
    val hasPartnerData by healthViewModel.hasPartnerData.collectAsState()
    
    // Track whether to show the add meal dialog
    var showAddMealDialog by remember { mutableStateOf(false) }
    
    // Track whether to show the date picker dialog
    var showDatePickerDialog by remember { mutableStateOf(false) }
    
    // Date formatter
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy") }
    
    // Get the current context
    val context = LocalContext.current
    
    // Health Connect permission request intent
    val permissionRequestIntent by healthViewModel.permissionRequestIntent.observeAsState()
    
    // Activity result launcher for Health Connect permissions
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        android.util.Log.d("HealthScreen", "Permission result received: ${result.resultCode}")
        // Check permissions again after the user returns from the permission screen
        healthViewModel.refreshHealthConnectPermissions()
        healthViewModel.clearPermissionRequestIntent()
        
        // Show a Toast message with the result
        val resultMessage = if (result.resultCode == -1) "Health Connect permissions granted" else "Health Connect permissions not granted"
        Toast.makeText(context, resultMessage, Toast.LENGTH_LONG).show()
    }
    
    // Get Health Connect availability state
    val isHealthConnectAvailable by healthViewModel.isHealthConnectAvailable.collectAsState()
    
    // State for showing snackbar messages
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Track permission request state
    var isRequestingPermissions by remember { mutableStateOf(false) }
    
    // Launch the permission request intent when it becomes available
    LaunchedEffect(permissionRequestIntent) {
        android.util.Log.d("HealthScreen", "Permission intent changed: $permissionRequestIntent")
        if (isHealthConnectAvailable) {
            permissionRequestIntent?.let { intent ->
                android.util.Log.d("HealthScreen", "Launching permission intent")
                try {
                    // Show a Toast message before launching the intent
                    Toast.makeText(context, "Opening Health Connect permissions...", Toast.LENGTH_SHORT).show()
                    
                    // Set requesting permissions state
                    isRequestingPermissions = true
                    
                    // Launch the permission intent
                    permissionLauncher.launch(intent)
                    
                    // Show a more visible snackbar
                    snackbarHostState.showSnackbar(
                        message = "Requesting Health Connect permissions...",
                        duration = SnackbarDuration.Short,
                        withDismissAction = true
                    )
                } catch (e: Exception) {
                    android.util.Log.e("HealthScreen", "Error launching permission intent", e)
                    e.printStackTrace()
                    healthViewModel.loadMockData()
                    isRequestingPermissions = false
                    
                    // Show a snackbar to inform the user
                    snackbarHostState.showSnackbar(
                        message = "Health Connect app not found. Using mock data instead.",
                        duration = SnackbarDuration.Long,
                        withDismissAction = true
                    )
                }
            }
        } else {
            // If Health Connect is not available, clear the intent and load mock data
            android.util.Log.d("HealthScreen", "Health Connect is not available, using mock data")
            healthViewModel.clearPermissionRequestIntent()
            healthViewModel.loadMockData()
            isRequestingPermissions = false
            
            // Show a snackbar to inform the user
            snackbarHostState.showSnackbar(
                message = "Health Connect is not available on this device. Using mock data.",
                duration = SnackbarDuration.Long,
                withDismissAction = true
            )
        }
    }
    
    // Reset requesting permissions state when permissions change
    LaunchedEffect(healthViewModel.hasHealthConnectPermissions.collectAsState().value) {
        isRequestingPermissions = false
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Health Dashboard") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    // Calendar icon to change date
                    IconButton(onClick = { showDatePickerDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.DateRange,
                            contentDescription = "Select Date"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddMealDialog = true },
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add Meal"
                )
            }
        },
        snackbarHost = { 
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = data.visuals.actionLabel?.let { actionLabel ->
                        { 
                            Button(onClick = { data.performAction() }) {
                                Text(actionLabel)
                            }
                        }
                    },
                    dismissAction = if (data.visuals.withDismissAction) {
                        {
                            IconButton(onClick = { data.dismiss() }) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "Dismiss"
                                )
                            }
                        }
                    } else null
                ) {
                    Text(data.visuals.message)
                }
            }
        }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp)
            ) {
                // Date selector at the top
                DateSelector(
                    selectedDate = selectedDate,
                    onDateChange = { healthViewModel.setSelectedDate(it) },
                    onDateClick = { showDatePickerDialog = true },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Health Connect permission card if needed
                val isHealthConnectAvailable by healthViewModel.isHealthConnectAvailable.collectAsState()
                val hasHealthConnectPermissions by healthViewModel.hasHealthConnectPermissions.collectAsState()
                
                // Only show the Health Connect permission card if permissions are not granted
                if (!hasHealthConnectPermissions) {
                    HealthConnectPermissionCard(
                        isHealthConnectAvailable = isHealthConnectAvailable,
                        hasPermissions = hasHealthConnectPermissions,
                        onConnectClick = { 
                            if (isHealthConnectAvailable) {
                                // Show immediate feedback before the request
                                Toast.makeText(context, "Preparing Health Connect permissions...", Toast.LENGTH_SHORT).show()
                                healthViewModel.requestHealthConnectPermissions()
                            } else {
                                // Open Google Play Store to install Health Connect
                                try {
                                    val playStoreIntent = Intent(Intent.ACTION_VIEW).apply {
                                        data = android.net.Uri.parse("market://details?id=com.google.android.apps.healthdata")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(playStoreIntent)
                                } catch (e: Exception) {
                                    // Fallback to browser if Play Store app is not available
                                    val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                                        data = android.net.Uri.parse("https://play.google.com/store/apps/details?id=com.google.android.apps.healthdata")
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    }
                                    context.startActivity(browserIntent)
                                }
                            }
                        },
                        onLearnMoreClick = {
                            // Open a web page with more information about Health Connect
                            val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse("https://developer.android.com/health-and-fitness/guides/health-connect")
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            context.startActivity(browserIntent)
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Partner highlights card (if available)
                if (partnerHighlights.isNotEmpty()) {
                    PartnerHighlightsCard(
                        highlights = partnerHighlights,
                        onAcknowledge = { highlightId -> coupleHealthViewModel.acknowledgeHighlight(highlightId) }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Health summary card
                HealthSummaryCard(
                    steps = healthMetrics.filter { it.type == HealthMetricType.STEPS }.sumOf { (it as? com.newton.couplespace.screens.health.data.models.StepsMetric)?.count ?: 0 },
                    calories = healthMetrics.filter { it.type == HealthMetricType.CALORIES_BURNED }.sumOf { (it as? com.newton.couplespace.screens.health.data.models.CaloriesBurnedMetric)?.calories ?: 0 },
                    activeMinutes = healthMetrics.filter { it.type == HealthMetricType.ACTIVE_MINUTES }.sumOf { (it as? com.newton.couplespace.screens.health.data.models.ActiveMinutesMetric)?.minutes ?: 0 },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Partner health summary - only show if partner data is available
                if (hasPartnerData) {
                    val partnerSteps = partnerHealthMetrics.filter { it.type == HealthMetricType.STEPS }
                        .sumOf { (it as? StepsMetric)?.count ?: 0 }
                    
                    val partnerCalories = partnerHealthMetrics.filter { it.type == HealthMetricType.CALORIES_BURNED }
                        .sumOf { (it as? CaloriesBurnedMetric)?.calories ?: 0 }
                    
                    val partnerActiveMinutes = partnerHealthMetrics.filter { it.type == HealthMetricType.ACTIVE_MINUTES }
                        .sumOf { (it as? ActiveMinutesMetric)?.minutes ?: 0 }
                    
                    val partnerHeartRate = partnerHealthMetrics.filter { it.type == HealthMetricType.HEART_RATE }
                        .map { (it as? HeartRateMetric)?.beatsPerMinute ?: 0 }
                        .firstOrNull() ?: 0
                    
                    val partnerSleepHours = partnerHealthMetrics.filter { it.type == HealthMetricType.SLEEP }
                        .map { (it as? SleepMetric)?.durationHours ?: 0f }
                        .firstOrNull() ?: 0f
                    
                    PartnerHealthSummaryCard(
                        steps = partnerSteps,
                        calories = partnerCalories,
                        activeMinutes = partnerActiveMinutes,
                        heartRate = partnerHeartRate,
                        sleepHours = partnerSleepHours,
                        onShareClick = {
                            // Share your own health metrics with partner
                            val metricToShare = healthMetrics.firstOrNull()
                            if (metricToShare != null) {
                                healthViewModel.shareHealthMetric(metricToShare)
                                Toast.makeText(context, "Health data shared with partner", Toast.LENGTH_SHORT).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Water tracker
                WaterTrackerCard(
                    totalWaterIntake = nutritionViewModel.totalWaterIntake.collectAsState().value,
                    waterGoal = nutritionViewModel.waterGoal.collectAsState().value,
                    onAddWater = { nutritionViewModel.recordWaterIntake(it) },
                    onUpdateGoal = { nutritionViewModel.updateWaterGoal(it) },
                    onSendReminder = { coupleHealthViewModel.sendHealthReminder(
                        com.newton.couplespace.screens.health.data.models.HealthReminderType.WATER_INTAKE,
                        "Don't forget to stay hydrated today!"
                    ) },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Meal logger
                MealLoggerCard(
                    meals = nutritionViewModel.meals.collectAsState().value,
                    nutritionSummary = nutritionViewModel.nutritionSummary.collectAsState().value,
                    onAddMeal = { showAddMealDialog = true },
                    onEditMeal = { nutritionViewModel.editMeal(it) },
                    onDeleteMeal = { nutritionViewModel.deleteMeal(it) },
                    onShareMeal = { id, isShared -> 
                        nutritionViewModel.updateMealSharedStatus(id, isShared) 
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Shared goals
                if (sharedGoals.isNotEmpty()) {
                    SharedGoalsCard(
                        goals = sharedGoals,
                        onUpdateProgress = { goalId, progress -> 
                            coupleHealthViewModel.updateGoalProgress(goalId, progress)
                        },
                        onCreateGoal = { /* Open goal creation dialog */ },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
                
                // Activity timeline
                ActivityTimeline(
                    healthMetrics = healthMetrics,
                    onMetricClick = { /* Handle metric click */ },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
        
        // Add meal dialog
        if (showAddMealDialog) {
            MealEntryDialog(
                onDismiss = { showAddMealDialog = false },
                onSave = { meal -> 
                    nutritionViewModel.saveMeal(meal)
                    showAddMealDialog = false 
                }
            )
        }
        
        // Date picker dialog
        if (showDatePickerDialog) {
            HealthDatePickerDialog(
                selectedDate = selectedDate,
                onDateSelected = { date ->
                    healthViewModel.setSelectedDate(date)
                },
                onDismiss = { showDatePickerDialog = false }
            )
        }
    }
}
