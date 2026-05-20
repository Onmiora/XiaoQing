package com.onmi.qing.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.ui.screens.developer.DeveloperScreen
import com.onmi.qing.ui.screens.settings.SettingsScreen
import com.onmi.qing.viewmodel.AchievementViewModel
import com.onmi.qing.viewmodel.PsychologyViewModel
import com.onmi.qing.viewmodel.SettingsViewModel

@Composable
fun SettingsNavHost(
    navController: NavHostController,
    demoModeManager: DemoModeManager,
    isDarkTheme: Boolean,
    followSystemTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onFollowSystemChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Settings.route,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
                scaleIn(initialScale = 0.85f, animationSpec = tween(200))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200)) +
                scaleOut(targetScale = 0.85f, animationSpec = tween(200))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(200)) +
                scaleIn(initialScale = 1.15f, animationSpec = tween(200))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(200)) +
                scaleOut(targetScale = 1.15f, animationSpec = tween(200))
        }
    ) {
        composable(Screen.Settings.route) {
            val settingsViewModel = hiltViewModel<SettingsViewModel>()
            val achievementViewModel = hiltViewModel<AchievementViewModel>()
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                achievementViewModel = achievementViewModel,
                isDarkTheme = isDarkTheme,
                followSystemTheme = followSystemTheme,
                onThemeChange = onThemeChange,
                onFollowSystemChange = onFollowSystemChange,
                onBackClick = onBackClick,
                onDeveloperClick = { navController.navigate(Screen.Developer.route) }
            )
        }

        composable(Screen.Developer.route) {
            val settingsViewModel = hiltViewModel<SettingsViewModel>()
            val psychologyViewModel = hiltViewModel<PsychologyViewModel>()
            val achievementViewModel = hiltViewModel<AchievementViewModel>()
            DeveloperScreen(
                psychologyViewModel = psychologyViewModel,
                achievementViewModel = achievementViewModel,
                settingsViewModel = settingsViewModel,
                demoModeManager = demoModeManager,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
