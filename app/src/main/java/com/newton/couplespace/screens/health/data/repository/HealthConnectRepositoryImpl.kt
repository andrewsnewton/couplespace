package com.newton.couplespace.screens.health.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.firebase.auth.FirebaseAuth
import com.newton.couplespace.screens.health.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import dagger.Lazy

/**
 * Implementation of HealthConnectRepository that interacts with Health Connect API
 * This class delegates specialized functionality to more focused repositories
 * for better code organization and maintainability
 */
@Singleton
class HealthConnectRepositoryImpl @Inject constructor(
    private val context: Context,
    private val auth: FirebaseAuth,
    private val caloriesRepository: Lazy<HealthConnectCaloriesRepository>,
    private val activityRepository: Lazy<HealthConnectActivityRepository>,
    private val heartRateRepository: Lazy<HealthConnectHeartRateRepository>,
    private val sleepRepository: Lazy<HealthConnectSleepRepository>
) : HealthConnectRepository {
    
    companion object {
        private const val TAG = "HealthConnectRepo"
        private const val HEALTH_CONNECT_PACKAGE = "com.google.android.apps.healthdata"
    }
    
    // Lazily initialize Health Connect client
    override val healthConnectClient by lazy {
        try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Health Connect client", e)
            null
        }
    }
    
    /**
     * Check if Health Connect is available on the device
     */
    override suspend fun isHealthConnectAvailable(): Boolean {
        return try {
            val info = context.packageManager.getPackageInfo(HEALTH_CONNECT_PACKAGE, 0)
            info != null
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }
    
    /**
     * Check if all required permissions are granted
     */
    override suspend fun checkPermissions(): Boolean {
        val client = healthConnectClient ?: return false
        val permissions = getRequiredPermissions()
        val granted = client.permissionController.getGrantedPermissions()
        return granted.containsAll(permissions)
    }
    
    /**
     * Request Health Connect permissions
     */
    override suspend fun requestPermissions(): PermissionController {
        val client = healthConnectClient ?: throw IllegalStateException("Health Connect client is not available")
        return client.permissionController
    }
    
    /**
     * Get the list of required permissions
     */
    override fun getRequiredPermissions(): Set<String> {
        return setOf(
            HealthPermission.getReadPermission(StepsRecord::class),
            HealthPermission.getReadPermission(HeartRateRecord::class),
            HealthPermission.getReadPermission(SleepSessionRecord::class),
            HealthPermission.getReadPermission(WeightRecord::class),
            HealthPermission.getReadPermission(DistanceRecord::class),
            HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
            HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class)
        )
    }
    
    /**
     * Get date range for health data
     */
    override suspend fun getDateRange(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        val startDate = today.minusDays(30)
        return Pair(startDate, today)
    }
    
    /**
     * Get calories burned for a specific date range
     * Delegates to the specialized CaloriesRepository
     */
    override suspend fun getCaloriesBurnedForDate(timeRange: TimeRangeFilter): Flow<CaloriesBurnedMetric> =
        caloriesRepository.get().getCaloriesBurnedForDate(timeRange)
    
    /**
     * Get active minutes for a specific date range
     * Delegates to the specialized ActivityRepository
     */
    override suspend fun getActiveMinutesForDate(timeRange: TimeRangeFilter): Flow<ActiveMinutesMetric> =
        activityRepository.get().getActiveMinutesForDate(timeRange)
    
    /**
     * Get steps for a specific date
     * Delegates to the specialized ActivityRepository
     */
    override suspend fun getStepsForDate(timeRange: TimeRangeFilter): Flow<StepsMetric> =
        activityRepository.get().getStepsForDate(timeRange)
    
    /**
     * Get heart rate for a specific date
     * Delegates to the specialized HeartRateRepository
     */
    override suspend fun getHeartRateForDate(timeRange: TimeRangeFilter): Flow<HeartRateMetric> =
        heartRateRepository.get().getHeartRateForDate(timeRange)
    
    /**
     * Get step count for a date range
     * Delegates to the specialized ActivityRepository
     */
    override suspend fun getStepCount(startDate: LocalDate, endDate: LocalDate): Flow<List<StepsMetric>> =
        activityRepository.get().getStepCount(startDate, endDate)
    
    /**
     * Get heart rate data for a date range
     * Delegates to the specialized HeartRateRepository
     */
    override suspend fun getHeartRateData(startDate: LocalDate, endDate: LocalDate): Flow<List<HeartRateMetric>> =
        heartRateRepository.get().getHeartRateData(startDate, endDate)
    
    /**
     * Get distance data for a date range
     * Delegates to the specialized ActivityRepository
     */
    override suspend fun getDistanceData(startDate: LocalDate, endDate: LocalDate): Flow<List<DistanceMetric>> =
        activityRepository.get().getDistanceData(startDate, endDate)
    
    /**
     * Get sleep data for a date range
     * Delegates to the specialized SleepRepository
     */
    override suspend fun getSleepData(startDate: LocalDate, endDate: LocalDate): Flow<List<SleepMetric>> =
        sleepRepository.get().getSleepData(startDate, endDate)
    
    /**
     * Get weight data for a date range
     * Delegates to the specialized ActivityRepository
     */
    override suspend fun getWeightData(startDate: LocalDate, endDate: LocalDate): Flow<List<WeightMetric>> =
        activityRepository.get().getWeightData(startDate, endDate)
    
    /**
     * Get sleep data for a specific date
     * Delegates to the specialized SleepRepository
     */
    override suspend fun getSleepDataForDate(timeRange: TimeRangeFilter): Flow<SleepMetric> =
        sleepRepository.get().getSleepDataForDate(timeRange)
    
    /**
     * Get calories burned data for a date range
     * Delegates to the specialized CaloriesRepository
     */
    override suspend fun getCaloriesBurnedData(startDate: LocalDate, endDate: LocalDate): Flow<List<CaloriesBurnedMetric>> =
        caloriesRepository.get().getCaloriesBurnedData(startDate, endDate)
    
    /**
     * Get steps data for a specific time range
     * Returns the total number of steps in the time range
     */
    override suspend fun getStepsData(timeRange: TimeRangeFilter): Long =
        activityRepository.get().getStepsData(timeRange)
    
    /**
     * Get calories data for a specific time range
     * Delegates to the specialized CaloriesRepository
     */
    override suspend fun getCaloriesData(timeRange: TimeRangeFilter): Double =
        caloriesRepository.get().getCaloriesData(timeRange)
    
    /**
     * Get active minutes data for a specific time range
     * Delegates to the specialized ActivityRepository
     */
    override suspend fun getActiveMinutesData(timeRange: TimeRangeFilter): Double =
        activityRepository.get().getActiveMinutesData(timeRange)
}
