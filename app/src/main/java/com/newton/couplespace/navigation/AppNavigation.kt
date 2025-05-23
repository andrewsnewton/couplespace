package com.newton.couplespace.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.newton.couplespace.screens.auth.LoginScreen
import com.newton.couplespace.screens.main.ChatScreen
import com.newton.couplespace.screens.main.HealthScreen
import com.newton.couplespace.screens.main.ProfileScreen
import com.newton.couplespace.screens.main.TimelineScreen
import com.newton.couplespace.screens.main.timeline.EnhancedTimelineScreen
import com.newton.couplespace.screens.onboarding.CoupleSetupScreen
import com.newton.couplespace.screens.onboarding.UserSetupScreen
import com.newton.couplespace.screens.onboarding.WelcomeScreen

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Welcome : Screen("welcome")
    object UserSetup : Screen("user_setup")
    object CoupleSetup : Screen("couple_setup")
    object Timeline : Screen("timeline")
    object EnhancedTimeline : Screen("enhanced_timeline")
    object Health : Screen("health")
    object Chat : Screen("chat")
    object Profile : Screen("profile")
}

@Composable
fun AppNavigation(navController: NavHostController, startDestination: String = Screen.Login.route) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Auth and onboarding flow
        composable(Screen.Login.route) {
            LoginScreen(navController = navController)
        }
        
        composable(Screen.Welcome.route) {
            WelcomeScreen(navController = navController)
        }
        composable(Screen.UserSetup.route) {
            UserSetupScreen(navController = navController)
        }
        composable(Screen.CoupleSetup.route) {
            CoupleSetupScreen(navController = navController)
        }
        
        // Main screens
        composable(Screen.Timeline.route) {
            TimelineScreen(navController = navController)
        }
        composable(Screen.EnhancedTimeline.route) {
            EnhancedTimelineScreen(navController = navController)
        }
        composable(Screen.Health.route) {
            HealthScreen(navController = navController)
        }
        composable(Screen.Chat.route) {
            ChatScreen(navController = navController)
        }
        composable(Screen.Profile.route) {
            ProfileScreen(navController = navController)
        }
    }
}
