package com.newton.couplespace.screens.main.timeline

import android.content.Context
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.newton.couplespace.models.EntryType
import com.newton.couplespace.models.TimelineEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTimelineEntryDialog(
    onDismiss: () -> Unit,
    onAddEntry: (TimelineEntry) -> Unit,
    isMyCalendar: Boolean
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(EntryType.EVENT) }
    var showDatePicker by remember { mutableStateOf(false) }
    
    // Get access to SharedPreferences for user ID
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) }
    
    // Check if we already have a stored user ID
    val userId = remember { 
        val savedUserId = sharedPrefs.getString("user_id", "") ?: ""
        if (savedUserId.isBlank()) {
            // If no saved ID, try to get from Firebase Auth
            val firebaseId = FirebaseAuth.getInstance().currentUser?.uid
            if (firebaseId != null) {
                // Save it for future use
                sharedPrefs.edit().putString("user_id", firebaseId).apply()
                mutableStateOf(firebaseId)
            } else {
                // For testing, generate a consistent test user ID
                val testId = "test_user_${System.currentTimeMillis()}"
                sharedPrefs.edit().putString("user_id", testId).apply()
                mutableStateOf(testId)
            }
        } else {
            mutableStateOf(savedUserId)
        }
    }
    
    Log.d("AddTimelineEntryDialog", "Dialog opened for ${if (isMyCalendar) "my" else "partner's"} calendar with user ID: ${userId.value}")
    
    val datePickerState = rememberDatePickerState()
    val selectedDate = remember { mutableStateOf(Calendar.getInstance().timeInMillis) }
    
    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        selectedDate.value = it
                    }
                    showDatePicker = false
                }) {
                    Text("Confirm")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "Add New Entry",
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.padding(bottom = 16.dp)
        )
        
        // Entry Type Selection
        var isDropdownExpanded by remember { mutableStateOf(false) }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            ExposedDropdownMenuBox(
                expanded = isDropdownExpanded,
                onExpandedChange = { isDropdownExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = selectedType.name.lowercase().replaceFirstChar { it.uppercase() },
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Entry Type") },
                    trailingIcon = { 
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded) 
                    },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth()
                )
                
                ExposedDropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    EntryType.values().forEach { type ->
                        DropdownMenuItem(
                            text = { 
                                Text(
                                    type.name.lowercase().replaceFirstChar { it.uppercase() },
                                    modifier = Modifier.fillMaxWidth()
                                ) 
                            },
                            onClick = {
                                selectedType = type
                                isDropdownExpanded = false
                            },
                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                        )
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Title Field
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Description Field
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Date Selection
        OutlinedTextField(
            value = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(Date(selectedDate.value)),
            onValueChange = {},
            readOnly = true,
            label = { Text("Date") },
            trailingIcon = {
                IconButton(onClick = { showDatePicker = true }) {
                    Icon(Icons.Default.CalendarMonth, contentDescription = "Select Date")
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
            
            TextButton(
                onClick = {
                    if (title.isNotBlank()) {
                        
                        val newEntry = TimelineEntry(
                            id = "", // Will be set by Firestore
                            userId = userId.value,
                            title = title,
                            description = description,
                            date = Date(selectedDate.value),
                            type = selectedType,
                            completed = if (selectedType == EntryType.REMINDER) false else null
                        )
                        
                        Log.d("AddTimelineEntryDialog", "Creating timeline entry for ${if (isMyCalendar) "my" else "partner's"} calendar with userId: ${userId.value}")
                        onAddEntry(newEntry)
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Text("Add")
            }
        }
    }
}

// Extension function to capitalize first letter
fun String.capitalize(): String {
    return if (isNotEmpty()) {
        this[0].uppercase() + substring(1)
    } else {
        this
    }
}
