package com.newton.couplespace.screens.main

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.newton.couplespace.models.EntryType
import com.newton.couplespace.models.TimelineEntry
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineScreen() {
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var partnerUserId by remember { mutableStateOf<String?>(null) }
    var timelineEntries by remember { mutableStateOf<List<TimelineEntry>>(emptyList()) }
    var isMyCalendar by remember { mutableStateOf(true) }
    var showAddEntryDialog by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(Date()) }
    var isLoading by remember { mutableStateOf(true) }
    
    // Get partner ID
    LaunchedEffect(currentUserId) {
        if (currentUserId.isNotBlank()) {
            FirebaseFirestore.getInstance().collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener { document ->
                    partnerUserId = document.getString("partnerId")
                    loadTimelineEntries(currentUserId, isMyCalendar, partnerUserId) { entries ->
                        timelineEntries = entries
                        isLoading = false
                    }
                }
        }
    }
    
    // Load timeline entries when calendar toggle changes
    LaunchedEffect(isMyCalendar) {
        isLoading = true
        loadTimelineEntries(currentUserId, isMyCalendar, partnerUserId) { entries ->
            timelineEntries = entries
            isLoading = false
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Timeline") }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddEntryDialog = true }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Entry")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Calendar Toggle
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                SegmentedButton(
                    selected = isMyCalendar,
                    onClick = { isMyCalendar = true },
                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                ) {
                    Text("My Calendar")
                }
                SegmentedButton(
                    selected = !isMyCalendar,
                    onClick = { isMyCalendar = false },
                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                ) {
                    Text("Partner's Calendar")
                }
            }
            
            // Calendar View (simplified for now)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(Date()),
                            style = MaterialTheme.typography.titleMedium
                        )
                        
                        IconButton(onClick = { /* Open full calendar */ }) {
                            Icon(Icons.Default.CalendarMonth, contentDescription = "Open Calendar")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    // Simple calendar days representation
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        // Just a placeholder for a real calendar implementation
                        for (day in 1..7) {
                            val hasEvent = day % 3 == 0
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = SimpleDateFormat("E", Locale.getDefault())
                                        .format(Calendar.getInstance().apply {
                                            set(Calendar.DAY_OF_WEEK, day)
                                        }.time),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                
                                Spacer(modifier = Modifier.height(4.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (day == Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
                                                MaterialTheme.colorScheme.primary
                                            else Color.Transparent
                                        )
                                        .clickable { /* Select date */ },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = day.toString(),
                                        color = if (day == Calendar.getInstance().get(Calendar.DAY_OF_WEEK))
                                            MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                                
                                if (hasEvent) {
                                    Box(
                                        modifier = Modifier
                                            .padding(top = 4.dp)
                                            .size(6.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primary)
                                    )
                                }
                            }
                        }
                    }
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
    
    // Add Entry Dialog
    if (showAddEntryDialog) {
        AddTimelineEntryDialog(
            onDismiss = { showAddEntryDialog = false },
            onEntryAdded = { entry ->
                // Add entry to Firestore
                val newEntry = entry.copy(
                    userId = currentUserId,
                    createdAt = Date(),
                    updatedAt = Date()
                )
                
                FirebaseFirestore.getInstance().collection("timelineEntries")
                    .add(newEntry)
                    .addOnSuccessListener {
                        // Refresh timeline entries
                        loadTimelineEntries(currentUserId, isMyCalendar, partnerUserId) { entries ->
                            timelineEntries = entries
                        }
                    }
                
                showAddEntryDialog = false
            }
        )
    }
}

@Composable
fun TimelineEntryItem(entry: TimelineEntry) {
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
            // Icon based on entry type
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(
                        when (entry.type) {
                            EntryType.EVENT -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            EntryType.MEMORY -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.2f)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (entry.type) {
                        EntryType.EVENT -> Icons.Default.Event
                        EntryType.MEMORY -> Icons.Default.PhotoCamera
                    },
                    contentDescription = entry.type.name,
                    tint = when (entry.type) {
                        EntryType.EVENT -> MaterialTheme.colorScheme.primary
                        EntryType.MEMORY -> MaterialTheme.colorScheme.secondary
                    }
                )
            }
            
            Spacer(modifier = Modifier.padding(horizontal = 8.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(entry.date),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                
                if (entry.description.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = entry.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            
            if (entry.nudgeEnabled) {
                Icon(
                    imageVector = Icons.Default.Notifications,
                    contentDescription = "Nudge Enabled",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimelineEntryDialog(
    onDismiss: () -> Unit,
    onEntryAdded: (TimelineEntry) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var entryType by remember { mutableStateOf(EntryType.EVENT) }
    var nudgeEnabled by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(Date()) }
    
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.time
    )
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Add Timeline Entry",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Title Field
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Description Field
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Date Picker
        OutlinedTextField(
            value = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault()).format(selectedDate),
            onValueChange = { },
            label = { Text("Date") },
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                }
            }
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Entry Type Dropdown
        var expanded by remember { mutableStateOf(false) }
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = entryType.name.lowercase().replaceFirstChar { it.uppercase() },
                onValueChange = { },
                readOnly = true,
                label = { Text("Type") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                modifier = Modifier
                    .fillMaxWidth()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                EntryType.values().forEach { type ->
                    DropdownMenuItem(
                        text = { Text(type.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        onClick = {
                            entryType = type
                            expanded = false
                        }
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Nudge Partner Checkbox
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = nudgeEnabled,
                onCheckedChange = { nudgeEnabled = it }
            )
            
            Text(
                text = "Nudge Partner",
                modifier = Modifier.clickable { nudgeEnabled = !nudgeEnabled }
            )
        }
        
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
            
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        onEntryAdded(
                            TimelineEntry(
                                title = title,
                                description = description,
                                date = selectedDate,
                                type = entryType,
                                nudgeEnabled = nudgeEnabled
                            )
                        )
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add Entry")
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
                            selectedDate = Date(it)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
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
}

private fun loadTimelineEntries(
    currentUserId: String,
    isMyCalendar: Boolean,
    partnerUserId: String?,
    onEntriesLoaded: (List<TimelineEntry>) -> Unit
) {
    val userId = if (isMyCalendar) currentUserId else partnerUserId
    
    if (userId.isNullOrBlank()) {
        onEntriesLoaded(emptyList())
        return
    }
    
    FirebaseFirestore.getInstance().collection("timelineEntries")
        .whereEqualTo("userId", userId)
        .get()
        .addOnSuccessListener { documents ->
            val entries = documents.mapNotNull { doc ->
                doc.toObject(TimelineEntry::class.java)
            }
            onEntriesLoaded(entries)
        }
        .addOnFailureListener {
            onEntriesLoaded(emptyList())
        }
}
