package com.newton.couplespace.di

import android.content.Context
import com.newton.couplespace.screens.health.data.repository.CoupleHealthRepository
import com.newton.couplespace.screens.health.data.repository.CoupleHealthRepositoryImpl
import com.newton.couplespace.screens.health.data.repository.HealthConnectRepository
import com.newton.couplespace.screens.health.data.repository.HealthConnectRepositoryImpl
import com.newton.couplespace.screens.health.data.repository.NutritionRepository
import com.newton.couplespace.screens.health.data.repository.NutritionRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.Binds
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
    fun provideHealthConnectRepository(
        @ApplicationContext context: Context,
        auth: FirebaseAuth
    ): HealthConnectRepository {
        return HealthConnectRepositoryImpl(context, auth)
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
