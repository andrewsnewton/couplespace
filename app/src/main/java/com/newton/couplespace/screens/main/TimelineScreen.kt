package com.newton.couplespace.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import com.google.firebase.auth.FirebaseAuth
import com.newton.couplespace.models.TimelineEntry
import com.newton.couplespace.navigation.MainScreenScaffold
import com.newton.couplespace.screens.main.timeline.AddTimelineEntryDialog
import com.newton.couplespace.screens.main.timeline.CalendarToggle
import com.newton.couplespace.screens.main.timeline.CalendarView
import com.newton.couplespace.screens.main.timeline.TimelineEntryItem
import com.newton.couplespace.screens.main.timeline.TimelineRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen(navController: NavController = rememberNavController()) {
    // Get shared preferences for user data
    val context = androidx.compose.ui.platform.LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_prefs", android.content.Context.MODE_PRIVATE) }
    
    // Get or generate a consistent user ID
    val currentUserId = remember {
        var storedId = sharedPrefs.getString("user_id", null) ?: ""
        
        // If no stored user ID, generate one and save it
        if (storedId.isBlank()) {
            // Try to get from Firebase Auth first
            storedId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
            
            // If still blank, generate a test user ID
            if (storedId.isBlank()) {
                storedId = "test_user_${System.currentTimeMillis()}"
                // Save it for future use
                sharedPrefs.edit().putString("user_id", storedId).apply()
            }
        }
        
        println("Using user ID for timeline: $storedId")
        storedId
    }
    var partnerUserId by remember { mutableStateOf<String?>(null) }
    var timelineEntries by remember { mutableStateOf<List<TimelineEntry>>(emptyList()) }
    var isMyCalendar by remember { mutableStateOf(true) }
    var showAddEntryDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedDate by remember { mutableStateOf(Date()) }
    var isCalendarExpanded by remember { mutableStateOf(false) }
    
    // Create repository instance with context
    val repository = remember { TimelineRepository(context) }
    
    // Check if running in emulator for fallback behavior
    val isEmulator = android.os.Build.PRODUCT.contains("sdk") || 
                    android.os.Build.PRODUCT.contains("emulator") ||
                    android.os.Build.FINGERPRINT.contains("generic")
    
    // We'll get the partner ID from Firebase via the TimelineRepository
    // This is a placeholder for UI state until we get the real partner ID
    LaunchedEffect(Unit) {
        android.util.Log.d("TimelineScreen", "Initializing TimelineScreen, will get partner ID from Firebase")
    }
    
    // Initial load of timeline entries
    LaunchedEffect(currentUserId, isMyCalendar) {
        if (currentUserId.isNotBlank()) {
            android.util.Log.d("TimelineScreen", "Loading initial timeline entries for ${if (isMyCalendar) "my" else "partner's"} calendar")
            repository.loadTimelineEntries(currentUserId, isMyCalendar) { entries ->
                timelineEntries = entries
                isLoading = false
            }
        } else {
            android.util.Log.e("TimelineScreen", "Current user ID is blank")
            isLoading = false
        }
    }
    
    // Load timeline entries when calendar toggle changes or date changes
    LaunchedEffect(isMyCalendar, selectedDate) {
        isLoading = true
        android.util.Log.d("TimelineScreen", "Loading timeline entries after calendar toggle/date change")
        repository.loadTimelineEntries(currentUserId, isMyCalendar) { entries ->
            // Filter entries by selected date if needed
            val calendar = Calendar.getInstance().apply { time = selectedDate }
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)
            
            val filteredEntries = entries.filter { entry ->
                val entryCalendar = Calendar.getInstance().apply { time = entry.date }
                val entryYear = entryCalendar.get(Calendar.YEAR)
                val entryMonth = entryCalendar.get(Calendar.MONTH)
                val entryDay = entryCalendar.get(Calendar.DAY_OF_MONTH)
                
                // Match entries for the selected date
                entryYear == year && entryMonth == month && entryDay == day
            }
            
            timelineEntries = filteredEntries
            isLoading = false
        }
    }
    
    // This is the key change - using MainScreenScaffold to enable bottom navigation
    MainScreenScaffold(
        navController = navController
    ) { innerPadding ->
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Timeline") }
                )
            },
            floatingActionButton = {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Nudge Button
                    FloatingActionButton(
                        onClick = {
                            repository.sendNudgeToPartner(currentUserId) { success ->
                                if (success) {
                                    // Show a toast or snackbar to confirm the nudge was sent
                                    android.widget.Toast.makeText(
                                        context,
                                        "Nudge sent to your partner!",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    android.widget.Toast.makeText(
                                        context,
                                        "Failed to send nudge. Please try again.",
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier
                            .padding(bottom = 16.dp)
                            .size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Nudge Partner",
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                    
                    // Add Entry Button
                    FloatingActionButton(
                        onClick = { showAddEntryDialog = true },
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier
                            .padding(bottom = 56.dp) // Add padding to position above bottom nav
                            .size(56.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Entry",
                            modifier = Modifier.padding(8.dp) // Make the icon larger
                        )
                    }
                }
            }
        ) { scaffoldPadding ->
            if (showAddEntryDialog) {
                Dialog(
                    onDismissRequest = { showAddEntryDialog = false },
                    content = {
                        androidx.compose.material3.Surface(
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                            color = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                            tonalElevation = 8.dp,
                            shadowElevation = 8.dp,
                            modifier = Modifier.fillMaxWidth(0.95f)
                        ) {
                            // Determine which user ID to use based on which calendar is active
                            val entryUserId = if (isMyCalendar) currentUserId else partnerUserId ?: ""
                            
                            AddTimelineEntryDialog(
                                onDismiss = { showAddEntryDialog = false },
                                onAddEntry = { newEntry ->
                                    android.util.Log.d("TimelineScreen", "Adding entry to ${if (isMyCalendar) "MY" else "PARTNER'S"} calendar")
                                    
                                    repository.saveTimelineEntry(
                                        newEntry = newEntry,
                                        currentUserId = currentUserId,
                                        isMyCalendar = isMyCalendar,
                                        isEmulator = isEmulator
                                    ) { success ->
                                        if (success) {
                                            // Refresh the timeline entries
                                            repository.loadTimelineEntries(currentUserId, isMyCalendar) { entries ->
                                                timelineEntries = entries
                                            }
                                        }
                                        showAddEntryDialog = false
                                    }
                                },
                                isMyCalendar = isMyCalendar
                            )
                        }
                    }
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(scaffoldPadding)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    // Calendar Toggle and Enhanced Timeline Button
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CalendarToggle(
                            isMyCalendar = isMyCalendar,
                            onToggleCalendar = { isMyCalendar = it }
                        )
                        
                        // Button to navigate to enhanced timeline
                        androidx.compose.material3.Button(
                            onClick = { 
                                // Navigate to the enhanced timeline screen
                                navController.navigate("enhanced_timeline") 
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text("Try Enhanced Timeline")
                        }
                    }
                    
                    // Calendar header with month/year and expand/collapse button
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(selectedDate),
                                style = MaterialTheme.typography.titleMedium
                            )
                            
                            IconButton(onClick = { isCalendarExpanded = !isCalendarExpanded }) {
                                Icon(
                                    imageVector = if (isCalendarExpanded) 
                                        Icons.Default.KeyboardArrowUp 
                                    else 
                                        Icons.Default.CalendarMonth,
                                    contentDescription = if (isCalendarExpanded) "Collapse Calendar" else "Expand Calendar"
                                )
                            }
                        }
                        
                        // Show calendar only when expanded
                        AnimatedVisibility(
                            visible = isCalendarExpanded,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            CalendarView(
                                selectedDate = selectedDate,
                                onDateSelected = { newDate ->
                                    selectedDate = newDate
                                    // Filter entries by the selected date if needed
                                    println("Selected date: ${SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(newDate)}")
                                }
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Timeline Entries
                    if (isLoading) {
                        Text(
                            text = "Loading entries...",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    } else if (timelineEntries.isEmpty()) {
                        Text(
                            text = if (isMyCalendar) "No entries in your calendar yet" else "No entries in your partner's calendar yet",
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            textAlign = TextAlign.Center
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(timelineEntries) { entry ->
                                TimelineEntryItem(entry)
                            }
                        }
                    }
                }
            }
        }
    }
}