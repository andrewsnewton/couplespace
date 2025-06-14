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
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.Instant
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

    // Track if we successfully loaded real data vs mock data
    private val _loadedRealData = MutableStateFlow(false)
    val loadedRealData: StateFlow<Boolean> = _loadedRealData.asStateFlow()
    
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
                // Then check permissions - force a direct check with the Health Connect client
                val healthConnectClient = HealthConnectClient.getOrCreate(getApplication())
                val requiredPermissions = getRequiredPermissions()
                
                try {
                    // Direct check with the Health Connect client
                    val granted = healthConnectClient.permissionController.getGrantedPermissions()
                    Log.d(TAG, "Direct check - granted permissions: ${granted.size}, required: ${requiredPermissions.size}")
                    
                    val hasAllPermissions = granted.containsAll(requiredPermissions)
                    Log.d(TAG, "Health Connect permissions granted (direct check): $hasAllPermissions")
                    
                    // Also log any missing permissions
                    if (!hasAllPermissions) {
                        val missing = requiredPermissions.filter { !granted.contains(it) }
                        Log.d(TAG, "Missing permissions: ${missing.joinToString()}")
                    }
                    
                    val permissionsChanged = _hasHealthConnectPermissions.value != hasAllPermissions
                    _hasHealthConnectPermissions.value = hasAllPermissions
                    
                    // If permissions changed and are now granted, reload health data
                    if (permissionsChanged && hasAllPermissions) {
                        Log.d(TAG, "Permissions changed to granted, reloading health data")
                        loadHealthData()
                    } else if (hasAllPermissions) {
                        // Even if permissions didn't change but they are granted, reload data
                        // This ensures we load real data after a permission grant
                        Log.d(TAG, "Permissions already granted, reloading health data anyway")
                        loadHealthData()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking permissions directly", e)
                    // Fall back to repository check
                    val granted = healthConnectRepository.checkPermissions()
                    Log.d(TAG, "Health Connect permissions granted (repository fallback): $granted")
                    _hasHealthConnectPermissions.value = granted
                    
                    if (granted) {
                        loadHealthData()
                    } else {
                        loadMockData()
                    }
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
                val permissions = getRequiredPermissions()
                
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
    /**
     * Gets the required Health Connect permissions directly
     */
    private fun getRequiredPermissions(): Set<String> {
        return setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
        )
    }
    
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
     * Loads health data from Health Connect if available and permissions are granted,
     * otherwise falls back to mock data
     */
    fun loadHealthData() {
        viewModelScope.launch {
            _isLoading.value = true
            var loadedRealData = false
            
            try {
                Log.d(TAG, "Loading health data - HC available: ${_isHealthConnectAvailable.value}, permissions: ${_hasHealthConnectPermissions.value}")
                
                // First, force a refresh of the permission status to ensure we have the latest
                val healthConnectClient = HealthConnectClient.getOrCreate(getApplication())
                val requiredPermissions = getRequiredPermissions()
                
                try {
                    val granted = healthConnectClient.permissionController.getGrantedPermissions()
                    val hasAllPermissions = granted.containsAll(requiredPermissions)
                    Log.d(TAG, "Direct permission check before loading data: $hasAllPermissions (${granted.size} granted, ${requiredPermissions.size} required)")
                    
                    // Update the permission state with the latest value
                    _hasHealthConnectPermissions.value = hasAllPermissions
                    
                    // If permissions are not granted, load mock data
                    if (!hasAllPermissions) {
                        Log.d(TAG, "Permissions not granted (direct check), loading mock data")
                        loadMockData()
                        loadPartnerHealthData()
                        return@launch
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error checking permissions directly before loading data", e)
                    // If we can't check permissions directly, fall back to repository check
                    val permissionsGranted = healthConnectRepository.checkPermissions()
                    if (!permissionsGranted) {
                        Log.d(TAG, "Permissions not granted (repository check), loading mock data")
                        loadMockData()
                        loadPartnerHealthData()
                        return@launch
                    }
                }
                
                val startOfDay = _selectedDate.value.atStartOfDay(ZoneId.systemDefault())
                val endOfDay = _selectedDate.value.plusDays(1).atStartOfDay(ZoneId.systemDefault())
                val timeRange = TimeRangeFilter.between(
                    startOfDay.toInstant(),
                    endOfDay.toInstant()
                )
                
                // Load real data from Health Connect
                Log.d(TAG, "Loading real health data from Health Connect for date: ${_selectedDate.value}")
                val metrics = mutableListOf<HealthMetric>()
                
                // Get steps data
                try {
                    val steps = healthConnectRepository.getStepsData(timeRange)
                    Log.d(TAG, "Steps data from Health Connect: $steps")
                    if (steps > 0) {
                        metrics.add(StepsMetric(
                            id = "hc-steps-${_selectedDate.value}",
                            userId = "user-id", // Should be replaced with actual user ID
                            timestamp = _selectedDate.value.atTime(12, 0).toInstant(ZoneOffset.UTC),
                            count = steps.toInt(),
                            source = "Health Connect",
                            isShared = false
                        ))
                        loadedRealData = true
                        Log.d(TAG, "Loaded real steps data: $steps")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading steps data", e)
                }
                
                // Get calories data
                try {
                    val calories = healthConnectRepository.getCaloriesData(timeRange)
                    Log.d(TAG, "Calories data from Health Connect: $calories")
                    if (calories > 0) {
                        metrics.add(CaloriesBurnedMetric(
                            id = "hc-calories-${_selectedDate.value}",
                            userId = "user-id", // Should be replaced with actual user ID
                            timestamp = _selectedDate.value.atTime(12, 0).toInstant(ZoneOffset.UTC),
                            calories = calories.toInt(),
                            source = "Health Connect",
                            activity = "Daily Activity",
                            isShared = false
                        ))
                        loadedRealData = true
                        Log.d(TAG, "Loaded real calories data: $calories")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading calories data", e)
                }
                
                // Get active minutes data
                try {
                    val activeMinutes = healthConnectRepository.getActiveMinutesData(timeRange)
                    Log.d(TAG, "Active minutes data from Health Connect: $activeMinutes")
                    if (activeMinutes > 0) {
                        metrics.add(ActiveMinutesMetric(
                            id = "hc-active-${_selectedDate.value}",
                            userId = "user-id", // Should be replaced with actual user ID
                            timestamp = _selectedDate.value.atTime(12, 0).toInstant(ZoneOffset.UTC),
                            minutes = activeMinutes.toInt(),
                            intensity = ActiveMinutesMetric.ActivityIntensity.MODERATE,
                            source = "Health Connect",
                            isShared = false
                        ))
                        loadedRealData = true
                        Log.d(TAG, "Loaded real active minutes data: $activeMinutes")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading active minutes data", e)
                }
                
                // If we couldn't load any real data, fall back to mock data
                if (metrics.isEmpty()) {
                    Log.d(TAG, "No real data loaded, falling back to mock data")
                    loadMockData()
                } else {
                    _healthMetrics.value = metrics
                    Log.d(TAG, "Successfully loaded real health data: ${metrics.size} metrics")
                }
                
                // Always load partner data
                loadPartnerHealthData()
                
            } catch (e: Exception) {
                Log.e(TAG, "Error loading health data", e)
                loadMockData()
                loadPartnerHealthData()
            } finally {
                _isLoading.value = false
                _loadedRealData.value = loadedRealData
                Log.d(TAG, "Health data loading complete, loaded real data: $loadedRealData")
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