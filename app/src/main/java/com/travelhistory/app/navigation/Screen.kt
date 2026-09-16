package com.travelhistory.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Map
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : Screen(
        route = "home",
        title = "Home",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Map : Screen(
        route = "map",
        title = "Map",
        selectedIcon = Icons.Filled.Map,
        unselectedIcon = Icons.Outlined.Map
    )

    data object History : Screen(
        route = "history",
        title = "History",
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History
    )

    data object Settings : Screen(
        route = "settings",
        title = "Settings",
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings
    )

    data object OfflineMaps : Screen(
        route = "offline_maps",
        title = "Offline Maps",
        selectedIcon = Icons.Filled.Map,
        unselectedIcon = Icons.Outlined.Map
    )
}

val bottomNavScreens = listOf(
    Screen.Home,
    Screen.Map,
    Screen.History,
    Screen.Settings
)
