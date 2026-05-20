package com.onmi.qing.ui.navigation

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
import com.onmi.qing.ui.screens.achievement.AchievementScreen
import com.onmi.qing.ui.screens.breathing.BreathingScreen
import com.onmi.qing.ui.screens.home.HomeScreen
import com.onmi.qing.ui.screens.mooddiary.MoodDiaryScreen
import com.onmi.qing.ui.screens.discover.DiscoverScreen
import com.onmi.qing.ui.screens.profile.ProfileScreen
import com.onmi.qing.viewmodel.AchievementViewModel
import com.onmi.qing.viewmodel.HomeViewModel
import com.onmi.qing.viewmodel.MoodViewModel
import com.onmi.qing.viewmodel.UsageStatsViewModel

// 导航主机组件
@Composable
fun QingNavHost(
    navController: NavHostController,
    demoModeManager: DemoModeManager,
    isDarkTheme: Boolean,
    followSystemTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onFollowSystemChange: (Boolean) -> Unit,
    paddingValues: PaddingValues,
    onLaunchChat: (String?) -> Unit = {},
    onLaunchHistory: () -> Unit = {},
    onLaunchSettings: () -> Unit = {},
    onLaunchTest: () -> Unit = {},
    onLaunchStress: () -> Unit = {},
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
            val moodViewModel = hiltViewModel<MoodViewModel>()
            HomeScreen(
                viewModel = homeViewModel,
                moodViewModel = moodViewModel,
                demoModeManager = demoModeManager,
                onStartChatClick = { onLaunchChat(null) },
                onBreathingClick = { navController.navigate(Screen.Breathing.route) },
                onStressDetectionClick = { onLaunchStress() }
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
                onHistoryClick = onLaunchHistory,
                onStressDetectionClick = { onLaunchStress() },
                onFunTestClick = { onLaunchTest() }
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
                onHistoryClick = onLaunchHistory,
                onSettingsClick = { onLaunchSettings() }
            )
        }

        // 子页面
        composable(Screen.Breathing.route) {
            val usageStatsViewModel = hiltViewModel<UsageStatsViewModel>()
            BreathingScreen(
                usageStatsViewModel = usageStatsViewModel,
                demoModeManager = demoModeManager,
                onBackClick = { navController.popBackStack() }
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

    }
}
