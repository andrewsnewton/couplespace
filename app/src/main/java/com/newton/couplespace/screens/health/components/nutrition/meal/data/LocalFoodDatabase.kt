package com.newton.couplespace.screens.health.components.nutrition.meal.data

import androidx.room.*
import com.newton.couplespace.screens.health.data.models.FoodItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDateTime
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Room entity for storing food items locally
 */
@Entity(tableName = "food_items")
data class FoodItemEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val servingSize: String,
    val calories: Int,
    val protein: Float,
    val carbs: Float,
    val fat: Float,
    val fiber: Float = 0f,
    val sugar: Float = 0f,
    val sodium: Float = 0f,
    val imageUrl: String? = null,
    val category: String = "",
    val isFavorite: Boolean = false,
    val barcode: String? = null,
    val isCustom: Boolean = false,
    val lastUsed: Long = System.currentTimeMillis()
)

/**
 * Room entity for storing meal entries
 */
@Entity(tableName = "meal_entries")
data class MealEntryEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val categoryId: String,
    val dateTime: Long = System.currentTimeMillis(),
    val notes: String = "",
    val imageUri: String? = null,
    val isFavorite: Boolean = false
)

/**
 * Cross-reference table for meal entries and food items
 */
@Entity(
    tableName = "meal_food_cross_ref",
    primaryKeys = ["mealId", "foodId"], // This composite PK already creates an index
    foreignKeys = [
        ForeignKey(
            entity = MealEntryEntity::class,
            parentColumns = ["id"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = FoodItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["foodId"])] // <--- ADD THIS INDEX explicitly
    // Although the composite primary key (mealId, foodId)
    // can often serve queries on foodId if it's the leading column
    // or if the DB optimizer is smart, Room's lint is specific here.
    // An index on just (foodId) can be beneficial for lookups
    // primarily filtering/joining on foodId.
)
data class MealFoodCrossRef(
    val mealId: String,
    // @ColumnInfo(index = true) // This is another way to add an index to a single column
    val foodId: String,
    val portionMultiplier: Double = 1.0
)

/**
 * Data class representing a meal with its food items
 */
class MealWithFoods {
    @Embedded
    lateinit var meal: MealEntryEntity

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = MealFoodCrossRef::class,
            parentColumn = "mealId",
            entityColumn = "foodId"
        )
    )
    var foods: List<FoodItemEntity> = emptyList()

    constructor() // Required empty constructor for Room

    @Ignore // Tell Room to ignore this constructor for its instantiation purposes
    constructor(meal: MealEntryEntity, foods: List<FoodItemEntity>) {
        this.meal = meal
        this.foods = foods
    }
}

/**
 * Data class representing a food item with its portion multiplier
 */
@DatabaseView("SELECT f.*, m.mealId, m.portionMultiplier FROM food_items f JOIN meal_food_cross_ref m ON f.id = m.foodId")
data class FoodItemWithPortion(
    @Embedded val foodItem: FoodItemEntity,
    val mealId: String,
    val portionMultiplier: Double
) {
    // Default no-arg constructor required by Room
    constructor() : this(FoodItemEntity("", "", "", 0, 0f, 0f, 0f), "", 1.0)
}

/**
 * DAO for food items
 */
@Dao
interface FoodItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItem(foodItem: FoodItemEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFoodItems(foodItems: List<FoodItemEntity>)

    @Update
    suspend fun updateFoodItem(foodItem: FoodItemEntity)

    @Delete
    suspend fun deleteFoodItem(foodItem: FoodItemEntity)

    @Query("SELECT * FROM food_items WHERE id = :id")
    suspend fun getFoodItemById(id: String): FoodItemEntity?

    @Query("SELECT * FROM food_items WHERE barcode = :barcode")
    suspend fun getFoodItemByBarcode(barcode: String): FoodItemEntity?

    @Query("SELECT * FROM food_items WHERE name LIKE '%' || :query || '%' ORDER BY lastUsed DESC LIMIT :limit")
    suspend fun searchFoodItems(query: String, limit: Int = 20): List<FoodItemEntity>

    @Query("SELECT * FROM food_items WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteFoodItems(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items WHERE isCustom = 1 ORDER BY name ASC")
    fun getCustomFoodItems(): Flow<List<FoodItemEntity>>

    @Query("SELECT * FROM food_items ORDER BY lastUsed DESC LIMIT :limit")
    fun getRecentFoodItems(limit: Int = 20): Flow<List<FoodItemEntity>>
}

/**
 * DAO for meal entries
 */
