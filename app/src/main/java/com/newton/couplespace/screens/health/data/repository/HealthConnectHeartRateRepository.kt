package com.newton.couplespace.screens.health.data.repository

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.firebase.auth.FirebaseAuth
import com.newton.couplespace.screens.health.data.models.HeartRateMetric
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for heart rate-related Health Connect data
 */
@Singleton
class HealthConnectHeartRateRepository @Inject constructor(
    private val healthConnectClient: HealthConnectClient?,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "HealthConnectHeartRateRepo"
    }

    /**
     * Get heart rate for a specific date
     */
    suspend fun getHeartRateForDate(timeRange: TimeRangeFilter): Flow<HeartRateMetric> = flow {
        val client = healthConnectClient ?: throw IllegalStateException("Health Connect not available")
        
        try {
            // Check permissions first
            val permissions = client.permissionController.getGrantedPermissions()
            val hasHeartRatePermission = permissions.contains(HealthPermission.getReadPermission(HeartRateRecord::class))
            
            if (!hasHeartRatePermission) {
                Log.w(TAG, "Missing heart rate permission - using mock data")
                emit(createMockHeartRateMetric(timeRange))
                return@flow
            }
            
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
            
            if (avgBpm <= 0) {
                Log.d(TAG, "No heart rate data found, using mock data")
                emit(createMockHeartRateMetric(timeRange))
                return@flow
            }
            
            // Get the midpoint of the time range for the timestamp
            val startMillis = timeRange.startTime?.toEpochMilli() ?: 0L
            val endMillis = timeRange.endTime?.toEpochMilli() ?: 0L
            val midTime = Instant.ofEpochMilli(startMillis + ((endMillis - startMillis) / 2))
            
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
            Log.e(TAG, "Error reading heart rate data", e)
            // Fall back to mock data if there's an error
            emit(createMockHeartRateMetric(timeRange))
        }
    }
    
    /**
     * Get heart rate data for a date range
     */
    suspend fun getHeartRateData(startDate: LocalDate, endDate: LocalDate): Flow<List<HeartRateMetric>> = flow {
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
            Log.e(TAG, "Error reading heart rate data", e)
            // Fall back to mock data if there's an error
            heartRateList.addAll(generateMockHeartRateData(startDate, endDate))
        }
        
        emit(heartRateList)
    }
    
    private fun createMockHeartRateMetric(timeRange: TimeRangeFilter): HeartRateMetric {
        val startMillis = timeRange.startTime?.toEpochMilli() ?: 0L
        val endMillis = timeRange.endTime?.toEpochMilli() ?: 0L
        val midTime = Instant.ofEpochMilli(startMillis + ((endMillis - startMillis) / 2))
        
        return HeartRateMetric(
            id = "mock-heart-rate-${midTime.toEpochMilli()}",
            userId = auth.currentUser?.uid ?: "mock-user-id",
            timestamp = midTime,
            beatsPerMinute = (60..100).random(),
            source = "Mock Data",
            isShared = false
        )
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
}
