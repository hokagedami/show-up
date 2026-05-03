package com.codekage.showup.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.ui.graphics.vector.ImageVector

data class BottomNavItem(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val route: Screen,
)

val bottomNavItems = listOf(
    BottomNavItem("Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, Screen.Dashboard),
    BottomNavItem("Calendar", Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth, Screen.Calendar),
    BottomNavItem("Reports", Icons.Filled.BarChart, Icons.Outlined.BarChart, Screen.Reports),
)
