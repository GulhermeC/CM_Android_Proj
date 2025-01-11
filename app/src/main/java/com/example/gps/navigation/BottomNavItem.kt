package com.example.gps.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.graphics.vector.ImageVector

sealed class BottomNavItem(val route: String, val label: String, val icon: ImageVector) {
    object Home : BottomNavItem("home", "Home", Icons.Filled.Home)
    object Map : BottomNavItem("map", "Map", Icons.Filled.LocationOn)
    object Profile : BottomNavItem("profile", "Profile", Icons.Filled.Person)
}
