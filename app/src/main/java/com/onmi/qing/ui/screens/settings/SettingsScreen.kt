package com.onmi.qing.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onmi.qing.ui.components.ConstrainedWidthContainer
import com.onmi.qing.viewmodel.AchievementViewModel
import com.onmi.qing.viewmodel.SettingsViewModel
import kotlinx.coroutines.launch

// 设置页面
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsViewModel: SettingsViewModel,
    achievementViewModel: AchievementViewModel,
    isDarkTheme: Boolean,
    followSystemTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onFollowSystemChange: (Boolean) -> Unit,
    onBackClick: () -> Unit,
    onDeveloperClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val chatCount by settingsViewModel.chatCount.collectAsState()
    val breathingCount by settingsViewModel.breathingCount.collectAsState()
    val achievementCount = achievementViewModel.getUnlockedCount()
    val totalAchievement = achievementViewModel.getTotalCount()

    var showClearConfirmDialog by remember { mutableStateOf(false) }
    var showResetDimensionDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var versionClickCount by remember { mutableStateOf(0) }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            TopAppBar(
                title = { Text("设置") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    ) { paddingValues ->
        ConstrainedWidthContainer(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
            // 外观
            item {
                SettingsSectionHeader(title = "外观")
            }

            item {
                SettingsCard {
                    // 跟随系统开关
                    SettingsToggleItem(
                        icon = Icons.Default.BrightnessAuto,
                        title = "跟随系统",
                        subtitle = if (followSystemTheme) "根据系统设置切换" else "已禁用",
                        checked = followSystemTheme,
                        onCheckedChange = { onFollowSystemChange(it) }
                    )

                    HorizontalDivider()

                    // 深色模式开关（跟随系统时禁用）
                    SettingsToggleItem(
                        icon = if (isDarkTheme) Icons.Default.DarkMode else Icons.Default.LightMode,
                        title = "深色模式",
                        subtitle = if (followSystemTheme) "跟随系统" else if (isDarkTheme) "已开启" else "已关闭",
                        checked = isDarkTheme,
                        onCheckedChange = { onThemeChange(it) },
                        enabled = !followSystemTheme
                    )
                }
            }

            // 数据管理
            item {
                SettingsSectionHeader(title = "数据管理")
            }

            item {
                SettingsCard {
                    // 当前数据统计
                    ListItem(
                        headlineContent = { Text("当前数据") },
                        supportingContent = {
                            Text(
                                text = "对话 ${chatCount} 次 | 呼吸 ${breathingCount} 次 | 成就 ${achievementCount}/${totalAchievement} 个",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    )

                    HorizontalDivider()

                    // 重置维度分
                    ListItem(
                        headlineContent = { Text("重置维度分") },
                        supportingContent = {
                            Text(
                                "让小晴重新认识你",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            TextButton(onClick = { showResetDimensionDialog = true }) {
                                Text("重置")
                            }
                        }
                    )

                    HorizontalDivider()

                    // 清除数据
                    ListItem(
                        headlineContent = {
                            Text(
                                "清除所有个人状态",
                                color = MaterialTheme.colorScheme.error
                            )
                        },
                        supportingContent = {
                            Text(
                                "重置对话次数、练习记录等数据",
                                style = MaterialTheme.typography.bodySmall
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error
                            )
                        },
                        trailingContent = {
                            TextButton(onClick = { showClearConfirmDialog = true }) {
                                Text("清除", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )
                }
            }

            // 版本信息
            item {
                SettingsSectionHeader(title = "关于")
            }

            item {
                SettingsCard {
                    ListItem(
                        headlineContent = { Text("应用版本") },
                        supportingContent = { Text("Qing 1.0 apolo by Onmiora") },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        modifier = Modifier.clickable {
                            versionClickCount++
                            if (versionClickCount >= 5) {
                                onDeveloperClick()
                                versionClickCount = 0
                            }
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
            }
        }
    }

    // 清除确认对话框
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error
                )
            },
            title = { Text("清除所有个人状态？") },
            text = {
                Text("此操作将重置所有对话记录、呼吸练习次数、签到天数和成就进度。此操作不可撤销。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.clearAllState {
                            scope.launch {
                                snackbarHostState.showSnackbar("已清除所有个人状态")
                            }
                        }
                        showClearConfirmDialog = false
                    }
                ) {
                    Text("确认清除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // 重置维度分确认对话框
    if (showResetDimensionDialog) {
        AlertDialog(
            onDismissRequest = { showResetDimensionDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            },
            title = { Text("重置维度分？") },
            text = {
                Text("此操作将把情绪稳定、自我认知、压力管理、社交自信、睡眠质量和自我关爱六个维度重置为默认值，让小晴可以重新认识你。")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.resetDimensionScores {
                            scope.launch {
                                snackbarHostState.showSnackbar("已重置维度分，小晴正在重新认识你...")
                            }
                        }
                        showResetDimensionDialog = false
                    }
                ) {
                    Text("确认重置")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDimensionDialog = false }) {
                    Text("取消")
                }
            }
        )
    }
}

// 分组标题
@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// 设置卡片容器
@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        content()
    }
}

// 开关设置项
@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle) },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = if (enabled) 1f else 0.5f)
            )
        },
        trailingContent = {
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer,
                    disabledCheckedThumbColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f),
                    disabledCheckedTrackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f)
                )
            )
        }
    )
}
