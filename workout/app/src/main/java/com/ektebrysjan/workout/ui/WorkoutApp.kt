package com.ektebrysjan.workout.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ektebrysjan.workout.R

private object Routes {
    const val LOG = "log"
    const val SUMMARY = "summary"
    const val SETTINGS = "settings"
    const val EDITOR_ARG = "id"
    const val EDITOR_PATTERN = "editor/{id}"
    fun editor(id: Long) = "editor/$id"
}

private data class TopTab(val route: String, val labelRes: Int, val icon: ImageVector)

@Composable
fun WorkoutApp(viewModel: WorkoutViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    val tabs = listOf(
        TopTab(Routes.LOG, R.string.nav_log, Icons.AutoMirrored.Filled.ListAlt),
        TopTab(Routes.SUMMARY, R.string.nav_summary, Icons.Default.BarChart),
        TopTab(Routes.SETTINGS, R.string.nav_settings, Icons.Default.Settings)
    )

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val showBottomBar = tabs.any { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        val selected =
                            currentDestination?.hierarchy?.any { it.route == tab.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(stringResource(tab.labelRes)) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.LOG,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LOG) {
                LogScreen(
                    viewModel = viewModel,
                    onOpenEntry = { id -> navController.navigate(Routes.editor(id)) },
                    onAdd = { navController.navigate(Routes.editor(0)) }
                )
            }
            composable(Routes.SUMMARY) {
                SummaryScreen(viewModel = viewModel)
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(viewModel = viewModel)
            }
            composable(
                Routes.EDITOR_PATTERN,
                arguments = listOf(navArgument(Routes.EDITOR_ARG) { type = NavType.LongType })
            ) { entry ->
                val id = entry.arguments?.getLong(Routes.EDITOR_ARG) ?: 0L
                WorkoutEditorScreen(
                    viewModel = viewModel,
                    entryId = id,
                    onDone = { navController.popBackStack() }
                )
            }
        }
    }
}
