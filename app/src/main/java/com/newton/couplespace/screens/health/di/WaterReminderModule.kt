package com.newton.couplespace.screens.health.di

import android.content.Context
import androidx.work.WorkManager
import com.newton.couplespace.screens.health.data.repository.NutritionRepository
import com.newton.couplespace.screens.health.service.WaterReminderManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

/**
 * Hilt module for providing water reminder related dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object WaterReminderModule {
    
    /**
     * Provides the application scope for long-running coroutines
     */
    @Singleton
    @Provides
    fun provideApplicationScope(): CoroutineScope {
        return CoroutineScope(SupervisorJob())
    }
    
    /**
     * Provides the WorkManager instance
     */
    @Singleton
    @Provides
    fun provideWorkManager(@ApplicationContext context: Context): WorkManager {
        return WorkManager.getInstance(context)
    }
    
    /**
     * Provides the WaterReminderManager
     */
    @Singleton
    @Provides
    fun provideWaterReminderManager(
        @ApplicationContext context: Context,
        nutritionRepository: NutritionRepository,
        applicationScope: CoroutineScope
    ): WaterReminderManager {
        return WaterReminderManager(context, nutritionRepository, applicationScope)
    }
}
