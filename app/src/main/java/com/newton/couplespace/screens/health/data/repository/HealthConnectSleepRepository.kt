package com.newton.couplespace.screens.health.data.repository

import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.google.firebase.auth.FirebaseAuth
import com.newton.couplespace.screens.health.data.models.SleepMetric
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for sleep-related Health Connect data
 */
@Singleton
class HealthConnectSleepRepository @Inject constructor(
    private val healthConnectClient: HealthConnectClient?,
    private val auth: FirebaseAuth
) {
    companion object {
        private const val TAG = "HealthConnectSleepRepo"
    }

    /**
     * Get sleep data for a specific date
     */
    suspend fun getSleepDataForDate(timeRange: TimeRangeFilter): Flow<SleepMetric> = flow {
        val client = healthConnectClient ?: throw IllegalStateException("Health Connect not available")
        
        try {
            // Check permissions first
            val permissions = client.permissionController.getGrantedPermissions()
            val hasSleepPermission = permissions.contains(HealthPermission.getReadPermission(SleepSessionRecord::class))
            
            if (!hasSleepPermission) {
                Log.w(TAG, "Missing sleep permission - using mock data")
                emit(createMockSleepMetric(timeRange))
                return@flow
            }
            
            val request = ReadRecordsRequest(
                recordType = SleepSessionRecord::class,
                timeRangeFilter = timeRange
            )
            
            val response = client.readRecords(request)
            Log.d(TAG, "Found ${response.records.size} sleep records")
            
            // If we have sleep records, use the longest one
            if (response.records.isNotEmpty()) {
                val sleepRecord = response.records.maxByOrNull { record ->
                    val start = record.startTime ?: Instant.EPOCH
                    val end = record.endTime ?: Instant.now()
                    Duration.between(start, end).toMinutes()
                }
                
                if (sleepRecord != null && sleepRecord.startTime != null && sleepRecord.endTime != null) {
                    val durationHours = Duration.between(sleepRecord.startTime, sleepRecord.endTime).toMinutes() / 60.0f
                    
                    emit(SleepMetric(
                        id = sleepRecord.metadata.id,
                        userId = auth.currentUser?.uid ?: "unknown",
                        timestamp = sleepRecord.startTime!!,
                        startTime = sleepRecord.startTime!!,
                        endTime = sleepRecord.endTime!!,
                        durationHours = durationHours,
                        source = sleepRecord.metadata.dataOrigin.packageName ?: "Health Connect",
                        isShared = false
                    ))
                    return@flow
                }
            }
            
            // If no valid records found, use mock data
            Log.d(TAG, "No valid sleep records found, using mock data")
            emit(createMockSleepMetric(timeRange))
        } catch (e: Exception) {
            Log.e(TAG, "Error reading sleep data", e)
            emit(createMockSleepMetric(timeRange))
        }
    }
    
    /**
     * Get sleep data for a date range
     */
    suspend fun getSleepData(startDate: LocalDate, endDate: LocalDate): Flow<List<SleepMetric>> = flow {
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
                val durationHours = if (record.startTime != null && record.endTime != null) {
                    Duration.between(record.startTime, record.endTime).toMinutes() / 60.0f
                } else {
                    0.0f
                }
                
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
            }.filter { it.durationHours > 0 } // Filter out invalid records
            
            emit(sleepData.ifEmpty { generateMockSleepData(startDate, endDate) })
        } catch (e: Exception) {
            Log.e(TAG, "Error reading sleep data", e)
            emit(generateMockSleepData(startDate, endDate))
        }
    }
    
    private fun createMockSleepMetric(timeRange: TimeRangeFilter): SleepMetric {
        val startMillis = timeRange.startTime?.toEpochMilli() ?: 0L
        val endMillis = timeRange.endTime?.toEpochMilli() ?: 0L
        val midTime = Instant.ofEpochMilli(startMillis + ((endMillis - startMillis) / 2))
        
        // Create a sleep session that starts at 11 PM and ends at 7 AM
        val sleepStart = midTime.atZone(ZoneId.systemDefault())
            .withHour(23)
            .withMinute(0)
            .withSecond(0)
            .toInstant()
        
        val sleepEnd = midTime.atZone(ZoneId.systemDefault())
            .plusDays(1)
            .withHour(7)
            .withMinute(0)
            .withSecond(0)
            .toInstant()
        
        val durationHours = Duration.between(sleepStart, sleepEnd).toMinutes() / 60.0f
        
        return SleepMetric(
            id = "mock-sleep-${midTime.toEpochMilli()}",
            userId = auth.currentUser?.uid ?: "mock-user-id",
            timestamp = sleepStart,
            startTime = sleepStart,
            endTime = sleepEnd,
            durationHours = durationHours,
            source = "Mock Data",
            isShared = false
        )
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
}
