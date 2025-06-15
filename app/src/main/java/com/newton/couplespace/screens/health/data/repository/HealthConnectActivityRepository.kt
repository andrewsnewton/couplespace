package com.newton.couplespace.screens.health.data.repository

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.aggregate.AggregationResult
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.firebase.auth.FirebaseAuth
import com.newton.couplespace.screens.health.data.models.ActiveMinutesMetric
import com.newton.couplespace.screens.health.data.models.DistanceMetric
import com.newton.couplespace.screens.health.data.models.StepsMetric
import com.newton.couplespace.screens.health.data.models.WeightMetric
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for activity-related Health Connect data
 */
@Singleton
class HealthConnectActivityRepository @Inject constructor(
    private val healthConnectClient: HealthConnectClient?,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "HealthConnectActivityRepo"
    }

    /**
     * Get active minutes for a specific date range
     * Prioritizes ExerciseSessionRecord for accurate activity data
     * Falls back to ActiveCaloriesBurnedRecord if exercise data is unavailable
     * Uses aggregation APIs for more efficient data retrieval
     */
    suspend fun getActiveMinutesForDate(timeRange: TimeRangeFilter): Flow<ActiveMinutesMetric> = flow {
        val client = healthConnectClient ?: run {
            Log.w(TAG, "Health Connect not available, emitting empty data for getActiveMinutesForDate.")
            emit(createEmptyActiveMinutesMetric(timeRange))
            return@flow
        }

        try {
            Log.d(TAG, "Getting active minutes for timeRange: ${timeRange.startTime} to ${timeRange.endTime}")

            // Check permissions first
            val permissions = client.permissionController.getGrantedPermissions()
            val hasExercisePermission = permissions.contains(HealthPermission.getReadPermission(ExerciseSessionRecord::class))
            val hasActivityPermission = permissions.contains(HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class))

            // Log all granted permissions to help with debugging
            Log.d(TAG, "Granted permissions for active minutes: ${permissions.size} total")
            Log.d(TAG, "Has ExerciseSessionRecord permission: $hasExercisePermission")
            Log.d(TAG, "Has ActiveCaloriesBurnedRecord permission: $hasActivityPermission")

            if (!hasExercisePermission && !hasActivityPermission) {
                Log.w(TAG, "Missing both exercise and active calories permissions - using empty data")
                emit(createEmptyActiveMinutesMetric(timeRange))
                return@flow
            }

            // Try multiple record types to get active minutes
            var totalMinutes = 0
            var dataSource = "Unknown"

            // 1. First try exercise sessions if we have permission - this is the most accurate source
            if (hasExercisePermission) {
                try {
                    // Try to use aggregation first for more efficient data retrieval
                    val aggregateRequest = AggregateRequest(
                        metrics = setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL),
                        timeRangeFilter = timeRange
                    )

                    val aggregateResponse = client.aggregate(aggregateRequest)
                    val exerciseDuration = aggregateResponse.get(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL)

                    if (exerciseDuration != null) {
                        // Convert duration to minutes
                        val minutes = exerciseDuration.toMinutes()
                        if (minutes > 0) {
                            totalMinutes = minutes.toInt()
                            dataSource = "Exercise Sessions (Aggregated)"
                            Log.d(TAG, "Using aggregated exercise duration: $totalMinutes minutes")
                        } else {
                            Log.d(TAG, "Aggregated exercise duration is zero or null")
                        }
                    } else {
                        Log.d(TAG, "No aggregated exercise data available, falling back to record-by-record")

                        // Fall back to reading individual exercise records
                        val exerciseRequest = ReadRecordsRequest(
                            recordType = ExerciseSessionRecord::class,
                            timeRangeFilter = timeRange
                        )

                        val exerciseResponse = client.readRecords(exerciseRequest)
                        Log.d(TAG, "Exercise sessions found: ${exerciseResponse.records.size}")

                        // Log each exercise session for debugging
                        exerciseResponse.records.forEachIndexed { index, record ->
                            val start = record.startTime
                            val end = record.endTime
                            val duration = if (start != null && end != null) {
                                java.time.Duration.between(start, end).toMinutes()
                            } else {
                                0L
                            }
                            Log.d(TAG, "Exercise session $index: $duration minutes, type: ${record.exerciseType}")
                        }

                        val exerciseMinutes = exerciseResponse.records.sumOf { record ->
                            val start = record.startTime ?: return@sumOf 0L
                            val end = record.endTime ?: return@sumOf 0L
                            java.time.Duration.between(start, end).toMinutes()
                        }.toInt()

                        if (exerciseMinutes > 0) {
                            totalMinutes = exerciseMinutes
                            dataSource = "Exercise Sessions (Records)"
                            Log.d(TAG, "Found $exerciseMinutes active minutes from ${exerciseResponse.records.size} exercise sessions")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading exercise session data: ${e.message}", e)
                    // Continue to try active calories if exercise sessions fail
                }
            } else {
                Log.d(TAG, "No exercise permission, skipping exercise sessions")
            }

            // 2. Also check activity calories if available and we have permission
            if (totalMinutes == 0 && hasActivityPermission) {
                try {
                    // Try aggregation first
                    val aggregateRequest = AggregateRequest(
                        metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                        timeRangeFilter = timeRange
                    )

                    val aggregateResponse = client.aggregate(aggregateRequest)
                    val activeCalories = aggregateResponse.get(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)

                    if (activeCalories != null) {
                        val calories = activeCalories.inCalories

                        // Filter out suspicious values
                        if (calories > 1000000) {
                            Log.w(TAG, "Found extremely large active calorie value: $calories, likely an error. Ignoring.")
                        } else if (calories > 10000) {
                            Log.w(TAG, "Found suspiciously high active calorie value: $calories, ignoring as it exceeds reasonable daily limit.")
                        } else {
                            // Estimate minutes based on calories (rough approximation: 10 calories = 1 active minute)
                            val activityMinutes = (calories / 10).toInt()
                            if (activityMinutes > 0) {
                                totalMinutes = activityMinutes
                                dataSource = "Active Calories (Aggregated)"
                                Log.d(TAG, "Using estimated $activityMinutes active minutes from aggregated calories burned")
                            }
                        }
                    } else {
                        Log.d(TAG, "No aggregated active calories data available, falling back to record-by-record")

                        // Fall back to reading individual records
                        val activityRequest = ReadRecordsRequest(
                            recordType = ActiveCaloriesBurnedRecord::class,
                            timeRangeFilter = timeRange
                        )

                        val activityResponse = client.readRecords(activityRequest)
                        Log.d(TAG, "Active calories records found: ${activityResponse.records.size}")

                        // Log each activity record for debugging
                        activityResponse.records.forEachIndexed { index, record ->
                            Log.d(TAG, "Active calories record $index: ${record.energy.inCalories} calories")
                        }
                        val activeCaloriesSum = activityResponse.records.sumOf { it.energy.inCalories }
                        if (activeCaloriesSum > 0 && activeCaloriesSum < 10000) {
                            totalMinutes = (activeCaloriesSum / 10).toInt()
                            dataSource = "Active Calories (Records)"
                            Log.d(TAG, "Using estimated $totalMinutes active minutes from records of calories burned")
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading active calories data: ${e.message}", e)
                }
            }

            if (totalMinutes == 0) {
                Log.w(TAG, "No active minutes data found from any source, using empty data")
                emit(createEmptyActiveMinutesMetric(timeRange))
                return@flow
            }

            // Get the midpoint of the time range for the timestamp
            val startMillis = timeRange.startTime?.toEpochMilli() ?: 0L
            val endMillis = timeRange.endTime?.toEpochMilli() ?: 0L
            val midTime = Instant.ofEpochMilli(startMillis + ((endMillis - startMillis) / 2))

            val metric = ActiveMinutesMetric(
                id = "active-${timeRange.startTime?.toEpochMilli() ?: System.currentTimeMillis()}",
                userId = auth.currentUser?.uid ?: "unknown",
                timestamp = midTime,
                minutes = totalMinutes,
                intensity = ActiveMinutesMetric.ActivityIntensity.MODERATE,
                source = "Health Connect ($dataSource)",
                isShared = false
            )

            Log.d(TAG, "Emitting real active minutes data: ${metric.minutes} minutes from $dataSource")
            emit(metric)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active minutes", e)
            emit(createEmptyActiveMinutesMetric(timeRange))
        }
    }

    /**
     * Get steps for a specific date
     */
    suspend fun getStepsForDate(timeRange: TimeRangeFilter): Flow<StepsMetric> = flow {
        val client = healthConnectClient ?: throw IllegalStateException("Health Connect not available")

        try {
            // Check permissions first
            val permissions = client.permissionController.getGrantedPermissions()
            val hasStepsPermission = permissions.contains(HealthPermission.getReadPermission(StepsRecord::class))

            if (!hasStepsPermission) {
                Log.w(TAG, "Missing steps permission - using empty data")
                emit(createEmptyStepsMetric(timeRange))
                return@flow
            }

            // Read steps data
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = timeRange
            )

            val response = client.readRecords(request)
            val totalSteps = response.records.sumOf { it.count.toLong() }

            if (totalSteps <= 0) {
                Log.d(TAG, "No steps data found, using empty data")
                emit(createEmptyStepsMetric(timeRange))
                return@flow
            }

            // Get the midpoint of the time range for the timestamp
            val startMillis = timeRange.startTime?.toEpochMilli() ?: 0L
            val endMillis = timeRange.endTime?.toEpochMilli() ?: 0L
            val midTime = Instant.ofEpochMilli(startMillis + ((endMillis - startMillis) / 2))

            emit(StepsMetric(
                id = "steps-${timeRange.startTime?.toEpochMilli() ?: System.currentTimeMillis()}",
                userId = auth.currentUser?.uid ?: "unknown",
                timestamp = midTime,
                count = totalSteps.toInt(),
                source = "Health Connect",
                isShared = false
            ))
        } catch (e: Exception) {
            Log.e(TAG, "Error reading steps data", e)
            emit(createEmptyStepsMetric(timeRange))
        }
    }

    /**
     * Get step count for a date range
     */
    suspend fun getStepCount(startDate: LocalDate, endDate: LocalDate): Flow<List<StepsMetric>> = flow {
        val client = healthConnectClient ?: run {
            emit(emptyList())
            return@flow
        }

        try {
            val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endTime = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )

            val response = client.readRecords(request)

            // Group by day
            val stepsByDay = response.records.groupBy { record ->
                record.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
            }

            val stepsMetrics = stepsByDay.map { (date, records) ->
                val totalSteps = records.sumOf { it.count.toLong() }

                StepsMetric(
                    id = "steps-${date}",
                    userId = auth.currentUser?.uid ?: "unknown",
                    timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    count = totalSteps.toInt(),
                    source = "Health Connect",
                    isShared = false
                )
            }

            if (stepsMetrics.isEmpty()) {
                emit(generateEmptyStepsData(startDate, endDate))
            } else {
                emit(stepsMetrics)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading steps data", e)
            emit(generateEmptyStepsData(startDate, endDate))
        }
    }

    /**
     * Get distance data for a date range
     */
    suspend fun getDistanceData(startDate: LocalDate, endDate: LocalDate): Flow<List<DistanceMetric>> = flow {
        val client = healthConnectClient ?: run {
            emit(emptyList())
            return@flow
        }

        try {
            val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endTime = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val request = ReadRecordsRequest(
                recordType = DistanceRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )

            val response = client.readRecords(request)

            // Group by day
            val distanceByDay = response.records.groupBy { record ->
                record.startTime.atZone(ZoneId.systemDefault()).toLocalDate()
            }

            val distanceMetrics = distanceByDay.map { (date, records) ->
                val totalDistanceMeters = records.sumOf { it.distance.inMeters }

                DistanceMetric(
                    id = "distance-${date}",
                    userId = auth.currentUser?.uid ?: "unknown",
                    timestamp = date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    distanceMeters = totalDistanceMeters,
                    source = "Health Connect",
                    isShared = false
                )
            }

            if (distanceMetrics.isEmpty()) {
                emit(generateEmptyDistanceData(startDate, endDate))
            } else {
                emit(distanceMetrics)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading distance data", e)
            emit(generateEmptyDistanceData(startDate, endDate))
        }
    }

    /**
     * Get weight data for a date range
     */
    suspend fun getWeightData(startDate: LocalDate, endDate: LocalDate): Flow<List<WeightMetric>> = flow {
        val client = healthConnectClient ?: run {
            emit(emptyList())
            return@flow
        }

        try {
            val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endTime = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()

            val request = ReadRecordsRequest(
                recordType = WeightRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )

            val response = client.readRecords(request)

            val weightMetrics = response.records.map { record ->
                WeightMetric(
                    id = record.metadata.id,
                    userId = auth.currentUser?.uid ?: "unknown",
                    timestamp = record.time,
                    weightKg = record.weight.inKilograms,
                    source = record.metadata.dataOrigin.packageName ?: "Health Connect",
                    isShared = false
                )
            }

            if (weightMetrics.isEmpty()) {
                emit(generateEmptyWeightData(startDate, endDate))
            } else {
                emit(weightMetrics)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading weight data", e)
            emit(generateEmptyWeightData(startDate, endDate))
        }
    }

    /**
     * Get steps data for a specific time range
     * Returns the total number of steps in the time range
     */
    suspend fun getStepsData(timeRange: TimeRangeFilter): Long {
        val client = healthConnectClient ?: return 0L

        return try {
            // Check permissions first
            val permissions = client.permissionController.getGrantedPermissions()
            val hasStepsPermission = permissions.contains(HealthPermission.getReadPermission(StepsRecord::class))

            if (!hasStepsPermission) {
                Log.w(TAG, "Missing steps permission - using empty data")
                return 0L
            }

            // Read steps data
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = timeRange
            )

            val response = client.readRecords(request)
            val totalSteps = response.records.sumOf { it.count.toLong() }

            if (totalSteps <= 0) {
                Log.d(TAG, "No steps data found, using empty data")
                return 0L
            }

            totalSteps
        } catch (e: Exception) {
            Log.e(TAG, "Error reading steps data", e)
            0L
        }
    }

    /**
     * Get active minutes data for a specific time range
     * Returns the total active minutes in the time range as a Double
     */
    suspend fun getActiveMinutesData(timeRange: TimeRangeFilter): Double {
        return getActiveMinutesDataValue(timeRange)
    }
    
    /**
     * Get active minutes data value for a specific time range
     * Returns the total active minutes in the time range as a Double
     * @deprecated Use getActiveMinutesData instead
     */
    suspend fun getActiveMinutesDataValue(timeRange: TimeRangeFilter): Double {
        val client = healthConnectClient ?: return 0.0

        return try {
            // Create aggregate request for active minutes
            val activeMinutesRequest = AggregateRequest(
                metrics = setOf(ExerciseSessionRecord.EXERCISE_DURATION_TOTAL),
                timeRangeFilter = timeRange
            )

            val activeMinutesResponse = client.aggregate(activeMinutesRequest)
            val activeDuration = activeMinutesResponse[ExerciseSessionRecord.EXERCISE_DURATION_TOTAL] ?: Duration.ZERO
            val activeMinutes = activeDuration.toMinutes().toDouble()

            // If no exercise data, try to estimate from active calories
            if (activeMinutes == 0.0) {
                try {
                    val caloriesRequest = AggregateRequest(
                        metrics = setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL),
                        timeRangeFilter = timeRange
                    )

                    val caloriesResponse = client.aggregate(caloriesRequest)
                    val activeCalories = caloriesResponse[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inCalories ?: 0.0

                    // Estimate minutes based on calories (rough approximation: 10 calories = 1 active minute)
                    if (activeCalories > 0 && activeCalories < 10000) {
                        return activeCalories / 10.0
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error getting active calories data as fallback", e)
                }
            }

            Log.d(TAG, "Retrieved $activeMinutes active minutes from Health Connect")
            activeMinutes
        } catch (e: Exception) {
            Log.e(TAG, "Error getting active minutes data", e)
            0.0
        }
    }

    // Helper methods for empty data
    private fun createEmptyActiveMinutesMetric(timeRange: TimeRangeFilter): ActiveMinutesMetric {
        val startMillis = timeRange.startTime?.toEpochMilli() ?: 0L
        val endMillis = timeRange.endTime?.toEpochMilli() ?: 0L
        val midTime = Instant.ofEpochMilli(startMillis + ((endMillis - startMillis) / 2))

        Log.d(TAG, "Creating empty active minutes data for ${ZonedDateTime.ofInstant(midTime, ZoneId.systemDefault()).toLocalDate()}")

        return ActiveMinutesMetric(
            id = "empty-active-${midTime.toEpochMilli()}",
            userId = auth.currentUser?.uid ?: "unknown-user-id",
            timestamp = midTime,
            minutes = 0,
            intensity = ActiveMinutesMetric.ActivityIntensity.LIGHT,
            source = "No Data Available",
            isShared = false
        )
    }

    private fun createEmptyStepsMetric(timeRange: TimeRangeFilter): StepsMetric {
        val startMillis = timeRange.startTime?.toEpochMilli() ?: 0L
        val endMillis = timeRange.endTime?.toEpochMilli() ?: 0L
        val midTime = Instant.ofEpochMilli(startMillis + ((endMillis - startMillis) / 2))

        return StepsMetric(
            id = "empty-steps-${midTime.toEpochMilli()}",
            userId = auth.currentUser?.uid ?: "unknown",
            timestamp = midTime,
            count = 0,
            source = "No Data Available",
            isShared = false
        )
    }

    private fun generateEmptyStepsData(startDate: LocalDate, endDate: LocalDate): List<StepsMetric> {
        val result = mutableListOf<StepsMetric>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            result.add(
                StepsMetric(
                    id = "empty-steps-${currentDate}",
                    userId = auth.currentUser?.uid ?: "unknown",
                    timestamp = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    count = 0,
                    source = "No Data Available",
                    isShared = false
                )
            )
            currentDate = currentDate.plusDays(1)
        }

        return result
    }

    private fun generateEmptyDistanceData(startDate: LocalDate, endDate: LocalDate): List<DistanceMetric> {
        val result = mutableListOf<DistanceMetric>()
        var currentDate = startDate

        while (!currentDate.isAfter(endDate)) {
            result.add(
                DistanceMetric(
                    id = "empty-distance-${currentDate}",
                    userId = auth.currentUser?.uid ?: "unknown",
                    timestamp = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    distanceMeters = 0.0,
                    source = "No Data Available",
                    isShared = false
                )
            )
            currentDate = currentDate.plusDays(1)
        }

        return result
    }

    private fun generateEmptyWeightData(startDate: LocalDate, endDate: LocalDate): List<WeightMetric> {
        val result = mutableListOf<WeightMetric>()
        var currentDate = startDate

        // Generate one weight record per day
        while (!currentDate.isAfter(endDate)) {
            result.add(
                WeightMetric(
                    id = "empty-weight-${currentDate}",
                    userId = auth.currentUser?.uid ?: "unknown",
                    timestamp = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    weightKg = 0.0,
                    source = "No Data Available",
                    isShared = false
                )
            )
            currentDate = currentDate.plusDays(1)
        }

        return result
    }
}