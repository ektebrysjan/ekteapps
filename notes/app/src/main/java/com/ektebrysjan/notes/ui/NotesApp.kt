package com.ektebrysjan.notes.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.ektebrysjan.notes.R

private object Routes {
    const val LIST = "list"
    const val SETTINGS = "settings"
    const val EDITOR_ARG = "noteId"
    const val EDITOR_PATTERN = "editor/{noteId}"
    fun editor(noteId: Long) = "editor/$noteId"
}

private data class TopTab(val route: String, val labelRes: Int, val icon: ImageVector)

@Composable
fun NotesApp(viewModel: NotesViewModel) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    // Surface export/import results as snackbars.
    androidx.compose.runtime.LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    val tabs = listOf(
        TopTab(Routes.LIST, R.string.nav_notes, Icons.AutoMirrored.Filled.Notes),
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
            startDestination = Routes.LIST,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Routes.LIST) {
                NotesListScreen(
                    viewModel = viewModel,
                    onOpenNote = { id -> navController.navigate(Routes.editor(id)) },
                    onAddNote = { navController.navigate(Routes.editor(0)) }
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(viewModel = viewModel)
            }
            composable(
                Routes.EDITOR_PATTERN,
                arguments = listOf(navArgument(Routes.EDITOR_ARG) { type = NavType.LongType })
            ) { entry ->
                val noteId = entry.arguments?.getLong(Routes.EDITOR_ARG) ?: 0L
                NoteEditorScreen(
                    viewModel = viewModel,
                    noteId = noteId,
                    onDone = { navController.popBackStack() }
                )
            }
        }
    }
}
