package com.newton.couplespace.screens.health.data.repository

import android.content.Intent
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.records.*
import androidx.health.connect.client.time.TimeRangeFilter
import com.newton.couplespace.screens.health.data.models.*
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

/**
 * Repository interface for accessing health data from Health Connect API
 */
interface HealthConnectRepository {
    /**
     * Get the HealthConnectClient instance
     */
    val healthConnectClient: HealthConnectClient?

    /**
     * Check if Health Connect is available on the device
     */
    suspend fun isHealthConnectAvailable(): Boolean

    /**
     * Check if all required permissions are granted
     */
    suspend fun checkPermissions(): Boolean

    /**
     * Request Health Connect permissions
     * 
     * @return PermissionController to launch the permission request
     */
    suspend fun requestPermissions(): PermissionController

    /**
     * Get the list of required permissions
     */
    fun getRequiredPermissions(): Set<String>

    /**
     * Get date range for health data
     */
    suspend fun getDateRange(): Pair<LocalDate, LocalDate>

    /**
     * Get steps for a specific date range
     */
    suspend fun getStepsForDate(timeRange: TimeRangeFilter): Flow<StepsMetric>

    /**
     * Get step count for a date range
     */
    suspend fun getStepCount(startDate: LocalDate, endDate: LocalDate): Flow<List<StepsMetric>>

    /**
     * Get distance data for a date range
     */
    suspend fun getDistanceData(startDate: LocalDate, endDate: LocalDate): Flow<List<DistanceMetric>>

    /**
     * Get heart rate data for a date range
     */
    suspend fun getHeartRateData(startDate: LocalDate, endDate: LocalDate): Flow<List<HeartRateMetric>>

    /**
     * Get sleep data for a date range
     */
    suspend fun getSleepData(startDate: LocalDate, endDate: LocalDate): Flow<List<SleepMetric>>

    /**
     * Get weight data for a date range
     */
    suspend fun getWeightData(startDate: LocalDate, endDate: LocalDate): Flow<List<WeightMetric>>

    /**
     * Get calories burned for a specific date range
     */
    suspend fun getCaloriesBurnedForDate(timeRange: TimeRangeFilter): Flow<CaloriesBurnedMetric>

    /**
     * Get sleep duration for a specific date range
     */
    suspend fun getSleepDataForDate(timeRange: TimeRangeFilter): Flow<SleepMetric>

    /**
     * Get heart rate for a specific date range
     */
    suspend fun getHeartRateForDate(timeRange: TimeRangeFilter): Flow<HeartRateMetric>


    /**
     * Get active minutes for a specific date range
     */
    suspend fun getActiveMinutesForDate(timeRange: TimeRangeFilter): Flow<ActiveMinutesMetric>

    /**
     * Get calories burned data for a date range
     */
    suspend fun getCaloriesBurnedData(startDate: LocalDate, endDate: LocalDate): Flow<List<CaloriesBurnedMetric>>
    
    /**
     * Get steps data for a specific time range
     * Returns the total number of steps in the time range
     */
    suspend fun getStepsData(timeRange: TimeRangeFilter): Long
    
    /**
     * Get calories data for a specific time range
     * Returns the total calories burned in the time range
     */
    suspend fun getCaloriesData(timeRange: TimeRangeFilter): Double
    
    /**
     * Get active minutes data for a specific time range
     * Returns the total active minutes in the time range
     */
    suspend fun getActiveMinutesData(timeRange: TimeRangeFilter): Double
}