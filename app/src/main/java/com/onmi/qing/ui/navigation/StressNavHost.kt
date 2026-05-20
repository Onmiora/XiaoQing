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
import com.onmi.qing.ui.screens.stressdetection.StressDetectionScreen
import com.onmi.qing.viewmodel.StressDetectionViewModel

@Composable
fun StressNavHost(
    navController: NavHostController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.StressDetection.route,
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
        composable(Screen.StressDetection.route) {
            val stressDetectionViewModel = hiltViewModel<StressDetectionViewModel>()
            StressDetectionScreen(
                viewModel = stressDetectionViewModel,
                onBackClick = onBackClick,
                onBreathingClick = { /* breathing is in MainActivity, can't navigate cross-activity */ }
            )
        }
    }
}
