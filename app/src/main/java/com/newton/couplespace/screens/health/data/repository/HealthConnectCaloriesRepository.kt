package com.newton.couplespace.screens.health.data.repository

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.firebase.auth.FirebaseAuth
import com.newton.couplespace.screens.health.data.models.CaloriesBurnedMetric
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for calories-related Health Connect data
 */
@Singleton
class HealthConnectCaloriesRepository @Inject constructor(
    private val healthConnectClient: HealthConnectClient?,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "HealthConnectCaloriesRepo"
        private const val CALORIES_UPPER_LIMIT = 10000.0 // Reasonable daily limit for calorie values
    }

    /**
     * Get calories burned for a specific date range (typically a single day).
     * Prioritizes ActiveCaloriesBurnedRecord over TotalCaloriesBurnedRecord.
     * Uses aggregation APIs for more efficient data retrieval and includes filtering for large values.
     */
    suspend fun getCaloriesBurnedForDate(timeRange: TimeRangeFilter): Flow<CaloriesBurnedMetric> = flow {
        val client = healthConnectClient ?: run {
            Log.w(TAG, "Health Connect not available, emitting empty data for getCaloriesBurnedForDate.")
            emit(createEmptyCaloriesMetric(timeRange))
            return@flow
        }

        try {
            Log.d(TAG, "Getting calories burned for timeRange: ${timeRange.startTime} to ${timeRange.endTime}")

            val permissions = client.permissionController.getGrantedPermissions()
            val hasTotalCaloriesPermission = permissions.contains(HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class))
            val hasActiveCaloriesPermission = permissions.contains(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class))

            Log.d(TAG, "Has TotalCaloriesBurnedRecord permission: $hasTotalCaloriesPermission")
            Log.d(TAG, "Has ActiveCaloriesBurnedRecord permission: $hasActiveCaloriesPermission")

            if (!hasTotalCaloriesPermission && !hasActiveCaloriesPermission) {
                Log.w(TAG, "Missing calories permissions - using empty data for getCaloriesBurnedForDate")
                emit(createEmptyCaloriesMetric(timeRange))
                return@flow
            }

            var totalCalories = 0.0
            var dataSource = "Unknown"

            // Prioritize ActiveCaloriesBurnedRecord
            if (hasActiveCaloriesPermission) {
                try {
                    val aggregateRequest = AggregateRequest(
                        metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                        timeRangeFilter = timeRange
                    )
                    val aggregateResponse = client.aggregate(aggregateRequest)
                    val activeCalories = aggregateResponse.get(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)

                    if (activeCalories != null) {
                        val calories = activeCalories.inCalories
                        if (calories > CALORIES_UPPER_LIMIT) {
                            Log.w(TAG, "Found suspiciously high active calorie value: $calories. Ignoring and trying total calories.")
                        } else {
                            totalCalories = calories
                            dataSource = "Health Connect - Active"
                            Log.d(TAG, "Using aggregated active calories: $totalCalories calories")
                        }
                    } else {
                        Log.d(TAG, "No aggregated active calories data available for this time range.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error aggregating active calories, trying individual records or total calories", e)
                }
            }

            // Fallback to TotalCaloriesBurnedRecord if active calories not obtained or suspicious
            if (totalCalories == 0.0 && hasTotalCaloriesPermission) {
                try {
                    val aggregateRequest = AggregateRequest(
                        metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                        timeRangeFilter = timeRange
                    )
                    val aggregateResponse = client.aggregate(aggregateRequest)
                    val totalCaloriesEnergy = aggregateResponse.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL)

                    if (totalCaloriesEnergy != null) {
                        val calories = totalCaloriesEnergy.inCalories
                        if (calories > CALORIES_UPPER_LIMIT) {
                            Log.w(TAG, "Found suspiciously high total calorie value: $calories. Ignoring.")
                        } else {
                            totalCalories = calories
                            dataSource = "Health Connect - Total"
                            Log.d(TAG, "Using aggregated total calories: $totalCalories calories")
                        }
                    } else {
                        Log.d(TAG, "No aggregated total calories data available for this time range.")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error aggregating total calories", e)
                }
            }

            // If still no valid data, use empty data
            if (totalCalories == 0.0) {
                Log.w(TAG, "No valid calories data found, using empty data")
                emit(createEmptyCaloriesMetric(timeRange))
                return@flow
            } else {
                val metric = CaloriesBurnedMetric(
                    id = "calories-${timeRange.startTime?.toEpochMilli() ?: System.currentTimeMillis()}",
                    userId = auth.currentUser?.uid ?: "unknown",
                    timestamp = timeRange.startTime ?: Instant.now(), // Use start time as timestamp
                    calories = totalCalories.toInt(),
                    source = dataSource,
                    isShared = false
                )
                Log.d(TAG, "Emitting real calories data: ${metric.calories} calories from ${metric.source}")
                emit(metric)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting calories burned for date", e)
            emit(createEmptyCaloriesMetric(timeRange))
        }
    }

    /**
     * Get calories burned data for a date range, returning a list of metrics (one per day).
     * Prioritizes ActiveCaloriesBurnedRecord over TotalCaloriesBurnedRecord for each day.
     */
    suspend fun getCaloriesBurnedData(startDate: LocalDate, endDate: LocalDate): Flow<List<CaloriesBurnedMetric>> = flow {
        val client = healthConnectClient ?: run {
            Log.w(TAG, "Health Connect not available, emitting empty data for getCaloriesBurnedData.")
            emit(generateEmptyCaloriesData(startDate, endDate))
            return@flow
        }

        try {
            val permissions = client.permissionController.getGrantedPermissions()
            val hasTotalCaloriesPermission = permissions.contains(HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class))
            val hasActiveCaloriesPermission = permissions.contains(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class))

            if (!hasTotalCaloriesPermission && !hasActiveCaloriesPermission) {
                Log.w(TAG, "Missing calories permissions - using empty data for getCaloriesBurnedData")
                emit(generateEmptyCaloriesData(startDate, endDate))
                return@flow
            }

            val caloriesMetrics = mutableListOf<CaloriesBurnedMetric>()
            var currentDate = startDate

            // Process each day individually
            while (!currentDate.isAfter(endDate)) {
                val dayStart = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val dayEnd = currentDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                val dayTimeRange = TimeRangeFilter.between(dayStart, dayEnd)

                var dailyCalories = 0.0
                var dailySource = "Mock Data" // Default to mock data

                // Try to get active calories first for the day
                if (hasActiveCaloriesPermission) {
                    try {
                        val activeAggregateRequest = AggregateRequest(
                            metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                            timeRangeFilter = dayTimeRange
                        )
                        val activeAggregateResponse = client.aggregate(activeAggregateRequest)
                        val activeCalories = activeAggregateResponse.get(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)

                        if (activeCalories != null) {
                            val calories = activeCalories.inCalories
                            if (calories <= CALORIES_UPPER_LIMIT) {
                                dailyCalories = calories
                                dailySource = "Health Connect - Active"
                            } else {
                                Log.w(TAG, "Suspiciously high active calories for $currentDate: $calories. Skipping.")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting active calories for $currentDate", e)
                    }
                }

                // Fall back to total calories if active calories not found or were suspicious
                if (dailyCalories == 0.0 && hasTotalCaloriesPermission) {
                    try {
                        val totalAggregateRequest = AggregateRequest(
                            metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                            timeRangeFilter = dayTimeRange
                        )
                        val totalAggregateResponse = client.aggregate(totalAggregateRequest)
                        val totalCaloriesEnergy = totalAggregateResponse.get(TotalCaloriesBurnedRecord.ENERGY_TOTAL)

                        if (totalCaloriesEnergy != null) {
                            val calories = totalCaloriesEnergy.inCalories
                            if (calories <= CALORIES_UPPER_LIMIT) {
                                dailyCalories = calories
                                dailySource = "Health Connect - Total"
                            } else {
                                Log.w(TAG, "Suspiciously high total calories for $currentDate: $calories. Skipping.")
                            }
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error getting total calories for $currentDate", e)
                    }
                }

                if (dailyCalories > 0.0) {
                    caloriesMetrics.add(
                        CaloriesBurnedMetric(
                            id = "calories-${dailySource.replace(" ", "")}-${currentDate}",
                            userId = auth.currentUser?.uid ?: "unknown",
                            timestamp = dayStart,
                            calories = dailyCalories.toInt(),
                            source = dailySource,
                            isShared = false
                        )
                    )
                } else {
                    // If no real data for this day, add empty data
                    caloriesMetrics.add(createEmptyCaloriesMetric(dayTimeRange))
                }
                currentDate = currentDate.plusDays(1)
            }

            emit(caloriesMetrics)
        } catch (e: Exception) {
            Log.e(TAG, "Error reading calories data over date range", e)
            emit(generateEmptyCaloriesData(startDate, endDate))
        }
    }

    /**
     * Get total calories burned for a specific time range.
     * This function only uses TotalCaloriesBurnedRecord and aggregates the sum.
     */
    suspend fun getCaloriesData(timeRange: TimeRangeFilter): Double {
        val client = healthConnectClient ?: return 0.0

        return try {
            val caloriesRequest = AggregateRequest(
                metrics = setOf(TotalCaloriesBurnedRecord.ENERGY_TOTAL),
                timeRangeFilter = timeRange
            )
            val caloriesResponse = client.aggregate(caloriesRequest)
            val calories = caloriesResponse[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inCalories ?: 0.0

            if (calories > CALORIES_UPPER_LIMIT) {
                Log.w(TAG, "Found suspiciously high aggregated calorie value: $calories, returning 0")
                0.0
            } else {
                Log.d(TAG, "Retrieved $calories calories from Health Connect")
                calories
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting calories data", e)
            0.0
        }
    }

    // Helper method for empty data
    private fun createEmptyCaloriesMetric(timeRange: TimeRangeFilter): CaloriesBurnedMetric {
        val startMillis = timeRange.startTime?.toEpochMilli() ?: 0L
        val endMillis = timeRange.endTime?.toEpochMilli() ?: 0L
        val midTime = Instant.ofEpochMilli(startMillis + ((endMillis - startMillis) / 2))

        Log.d(TAG, "Creating empty calories data for ${ZonedDateTime.ofInstant(midTime, ZoneId.systemDefault()).toLocalDate()}")

        return CaloriesBurnedMetric(
            id = "empty-calories-${midTime.toEpochMilli()}",
            userId = auth.currentUser?.uid ?: "unknown-user-id",
            timestamp = midTime,
            calories = 0,
            source = "No Data Available",
            isShared = false
        )
    }

    private fun generateEmptyCaloriesData(startDate: LocalDate, endDate: LocalDate): List<CaloriesBurnedMetric> {
        val result = mutableListOf<CaloriesBurnedMetric>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            val dayStart = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val dayEnd = currentDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            val dayTimeRange = TimeRangeFilter.between(dayStart, dayEnd)
            result.add(createEmptyCaloriesMetric(dayTimeRange))
            currentDate = currentDate.plusDays(1)
        }
        return result
    }
}