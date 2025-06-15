package com.newton.couplespace.di

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import com.newton.couplespace.screens.health.data.repository.*
import dagger.Module
import dagger.Provides
import dagger.Binds
import dagger.Lazy
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.newton.couplespace.screens.health.data.remote.FoodDataCentralApi

/**
 * Provides repository instances for dependency injection
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth {
        return FirebaseAuth.getInstance()
    }

    @Provides
    @Singleton
    fun provideFirebaseFirestore(): FirebaseFirestore {
        return FirebaseFirestore.getInstance()
    }
    
    @Provides
    @Singleton
    fun provideHealthConnectClient(@ApplicationContext context: Context): HealthConnectClient? {
        return try {
            HealthConnectClient.getOrCreate(context)
        } catch (e: Exception) {
            null
        }
    }
    
    @Provides
    @Singleton
    fun provideHealthConnectCaloriesRepository(
        healthConnectClient: HealthConnectClient?,
        auth: FirebaseAuth
    ): HealthConnectCaloriesRepository {
        return HealthConnectCaloriesRepository(healthConnectClient, auth)
    }
    
    @Provides
    @Singleton
    fun provideHealthConnectActivityRepository(
        healthConnectClient: HealthConnectClient?,
        auth: FirebaseAuth
    ): HealthConnectActivityRepository {
        return HealthConnectActivityRepository(healthConnectClient, auth)
    }
    
    @Provides
    @Singleton
    fun provideHealthConnectHeartRateRepository(
        healthConnectClient: HealthConnectClient?,
        auth: FirebaseAuth
    ): HealthConnectHeartRateRepository {
        return HealthConnectHeartRateRepository(healthConnectClient, auth)
    }
    
    @Provides
    @Singleton
    fun provideHealthConnectSleepRepository(
        healthConnectClient: HealthConnectClient?,
        auth: FirebaseAuth
    ): HealthConnectSleepRepository {
        return HealthConnectSleepRepository(healthConnectClient, auth)
    }

    @Provides
    @Singleton
    fun provideHealthConnectRepository(
        @ApplicationContext context: Context,
        auth: FirebaseAuth,
        caloriesRepository: Lazy<HealthConnectCaloriesRepository>,
        activityRepository: Lazy<HealthConnectActivityRepository>,
        heartRateRepository: Lazy<HealthConnectHeartRateRepository>,
        sleepRepository: Lazy<HealthConnectSleepRepository>
    ): HealthConnectRepository {
        return HealthConnectRepositoryImpl(
            context,
            auth,
            caloriesRepository,
            activityRepository,
            heartRateRepository,
            sleepRepository
        )
    }

    @Provides
    @Singleton
    fun provideNutritionRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        foodApi: FoodDataCentralApi
    ): NutritionRepository {
        return NutritionRepositoryImpl(firestore, auth, foodApi)
    }

    @Provides
    @Singleton
    fun provideCoupleHealthRepository(
        firestore: FirebaseFirestore,
        auth: FirebaseAuth,
        healthConnectRepository: HealthConnectRepository
    ): CoupleHealthRepository {
        return CoupleHealthRepositoryImpl(firestore, auth, healthConnectRepository)
    }
}
