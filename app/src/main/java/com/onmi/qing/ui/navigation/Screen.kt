package com.onmi.qing.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Mood
import androidx.compose.material.icons.outlined.Person
import androidx.compose.ui.graphics.vector.ImageVector

// 导航路由定义
sealed class Screen(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    // 底部导航 Tab
    data object Home : Screen(
        route = "home",
        title = "首页",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    data object Discover : Screen(
        route = "discover",
        title = "发现",
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore
    )

    data object Profile : Screen(
        route = "profile",
        title = "我的",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    // 底部导航 - 心情日记
    data object MoodDiary : Screen(
        route = "mood_diary",
        title = "心情",
        selectedIcon = Icons.Filled.Mood,
        unselectedIcon = Icons.Outlined.Mood
    )

    // 子页面
    data object Chat : Screen(
        route = "chat/{sessionId}",
        title = "小晴",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Explore
    ) {
        fun createRoute(sessionId: String? = null): String {
            return if (sessionId != null) "chat/$sessionId" else "chat/new"
        }
    }

    data object Breathing : Screen(
        route = "breathing",
        title = "呼吸练习",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Explore
    )

    data object StressDetection : Screen(
        route = "stress_detection",
        title = "压力检测",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Explore
    )

    data object History : Screen(
        route = "history",
        title = "对话历史",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Explore
    )

    data object Achievement : Screen(
        route = "achievement",
        title = "成就",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Explore
    )

    // 子页面
    data object Settings : Screen(
        route = "settings",
        title = "设置",
        selectedIcon = Icons.Filled.DarkMode,
        unselectedIcon = Icons.Outlined.DarkMode
    )

    data object Developer : Screen(
        route = "developer",
        title = "开发者选项",
        selectedIcon = Icons.Filled.Code,
        unselectedIcon = Icons.Outlined.Code
    )

    data object SbtiTest : Screen(
        route = "sbti_test",
        title = "SBTI测试",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Explore
    )

    data object MbtiTest : Screen(
        route = "mbti_test",
        title = "MBTI测试",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Explore
    )

    data object TestSelection : Screen(
        route = "test_selection",
        title = "选择测试",
        selectedIcon = Icons.Filled.Psychology,
        unselectedIcon = Icons.Outlined.Explore
    )

    companion object {
        val bottomNavItems = listOf(Home, Discover, MoodDiary, Profile)
    }
}
