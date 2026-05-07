package org.davidnardya.vocalcoach.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navDeepLink
import androidx.navigation.toRoute
import kotlinx.coroutines.flow.collectLatest
import org.davidnardya.vocalcoach.core.util.DeepLinkHandler
import org.davidnardya.vocalcoach.core.util.createPermissionManager
import org.davidnardya.vocalcoach.di.*
import org.davidnardya.vocalcoach.presentation.screens.game.AudioClassificationRoute
import org.davidnardya.vocalcoach.presentation.screens.game.rememberAudioClassificationViewModel
import org.davidnardya.vocalcoach.presentation.screens.settings.SettingsScreen
import org.davidnardya.vocalcoach.presentation.screens.settings.SettingsViewModel
import org.davidnardya.vocalcoach.presentation.screens.welcome.WelcomeScreen
import org.davidnardya.vocalcoach.presentation.theme.BrutalMetalTheme

@Composable
fun App() {
    BrutalMetalTheme {
        val navController = rememberNavController()
        val permissionManager = remember { createPermissionManager() }
        
        LaunchedEffect(Unit) {
            DeepLinkHandler.deepLinkFlow.collectLatest { url ->
                url?.let {
                    if (it.startsWith("growlgame://start")) {
                        navController.navigate(GameRoute()) {
                            launchSingleTop = true
                        }
                    }
                    DeepLinkHandler.clearDeepLink()
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = WelcomeRoute,
            modifier = Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            composable<WelcomeRoute> {
                WelcomeScreen(
                    permissionManager = permissionManager,
                    onStartGame = {
                        navController.navigate(GameRoute(stage = it))
                    },
                    onNavigateToSettings = {
                        navController.navigate(SettingsRoute)
                    },
                )
            }
            composable<GameRoute>(
                deepLinks = listOf(
                    navDeepLink<GameRoute>(basePath = "growlgame://start")
                )
            ) {
                val route = it.toRoute<GameRoute>()
                val viewModel = rememberAudioClassificationViewModel(route.stage)
                AudioClassificationRoute(
                    viewModel = viewModel,
                    onBack = { navController.popBackStack() }
                )
            }
            composable<SettingsRoute> {
                val settingsViewModel: SettingsViewModel = viewModel {
                    SettingsViewModel(
                        createGetNotificationSettingsUseCase(),
                        createSaveSettingsUseCase(),
                        createResetScoreUseCase(),
                        createNotificationScheduler()
                    )
                }
                SettingsScreen(
                    viewModel = settingsViewModel,
                    permissionManager = permissionManager,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
