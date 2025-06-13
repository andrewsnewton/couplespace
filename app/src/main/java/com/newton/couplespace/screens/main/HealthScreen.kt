@file:OptIn(ExperimentalMaterial3Api::class)
package com.newton.couplespace.screens.main

import android.util.Log
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import com.newton.couplespace.screens.health.HealthScreen
import com.newton.couplespace.screens.health.viewmodel.CoupleHealthViewModel
import com.newton.couplespace.screens.health.viewmodel.HealthViewModel
import com.newton.couplespace.screens.health.viewmodel.NutritionViewModel

private const val TAG = "HealthScreenWrapper"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen(navController: NavController = rememberNavController()) {
    // Log navigation state
    LaunchedEffect(Unit) {
        Log.d(TAG, "HealthScreen Composable started")
    }
    
    // Add detailed logging before each ViewModel initialization
    Log.d(TAG, "Attempting to initialize HealthViewModel")
    val healthViewModel: HealthViewModel = hiltViewModel()
    Log.d(TAG, "HealthViewModel initialized successfully")
    
    Log.d(TAG, "Attempting to initialize NutritionViewModel")
    val nutritionViewModel: NutritionViewModel = hiltViewModel()
    Log.d(TAG, "NutritionViewModel initialized successfully")
    
    Log.d(TAG, "Attempting to initialize CoupleHealthViewModel")
    val coupleHealthViewModel: CoupleHealthViewModel = hiltViewModel()
    Log.d(TAG, "CoupleHealthViewModel initialized successfully")
    
    Log.d(TAG, "All ViewModels initialized, calling main HealthScreen implementation")
    // Call the main implementation
    com.newton.couplespace.screens.health.HealthScreen(
        onBackClick = { navController.navigateUp() },
        healthViewModel = healthViewModel,
        nutritionViewModel = nutritionViewModel,
        coupleHealthViewModel = coupleHealthViewModel
    )
}
