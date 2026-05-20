package com.onmi.qing.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.hilt.navigation.compose.hiltViewModel
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.data.repository.ChatRepository
import com.onmi.qing.ui.screens.chat.ChatScreen
import com.onmi.qing.ui.screens.history.HistoryScreen
import com.onmi.qing.viewmodel.AnalysisViewModel
import com.onmi.qing.viewmodel.ChatViewModel

@Composable
fun ChatNavHost(
    navController: NavHostController,
    startDestination: String,
    chatRepository: ChatRepository,
    demoModeManager: DemoModeManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
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
        composable(
            route = Screen.Chat.route,
            arguments = listOf(
                navArgument("sessionId") {
                    type = NavType.StringType
                    defaultValue = "new"
                }
            )
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId")
            val chatViewModel: ChatViewModel = hiltViewModel<ChatViewModel, ChatViewModel.ChatViewModelFactory>(
                creationCallback = { factory ->
                    factory.create(sessionId)
                }
            )
            ChatScreen(
                viewModel = chatViewModel,
                demoModeManager = demoModeManager,
                onHistoryClick = { navController.navigate(Screen.History.route) },
                onNewChatClick = { chatViewModel.createNewSession() },
                onBackClick = onBackClick,
                onBreathingClick = {},
                onStressDetectionClick = {},
                onFunTestClick = {}
            )
        }

        composable(Screen.History.route) {
            val analysisViewModel = hiltViewModel<AnalysisViewModel>()
            HistoryScreen(
                onBackClick = { navController.popBackStack() },
                onSessionClick = { session ->
                    navController.navigate(Screen.Chat.createRoute(session.id))
                },
                analysisViewModel = analysisViewModel,
                chatRepository = chatRepository,
                demoModeManager = demoModeManager
            )
        }
    }
}
