package com.onmi.qing.ui.screens.developer

import android.widget.Toast
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.Achievement
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.viewmodel.AchievementViewModel
import com.onmi.qing.viewmodel.PsychologyViewModel
import com.onmi.qing.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// 开发者选项页面 - 用于调试和测试，手动调整六维度分数和成就状态
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(
    psychologyViewModel: PsychologyViewModel,
    achievementViewModel: AchievementViewModel,
    settingsViewModel: SettingsViewModel,
    demoModeManager: DemoModeManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val psychologyDimensions by psychologyViewModel.psychologyDimensions.collectAsState()
    val achievements by achievementViewModel.achievements.collectAsState()
    val apiUrl by settingsViewModel.apiUrlInternal.collectAsState()
    val isDemoMode by demoModeManager.isDemoMode.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var tempApiUrl by remember(apiUrl) { mutableStateOf(apiUrl) }
    var apiEdited by remember { mutableStateOf(false) }
    var isTestingApi by remember { mutableStateOf(false) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            TopAppBar(
                title = { Text("开发者选项") },
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
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // 测试合集
            item {
                SectionHeader(title = "测试合集")
            }

            item {
                TestCarousel(
                    items = testItems,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }

            // 演示模式开关
            item {
                SectionHeader(title = "演示模式")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    ListItem(
                        headlineContent = { 
                            Text(
                                text = "演示模式",
                                style = MaterialTheme.typography.bodyLarge
                            ) 
                        },
                        supportingContent = {
                            Text(
                                text = if (isDemoMode) 
                                    "当前处于演示模式，重启后恢复用户数据" 
                                else 
                                    "开启后将显示预设的演示内容",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                tint = if (isDemoMode)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingContent = {
                            Switch(
                                checked = isDemoMode,
                                onCheckedChange = { enable ->
                                    scope.launch {
                                        if (enable) {
                                            demoModeManager.enableDemoMode()
                                            Toast.makeText(context, "已开启演示模式", Toast.LENGTH_SHORT).show()
                                        } else {
                                            demoModeManager.disableDemoMode()
                                            Toast.makeText(context, "已关闭演示模式", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            )
                        }
                    )
                }
            }

            // API 地址设置
            item {
                SectionHeader(title = "小晴 AI 配置")
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    ListItem(
                        headlineContent = { Text("API 地址") },
                        supportingContent = {
                            OutlinedTextField(
                                value = tempApiUrl,
                                onValueChange = {
                                    tempApiUrl = it
                                    apiEdited = true
                                },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                shape = RoundedCornerShape(16.dp)
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Cloud,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingContent = {
                            Row {
                                // Test connection button (always visible)
                                IconButton(
                                    onClick = {
                                        isTestingApi = true
                                        scope.launch {
                                            val result = settingsViewModel.testApiConnection(tempApiUrl)
                                            Toast.makeText(context, result, Toast.LENGTH_SHORT).show()
                                            isTestingApi = false
                                        }
                                    },
                                    enabled = !isTestingApi
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Wifi,
                                        contentDescription = "测试连接",
                                        tint = if (isTestingApi)
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                        else
                                            MaterialTheme.colorScheme.primary
                                    )
                                }
                                if (apiEdited) {
                                    IconButton(onClick = {
                                        tempApiUrl = SettingsViewModel.DEFAULT_API_URL
                                        apiEdited = false
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "恢复默认",
                                            tint = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                    IconButton(onClick = {
                                        settingsViewModel.updateApiUrl(tempApiUrl)
                                        apiEdited = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar("API地址已保存")
                                        }
                                    }) {
                                        Icon(
                                            imageVector = Icons.Default.Save,
                                            contentDescription = "保存",
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                        }
                    )
                }
            }

            // 六维度调整
            item {
                SectionHeader(title = "心理维度调整")
            }

            item {
                DimensionSliderCard(
                    label = "情绪稳定",
                    value = psychologyDimensions.moodStability,
                    onValueChange = { psychologyViewModel.setDimensionProgress("moodStability", it) },
                    enabled = !isDemoMode
                )
            }

            item {
                DimensionSliderCard(
                    label = "自我认知",
                    value = psychologyDimensions.selfAwareness,
                    onValueChange = { psychologyViewModel.setDimensionProgress("selfAwareness", it) },
                    enabled = !isDemoMode
                )
            }

            item {
                DimensionSliderCard(
                    label = "压力管理",
                    value = psychologyDimensions.stressManagement,
                    onValueChange = { psychologyViewModel.setDimensionProgress("stressManagement", it) },
                    enabled = !isDemoMode
                )
            }

            item {
                DimensionSliderCard(
                    label = "社交信心",
                    value = psychologyDimensions.socialConfidence,
                    onValueChange = { psychologyViewModel.setDimensionProgress("socialConfidence", it) },
                    enabled = !isDemoMode
                )
            }

            item {
                DimensionSliderCard(
                    label = "睡眠质量",
                    value = psychologyDimensions.sleepQuality,
                    onValueChange = { psychologyViewModel.setDimensionProgress("sleepQuality", it) },
                    enabled = !isDemoMode
                )
            }

            item {
                DimensionSliderCard(
                    label = "自我关怀",
                    value = psychologyDimensions.selfCare,
                    onValueChange = { psychologyViewModel.setDimensionProgress("selfCare", it) },
                    enabled = !isDemoMode
                )
            }

            // 成就管理
            item {
                SectionHeader(title = "成就管理")
            }

            items(achievements) { achievement ->
                AchievementToggleItem(
                    achievement = achievement,
                    onToggle = { unlocked ->
                        if (unlocked) {
                            achievementViewModel.unlockAchievement(achievement.id)
                        } else {
                            achievementViewModel.lockAchievement(achievement.id)
                        }
                    },
                    enabled = !isDemoMode
                )
            }

            item {
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 8.dp)
    )
}

@Composable
private fun DimensionSliderCard(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    enabled: Boolean = true
) {
    var sliderValue by remember(value) { mutableFloatStateOf(value) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (enabled)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = "${(sliderValue * 100).toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = sliderValue,
                onValueChange = { sliderValue = it },
                onValueChangeFinished = { onValueChange(sliderValue) },
                valueRange = 0f..1f,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled
            )
            if (!enabled) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "演示模式下不可调整",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun AchievementToggleItem(
    achievement: Achievement,
    onToggle: (Boolean) -> Unit,
    enabled: Boolean = true
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = achievement.name,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (achievement.isUnlocked)
                        MaterialTheme.colorScheme.primary
                    else if (enabled)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Text(
                    text = achievement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = achievement.isUnlocked,
                onCheckedChange = onToggle,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primary,
                    checkedTrackColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    }
}

// 测试合集 Carousel

private data class TestItem(
    val id: String,
    val title: String,
    val description: String,
    val backgroundColor: Color,
    val accentColor: Color,
    val onClick: () -> Unit
)

private val testItems = listOf(
    TestItem(
        id = "psychology",
        title = "心理测试",
        description = "六维度心理状态评估",
        backgroundColor = Color(0xFFE3F2FD),
        accentColor = Color(0xFF1976D2),
        onClick = { }
    ),
    TestItem(
        id = "memory",
        title = "记忆测试",
        description = "短时记忆能力评估",
        backgroundColor = Color(0xFFE8F5E9),
        accentColor = Color(0xFF388E3C),
        onClick = { }
    ),
    TestItem(
        id = "attention",
        title = "注意力测试",
        description = "专注力与反应速度",
        backgroundColor = Color(0xFFF3E5F5),
        accentColor = Color(0xFF7B1FA2),
        onClick = { }
    ),
    TestItem(
        id = "stress",
        title = "压力测试",
        description = "压力水平与应对能力",
        backgroundColor = Color(0xFFFFF3E0),
        accentColor = Color(0xFFF57C00),
        onClick = { }
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TestCarousel(
    items: List<TestItem>,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { items.size })

    Column(modifier = modifier) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            beyondViewportPageCount = 1
        ) { page ->
            val item = items[page]
            TestCarouselCard(
                item = item,
                onClick = { item.onClick() }
            )
        }

        // 页面指示器
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center
        ) {
            repeat(items.size) { index ->
                val isSelected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (isSelected) 10.dp else 8.dp)
                        .background(
                            color = if (isSelected)
                                MaterialTheme.colorScheme.primary
                            else
                                MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(50)
                        )
                )
            }
        }
    }
}

@Composable
private fun TestCarouselCard(
    item: TestItem,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(
            containerColor = item.backgroundColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold,
                color = item.accentColor
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = item.accentColor.copy(alpha = 0.8f)
            )
        }
    }
}
