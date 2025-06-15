package com.newton.couplespace.screens.health.service

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.newton.couplespace.screens.health.data.repository.NutritionRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory for creating WaterReminderWorker instances with proper dependency injection
 */
@Singleton
class WaterReminderWorkerFactory @Inject constructor(
    private val nutritionRepository: NutritionRepository
) : WorkerFactory() {
    
    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when(workerClassName) {
            WaterReminderWorker::class.java.name -> 
                WaterReminderWorker(appContext, workerParameters, nutritionRepository)
            else -> null
        }
    }
}
