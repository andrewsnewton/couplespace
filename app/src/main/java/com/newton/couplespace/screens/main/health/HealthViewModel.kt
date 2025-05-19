package com.newton.couplespace.screens.main.health

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.ktx.firestoreSettings
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase as FirebaseKtx
import com.newton.couplespace.models.HealthLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.*

class HealthViewModel : ViewModel() {
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val db: FirebaseFirestore = FirebaseKtx.firestore
    
    private val _healthLogs = MutableStateFlow<List<HealthLog>>(emptyList())
    val healthLogs: StateFlow<List<HealthLog>> = _healthLogs
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading
    
    private val _selectedDate = mutableStateOf(Calendar.getInstance())
    val selectedDate = _selectedDate
    
    private val _showAddDialog = mutableStateOf(false)
    val showAddDialog = _showAddDialog
    
    private val _editingLog = mutableStateOf<HealthLog?>(null)
    val editingLog = _editingLog
    
    init {
        // Configure Firestore settings
        val settings = firestoreSettings {
            isPersistenceEnabled = true
        }
        db.firestoreSettings = settings
        
        loadHealthLogs()
    }
    
    fun loadHealthLogs() {
        Log.d("HealthViewModel", "loadHealthLogs() called")
        val currentUser = auth.currentUser
        
        if (currentUser == null) {
            Log.e("HealthViewModel", "User not authenticated - cannot load logs")
            _healthLogs.value = emptyList()
            _isLoading.value = false
            return
        }
        
        Log.d("HealthViewModel", "Loading logs for user: ${currentUser.uid}")
        _isLoading.value = true
        
        viewModelScope.launch {
            try {
                val calendar = _selectedDate.value
                
                // Set to start of day
                calendar.set(Calendar.HOUR_OF_DAY, 0)
                calendar.set(Calendar.MINUTE, 0)
                calendar.set(Calendar.SECOND, 0)
                calendar.set(Calendar.MILLISECOND, 0)
                val startOfDay = calendar.time
                
                // Set to end of day
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                calendar.add(Calendar.MILLISECOND, -1)
                val endOfDay = calendar.time
                
                Log.d("HealthViewModel", "Date range: $startOfDay to $endOfDay")
                
                Log.d("HealthViewModel", "Building Firestore query...")
                val query = db.collection("healthLogs")
                    .whereEqualTo("userId", currentUser.uid)
                    .whereGreaterThanOrEqualTo("time", startOfDay)
                    .whereLessThanOrEqualTo("time", endOfDay)
                
                Log.d("HealthViewModel", "Executing Firestore query...")
                val result = query.get().await()
                Log.d("HealthViewModel", "Query completed, found ${result.size()} documents")
                
                val logs = result.documents.mapNotNull { doc ->
                    try {
                        val data = doc.data ?: run {
                            Log.w("HealthViewModel", "Document ${doc.id} has no data")
                            return@mapNotNull null
                        }
                        HealthLog.fromMap(data + ("id" to doc.id)).also { log ->
                            Log.d("HealthViewModel", "Successfully parsed log: ${log.id}, ${log.foodName}, ${log.calories} cal")
                        }
                    } catch (e: Exception) {
                        Log.e("HealthViewModel", "Error parsing document ${doc.id}", e)
                        null
                    }
                }
                
                Log.d("HealthViewModel", "Updating UI with ${logs.size} logs")
                _healthLogs.value = logs
                
            } catch (e: Exception) {
                Log.e("HealthViewModel", "Error in loadHealthLogs", e)
                _healthLogs.value = emptyList()
            } finally {
                Log.d("HealthViewModel", "Loading complete, setting isLoading to false")
                _isLoading.value = false
            }
        }
    }
    
    fun saveHealthLog(log: HealthLog, onResult: (Boolean) -> Unit) {
        Log.d("HealthViewModel", "Starting to save health log: $log")
        
        viewModelScope.launch {
            try {
                val currentUser = auth.currentUser
                Log.d("HealthViewModel", "Current user: ${currentUser?.uid ?: "null"}")
                
                if (currentUser == null) {
                    Log.e("HealthViewModel", "User not authenticated - cannot save log")
                    onResult(false)
                    return@launch
                }
                
                val logToSave = log.copy(userId = currentUser.uid)
                Log.d("HealthViewModel", "Prepared log to save: $logToSave")
                
                val success = if (logToSave.id.isNotEmpty()) {
                    Log.d("HealthViewModel", "Updating existing log with ID: ${logToSave.id}")
                    try {
                        db.collection("healthLogs")
                            .document(logToSave.id)
                            .set(logToSave.toMap())
                            .await()
                        Log.d("HealthViewModel", "Successfully updated log with ID: ${logToSave.id}")
                        true
                    } catch (e: Exception) {
                        Log.e("HealthViewModel", "Error updating log ${logToSave.id}", e)
                        false
                    }
                } else {
                    Log.d("HealthViewModel", "Creating new log")
                    try {
                        val docRef = db.collection("healthLogs")
                            .add(logToSave.toMap())
                            .await()
                        Log.d("HealthViewModel", "Successfully created log with ID: ${docRef.id}")
                        true
                    } catch (e: Exception) {
                        Log.e("HealthViewModel", "Error creating new log", e)
                        false
                    }
                }
                
                if (success) {
                    Log.d("HealthViewModel", "Log saved successfully, reloading logs...")
                    loadHealthLogs()
                } else {
                    Log.e("HealthViewModel", "Failed to save log, not reloading")
                }
                
                onResult(success)
                
            } catch (e: Exception) {
                Log.e("HealthViewModel", "Unexpected error in saveHealthLog", e)
                onResult(false)
            }
        }
    }
    
    fun deleteHealthLog(log: HealthLog, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            try {
                if (log.id.isEmpty()) {
                    Log.e("HealthViewModel", "Cannot delete log: empty ID")
                    return@launch
                }
                
                _isLoading.value = true
                Log.d("HealthViewModel", "Deleting log with ID: ${log.id}")
                
                db.collection("healthLogs")
                    .document(log.id)
                    .delete()
                    .await()
                
                Log.d("HealthViewModel", "Successfully deleted log with ID: ${log.id}")
                
                // Reload logs to update the UI
                loadHealthLogs()
                onSuccess()
                
            } catch (e: Exception) {
                Log.e("HealthViewModel", "Error deleting health log", e)
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun setSelectedDate(calendar: Calendar) {
        _selectedDate.value = calendar
        loadHealthLogs()
    }
    
    fun showAddDialog(show: Boolean, log: HealthLog? = null) {
        _editingLog.value = log
        _showAddDialog.value = show
    }
    
    fun getTotalCalories(): Int {
        return _healthLogs.value.sumOf { it.calories.toInt() }
    }
}
