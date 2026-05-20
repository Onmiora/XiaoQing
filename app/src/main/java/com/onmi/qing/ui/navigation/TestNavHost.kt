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
import com.onmi.qing.ui.screens.discover.TestSelectionScreen
import com.onmi.qing.ui.screens.mbti.MbtiTestScreen
import com.onmi.qing.ui.screens.sbti.SbtiTestScreen
import com.onmi.qing.viewmodel.MbtiViewModel
import com.onmi.qing.viewmodel.SbtiViewModel

@Composable
fun TestNavHost(
    navController: NavHostController,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.TestSelection.route,
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
        composable(Screen.TestSelection.route) {
            TestSelectionScreen(
                onNavigateToSbti = {
                    navController.navigate(Screen.SbtiTest.route) {
                        popUpTo(Screen.TestSelection.route) { inclusive = true }
                    }
                },
                onNavigateToMbti = {
                    navController.navigate(Screen.MbtiTest.route) {
                        popUpTo(Screen.TestSelection.route) { inclusive = true }
                    }
                },
                onBackClick = onBackClick
            )
        }

        composable(Screen.SbtiTest.route) {
            val sbtiViewModel = hiltViewModel<SbtiViewModel>()
            SbtiTestScreen(
                viewModel = sbtiViewModel,
                onBackClick = { navController.popBackStack() },
                onComplete = { _ -> onBackClick() }
            )
        }

        composable(Screen.MbtiTest.route) {
            val mbtiViewModel = hiltViewModel<MbtiViewModel>()
            MbtiTestScreen(
                viewModel = mbtiViewModel,
                onBackClick = { navController.popBackStack() },
                onComplete = { onBackClick() }
            )
        }
    }
}
