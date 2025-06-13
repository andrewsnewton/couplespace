package com.newton.couplespace.screens.health.viewmodel.metrics

import android.app.Application
import android.util.Log
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.newton.couplespace.screens.health.data.models.*
import com.newton.couplespace.screens.health.data.repository.HealthConnectRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.*
import javax.inject.Inject

/**
 * ViewModel responsible for loading and managing health metrics data
 */
@HiltViewModel
class HealthMetricsViewModel @Inject constructor(
    application: Application,
    private val healthConnectRepository: HealthConnectRepository
) : AndroidViewModel(application) {

    companion object {
        private const val TAG = "HealthMetricsViewModel"
    }

    private val _selectedDate = MutableStateFlow(LocalDate.now())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    // Health metrics for the current day
    private val _healthMetrics = MutableStateFlow<List<HealthMetric>>(emptyList())
    val healthMetrics: StateFlow<List<HealthMetric>> = _healthMetrics.asStateFlow()

    // Flag to indicate if Health Connect permissions are available
    private val _hasHealthConnectPermissions = MutableStateFlow(false)

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
        loadHealthData()
    }

    /**
     * Updates the Health Connect permissions status
     */
    fun updatePermissionsStatus(hasPermissions: Boolean) {
        _hasHealthConnectPermissions.value = hasPermissions
        // If permissions changed, reload health data
        loadHealthData()
    }

    /**
     * Loads health data for the selected date
     * Automatically synchronizes data from Health Connect when permissions are granted
     */
    fun loadHealthData() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (!_hasHealthConnectPermissions.value) {
                    Log.d(TAG, "No Health Connect permissions, loading mock data")
                    loadMockData()
                    return@launch
                }
                
                val startOfDay = _selectedDate.value.atStartOfDay(ZoneId.systemDefault())
                val endOfDay = _selectedDate.value.plusDays(1).atStartOfDay(ZoneId.systemDefault())
                val timeRange = TimeRangeFilter.between(
                    startOfDay.toInstant(),
                    endOfDay.toInstant()
                )
                
                Log.d(TAG, "Synchronizing health data from Health Connect for ${_selectedDate.value}")
                
                // Use Flow.combine to collect all health metrics in parallel
                // This automatically synchronizes data from Health Connect when permissions are granted
                // Try to load data from Health Connect, but if any stream fails, we'll load all mock data
                try {
                    combine(
                        healthConnectRepository.getStepsForDate(timeRange),
                        healthConnectRepository.getHeartRateForDate(timeRange),
                        healthConnectRepository.getSleepDataForDate(timeRange),
                        healthConnectRepository.getCaloriesBurnedForDate(timeRange),
                        healthConnectRepository.getActiveMinutesForDate(timeRange)
                    ) { steps, heartRate, sleep, calories, activeMinutes ->
                        val metrics = listOf(steps, heartRate, sleep, calories, activeMinutes)
                        _healthMetrics.value = metrics
                        Log.d(TAG, "Successfully synchronized ${metrics.size} health metrics from Health Connect")
                    }.collect()
                } catch (e: Exception) {
                    Log.e(TAG, "Error collecting health data from Health Connect, using mock data", e)
                    loadMockData()
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Error synchronizing health data", e)
                loadMockData()
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Loads mock health data when Health Connect is not available
     */
    private fun loadMockData() {
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
        Log.d(TAG, "Loaded mock health data with ${mockMetrics.size} metrics")
    }
}