@Dao
interface MealEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealEntry(mealEntry: MealEntryEntity): Long

    @Update
    suspend fun updateMealEntry(mealEntry: MealEntryEntity)

    @Delete
    suspend fun deleteMealEntry(mealEntry: MealEntryEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealFoodCrossRef(crossRef: MealFoodCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMealFoodCrossRefs(crossRefs: List<MealFoodCrossRef>)

    @Delete
    suspend fun deleteMealFoodCrossRef(crossRef: MealFoodCrossRef)

    @Query("DELETE FROM meal_food_cross_ref WHERE mealId = :mealId")
    suspend fun deleteMealFoodCrossRefsByMealId(mealId: String)

    @Transaction
    @Query("SELECT * FROM meal_entries WHERE id = :id")
    suspend fun getMealWithFoodsById(id: String): MealWithFoods?

    @Transaction
    @Query("SELECT * FROM meal_entries WHERE dateTime BETWEEN :startTime AND :endTime ORDER BY dateTime ASC")
    fun getMealsForDateRange(startTime: Long, endTime: Long): Flow<List<MealWithFoods>>

    @Transaction
    @Query("SELECT * FROM meal_entries WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteMeals(): Flow<List<MealWithFoods>>

    @Transaction
    @Query("SELECT * FROM FoodItemWithPortion WHERE mealId = :mealId")
    fun getFoodItemsWithPortionForMealList(mealId: String): List<FoodItemWithPortion> // Non-Flow for synchronous call within map
}

/**
 * Room database for food and meal data
 */
@Database(
    entities = [
        FoodItemEntity::class,
        MealEntryEntity::class,
        MealFoodCrossRef::class
    ],
    views = [
        FoodItemWithPortion::class
    ],
    version = 1,
    exportSchema = false
)
abstract class LocalFoodDatabase : RoomDatabase() {
    abstract fun foodItemDao(): FoodItemDao
    abstract fun mealEntryDao(): MealEntryDao

    companion object {
        @Volatile
        private var INSTANCE: LocalFoodDatabase? = null
        const val DATABASE_NAME = "local_food_database"

        fun getInstance(context: android.content.Context): LocalFoodDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = androidx.room.Room.databaseBuilder(
                    context.applicationContext,
                    LocalFoodDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

/**
 * Extension functions to convert between Room entities and domain models
 */
fun FoodItemEntity.toDomainModel(): FoodItem {
    return FoodItem(
        id = id,
        name = name,
        servingSize = servingSize,
        calories = calories,
        protein = protein,
        carbs = carbs,
        fat = fat,
        fiber = fiber,
        sugar = sugar,
        sodium = sodium,
        imageUrl = imageUrl,
        category = category,
        isFavorite = isFavorite,
        barcode = barcode,
        lastUsed = lastUsed
    )
}

fun FoodItem.toEntity(isCustom: Boolean = false): FoodItemEntity {
    return FoodItemEntity(
        id = id,
        name = name,
        servingSize = servingSize,
        calories = calories,
        protein = protein,
        carbs = carbs,
        fat = fat,
        fiber = fiber,
        sugar = sugar,
        sodium = sodium,
        imageUrl = imageUrl,
        category = category,
        isFavorite = isFavorite,
        barcode = barcode,
        isCustom = isCustom,
        lastUsed = lastUsed
    )
}

/**
 * Repository for managing food data from both local database and remote API
 */
@Singleton
class LocalFoodRepository @Inject constructor(private val foodItemDao: FoodItemDao,
    private val nutritionApiService: NutritionApiService
) {
    /**
     * Search for food items by name, combining results from local database and remote API
     */
    suspend fun searchFoodByName(query: String, limit: Int = 20): List<FoodItem> {
        // First search local database
        val localResults = foodItemDao.searchFoodItems(query, limit)
            .map { it.toDomainModel() }

        // If we have enough results from local database, return them
        if (localResults.size >= limit) {
            return localResults
        }

        // Otherwise, search remote API and cache results
        try {
            val remoteResults = nutritionApiService.searchFoodByName(
                query = query,
                pageSize = limit - localResults.size
            )

            // Cache remote results in local database
            foodItemDao.insertFoodItems(remoteResults.map { it.toEntity() })

            // Combine local and remote results, removing duplicates
            return (localResults + remoteResults)
                .distinctBy { it.id }
                .take(limit)
        } catch (e: Exception) {
            // If remote API fails, return local results only
            return localResults
        }
    }

    /**
     * Get food item by barcode, checking local database first then remote API
     */
    suspend fun getFoodByBarcode(barcode: String): FoodItem? {
        // First check local database
        val localResult = foodItemDao.getFoodItemByBarcode(barcode)?.toDomainModel()

        if (localResult != null) {
            return localResult
        }

        // If not found locally, check remote API and cache result
        try {
            val remoteResult = nutritionApiService.getFoodByBarcode(barcode)

            if (remoteResult != null) {
                foodItemDao.insertFoodItem(remoteResult.toEntity())
            }

            return remoteResult
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Get food item details by ID, checking local database first then remote API
     */
    suspend fun getFoodDetails(foodId: String): FoodItem? {
        // First check local database
        val localResult = foodItemDao.getFoodItemById(foodId)?.toDomainModel()

        if (localResult != null) {
            return localResult
        }

        // If not found locally, check remote API and cache result
        try {
            val remoteResult = nutritionApiService.getFoodDetails(foodId)

            if (remoteResult != null) {
                foodItemDao.insertFoodItem(remoteResult.toEntity())
            }

            return remoteResult
        } catch (e: Exception) {
            return null
        }
    }

    /**
     * Save a custom food item to the local database
     */
    suspend fun saveCustomFoodItem(foodItem: FoodItem) {
        foodItemDao.insertFoodItem(foodItem.toEntity(isCustom = true))
    }

    /**
     * Update a food item in the local database
     */
    suspend fun updateFoodItem(foodItem: FoodItem) {
        foodItemDao.updateFoodItem(foodItem.toEntity())
    }

    /**
     * Delete a food item from the local database
     */
    suspend fun deleteFoodItem(foodItem: FoodItem) {
        foodItemDao.deleteFoodItem(foodItem.toEntity())
    }

    /**
     * Get favorite food items from the local database
     */
    fun getFavoriteFoodItems(): Flow<List<FoodItem>> {
        return foodItemDao.getFavoriteFoodItems()
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    /**
     * Get custom food items from the local database
     */
    fun getCustomFoodItems(): Flow<List<FoodItem>> {
        return foodItemDao.getCustomFoodItems()
            .map { entities -> entities.map { it.toDomainModel() } }
    }

    /**
     * Get recent food items from the local database
     */
    fun getRecentFoodItems(limit: Int = 20): Flow<List<FoodItem>> {
        return foodItemDao.getRecentFoodItems(limit)
            .map { entities -> entities.map { it.toDomainModel() } }
    }
}
