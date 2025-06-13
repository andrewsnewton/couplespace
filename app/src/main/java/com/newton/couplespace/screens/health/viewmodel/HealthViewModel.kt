package com.newton.couplespace.screens.health.viewmodel

import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.newton.couplespace.R
import com.newton.couplespace.screens.health.data.models.*
import com.newton.couplespace.screens.health.data.repository.HealthConnectRepository
import com.newton.couplespace.screens.health.data.repository.CoupleHealthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.Serializable
import java.time.*
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class HealthViewModel @Inject constructor(
    application: Application,
    private val healthConnectRepository: HealthConnectRepository,
    private val coupleHealthRepository: CoupleHealthRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "HealthViewModel"
    }

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Health metrics for the current day
    private val _healthMetrics = MutableStateFlow<List<HealthMetric>>(emptyList())
    val healthMetrics: StateFlow<List<HealthMetric>> = _healthMetrics.asStateFlow()
    
    // Partner health metrics for the current day
    private val _partnerHealthMetrics = MutableStateFlow<List<HealthMetric>>(emptyList())
    val partnerHealthMetrics: StateFlow<List<HealthMetric>> = _partnerHealthMetrics.asStateFlow()
    
    // Flag to indicate if partner data is available
    private val _hasPartnerData = MutableStateFlow(false)
    val hasPartnerData: StateFlow<Boolean> = _hasPartnerData.asStateFlow()

    private val _isHealthConnectAvailable = MutableStateFlow(false)
    val isHealthConnectAvailable: StateFlow<Boolean> = _isHealthConnectAvailable.asStateFlow()

    private val _hasHealthConnectPermissions = MutableStateFlow(false)
    val hasHealthConnectPermissions: StateFlow<Boolean> = _hasHealthConnectPermissions.asStateFlow()

    private val _permissionRequestIntent = MutableLiveData<Intent?>(null)
    val permissionRequestIntent: LiveData<Intent?> = _permissionRequestIntent

    init {
        checkHealthConnectAvailability()
        // We'll load health data after checking permissions to avoid loading mock data unnecessarily
    }

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        loadHealthData()
    }

    fun refreshHealthConnectPermissions() {
        viewModelScope.launch {
            Log.d(TAG, "Refreshing Health Connect permissions")
            // First check if Health Connect is available
            _isHealthConnectAvailable.value = HealthConnectClient.getSdkStatus(getApplication()) == HealthConnectClient.SDK_AVAILABLE
            Log.d(TAG, "Health Connect available (refresh): ${_isHealthConnectAvailable.value}")
            
            if (_isHealthConnectAvailable.value) {
                // Then check permissions
                val granted = healthConnectRepository.checkPermissions()
                Log.d(TAG, "Health Connect permissions granted (refresh): $granted")
                val permissionsChanged = _hasHealthConnectPermissions.value != granted
                _hasHealthConnectPermissions.value = granted
                
                // If permissions changed and are now granted, reload health data
                if (permissionsChanged && granted) {
                    Log.d(TAG, "Permissions changed to granted, reloading health data")
                    loadHealthData()
                }
            } else {
                _hasHealthConnectPermissions.value = false
                loadMockData()
            }
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
                    loadMockData()
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
                        loadMockData()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error in requestHealthConnectPermissions", e)
                e.printStackTrace()
                
                // Fallback to load mock data if Health Connect is not available
                _isHealthConnectAvailable.value = false
                _hasHealthConnectPermissions.value = false
                loadMockData()
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
    
    private fun checkHealthConnectAvailability() {
        viewModelScope.launch {
            try {
                Log.d(TAG, "Checking Health Connect availability")
                _isHealthConnectAvailable.value = HealthConnectClient.getSdkStatus(getApplication()) == HealthConnectClient.SDK_AVAILABLE
                Log.d(TAG, "Health Connect available: ${_isHealthConnectAvailable.value}")
                
                if (_isHealthConnectAvailable.value) {
                    checkPermissions()
                } else {
                    // If Health Connect is not available, we should load mock data
                    _hasHealthConnectPermissions.value = false
                    loadMockData()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error checking Health Connect availability", e)
                _isHealthConnectAvailable.value = false
                _hasHealthConnectPermissions.value = false
                loadMockData()
            }
        }
    }
    
    private fun checkPermissions() {
        viewModelScope.launch {
            try {
                if (!_isHealthConnectAvailable.value) {
                    Log.d(TAG, "Health Connect not available, can't check permissions")
                    _hasHealthConnectPermissions.value = false
                    loadMockData()
                    return@launch
                }

                val granted = healthConnectRepository.checkPermissions()
                Log.d(TAG, "Health Connect permissions granted: $granted")
                _hasHealthConnectPermissions.value = granted
                
                // Always load health data after checking permissions
                // If permissions are granted, it will load real data
                // If not, it will load mock data inside loadHealthData()
                loadHealthData()
            } catch (e: Exception) {
                Log.e(TAG, "Error checking permissions", e)
                _hasHealthConnectPermissions.value = false
                loadMockData()
            }
        }
    }
    
    /**
     * Loads health data for the selected date
     */
    fun loadHealthData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // First check if permissions are granted
                if (!_isHealthConnectAvailable.value || !_hasHealthConnectPermissions.value) {
                    Log.d(TAG, "No Health Connect permissions or not available, loading mock data")
                    Log.d(TAG, "Health Connect available: ${_isHealthConnectAvailable.value}, permissions granted: ${_hasHealthConnectPermissions.value}")
                    loadMockData()
                    // Still try to load partner data even if we don't have Health Connect permissions
                    loadPartnerHealthData()
                    return@launch
                }
                
                // Force check permissions again to ensure they're still granted
                val permissionsStillGranted = healthConnectRepository.checkPermissions()
                if (!permissionsStillGranted) {
                    Log.d(TAG, "Permissions were revoked, loading mock data")
                    _hasHealthConnectPermissions.value = false
                    loadMockData()
                    loadPartnerHealthData()
                    return@launch
                }
                
                val startOfDay = _selectedDate.value.atStartOfDay(ZoneId.systemDefault())
                val endOfDay = _selectedDate.value.plusDays(1).atStartOfDay(ZoneId.systemDefault())
                val timeRange = TimeRangeFilter.between(
                    startOfDay.toInstant(),
                    endOfDay.toInstant()
                )
                
                Log.d(TAG, "Loading health data for ${_selectedDate.value} with time range: $timeRange")
                
                val metrics = mutableListOf<HealthMetric>()
                var hasLoadedAnyData = false
                
                // Collect steps data
                try {
                    healthConnectRepository.getStepsForDate(timeRange).collect { stepsMetric ->
                        Log.d(TAG, "Loaded steps data: $stepsMetric")
                        metrics.add(stepsMetric)
                        hasLoadedAnyData = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading steps", e)
                }
                
                // Collect heart rate data
                try {
                    healthConnectRepository.getHeartRateForDate(timeRange).collect { heartRateMetric ->
                        Log.d(TAG, "Loaded heart rate data: $heartRateMetric")
                        metrics.add(heartRateMetric)
                        hasLoadedAnyData = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading heart rate", e)
                }
                
                // Collect sleep data
                try {
                    healthConnectRepository.getSleepDataForDate(timeRange).collect { sleepMetric ->
                        Log.d(TAG, "Loaded sleep data: $sleepMetric")
                        metrics.add(sleepMetric)
                        hasLoadedAnyData = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading sleep", e)
                }
                
                // Collect calories burned data
                try {
                    healthConnectRepository.getCaloriesBurnedForDate(timeRange).collect { caloriesMetric ->
                        Log.d(TAG, "Loaded calories data: $caloriesMetric")
                        metrics.add(caloriesMetric)
                        hasLoadedAnyData = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading calories burned", e)
                }
                
                // Collect active minutes data
                try {
                    healthConnectRepository.getActiveMinutesForDate(timeRange).collect { activeMinutesMetric ->
                        Log.d(TAG, "Loaded active minutes data: $activeMinutesMetric")
                        metrics.add(activeMinutesMetric)
                        hasLoadedAnyData = true
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading active minutes", e)
                }
                
                if (!hasLoadedAnyData || metrics.isEmpty()) {
                    Log.d(TAG, "No health metrics found, loading mock data")
                    loadMockData()
                } else {
                    _healthMetrics.value = metrics
                    Log.d(TAG, "Successfully loaded ${metrics.size} health metrics")
                }
                
                // Also load partner health data if available
                loadPartnerHealthData()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading health data", e)
                loadMockData()
                // Still try to load partner data even if we had an error with our own data
                loadPartnerHealthData()
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Loads partner health data if available
     */
    private fun loadPartnerHealthData() {
        viewModelScope.launch {
            try {
                // Check if there's a partner connected in the couple space
                val hasPartner = coupleHealthRepository.hasConnectedPartner()
                if (!hasPartner) {
                    _hasPartnerData.value = false
                    Log.d(TAG, "No connected partner found")
                    return@launch
                }
                
                val startOfDay = _selectedDate.value.atStartOfDay(ZoneId.systemDefault())
                val endOfDay = _selectedDate.value.plusDays(1).atStartOfDay(ZoneId.systemDefault())
                
                // Get partner metrics from the couple health repository
                coupleHealthRepository.getPartnerHealthMetrics(_selectedDate.value).collect { partnerMetrics ->
                    if (partnerMetrics.isNotEmpty()) {
                        _partnerHealthMetrics.value = partnerMetrics
                        _hasPartnerData.value = true
                        Log.d(TAG, "Successfully loaded ${partnerMetrics.size} partner health metrics")
                    } else {
                        _hasPartnerData.value = false
                        Log.d(TAG, "No partner health metrics available for this date")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading partner health data", e)
                _hasPartnerData.value = false
            }
        }
    }

    /**
     * Shares a health metric with the partner through the couple health repository
     */
    fun shareHealthMetric(metric: HealthMetric) {
        viewModelScope.launch {
            try {
                coupleHealthRepository.shareHealthMetric(metric)
                Log.d(TAG, "Successfully shared health metric: ${metric.type}")
            } catch (e: Exception) {
                Log.e(TAG, "Error sharing health metric", e)
            }
        }
    }

    fun loadMockData() {
        val mockMetrics = mutableListOf<HealthMetric>()
        
        // Mock steps data
        mockMetrics.add(
            StepsMetric(
                id = "mock-steps-${_selectedDate.value}",
                userId = "mock-user-id",
                timestamp = _selectedDate.value.atTime(12, 0).toInstant(ZoneOffset.UTC),
                count = 7500,
                source = "Mock Data",
                isShared = false
            )
        )
        
        // Mock heart rate data
        mockMetrics.add(
            HeartRateMetric(
                id = "mock-heart-rate-${_selectedDate.value}",
                userId = "mock-user-id",
                timestamp = _selectedDate.value.atTime(12, 0).toInstant(ZoneOffset.UTC),
                beatsPerMinute = 72,
                source = "Mock Data",
                isShared = false
            )
        )
        
        // Mock sleep data
        val sleepStart = _selectedDate.value.minusDays(1).atTime(23, 0).toInstant(ZoneOffset.UTC)
        val sleepEnd = _selectedDate.value.atTime(7, 30).toInstant(ZoneOffset.UTC)
        mockMetrics.add(
            SleepMetric(
                id = "mock-sleep-${_selectedDate.value}",
                userId = "mock-user-id",
                timestamp = sleepStart,
                startTime = sleepStart,
                endTime = sleepEnd,
                durationHours = 8.5f, // 8.5 hours
                source = "Mock Data",
                isShared = false
            )
        )
        
        // Mock calories burned
        mockMetrics.add(
            CaloriesBurnedMetric(
                id = "mock-calories-${_selectedDate.value}",
                userId = "mock-user-id",
                timestamp = _selectedDate.value.atTime(12, 0).toInstant(ZoneOffset.UTC),
                calories = 450,
                source = "Mock Data",
                isShared = false
            )
        )
        
        // Mock active minutes
        mockMetrics.add(
            ActiveMinutesMetric(
                id = "mock-active-${_selectedDate.value}",
                userId = "mock-user-id",
                timestamp = _selectedDate.value.atTime(12, 0).toInstant(ZoneOffset.UTC),
                minutes = 60,
                intensity = ActiveMinutesMetric.ActivityIntensity.MODERATE,
                source = "Mock Data",
                isShared = false
            )
        )
        
        _healthMetrics.value = mockMetrics
    }
}