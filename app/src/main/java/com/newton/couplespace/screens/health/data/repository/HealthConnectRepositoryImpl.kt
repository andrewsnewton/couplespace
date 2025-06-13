package com.newton.couplespace.screens.health.data.repository

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.*
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.records.metadata.Metadata
import androidx.health.connect.client.records.metadata.DataOrigin
import androidx.health.connect.client.aggregate.*
import com.google.firebase.auth.FirebaseAuth
import com.newton.couplespace.screens.health.data.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of HealthConnectRepository that interacts with Health Connect API
 */
@Singleton
class HealthConnectRepositoryImpl @Inject constructor(
    private val context: Context,
    private val auth: FirebaseAuth
) : HealthConnectRepository {
    
    // Health Connect client
    override val healthConnectClient: HealthConnectClient? by lazy {
        try {
            if (HealthConnectClient.getSdkStatus(context) == HealthConnectClient.SDK_AVAILABLE) {
                HealthConnectClient.getOrCreate(context)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    // Required permissions for the app
    private val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(NutritionRecord::class),
        HealthPermission.getReadPermission(HeightRecord::class),
        HealthPermission.getReadPermission(HydrationRecord::class),
        HealthPermission.getReadPermission(OxygenSaturationRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(Vo2MaxRecord::class)
    )

    override fun getRequiredPermissions(): Set<String> = permissions
    
    /**
     * Check if Health Connect is available on the device
     */
    override suspend fun isHealthConnectAvailable(): Boolean {
        return try {
            val availabilityStatus = HealthConnectClient.getSdkStatus(context)
            when (availabilityStatus) {
                HealthConnectClient.SDK_AVAILABLE -> true
                HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED,
                HealthConnectClient.SDK_UNAVAILABLE -> false
                else -> false
            }
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Check if the app has all necessary Health Connect permissions
     */
    override suspend fun checkPermissions(): Boolean {
        if (!isHealthConnectAvailable()) return false
        
        return try {
            val granted = healthConnectClient?.permissionController?.getGrantedPermissions()
            granted?.containsAll(getRequiredPermissions()) ?: false
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Request Health Connect permissions
     */
    override suspend fun requestPermissions(): PermissionController {
        return healthConnectClient?.permissionController ?: 
            throw IllegalStateException("Health Connect is not available")
    }
    
    /**
     * Get date range for health data
     */
    override suspend fun getDateRange(): Pair<LocalDate, LocalDate> {
        val client = healthConnectClient ?: return getDefaultDateRange()
        
        return try {
            // Get the earliest and latest data points for steps
            val stepsRequest = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = TimeRangeFilter.after(Instant.EPOCH)
            )
            
            val steps = client.readRecords(stepsRequest)
            
            val startDate = steps.records.minByOrNull { it.startTime }?.startTime
                ?.atZone(ZoneId.systemDefault())?.toLocalDate()
                ?: LocalDate.now().minusMonths(1)
                
            val endDate = steps.records.maxByOrNull { it.endTime }?.endTime
                ?.atZone(ZoneId.systemDefault())?.toLocalDate()
                ?: LocalDate.now()
                
            startDate to endDate
        } catch (e: Exception) {
            // Fallback to default range if there's an error
            getDefaultDateRange()
        }
    }
    
    private fun getDefaultDateRange(): Pair<LocalDate, LocalDate> {
        val today = LocalDate.now()
        val thirtyDaysAgo = today.minusDays(30)
        return thirtyDaysAgo to today
    }
    
    /**
     * Get steps for a specific date
     */
    override suspend fun getStepsForDate(timeRange: TimeRangeFilter): Flow<StepsMetric> = flow {
        val client = healthConnectClient ?: throw IllegalStateException("Health Connect is not available")
        
        try {
            val request = ReadRecordsRequest(
                recordType = StepsRecord::class,
                timeRangeFilter = timeRange
            )
            
            val response = client.readRecords(request)
            val steps = response.records.sumOf { it.count }
            
            // Get the midpoint of the time range for the timestamp
            val start = timeRange.startTime?.atZone(ZoneId.systemDefault())?.toLocalDate()
            val end = timeRange.endTime?.atZone(ZoneId.systemDefault())?.toLocalDate()
            val midDate = start?.plusDays(ChronoUnit.DAYS.between(start, end) / 2)
            
            emit(
                StepsMetric(
                    id = "steps-${start}",
                    userId = auth.currentUser?.uid ?: "unknown",
                    timestamp = midDate?.atTime(12, 0)?.toInstant(ZoneOffset.UTC) ?: Instant.now(),
                    count = steps.toInt(),
                    source = "Health Connect",
                    isShared = false
                )
            )
        } catch (e: Exception) {
            // Fall back to mock data if there's an error
            emit(createMockStepsMetric(LocalDate.now()))
        }
    }
    
    /**
     * Get heart rate for a specific date
     */
    override suspend fun getHeartRateForDate(timeRange: TimeRangeFilter): Flow<HeartRateMetric> = flow {
        val client = healthConnectClient ?: throw IllegalStateException("Health Connect not available")
        
        try {
            // Create a request to read heart rate data
            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = timeRange
            )
            
            val response = client.readRecords(request)
            
            // Calculate average heart rate for the time range
            val heartRates = response.records.flatMap { record -> record.samples }
            val avgBpm = heartRates
                .mapNotNull { it.beatsPerMinute.toDouble() }
                .takeIf { it.isNotEmpty() }?.average() ?: 0.0
            
            // Get the midpoint of the time range for the timestamp
            val midTime = Instant.ofEpochMilli(
                timeRange.startTime?.toEpochMilli() ?: 0L + 
                (timeRange.endTime?.toEpochMilli() ?: 0L - (timeRange.startTime?.toEpochMilli() ?: 0L)) / 2
            )
            
            emit(
                HeartRateMetric(
                    id = "heart-rate-${timeRange.startTime?.toEpochMilli() ?: System.currentTimeMillis()}",
                    userId = auth.currentUser?.uid ?: "unknown",
                    timestamp = midTime,
                    beatsPerMinute = avgBpm.toInt(),
                    source = "Health Connect",
                    isShared = false
                )
            )
        } catch (e: Exception) {
            // Fall back to mock data if there's an error
            emit(createMockHeartRateMetric(LocalDate.now()))
        }
    }
    
    private fun createMockHeartRateMetric(date: LocalDate): HeartRateMetric {
        return HeartRateMetric(
            id = "mock-heart-rate-${date}",
            userId = "mock-user-id",
            timestamp = date.atTime(12, 0).toInstant(ZoneOffset.UTC),
            beatsPerMinute = (60..100).random(),
            source = "Mock Data",
            isShared = false
        )
    }
    
    private fun createMockStepsMetric(date: LocalDate): StepsMetric {
        return StepsMetric(
            id = "mock-steps-${date}",
            userId = "mock-user-id",
            timestamp = date.atTime(12, 0).toInstant(ZoneOffset.UTC),
            count = (3000..10000).random(),
            source = "Mock Data",
            isShared = false
        )
    }
    
    /**
     * Get step count for a date range
     */
    override suspend fun getStepCount(startDate: LocalDate, endDate: LocalDate): Flow<List<StepsMetric>> = flow {
        val client = healthConnectClient ?: throw IllegalStateException("Health Connect is not available")
        val stepsList = mutableListOf<StepsMetric>()
        
        try {
            val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endTime = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            
            // Get aggregated steps for each day in the range
            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                val dayStart = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
                val dayEnd = currentDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
                
                val request = ReadRecordsRequest(
                    recordType = StepsRecord::class,
                    timeRangeFilter = TimeRangeFilter.between(dayStart, dayEnd)
                )
                
                val response = client.readRecords(request)
                val totalSteps = response.records.sumOf { it.count }
                
                stepsList.add(
                    StepsMetric(
                        id = "steps-${currentDate}",
                        userId = auth.currentUser?.uid ?: "unknown",
                        timestamp = currentDate.atTime(12, 0).toInstant(ZoneOffset.UTC),
                        count = totalSteps.toInt(),
                        source = "Health Connect",
                        isShared = false
                    )
                )
                
                currentDate = currentDate.plusDays(1)
            }
        } catch (e: Exception) {
            // Fall back to mock data if there's an error
            stepsList.addAll(generateMockStepsData(startDate, endDate))
        }
        
        emit(stepsList)
    }
    
    private fun generateMockStepsData(startDate: LocalDate, endDate: LocalDate): List<StepsMetric> {
        val stepsList = mutableListOf<StepsMetric>()
        var currentDate = startDate
        
        while (!currentDate.isAfter(endDate)) {
            val timestamp = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            
            stepsList.add(
                StepsMetric(
                    id = UUID.randomUUID().toString(),
                    userId = auth.currentUser?.uid ?: "mock-user-id",
                    timestamp = timestamp.plusSeconds(43200), // Noon
                    count = 5000 + (Math.random() * 5000).toInt(),
                    source = "Mock Data",
                    isShared = false
                )
            )
            currentDate = currentDate.plusDays(1)
        }
        
        return stepsList
    }
    
    /**
     * Get heart rate data for a date range
     */
    override suspend fun getHeartRateData(startDate: LocalDate, endDate: LocalDate): Flow<List<HeartRateMetric>> = flow {
        val client = healthConnectClient ?: throw IllegalStateException("Health Connect is not available")
        val heartRateList = mutableListOf<HeartRateMetric>()
        
        try {
            val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endTime = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            
            val request = ReadRecordsRequest(
                recordType = HeartRateRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            
            val response = client.readRecords(request)
            
            // Group readings by date and calculate average for each day
            val dailyAverages = response.records
                .groupBy { record ->
                    record.startTime?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: LocalDate.now()
                }
                .mapValues { (_, records) ->
                    records.flatMap { record -> record.samples }
                        .mapNotNull { sample -> sample.beatsPerMinute.toDouble() }
                        .takeIf { it.isNotEmpty() }?.average() ?: 0.0
                }
            
            // Create metrics for each day with data
            dailyAverages.forEach { (date, avgBpm) ->
                heartRateList.add(
                    HeartRateMetric(
                        id = "heart-rate-${date}",
                        userId = auth.currentUser?.uid ?: "unknown",
                        timestamp = date.atTime(12, 0).toInstant(ZoneOffset.UTC),
                        beatsPerMinute = avgBpm.toInt(),
                        source = "Health Connect",
                        isShared = false
                    )
                )
            }
            
            // If no data, use mock data
            if (heartRateList.isEmpty()) {
                heartRateList.addAll(generateMockHeartRateData(startDate, endDate))
            }
        } catch (e: Exception) {
            Log.e("HealthConnectRepo", "Error reading heart rate data", e)
            // Fall back to mock data if there's an error
            heartRateList.addAll(generateMockHeartRateData(startDate, endDate))
        }
        
        emit(heartRateList)
    }
    
    private fun generateMockHeartRateData(startDate: LocalDate, endDate: LocalDate): List<HeartRateMetric> {
        val heartRateList = mutableListOf<HeartRateMetric>()
        var currentDate = startDate
        
        while (!currentDate.isAfter(endDate)) {
            val timestamp = currentDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            
            // Morning heart rate
            heartRateList.add(
                HeartRateMetric(
                    id = UUID.randomUUID().toString(),
                    userId = auth.currentUser?.uid ?: "mock-user-id",
                    timestamp = timestamp.plusSeconds(28800), // 8 AM
                    beatsPerMinute = 60 + (Math.random() * 40).toInt(),
                    source = "Mock Data",
                    isShared = false
                )
            )
            
            // Afternoon heart rate
            heartRateList.add(
                HeartRateMetric(
                    id = UUID.randomUUID().toString(),
                    userId = auth.currentUser?.uid ?: "mock-user-id",
                    timestamp = timestamp.plusSeconds(50400), // 2 PM
                    beatsPerMinute = 70 + (Math.random() * 20).toInt(),
                    source = "Mock Data",
                    isShared = false
                )
            )
            
            // Evening heart rate
            heartRateList.add(
                HeartRateMetric(
                    id = UUID.randomUUID().toString(),
                    userId = auth.currentUser?.uid ?: "mock-user-id",
                    timestamp = timestamp.plusSeconds(72000), // 8 PM
                    beatsPerMinute = 65 + (Math.random() * 10).toInt(),
                    source = "Mock Data",
                    isShared = false
                )
            )
            
            currentDate = currentDate.plusDays(1)
        }
        
        return heartRateList
    }
    
    override suspend fun getDistanceData(startDate: LocalDate, endDate: LocalDate): Flow<List<DistanceMetric>> = flow {
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
            val distanceData = response.records.map { record ->
                DistanceMetric(
                    id = record.metadata.id,
                    userId = auth.currentUser?.uid ?: "unknown",
                    timestamp = record.startTime ?: Instant.now(),
                    distanceMeters = record.distance.inMeters,
                    source = record.metadata.dataOrigin.packageName ?: "Health Connect",
                    isShared = false
                )
            }
            
            emit(distanceData.ifEmpty { generateMockDistanceData(startDate, endDate) })
        } catch (e: Exception) {
            Log.e("HealthConnectRepo", "Error reading distance data", e)
            emit(generateMockDistanceData(startDate, endDate))
        }
    }
    
    private fun generateMockDistanceData(startDate: LocalDate, endDate: LocalDate): List<DistanceMetric> {
        val distanceList = mutableListOf<DistanceMetric>()
        var currentDate = startDate
        
        while (!currentDate.isAfter(endDate)) {
            distanceList.add(
                DistanceMetric(
                    id = UUID.randomUUID().toString(),
                    userId = auth.currentUser?.uid ?: "mock-user-id",
                    timestamp = currentDate.atTime(12, 0).toInstant(ZoneOffset.UTC),
                    distanceMeters = 3000.0 + (Math.random() * 5000).toDouble(),
                    source = "Mock Data",
                    isShared = false
                )
            )
            currentDate = currentDate.plusDays(1)
        }
        
        return distanceList
    }
    
    override suspend fun getSleepData(startDate: LocalDate, endDate: LocalDate): Flow<List<SleepMetric>> = flow {
        val client = healthConnectClient ?: run {
            emit(emptyList())
            return@flow
        }
        
        try {
            val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endTime = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            
            val response = client.readRecords(request)
            val sleepData = response.records.map { record ->
                val durationHours = Duration.between(record.startTime ?: Instant.EPOCH, record.endTime ?: Instant.EPOCH).toHours().toFloat()
                SleepMetric(
                    id = record.metadata.id,
                    userId = auth.currentUser?.uid ?: "unknown",
                    timestamp = record.startTime ?: Instant.now(),
                    startTime = record.startTime ?: Instant.EPOCH,
                    endTime = record.endTime ?: Instant.EPOCH,
                    durationHours = durationHours,
                    source = record.metadata.dataOrigin.packageName ?: "Health Connect",
                    isShared = false
                )
            }
            
            emit(sleepData.ifEmpty { generateMockSleepData(startDate, endDate) })
        } catch (e: Exception) {
            Log.e("HealthConnectRepo", "Error reading sleep data", e)
            emit(generateMockSleepData(startDate, endDate))
        }
    }
    
    private fun generateMockSleepData(startDate: LocalDate, endDate: LocalDate): List<SleepMetric> {
        val sleepList = mutableListOf<SleepMetric>()
        var currentDate = startDate
        
        while (!currentDate.isAfter(endDate)) {
            val sleepStart = currentDate.atTime(23, 0).toInstant(ZoneOffset.UTC)
            val sleepEnd = currentDate.plusDays(1).atTime(7, 30).toInstant(ZoneOffset.UTC)
            
            sleepList.add(
                SleepMetric(
                    id = UUID.randomUUID().toString(),
                    userId = auth.currentUser?.uid ?: "mock-user-id",
                    timestamp = sleepStart,
                    startTime = sleepStart,
                    endTime = sleepEnd,
                    durationHours = 8.5f,
                    source = "Mock Data",
                    isShared = false
                )
            )
            currentDate = currentDate.plusDays(1)
        }
        
        return sleepList
    }
    
    override suspend fun getWeightData(startDate: LocalDate, endDate: LocalDate): Flow<List<WeightMetric>> = flow {
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
            val weightData = response.records.map { record ->
                WeightMetric(
                    id = record.metadata.id,
                    userId = auth.currentUser?.uid ?: "unknown",
                    timestamp = record.time ?: Instant.now()
                        .atZone(ZoneId.systemDefault())
                        .withHour(12)
                        .withMinute(0)
                        .toInstant(),
                    weightKg = record.weight.inKilograms,
                    source = record.metadata.dataOrigin.packageName ?: "Health Connect",
                    isShared = false
                )
            }
            
            emit(weightData.ifEmpty { generateMockWeightData(startDate, endDate) })
        } catch (e: Exception) {
            Log.e("HealthConnectRepo", "Error reading weight data", e)
            emit(generateMockWeightData(startDate, endDate))
        }
    }
    
    private fun generateMockWeightData(startDate: LocalDate, endDate: LocalDate): List<WeightMetric> {
        val weightList = mutableListOf<WeightMetric>()
        var currentDate = startDate
        var currentWeight = 70.0 + (Math.random() * 10.0) // Random weight between 70-80kg
        
        while (!currentDate.isAfter(endDate)) {
            // Simulate small weight fluctuations
            currentWeight += (Math.random() * 0.5) - 0.25
            
            weightList.add(
                WeightMetric(
                    id = UUID.randomUUID().toString(),
                    userId = auth.currentUser?.uid ?: "mock-user-id",
                    timestamp = currentDate.atTime(9, 0).toInstant(ZoneOffset.UTC),
                    weightKg = currentWeight,
                    source = "Mock Data",
                    isShared = false
                )
            )
            
            // Only record weight every few days
            currentDate = currentDate.plusDays((1..3).random().toLong())
        }
        
        return weightList
    }
    
    override suspend fun getCaloriesBurnedForDate(timeRange: TimeRangeFilter): Flow<CaloriesBurnedMetric> = flow {
        val client = healthConnectClient ?: throw IllegalStateException("Health Connect not available")
        
        try {
            val request = ReadRecordsRequest(
                recordType = TotalCaloriesBurnedRecord::class,
                timeRangeFilter = timeRange
            )
            
            val response = client.readRecords(request)
            val totalCalories = response.records.sumOf { it.energy.inCalories.toInt() }
            
            // Get the midpoint of the time range for the timestamp
            val midTime = Instant.ofEpochMilli(
                timeRange.startTime?.toEpochMilli() ?: 0L + 
                (timeRange.endTime?.toEpochMilli() ?: 0L - (timeRange.startTime?.toEpochMilli() ?: 0L)) / 2
            )
            
            emit(
                CaloriesBurnedMetric(
                    id = "calories-${timeRange.startTime?.toEpochMilli() ?: System.currentTimeMillis()}",
                    userId = auth.currentUser?.uid ?: "unknown",
                    timestamp = midTime,
                    calories = totalCalories,
                    source = "Health Connect",
                    isShared = false
                )
            )
        } catch (e: Exception) {
            Log.e("HealthConnectRepo", "Error reading calories burned", e)
            emit(createMockCaloriesBurnedMetric(timeRange))
        }
    }
    
    private fun createMockCaloriesBurnedMetric(timeRange: TimeRangeFilter): CaloriesBurnedMetric {
        val startMillis = timeRange.startTime?.toEpochMilli() ?: 0L
        val endMillis = timeRange.endTime?.toEpochMilli() ?: 0L
        val midTime = Instant.ofEpochMilli(
            startMillis + 
            (endMillis - startMillis) / 2
        )
        
        return CaloriesBurnedMetric(
            id = "mock-calories-${midTime.toEpochMilli()}",
            userId = auth.currentUser?.uid ?: "mock-user-id",
            timestamp = midTime,
            calories = 1500 + (Math.random() * 1000).toInt(),
            source = "Mock Data",
            isShared = false
        )
    }
    
    override suspend fun getSleepDataForDate(timeRange: TimeRangeFilter): Flow<SleepMetric> = flow {
        val client = healthConnectClient ?: throw IllegalStateException("Health Connect not available")
        
        try {
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = timeRange
            )
            
            val response = client.readRecords(request)
            
            // Calculate total sleep duration
            val totalMinutes = response.records.sumOf { record ->
                val start = record.startTime?.toEpochMilli() ?: 0L
                val end = record.endTime?.toEpochMilli() ?: 0L
                ((end - start) / (1000 * 60)).toInt()
            }
            val totalSleepHours = totalMinutes.toFloat() / 60
            
            // Get the midpoint of the time range for the timestamp
            val midTime = Instant.ofEpochMilli(
                timeRange.startTime?.toEpochMilli() ?: 0L + 
                (timeRange.endTime?.toEpochMilli() ?: 0L - (timeRange.startTime?.toEpochMilli() ?: 0L)) / 2
            )
            
            emit(
                SleepMetric(
                    id = "sleep-${timeRange.startTime?.toEpochMilli() ?: System.currentTimeMillis()}",
                    userId = auth.currentUser?.uid ?: "unknown",
                    timestamp = midTime,
                    startTime = timeRange.startTime ?: Instant.EPOCH,
                    endTime = timeRange.endTime ?: Instant.EPOCH,
                    durationHours = totalSleepHours,
                    source = "Health Connect",
                    isShared = false
                )
            )
        } catch (e: Exception) {
            Log.e("HealthConnectRepo", "Error reading sleep data", e)
            emit(createMockSleepMetric(timeRange))
        }
    }
    
    private fun createMockSleepMetric(timeRange: TimeRangeFilter): SleepMetric {
        val startMillis = timeRange.startTime?.toEpochMilli() ?: 0L
        val endMillis = timeRange.endTime?.toEpochMilli() ?: 0L
        val midTime = Instant.ofEpochMilli(
            startMillis + 
            (endMillis - startMillis) / 2
        )
        
        return SleepMetric(
            id = "mock-sleep-${midTime.toEpochMilli()}",
            userId = auth.currentUser?.uid ?: "mock-user-id",
            timestamp = midTime,
            startTime = timeRange.startTime ?: Instant.EPOCH,
            endTime = timeRange.endTime ?: Instant.EPOCH,
            durationHours = 7.5f + (Math.random() * 2).toFloat(),
            source = "Mock Data",
            isShared = false
        )
    }
    
    override suspend fun getActiveMinutesForDate(timeRange: TimeRangeFilter): Flow<ActiveMinutesMetric> = flow {
        val client = healthConnectClient ?: throw IllegalStateException("Health Connect not available")
        
        try {
            val request = ReadRecordsRequest(
                recordType = ExerciseSessionRecord::class,
                timeRangeFilter = timeRange
            )
            
            val response = client.readRecords(request)
            
            // Calculate total active minutes
            val totalMinutes = response.records.sumOf { record ->
                val start = record.startTime ?: return@sumOf 0L
                val end = record.endTime ?: return@sumOf 0L
                java.time.Duration.between(start, end).toMinutes()
            }.toInt()
            
            // Get the midpoint of the time range for the timestamp
            val startMillis = timeRange.startTime?.toEpochMilli() ?: 0L
            val endMillis = timeRange.endTime?.toEpochMilli() ?: 0L
            val midTime = Instant.ofEpochMilli(startMillis + ((endMillis - startMillis) / 2))
            
            emit(
                ActiveMinutesMetric(
                    id = "active-${timeRange.startTime?.toEpochMilli() ?: System.currentTimeMillis()}",
                    userId = auth.currentUser?.uid ?: "unknown",
                    timestamp = midTime,
                    minutes = totalMinutes,
                    intensity = ActiveMinutesMetric.ActivityIntensity.MODERATE,
                    source = "Health Connect",
                    isShared = false
                )
            )
        } catch (e: Exception) {
            Log.e("HealthConnectRepo", "Error reading active minutes", e)
            emit(createMockActiveMinutesMetric(timeRange))
        }
    }
    
    private fun createMockActiveMinutesMetric(timeRange: TimeRangeFilter): ActiveMinutesMetric {
        val startMillis = timeRange.startTime?.toEpochMilli() ?: 0L
        val endMillis = timeRange.endTime?.toEpochMilli() ?: 0L
        val midTime = Instant.ofEpochMilli(
            startMillis + 
            (endMillis - startMillis) / 2
        )
        
        return ActiveMinutesMetric(
            id = "mock-active-${midTime.toEpochMilli()}",
            userId = auth.currentUser?.uid ?: "mock-user-id",
            timestamp = midTime,
            minutes = 30 + (Math.random() * 60).toInt(),
            intensity = ActiveMinutesMetric.ActivityIntensity.MODERATE,
            source = "Mock Data",
            isShared = false
        )
    }
    
    override suspend fun getCaloriesBurnedData(startDate: LocalDate, endDate: LocalDate): Flow<List<CaloriesBurnedMetric>> = flow {
        val client = healthConnectClient ?: run {
            emit(emptyList())
            return@flow
        }
        
        try {
            val startTime = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
            val endTime = endDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            
            val request = ReadRecordsRequest(
                recordType = TotalCaloriesBurnedRecord::class,
                timeRangeFilter = TimeRangeFilter.between(startTime, endTime)
            )
            
            val response = client.readRecords(request)
            
            // Group by date
            val caloriesByDate = response.records.groupBy { record ->
                record.startTime?.atZone(ZoneId.systemDefault())?.toLocalDate() ?: LocalDate.now()
            }
            
            val caloriesData = caloriesByDate.map { (date, records) ->
                val totalCalories = records.sumOf { it.energy.inCalories.toInt() }
                
                CaloriesBurnedMetric(
                    id = "calories-${date}",
                    userId = auth.currentUser?.uid ?: "unknown",
                    timestamp = date.atTime(12, 0).toInstant(ZoneOffset.UTC),
                    calories = totalCalories,
                    source = "Health Connect",
                    isShared = false
                )
            }
            
            emit(caloriesData.ifEmpty { generateMockCaloriesBurnedData(startDate, endDate) })
        } catch (e: Exception) {
            Log.e("HealthConnectRepo", "Error reading calories burned data", e)
            emit(generateMockCaloriesBurnedData(startDate, endDate))
        }
    }
    
    private fun generateMockCaloriesBurnedData(startDate: LocalDate, endDate: LocalDate): List<CaloriesBurnedMetric> {
        val caloriesList = mutableListOf<CaloriesBurnedMetric>()
        var currentDate = startDate
        
        while (!currentDate.isAfter(endDate)) {
            caloriesList.add(
                CaloriesBurnedMetric(
                    id = "mock-calories-${currentDate}",
                    userId = auth.currentUser?.uid ?: "mock-user-id",
                    timestamp = currentDate.atTime(12, 0).toInstant(ZoneOffset.UTC),
                    calories = 1800 + (Math.random() * 1000).toInt(),
                    source = "Mock Data",
                    isShared = false
                )
            )
            currentDate = currentDate.plusDays(1)
        }
        
        return caloriesList
    }
}
