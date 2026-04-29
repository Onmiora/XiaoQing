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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.data.repository.ChatRepository
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
import com.onmi.qing.viewmodel.AchievementViewModel
import com.onmi.qing.viewmodel.AnalysisViewModel
import com.onmi.qing.viewmodel.ChatViewModel
import com.onmi.qing.viewmodel.HomeViewModel
import com.onmi.qing.viewmodel.MoodViewModel
import com.onmi.qing.viewmodel.PsychologyViewModel
import com.onmi.qing.viewmodel.SettingsViewModel
import com.onmi.qing.viewmodel.StressDetectionViewModel
import com.onmi.qing.viewmodel.UsageStatsViewModel
import com.onmi.qing.viewmodel.SbtiViewModel
import com.onmi.qing.viewmodel.MbtiViewModel

// 导航主机组件
@Composable
fun QingNavHost(
    navController: NavHostController,
    chatRepository: ChatRepository,
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
            val homeViewModel = hiltViewModel<HomeViewModel>()
            val achievementViewModel = hiltViewModel<AchievementViewModel>()
            val usageStatsViewModel = hiltViewModel<UsageStatsViewModel>()
            val moodViewModel = hiltViewModel<MoodViewModel>()
            HomeScreen(
                viewModel = homeViewModel,
                achievementViewModel = achievementViewModel,
                usageStatsViewModel = usageStatsViewModel,
                moodViewModel = moodViewModel,
                demoModeManager = demoModeManager,
                onStartChatClick = { navController.navigate(Screen.Chat.route) },
                onBreathingClick = { navController.navigate(Screen.Breathing.route) },
                onAchievementClick = { navController.navigate(Screen.Achievement.route) },
                onStressDetectionClick = { navController.navigate(Screen.StressDetection.route) }
            )
        }

        composable(Screen.Discover.route) {
            val achievementViewModel = hiltViewModel<AchievementViewModel>()
            val usageStatsViewModel = hiltViewModel<UsageStatsViewModel>()
            val moodViewModel = hiltViewModel<MoodViewModel>()
            DiscoverScreen(
                achievementViewModel = achievementViewModel,
                usageStatsViewModel = usageStatsViewModel,
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
            val moodViewModel = hiltViewModel<MoodViewModel>()
            MoodDiaryScreen(
                viewModel = moodViewModel,
                demoModeManager = demoModeManager
            )
        }

        composable(Screen.Profile.route) {
            val achievementViewModel = hiltViewModel<AchievementViewModel>()
            val usageStatsViewModel = hiltViewModel<UsageStatsViewModel>()
            val moodViewModel = hiltViewModel<MoodViewModel>()
            ProfileScreen(
                achievementViewModel = achievementViewModel,
                usageStatsViewModel = usageStatsViewModel,
                moodViewModel = moodViewModel,
                demoModeManager = demoModeManager,
                onAchievementClick = { navController.navigate(Screen.Achievement.route) },
                onHistoryClick = { navController.navigate(Screen.History.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) }
            )
        }

        // 子页面
        composable(Screen.Chat.route) {
            val chatViewModel = hiltViewModel<ChatViewModel>()
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
            val usageStatsViewModel = hiltViewModel<UsageStatsViewModel>()
            BreathingScreen(
                usageStatsViewModel = usageStatsViewModel,
                demoModeManager = demoModeManager,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.StressDetection.route) {
            val stressDetectionViewModel = hiltViewModel<StressDetectionViewModel>()
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
            val analysisViewModel = hiltViewModel<AnalysisViewModel>()
            HistoryScreen(
                onBackClick = { navController.popBackStack() },
                onSessionClick = { session ->
                    navController.navigate(Screen.Chat.route)
                },
                analysisViewModel = analysisViewModel,
                chatRepository = chatRepository,
                demoModeManager = demoModeManager
            )
        }

        composable(Screen.Achievement.route) {
            val achievementViewModel = hiltViewModel<AchievementViewModel>()
            AchievementScreen(
                viewModel = achievementViewModel,
                demoModeManager = demoModeManager,
                onBackClick = { navController.popBackStack() }
            )
        }

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
                onBackClick = { navController.popBackStack() },
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

        composable(Screen.SbtiTest.route) {
            val sbtiViewModel = hiltViewModel<SbtiViewModel>()
            SbtiTestScreen(
                viewModel = sbtiViewModel,
                onBackClick = { navController.popBackStack() },
                onComplete = { navController.popBackStack() }
            )
        }

        composable(Screen.MbtiTest.route) {
            val mbtiViewModel = hiltViewModel<MbtiViewModel>()
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
