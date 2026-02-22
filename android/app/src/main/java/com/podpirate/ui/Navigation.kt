package com.podpirate.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.podpirate.ui.screens.*

sealed class Screen(val route: String, val label: String, val icon: ImageVector?) {
    data object Feed : Screen("feed", "Feed", Icons.Default.Home)
    data object Queue : Screen("queue", "Queue", Icons.AutoMirrored.Filled.QueueMusic)
    data object Search : Screen("search", "Search", Icons.Default.Search)
    data object Downloads : Screen("downloads", "Downloads", Icons.Default.Download)
    data object Subscriptions : Screen("subscriptions", "Subs", Icons.Default.Subscriptions)
    data object PodcastDetail : Screen("podcast/{podcastId}", "Podcast", null)
    data object EpisodePlayer : Screen("episode/{episodeId}", "Episode", null)
}

val bottomNavItems = listOf(Screen.Feed, Screen.Queue, Screen.Search, Screen.Downloads, Screen.Subscriptions)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PodPirateNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    val showBottomBar = bottomNavItems.any { screen ->
        currentDestination?.hierarchy?.any { it.route == screen.route } == true
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("☠ PodPirate") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon!!, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                            onClick = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Feed.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Feed.route) {
                FeedScreen(
                    onEpisodeClick = { navController.navigate("episode/$it") },
                )
            }
            composable(Screen.Queue.route) {
                QueueScreen()
            }
            composable(Screen.Search.route) {
                SearchScreen()
            }
            composable(Screen.Downloads.route) {
                DownloadsScreen(
                    onEpisodeClick = { navController.navigate("episode/$it") },
                )
            }
            composable(Screen.Subscriptions.route) {
                SubscriptionsScreen(
                    onPodcastClick = { navController.navigate("podcast/$it") },
                )
            }
            composable(
                Screen.PodcastDetail.route,
                arguments = listOf(navArgument("podcastId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val podcastId = backStackEntry.arguments?.getLong("podcastId") ?: return@composable
                PodcastDetailScreen(
                    podcastId = podcastId,
                    onEpisodeClick = { navController.navigate("episode/$it") },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(
                Screen.EpisodePlayer.route,
                arguments = listOf(navArgument("episodeId") { type = NavType.LongType }),
            ) { backStackEntry ->
                val episodeId = backStackEntry.arguments?.getLong("episodeId") ?: return@composable
                EpisodePlayerScreen(
                    episodeId = episodeId,
                    onBack = { navController.popBackStack() },
                )
            }
        }
    }
}
