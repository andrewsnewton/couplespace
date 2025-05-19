package com.newton.couplespace.screens.main.health

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.newton.couplespace.models.HealthLog
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AddMealDialog(
    onDismiss: () -> Unit,
    onConfirm: (HealthLog) -> Unit,
    initialLog: HealthLog? = null,
    onDelete: (() -> Unit)? = null
) {
    var foodName by remember { mutableStateOf(initialLog?.foodName ?: "") }
    var quantity by remember { mutableStateOf(initialLog?.quantity?.toString() ?: "1.0") }
    var calories by remember { mutableStateOf(initialLog?.calories?.toString() ?: "") }
    var notes by remember { mutableStateOf(initialLog?.notes ?: "") }
    var time by remember { 
        mutableStateOf(
            initialLog?.time ?: Date()
        ) 
    }

    val dateTimeFormat = remember { SimpleDateFormat("MMM d, yyyy hh:mm a", Locale.getDefault()) }
    val dateTimeString = remember(time) { dateTimeFormat.format(time) }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = if (initialLog == null) "Add Meal" else "Edit Meal",
                    style = MaterialTheme.typography.headlineSmall
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = foodName,
                    onValueChange = { foodName = it },
                    label = { Text("Food Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = quantity,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) quantity = it },
                        label = { Text("Quantity") },
                        modifier = Modifier.weight(1f)
                    )
                    
                    OutlinedTextField(
                        value = calories,
                        onValueChange = { if (it.isEmpty() || it.matches(Regex("^\\d*$"))) calories = it },
                        label = { Text("Calories") },
                        modifier = Modifier.weight(1f)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text("Time: $dateTimeString")
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    if (onDelete != null) {
                        TextButton(
                            onClick = { 
                                onDelete()
                                onDismiss() 
                            },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Text("Delete")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            val log = HealthLog(
                                id = initialLog?.id ?: "",
                                userId = "", // Will be set in ViewModel
                                foodName = foodName,
                                quantity = quantity.toDoubleOrNull() ?: 1.0,
                                calories = calories.toIntOrNull() ?: 0,
                                time = time,
                                notes = notes
                            )
                            onConfirm(log)
                            onDismiss()
                        },
                        enabled = foodName.isNotBlank() && (quantity.isNotBlank() || calories.isNotBlank())
                    ) {
                        Text(if (initialLog == null) "Add" else "Save")
                    }
                }
            }
        }
    }
}
