package com.bjwag.mensaminus.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val icon: ImageVector) {
    object Meals : Screen("meals", Icons.Default.RestaurantMenu)
    object Canteens : Screen("canteens", Icons.Default.Home)
    object Settings : Screen("settings", Icons.Default.Settings)
    object CanteenDetail : Screen("canteen/{canteenId}", Icons.Default.Home) {
        fun createRoute(canteenId: Int) = "canteen/$canteenId"
    }
}