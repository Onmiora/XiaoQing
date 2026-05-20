@file:OptIn(ExperimentalMaterial3WindowSizeClassApi::class)

package com.onmi.qing

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.onmi.qing.ui.components.FloatingPillNavBar
import com.onmi.qing.ui.components.VerticalPillNavBar
import com.onmi.qing.ui.components.PermissionBottomSheet
import com.onmi.qing.ui.components.toFloatingNavItems
import com.onmi.qing.ui.navigation.QingNavHost
import com.onmi.qing.ui.navigation.Screen
import com.onmi.qing.ui.theme.QingTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    // 权限请求Launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        // 权限授予结果处理
        val allGranted = permissions.all { it.value }
        // 更新权限状态
        _hasPermissions.value = allGranted
    }

    private lateinit var _hasPermissions: MutableStateFlow<Boolean>

    // 需要申请的权限列表
    private val requiredPermissions: Array<String>
        get() {
            val permissions = mutableListOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
            // 始终申请位置权限，因为测试发现在高版本 Android 上也需要位置权限才能扫描 BLE 设备
            permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
            return permissions.toTypedArray()
        }

    // 检查是否所有权限都已授予
    private fun checkPermissions(): Boolean {
        return requiredPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    // 请求权限
    private fun requestPermissions() {
        permissionLauncher.launch(requiredPermissions)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 初始化权限状态（必须在 super.onCreate() 之后）
        _hasPermissions = MutableStateFlow(checkPermissions())

        val application = application as QingApplication

        setContent {
            // Calculate window size class inside composable context
            val windowSizeClass = calculateWindowSizeClass(this@MainActivity)

            val scope = rememberCoroutineScope()

            // 权限状态
            val hasPermissions by _hasPermissions.collectAsState()

            // Collect theme from DataStore
            val userPrefs by application.dataStore.userPreferences.collectAsState(
                initial = com.onmi.qing.data.datastore.UserPreferences()
            )

            // Calculate actual theme based on followSystemTheme setting
            val systemDark = isSystemInDarkTheme()
            val isDarkTheme = if (userPrefs.followSystemTheme) {
                systemDark
            } else {
                userPrefs.isDarkTheme
            }

            // 权限 BottomSheet 状态 - 只有在没有权限时才显示
            val showPermissionSheet = !hasPermissions

            QingTheme(darkTheme = isDarkTheme) {
                QingApp(
                    windowWidthSizeClass = windowSizeClass.widthSizeClass,
                    isDarkTheme = isDarkTheme,
                    followSystemTheme = userPrefs.followSystemTheme,
                    onThemeChange = { isDark ->
                        scope.launch { application.dataStore.updateTheme(isDark) }
                    },
                    onFollowSystemChange = { follow ->
                        scope.launch { application.dataStore.updateFollowSystemTheme(follow) }
                    },
                    application = application,
                    hasPermissions = hasPermissions,
                    onRequestPermissions = { requestPermissions() }
                )
            }

            // 权限申请 BottomSheet
            if (showPermissionSheet) {
                PermissionBottomSheet(
                    onDismiss = { /* 用户选择稍后再说，关闭但不阻止使用 */ },
                    onPermissionsGranted = {
                        requestPermissions()
                    }
                )
            }
        }
    }
}

@Composable
fun QingApp(
    windowWidthSizeClass: WindowWidthSizeClass,
    isDarkTheme: Boolean,
    followSystemTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onFollowSystemChange: (Boolean) -> Unit,
    application: QingApplication,
    hasPermissions: Boolean = true,
    onRequestPermissions: () -> Unit = {}
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // 判断是否显示底部导航（子页面隐藏）
    val showBottomBar = currentRoute in Screen.bottomNavItems.map { it.route }

    // Use NavigationRail for tablets/foldables, BottomNavigation for phones
    val useNavigationRail = windowWidthSizeClass != WindowWidthSizeClass.Compact

    if (useNavigationRail) {
        // VerticalPillNavBar for tablets/foldables - overlay on content
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                QingNavHost(
                    navController = navController,
                    demoModeManager = application.demoModeManager,
                    isDarkTheme = isDarkTheme,
                    followSystemTheme = followSystemTheme,
                    onThemeChange = onThemeChange,
                    onFollowSystemChange = onFollowSystemChange,
                    paddingValues = androidx.compose.foundation.layout.PaddingValues(),
                    onLaunchChat = { sessionId ->
                        context.startActivity(ChatActivity.createIntent(context, sessionId))
                    },
                    onLaunchHistory = {
                        context.startActivity(ChatActivity.createHistoryIntent(context))
                    },
                    onLaunchSettings = {
                        context.startActivity(SettingsActivity.createIntent(context))
                    },
                    onLaunchTest = {
                        context.startActivity(TestActivity.createIntent(context))
                    },
                    onLaunchStress = {
                        context.startActivity(StressActivity.createIntent(context))
                    }
                )
            }

            // Vertical pill nav bar overlaid on the left side
            VerticalPillNavBar(
                visible = showBottomBar,
                currentRoute = currentRoute ?: Screen.Home.route,
                navItems = Screen.toFloatingNavItems(),
                onNavigate = { route ->
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier
                    .align(androidx.compose.ui.Alignment.CenterStart)
                    .padding(start = 16.dp)
                    .navigationBarsPadding()
            )
        }
    } else {
        // Floating Pill Navigation for phones - overlay on content
        Box(modifier = Modifier.fillMaxSize()) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                QingNavHost(
                    navController = navController,
                    demoModeManager = application.demoModeManager,
                    isDarkTheme = isDarkTheme,
                    followSystemTheme = followSystemTheme,
                    onThemeChange = onThemeChange,
                    onFollowSystemChange = onFollowSystemChange,
                    paddingValues = androidx.compose.foundation.layout.PaddingValues(),
                    onLaunchChat = { sessionId ->
                        context.startActivity(ChatActivity.createIntent(context, sessionId))
                    },
                    onLaunchHistory = {
                        context.startActivity(ChatActivity.createHistoryIntent(context))
                    },
                    onLaunchSettings = {
                        context.startActivity(SettingsActivity.createIntent(context))
                    },
                    onLaunchTest = {
                        context.startActivity(TestActivity.createIntent(context))
                    },
                    onLaunchStress = {
                        context.startActivity(StressActivity.createIntent(context))
                    }
                )
            }

            // Floating pill navigation overlaid at bottom
            FloatingPillNavBar(
                visible = showBottomBar,
                currentRoute = currentRoute ?: Screen.Home.route,
                navItems = Screen.toFloatingNavItems(),
                onNavigate = { route ->
                    if (currentRoute != route) {
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .align(androidx.compose.ui.Alignment.BottomCenter)
                    .navigationBarsPadding()
            )
        }
    }
}
