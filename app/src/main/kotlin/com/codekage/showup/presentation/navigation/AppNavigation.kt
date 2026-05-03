package com.codekage.showup.presentation.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.codekage.showup.AppContainer
import com.codekage.showup.presentation.addjob.AddEditJobScreen
import com.codekage.showup.presentation.calendar.CalendarScreen
import com.codekage.showup.presentation.dashboard.DashboardScreen
import com.codekage.showup.presentation.jobdetail.JobDetailScreen
import com.codekage.showup.presentation.onboarding.OnboardingScreen
import com.codekage.showup.presentation.reports.ReportsScreen
import com.codekage.showup.presentation.settings.SettingsScreen

@Composable
fun AppNavigation(appContainer: AppContainer) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ""

    val showBottomBar = bottomNavItems.any { item ->
        currentRoute.endsWith(item.route::class.qualifiedName ?: "")
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentRoute.endsWith(item.route::class.qualifiedName ?: "")
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(Screen.Dashboard) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(if (selected) item.selectedIcon else item.unselectedIcon, contentDescription = item.label) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            NavHost(navController, startDestination = Screen.Dashboard) {
                composable<Screen.Dashboard> {
                    DashboardScreen(
                        appContainer = appContainer,
                        onSettingsClick = { navController.navigate(Screen.Settings) },
                        onAddJobClick = { navController.navigate(Screen.AddJob) },
                        onJobClick = { id -> navController.navigate(Screen.JobDetail(id)) },
                    )
                }
                composable<Screen.Calendar> { CalendarScreen(appContainer) }
                composable<Screen.Reports> {
                    ReportsScreen(appContainer, onSettingsClick = { navController.navigate(Screen.Settings) })
                }
                composable<Screen.Settings> {
                    SettingsScreen(appContainer, onBack = { navController.popBackStack() })
                }
                composable<Screen.AddJob> {
                    AddEditJobScreen(
                        appContainer = appContainer,
                        jobId = null,
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                }
                composable<Screen.EditJob> { entry ->
                    val args = entry.toRoute<Screen.EditJob>()
                    AddEditJobScreen(
                        appContainer = appContainer,
                        jobId = args.jobId,
                        onBack = { navController.popBackStack() },
                        onSaved = { navController.popBackStack() },
                    )
                }
                composable<Screen.JobDetail> { entry ->
                    val args = entry.toRoute<Screen.JobDetail>()
                    JobDetailScreen(
                        appContainer = appContainer,
                        jobId = args.jobId,
                        onBack = { navController.popBackStack() },
                        onEdit = { id -> navController.navigate(Screen.EditJob(id)) },
                        onDeleted = { navController.popBackStack() },
                    )
                }
                composable<Screen.Onboarding> {
                    OnboardingScreen(onContinue = { navController.navigate(Screen.Dashboard) { popUpTo(Screen.Onboarding) { inclusive = true } } })
                }
            }
        }
    }
}
