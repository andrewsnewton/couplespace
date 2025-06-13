package com.newton.couplespace.screens.health.data.remote

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Retrofit interface for USDA FoodData Central API
 * https://fdc.nal.usda.gov/api-spec/fdc_api.html
 */
interface FoodDataCentralApi {
    @GET("foods/search")
    suspend fun searchFoods(
        @Query("query") query: String,
        @Query("pageSize") pageSize: Int = 25,
        @Query("dataType") dataType: String = "SR Legacy,Survey (FNDDS),Foundation,Branded",
        @Query("api_key") apiKey: String = FOOD_DATA_API_KEY
    ): FoodSearchResponse
    
    @GET("food/{fdcId}")
    suspend fun getFoodDetails(
        @Path("fdcId") fdcId: String,
        @Query("api_key") apiKey: String = FOOD_DATA_API_KEY
    ): FoodDetailResponse
    
    companion object {
        // In a real app, this would be stored in BuildConfig, .env file, or fetched securely
        const val FOOD_DATA_API_KEY = "DEMO_KEY" // Replace with actual API key
        const val BASE_URL = "https://api.nal.usda.gov/fdc/v1/"
    }
}

/**
 * Response model for food search
 */
data class FoodSearchResponse(
    val totalHits: Int = 0,
    val currentPage: Int = 0,
    val totalPages: Int = 0,
    val foods: List<FoodSearchItem> = emptyList()
)

/**
 * Food item in search results
 */
data class FoodSearchItem(
    val fdcId: String = "",
    val description: String = "",
    val dataType: String = "",
    val brandName: String? = null,
    val brandOwner: String? = null,
    val ingredients: String? = null,
    val servingSize: Double? = null,
    val servingSizeUnit: String? = null,
    val foodNutrients: List<FoodNutrient> = emptyList()
)

/**
 * Nutrient information for a food
 */
data class FoodNutrient(
    val nutrientId: Int = 0,
    val nutrientName: String = "",
    val nutrientNumber: String = "",
    val unitName: String = "",
    val value: Double = 0.0
)

/**
 * Detailed response for a specific food
 */
data class FoodDetailResponse(
    val fdcId: String = "",
    val description: String = "",
    val dataType: String = "",
    val publicationDate: String = "",
    val foodNutrients: List<FoodNutrientDetail> = emptyList(),
    val ingredients: String? = null,
    val brandName: String? = null,
    val servingSize: Double? = null,
    val servingSizeUnit: String? = null
)

/**
 * Detailed nutrient information
 */
data class FoodNutrientDetail(
    val nutrient: Nutrient,
    val amount: Double = 0.0
)

/**
 * Nutrient information
 */
data class Nutrient(
    val id: Int = 0,
    val number: String = "",
    val name: String = "",
    val unitName: String = ""
)
