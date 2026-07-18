package com.cortex.dl.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.cortex.dl.ui.theme.CortexBlue
import com.cortex.dl.ui.theme.CortexCyan
import com.cortex.dl.ui.theme.CortexDarkBackground
import com.cortex.dl.ui.theme.CortexSurface
import com.cortex.dl.ui.theme.CortexTextPrimary
import com.cortex.dl.ui.theme.CortexTextSecondary

import androidx.compose.ui.res.stringResource

sealed class Screen(val route: String, val titleRes: Int, val selectedIcon: ImageVector, val unselectedIcon: ImageVector) {
    object Home : Screen("home", com.cortex.dl.R.string.home, Icons.Filled.Home, Icons.Outlined.Home)
    object History : Screen("history", com.cortex.dl.R.string.download_history, Icons.Filled.History, Icons.Outlined.History)
    object Settings : Screen("settings", com.cortex.dl.R.string.settings, Icons.Filled.Settings, Icons.Outlined.Settings)
}

val items = listOf(
    Screen.Home,
    Screen.History,
    Screen.Settings
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    Scaffold(
        containerColor = CortexDarkBackground,
        bottomBar = {
            NavigationBar(
                containerColor = CortexSurface,
                contentColor = CortexTextPrimary
            ) {
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentDestination = navBackStackEntry?.destination
                items.forEach { screen ->
                    val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                    NavigationBarItem(
                        icon = {
                            Icon(
                                imageVector = if (selected) screen.selectedIcon else screen.unselectedIcon,
                                contentDescription = stringResource(id = screen.titleRes)
                            )
                        },
                        label = { Text(stringResource(id = screen.titleRes)) },
                        selected = selected,
                        onClick = {
                            navController.navigate(screen.route) {
                                // Pop up to the start destination of the graph to
                                // avoid building up a large stack of destinations
                                // on the back stack as users select items
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                // Avoid multiple copies of the same destination when
                                // reselecting the same item
                                launchSingleTop = true
                                // Restore state when reselecting a previously selected item
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = CortexDarkBackground,
                            unselectedIconColor = CortexTextSecondary,
                            selectedTextColor = CortexCyan,
                            unselectedTextColor = CortexTextSecondary,
                            indicatorColor = CortexCyan
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) { 
                MainScreen(
                    onNavigateToHistory = {
                        navController.navigate(Screen.History.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                ) 
            }
            composable(Screen.History.route) { DownloadsHistoryScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}
