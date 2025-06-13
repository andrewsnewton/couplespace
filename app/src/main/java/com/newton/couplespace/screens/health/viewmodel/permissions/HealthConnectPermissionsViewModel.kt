package com.newton.couplespace.screens.health.viewmodel.permissions

import android.app.Application
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.newton.couplespace.screens.health.data.repository.HealthConnectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel responsible for managing Health Connect permissions and availability
 */
@HiltViewModel
class HealthConnectPermissionsViewModel @Inject constructor(
    application: Application,
    private val healthConnectRepository: HealthConnectRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "HealthConnectPermissions"
    }

    private val _isHealthConnectAvailable = MutableStateFlow(false)
    val isHealthConnectAvailable: StateFlow<Boolean> = _isHealthConnectAvailable.asStateFlow()

    private val _hasHealthConnectPermissions = MutableStateFlow(false)
    val hasHealthConnectPermissions: StateFlow<Boolean> = _hasHealthConnectPermissions.asStateFlow()
    
    // Status message for permission changes (for UI feedback)
    private val _permissionStatusMessage = MutableStateFlow<String?>(null)
    val permissionStatusMessage: StateFlow<String?> = _permissionStatusMessage.asStateFlow()

    private val _permissionRequestIntent = MutableLiveData<Intent?>(null)
    val permissionRequestIntent: LiveData<Intent?> = _permissionRequestIntent

    init {
        checkHealthConnectAvailability()
    }

    /**
     * Refreshes the Health Connect permissions status
     */
    fun refreshHealthConnectPermissions() {
        Log.d(TAG, "Refreshing Health Connect permissions status")
        viewModelScope.launch {
            checkPermissions()
        }
    }

    fun requestHealthConnectPermissions() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Checking if Health Connect is available")
                val isAvailable = HealthConnectClient.getSdkStatus(getApplication()) == HealthConnectClient.SDK_AVAILABLE
                
                if (!isAvailable) {
                    Log.d(TAG, "Health Connect is not available on this device")
                    _isHealthConnectAvailable.value = false
                    _hasHealthConnectPermissions.value = false
                    return@launch
                }
                
                Log.d(TAG, "Health Connect is available, requesting permissions")
                val permissions = healthConnectRepository.getRequiredPermissions()
                
                try {
                    // Try to use the standard permission request approach with rationale support
                    val healthConnectClient = HealthConnectClient.getOrCreate(getApplication())
                    
                    // Create the intent for requesting permissions using the contract
                    val requestPermissionContract = PermissionController.createRequestPermissionResultContract()
                    val intent = requestPermissionContract.createIntent(getApplication(), permissions)
                    
                    // Create a Bundle for the rationale messages
                    val rationaleBundle = Bundle()
                    val rationaleMessages = getPermissionRationaleMessages(permissions)
                    rationaleMessages.forEach { (permission, message) ->
                        rationaleBundle.putString(permission, message)
                    }
                    
                    // Add rationale information and app name
                    intent.putExtra(
                        "android.health.connect.extra.REQUEST_PERMISSIONS_RATIONALE_MESSAGES",
                        rationaleBundle
                    )
                    intent.putExtra(
                        "android.health.connect.extra.REQUEST_PERMISSIONS_APP_NAME",
                        getApplicationName()
                    )
                    
                    Log.d(TAG, "Permission request intent created with rationale support: $intent")
                    _permissionRequestIntent.value = intent
                } catch (e: Exception) {
                    Log.e(TAG, "Error with standard permission request, trying fallback", e)
                    
                    // Fallback to direct settings intent if the standard approach fails
                    try {
                        val settingsIntent = Intent("android.health.connect.action.HEALTH_HOME_SETTINGS")
                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        
                        Log.d(TAG, "Created fallback settings intent: $settingsIntent")
                        _permissionRequestIntent.value = settingsIntent
                    } catch (e2: Exception) {
                        Log.e(TAG, "All permission request approaches failed", e2)
                        _isHealthConnectAvailable.value = false
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in requestHealthConnectPermissions", e)
                e.printStackTrace()
                
                // Fallback if Health Connect is not available
                _isHealthConnectAvailable.value = false
                _hasHealthConnectPermissions.value = false
            }
        }
    }
    
    /**
     * Gets the application name for use in permission rationales
     */
    private fun getApplicationName(): String {
        val applicationInfo = getApplication<Application>().applicationInfo
        val stringId = applicationInfo.labelRes
        return if (stringId == 0) {
            applicationInfo.nonLocalizedLabel.toString()
        } else {
            getApplication<Application>().getString(stringId)
        }
    }
    
    /**
     * Creates permission rationale messages for Health Connect permissions
     */
    private fun getPermissionRationaleMessages(permissions: Set<String>): Map<String, String> {
        val rationaleMessages = mutableMapOf<String, String>()
        
        // Group permissions by category for better user understanding
        val hasStepsPermission = permissions.contains(HealthPermission.getReadPermission(StepsRecord::class))
        val hasHeartRatePermission = permissions.contains(HealthPermission.getReadPermission(HeartRateRecord::class))
        val hasSleepPermission = permissions.contains(HealthPermission.getReadPermission(SleepSessionRecord::class))
        val hasCaloriesPermission = permissions.contains(HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class))
        val hasActivityPermission = permissions.contains(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class))
        
        // Add rationale messages for each permission category
        if (hasStepsPermission) {
            rationaleMessages[HealthPermission.getReadPermission(StepsRecord::class)] = 
                "To track your daily step count and help you meet your fitness goals"
        }
        
        if (hasHeartRatePermission) {
            rationaleMessages[HealthPermission.getReadPermission(HeartRateRecord::class)] = 
                "To monitor your heart rate and provide health insights"
        }
        
        if (hasSleepPermission) {
            rationaleMessages[HealthPermission.getReadPermission(SleepSessionRecord::class)] = 
                "To analyze your sleep patterns and help improve your rest"
        }
        
        if (hasCaloriesPermission) {
            rationaleMessages[HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class)] = 
                "To track calories burned and help with nutrition planning"
        }
        
        if (hasActivityPermission) {
            rationaleMessages[HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)] = 
                "To monitor your active minutes and help you stay fit"
        }
        
        // Add a default message for any other permissions
        permissions.forEach { permission ->
            if (!rationaleMessages.containsKey(permission)) {
                rationaleMessages[permission] = "To provide you with comprehensive health tracking features"
            }
        }
        
        return rationaleMessages
    }

    fun clearPermissionRequestIntent() {
        _permissionRequestIntent.value = null
    }

    fun checkHealthConnectAvailability() {
        viewModelScope.launch {
            try {
                val availability = healthConnectRepository.isHealthConnectAvailable()
                _isHealthConnectAvailable.value = availability
                
                Log.d(TAG, "Health Connect availability: $availability")
                
                if (_isHealthConnectAvailable.value) {
                    checkPermissions()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking Health Connect availability", e)
                _isHealthConnectAvailable.value = false
            }
        }
    }

    /**
     * Checks if Health Connect permissions are granted
     * Updates permission status message for UI feedback
     */
    fun checkPermissions() {
        viewModelScope.launch {
            try {
                if (!_isHealthConnectAvailable.value) {
                    _hasHealthConnectPermissions.value = false
                    return@launch
                }

                val granted = healthConnectRepository.checkPermissions()
                val previouslyGranted = _hasHealthConnectPermissions.value
                _hasHealthConnectPermissions.value = granted
                
                if (granted) {
                    // Show a log message and update status message if permissions were just granted
                    if (!previouslyGranted) {
                        Log.d(TAG, "Health Connect permissions newly granted")
                        _permissionStatusMessage.value = "Health Connect connected! Syncing your health data..."
                        
                        // Clear the message after a delay
                        viewModelScope.launch {
                            kotlinx.coroutines.delay(3000)
                            _permissionStatusMessage.value = null
                        }
                    }
                } else if (previouslyGranted) {
                    // Permissions were revoked
                    Log.d(TAG, "Health Connect permissions were revoked")
                    _permissionStatusMessage.value = "Health Connect permissions were revoked. Using mock data."
                    
                    // Clear the message after a delay
                    viewModelScope.launch {
                        kotlinx.coroutines.delay(3000)
                        _permissionStatusMessage.value = null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking permissions", e)
                _hasHealthConnectPermissions.value = false
                _permissionStatusMessage.value = "Error checking Health Connect permissions"
                
                // Clear the message after a delay
                viewModelScope.launch {
                    kotlinx.coroutines.delay(3000)
                    _permissionStatusMessage.value = null
                }
            }
        }
    }
}
