package com.newton.couplespace.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.CalendarViewMonth
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Timeline : BottomNavItem(
        route = Screen.Timeline.route,
        title = "Timeline",
        icon = Icons.Default.DateRange
    )
    
    object EnhancedTimeline : BottomNavItem(
        route = Screen.EnhancedTimeline.route,
        title = "Enhanced",
        icon = Icons.Default.CalendarViewMonth
    )
    
    object Health : BottomNavItem(
        route = Screen.Health.route,
        title = "Health",
        icon = Icons.Default.MonitorHeart
    )
    
    object Chat : BottomNavItem(
        route = Screen.Chat.route,
        title = "Chat",
        icon = Icons.AutoMirrored.Filled.Chat
    )
    
    object Profile : BottomNavItem(
        route = Screen.Profile.route,
        title = "Profile",
        icon = Icons.Default.Person
    )
}

@Composable
fun MainBottomNavigation(navController: NavController) {
    val items = listOf(
        BottomNavItem.Timeline,
        BottomNavItem.EnhancedTimeline,
        BottomNavItem.Health,
        BottomNavItem.Chat,
        BottomNavItem.Profile
    )
    
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        
        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                onClick = {
                    navController.navigate(item.route) {
                        // Pop up to the start destination of the graph to
                        // avoid building up a large stack of destinations
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        // Avoid multiple copies of the same destination when
                        // reselecting the same item
                        launchSingleTop = true
                        // Restore state when reselecting a previously selected item
                        restoreState = true
                    }
                }
            )
        }
    }
}

@Composable
fun MainScreenScaffold(
    navController: NavController,
    content: @Composable (Modifier) -> Unit
) {
    Scaffold(
        bottomBar = { MainBottomNavigation(navController) }
    ) { innerPadding ->
        content(Modifier.padding(innerPadding))
    }
}
