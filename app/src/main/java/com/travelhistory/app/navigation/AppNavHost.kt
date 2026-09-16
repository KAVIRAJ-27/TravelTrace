package com.travelhistory.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.travelhistory.app.ui.history.HistoryScreen
import com.travelhistory.app.ui.home.HomeScreen
import com.travelhistory.app.ui.map.MapScreen
import com.travelhistory.app.ui.settings.SettingsScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateToMap = {
                    navController.navigate(Screen.Map.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate(Screen.Settings.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
        composable(
            route = "${Screen.Map.route}?date={date}&tripId={tripId}",
            arguments = listOf(
                navArgument("date") {
                    type = NavType.LongType
                    defaultValue = -1L
                },
                navArgument("tripId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val dateArg = backStackEntry.arguments?.getLong("date") ?: -1L
            val tripIdArg = backStackEntry.arguments?.getLong("tripId") ?: -1L
            MapScreen(
                initialDateMillis = if (dateArg > 0) dateArg else null,
                initialTripId = if (tripIdArg > 0) tripIdArg else null,
                onNavigateToOfflineMaps = {
                    navController.navigate(Screen.OfflineMaps.route)
                }
            )
        }
        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateToMap = { selectedDateMillis ->
                    navController.navigate("${Screen.Map.route}?date=$selectedDateMillis&tripId=-1") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(Screen.Settings.route) {
            SettingsScreen(
                onNavigateToOfflineMaps = {
                    navController.navigate(Screen.OfflineMaps.route)
                }
            )
        }
        composable(Screen.OfflineMaps.route) {
            com.travelhistory.app.ui.map.offline.OfflineMapsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
