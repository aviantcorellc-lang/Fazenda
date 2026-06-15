package com.fazenda.app.ui.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Grass
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.fazenda.app.ui.screen.catalog.AddPlantScreen
import com.fazenda.app.ui.screen.catalog.CategoriesScreen
import com.fazenda.app.ui.screen.catalog.CatalogScreen
import com.fazenda.app.ui.screen.catalog.EditPlantScreen
import com.fazenda.app.ui.screen.catalog.PlantDetailsScreen
import com.fazenda.app.ui.screen.catalog.SettingsScreen
import com.fazenda.app.ui.screen.catalog.ZonesScreen
import com.fazenda.app.ui.screen.dashboard.DashboardScreen
import com.fazenda.app.ui.screen.journal.CreateLogScreen
import com.fazenda.app.ui.screen.journal.JournalScreen
import com.fazenda.app.ui.screen.map.MapScreen
import com.fazenda.app.ui.screen.dashboard.SchedulesScreen
import com.fazenda.app.ui.screen.knowledge.KnowledgeBaseScreen
import com.fazenda.app.ui.screen.search.SearchScreen
import com.fazenda.app.ui.screen.onboarding.OnboardingScreen
import androidx.compose.ui.platform.LocalContext
import com.fazenda.app.ui.viewmodel.CatalogViewModel

sealed class Screen(val route: String, val icon: ImageVector, val label: String) {
    data object Dashboard : Screen("dashboard", Icons.Default.Dashboard, "План")
    data object Catalog : Screen("catalog", Icons.Default.Grass, "Каталог")
    data object Journal : Screen("journal", Icons.Default.History, "Журнал")
}

sealed class DetailScreen(val route: String) {
    data object PlantDetails : DetailScreen("plant_details/{plantId}") {
        fun createRoute(plantId: Long) = "plant_details/$plantId"
    }
    data object EditPlant : DetailScreen("edit_plant/{plantId}") {
        fun createRoute(plantId: Long) = "edit_plant/$plantId"
    }
    data object AddPlant : DetailScreen("add_plant")
    data object CreateLog : DetailScreen("create_log")
    data object Zones : DetailScreen("zones")
    data object Categories : DetailScreen("categories")
    data object Map : DetailScreen("map")
    data object Settings : DetailScreen("settings")
    data object Schedules : DetailScreen("schedules")
    data object KnowledgeBase : DetailScreen("knowledge_base")
    data object Search : DetailScreen("search")
    data object Onboarding : DetailScreen("onboarding")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val bottomNavItems = listOf(Screen.Dashboard, Screen.Catalog, Screen.Journal)
    val showBottomBar = currentRoute in bottomNavItems.map { it.route }

    val context = LocalContext.current
    val startDestination = remember {
        val sharedPrefs = context.getSharedPreferences("fazenda_prefs", android.content.Context.MODE_PRIVATE)
        val onboardingCompleted = sharedPrefs.getBoolean("onboarding_completed", false)
        if (onboardingCompleted) Screen.Dashboard.route else DetailScreen.Onboarding.route
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { screen ->
                        NavigationBarItem(
                            icon = { Icon(screen.icon, contentDescription = screen.label) },
                            label = { Text(screen.label) },
                            selected = currentRoute == screen.route,
                            onClick = {
                                if (currentRoute != screen.route) {
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Dashboard.route) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding).imePadding()
        ) {
            composable(Screen.Dashboard.route) {
                DashboardScreen(
                    onNavigateToCreateLog = {
                        navController.navigate(DetailScreen.CreateLog.route)
                    },
                    onNavigateToMap = {
                        navController.navigate(DetailScreen.Map.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(DetailScreen.Settings.route)
                    },
                    onNavigateToSchedules = {
                        navController.navigate(DetailScreen.Schedules.route)
                    },
                    onNavigateToKnowledgeBase = {
                        navController.navigate(DetailScreen.KnowledgeBase.route)
                    },
                    onNavigateToSearch = {
                        navController.navigate(DetailScreen.Search.route)
                    }
                )
            }

            composable(Screen.Catalog.route) {
                CatalogScreen(
                    onPlantClick = { plantId ->
                        navController.navigate(DetailScreen.PlantDetails.createRoute(plantId))
                    },
                    onPlantEdit = { plantId ->
                        navController.navigate(DetailScreen.EditPlant.createRoute(plantId))
                    },
                    onAddPlant = {
                        navController.navigate(DetailScreen.AddPlant.route)
                    },
                    onNavigateToSettings = {
                        navController.navigate(DetailScreen.Settings.route)
                    },
                    onNavigateToZones = {
                        navController.navigate(DetailScreen.Zones.route)
                    },
                    onNavigateToCategories = {
                        navController.navigate(DetailScreen.Categories.route)
                    }
                )
            }

            composable(Screen.Journal.route) {
                JournalScreen(
                    onNavigateToCreateLog = {
                        navController.navigate(DetailScreen.CreateLog.route)
                    }
                )
            }

            composable(
                route = DetailScreen.PlantDetails.route,
                arguments = listOf(navArgument("plantId") { type = NavType.LongType })
            ) { backStackEntry ->
                val plantId = backStackEntry.arguments?.getLong("plantId") ?: return@composable
                PlantDetailsScreen(
                    plantId = plantId,
                    onNavigateBack = { navController.popBackStack() },
                    onEditClick = { navController.navigate(DetailScreen.EditPlant.createRoute(plantId)) }
                )
            }

            composable(
                route = DetailScreen.EditPlant.route,
                arguments = listOf(navArgument("plantId") { type = NavType.LongType })
            ) { backStackEntry ->
                val plantId = backStackEntry.arguments?.getLong("plantId") ?: return@composable
                EditPlantScreen(
                    plantId = plantId,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(DetailScreen.AddPlant.route) {
                AddPlantScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(DetailScreen.CreateLog.route) {
                CreateLogScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(DetailScreen.Zones.route) {
                ZonesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(DetailScreen.Categories.route) {
                CategoriesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(DetailScreen.Map.route) {
                val catalogViewModel: CatalogViewModel = viewModel()
                val plantsWithPhotos by catalogViewModel.allPlantsWithPhotos.collectAsState()
                MapScreen(
                    plants = plantsWithPhotos.map { it.plant },
                    onPlantClick = { plantId ->
                        navController.navigate(DetailScreen.PlantDetails.createRoute(plantId))
                    },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(DetailScreen.Settings.route) {
                SettingsScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onEditCategories = { navController.navigate(DetailScreen.Categories.route) },
                    onEditZones = { navController.navigate(DetailScreen.Zones.route) }
                )
            }

            composable(DetailScreen.Schedules.route) {
                SchedulesScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(DetailScreen.KnowledgeBase.route) {
                KnowledgeBaseScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable(DetailScreen.Search.route) {
                SearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPlantClick = { plantId ->
                        navController.navigate(DetailScreen.PlantDetails.createRoute(plantId))
                    },
                    onPlantEdit = { plantId ->
                        navController.navigate(DetailScreen.EditPlant.createRoute(plantId))
                    }
                )
            }

            composable(DetailScreen.Onboarding.route) {
                OnboardingScreen(
                    onFinished = {
                        navController.navigate(Screen.Dashboard.route) {
                            popUpTo(DetailScreen.Onboarding.route) { inclusive = true }
                        }
                    }
                )
            }
        }
    }
}
