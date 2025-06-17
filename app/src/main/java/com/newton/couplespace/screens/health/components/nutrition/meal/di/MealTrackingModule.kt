package com.newton.couplespace.screens.health.components.nutrition.meal.di

import android.content.Context
import androidx.room.Room
import com.newton.couplespace.screens.health.components.nutrition.meal.data.FoodDataCentralApiService
import com.newton.couplespace.screens.health.components.nutrition.meal.data.FoodItemDao
import com.newton.couplespace.screens.health.components.nutrition.meal.data.LocalFoodRepository
import com.newton.couplespace.screens.health.components.nutrition.meal.data.FoodSearchRepository
import com.newton.couplespace.screens.health.components.nutrition.meal.data.MealEntryDao
import com.newton.couplespace.screens.health.components.nutrition.meal.data.NutritionApiService
import com.newton.couplespace.screens.health.components.nutrition.meal.data.LocalFoodDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for providing meal tracking dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object MealTrackingModule {

    /**
     * Provides the Room database for local food data storage
     */
    @Provides
    @Singleton
    fun provideLocalFoodDatabase(@ApplicationContext context: Context): LocalFoodDatabase {
        return Room.databaseBuilder(
            context,
            LocalFoodDatabase::class.java,
            "food_database"
        )
        .fallbackToDestructiveMigration() // For simplicity during development
        .build()
    }

    /**
     * Provides the FoodItemDao for accessing food items in the database
     */
    @Provides
    @Singleton
    fun provideFoodItemDao(database: LocalFoodDatabase): FoodItemDao {
        return database.foodItemDao()
    }

    /**
     * Provides the MealEntryDao for accessing meal entries in the database
     */
    @Provides
    @Singleton
    fun provideMealEntryDao(database: LocalFoodDatabase): MealEntryDao {
        return database.mealEntryDao()
    }

    /**
     * Provides the NutritionApiService for fetching nutrition data from external APIs
     * Currently using a mock implementation
     */
    @Provides
    @Singleton
    fun provideNutritionApiService(): NutritionApiService {
        // For now, use the mock implementation
        // This can be replaced with a real implementation later
        return FoodDataCentralApiService(apiKey = "")
    }

    /**
     * Provides the FoodRepository for accessing food data
     */
    @Provides
    @Singleton
    fun provideFoodRepository(
        foodItemDao: FoodItemDao,
        nutritionApiService: NutritionApiService
    ): LocalFoodRepository {
        return LocalFoodRepository(foodItemDao, nutritionApiService)
    }

    /**
     * Provides the FoodSearchRepository for searching and managing food data
     */
    @Provides
    @Singleton
    fun provideFoodSearchRepository(
        foodRepository: LocalFoodRepository,
        mealEntryDao: MealEntryDao
    ): FoodSearchRepository {
        return FoodSearchRepository(
            foodRepository = foodRepository,
            mealEntryDao = mealEntryDao
        )
    }
}
