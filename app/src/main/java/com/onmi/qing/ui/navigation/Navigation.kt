package com.onmi.qing.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.ui.screens.achievement.AchievementScreen
import com.onmi.qing.ui.screens.breathing.BreathingScreen
import com.onmi.qing.ui.screens.chat.ChatScreen
import com.onmi.qing.ui.screens.history.HistoryScreen
import com.onmi.qing.ui.screens.home.HomeScreen
import com.onmi.qing.ui.screens.mooddiary.MoodDiaryScreen
import com.onmi.qing.ui.screens.developer.DeveloperScreen
import com.onmi.qing.ui.screens.discover.DiscoverScreen
import com.onmi.qing.ui.screens.profile.ProfileScreen
import com.onmi.qing.ui.screens.settings.SettingsScreen
import com.onmi.qing.ui.screens.stressdetection.StressDetectionScreen
import com.onmi.qing.ui.screens.sbti.SbtiTestScreen
import com.onmi.qing.ui.screens.mbti.MbtiTestScreen
import com.onmi.qing.ui.screens.discover.TestSelectionScreen
import com.onmi.qing.viewmodel.ChatViewModel
import com.onmi.qing.viewmodel.HomeViewModel
import com.onmi.qing.viewmodel.MoodViewModel
import com.onmi.qing.viewmodel.SettingsViewModel
import com.onmi.qing.viewmodel.StateViewModel
import com.onmi.qing.viewmodel.StressDetectionViewModel
import com.onmi.qing.viewmodel.SbtiViewModel
import com.onmi.qing.viewmodel.MbtiViewModel

// 导航主机组件
@Composable
fun QingNavHost(
    navController: NavHostController,
    homeViewModel: HomeViewModel,
    chatViewModel: ChatViewModel,
    stateViewModel: StateViewModel,
    settingsViewModel: SettingsViewModel,
    moodViewModel: MoodViewModel,
    stressDetectionViewModel: StressDetectionViewModel,
    sbtiViewModel: SbtiViewModel,
    mbtiViewModel: MbtiViewModel,
    dataStore: QingDataStore,
    demoModeManager: DemoModeManager,
    isDarkTheme: Boolean,
    followSystemTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onFollowSystemChange: (Boolean) -> Unit,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier.padding(paddingValues),
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
                scaleIn(
                    initialScale = 0.85f,
                    animationSpec = tween(200)
                )
        },
        exitTransition = {
            fadeOut(animationSpec = tween(200)) +
                scaleOut(
                    targetScale = 0.85f,
                    animationSpec = tween(200)
                )
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(200)) +
                scaleIn(
                    initialScale = 1.15f,
                    animationSpec = tween(200)
                )
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(200)) +
                scaleOut(
                    targetScale = 1.15f,
                    animationSpec = tween(200)
                )
        }
    ) {
        // 底部导航页面
        composable(Screen.Home.route) {
            HomeScreen(
                viewModel = homeViewModel,
                stateViewModel = stateViewModel,
                moodViewModel = moodViewModel,
                demoModeManager = demoModeManager,
                onStartChatClick = { navController.navigate(Screen.Chat.route) },
                onBreathingClick = { navController.navigate(Screen.Breathing.route) },
                onAchievementClick = { navController.navigate(Screen.Achievement.route) },
                onStressDetectionClick = { navController.navigate(Screen.StressDetection.route) }
            )
        }

        composable(Screen.Discover.route) {
            DiscoverScreen(
                stateViewModel = stateViewModel,
                moodViewModel = moodViewModel,
                demoModeManager = demoModeManager,
                onBreathingClick = { navController.navigate(Screen.Breathing.route) },
                onAchievementClick = { navController.navigate(Screen.Achievement.route) },
                onHistoryClick = { navController.navigate(Screen.History.route) },
                onStressDetectionClick = { navController.navigate(Screen.StressDetection.route) },
                onFunTestClick = { navController.navigate(Screen.TestSelection.route) }
            )
        }

        composable(Screen.MoodDiary.route) {
            MoodDiaryScreen(
                viewModel = moodViewModel,
                demoModeManager = demoModeManager
            )
        }

        composable(Screen.Profile.route) {
            ProfileScreen(
                stateViewModel = stateViewModel,
                moodViewModel = moodViewModel,
                demoModeManager = demoModeManager,
                onAchievementClick = { navController.navigate(Screen.Achievement.route) },
                onHistoryClick = { navController.navigate(Screen.History.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        // 子页面
        composable(Screen.Chat.route) {
            ChatScreen(
                viewModel = chatViewModel,
                demoModeManager = demoModeManager,
                onHistoryClick = { navController.navigate(Screen.History.route) },
                onNewChatClick = { chatViewModel.createNewSession() },
                onBreathingClick = { navController.navigate(Screen.Breathing.route) },
                onFunTestClick = { navController.navigate(Screen.TestSelection.route) }
            )
        }

        composable(Screen.Breathing.route) {
            BreathingScreen(
                stateViewModel = stateViewModel,
                demoModeManager = demoModeManager,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.StressDetection.route) {
            StressDetectionScreen(
                viewModel = stressDetectionViewModel,
                onBackClick = { navController.popBackStack() },
                onBreathingClick = {
                    navController.popBackStack()
                    navController.navigate(Screen.Breathing.route)
                }
            )
        }

        composable(Screen.History.route) {
            HistoryScreen(
                onBackClick = { navController.popBackStack() },
                onSessionClick = { session ->
                    chatViewModel.loadSession(session)
                    navController.navigate(Screen.Chat.route)
                },
                stateViewModel = stateViewModel,
                dataStore = dataStore,
                demoModeManager = demoModeManager
            )
        }

        composable(Screen.Achievement.route) {
            AchievementScreen(
                viewModel = stateViewModel,
                demoModeManager = demoModeManager,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Settings.route) {
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                stateViewModel = stateViewModel,
                isDarkTheme = isDarkTheme,
                followSystemTheme = followSystemTheme,
                onThemeChange = onThemeChange,
                onFollowSystemChange = onFollowSystemChange,
                onBackClick = { navController.popBackStack() },
                onDeveloperClick = { navController.navigate(Screen.Developer.route) }
            )
        }

        composable(Screen.Developer.route) {
            DeveloperScreen(
                stateViewModel = stateViewModel,
                settingsViewModel = settingsViewModel,
                demoModeManager = demoModeManager,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.SbtiTest.route) {
            SbtiTestScreen(
                viewModel = sbtiViewModel,
                onBackClick = { navController.popBackStack() },
                onComplete = { navController.popBackStack() }
            )
        }

        composable(Screen.MbtiTest.route) {
            MbtiTestScreen(
                viewModel = mbtiViewModel,
                onBackClick = { navController.popBackStack() },
                onComplete = { navController.popBackStack() }
            )
        }

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
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}