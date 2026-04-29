package com.onmi.qing.ui.screens.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.PsychologyDimension
import com.onmi.qing.ui.components.AnimatedCard
import com.onmi.qing.ui.components.toDimensionIcon
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.ui.components.GradientProgressBar
import com.onmi.qing.ui.components.adaptiveHorizontalPadding
import com.onmi.qing.ui.components.GlowProgressRing
import com.onmi.qing.ui.components.MoodBottomSheet
import com.onmi.qing.viewmodel.AchievementViewModel
import com.onmi.qing.viewmodel.HomeViewModel
import com.onmi.qing.viewmodel.UsageStatsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    achievementViewModel: AchievementViewModel,
    usageStatsViewModel: UsageStatsViewModel,
    moodViewModel: com.onmi.qing.viewmodel.MoodViewModel,
    demoModeManager: DemoModeManager,
    onStartChatClick: () -> Unit,
    onBreathingClick: () -> Unit,
    onAchievementClick: () -> Unit,
    onStressDetectionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val psychologyDimensions by viewModel.psychologyDimensions.collectAsState()
    val dimensions by viewModel.allDimensions.collectAsState()
    val overallScore = ((psychologyDimensions.moodStability + psychologyDimensions.selfAwareness + psychologyDimensions.stressManagement +
            psychologyDimensions.socialConfidence + psychologyDimensions.sleepQuality + psychologyDimensions.selfCare) / 6 * 100).toInt()
    val achievements by achievementViewModel.achievements.collectAsState()
    val unlockedCount by achievementViewModel.unlockedCount.collectAsState()

    val hour = SimpleDateFormat("HH", Locale.getDefault()).format(Date()).toInt()
    val greeting = when {
        hour < 6 -> "夜深了，注意休息哦"
        hour < 9 -> "早上好！今天也要加油呀"
        hour < 12 -> "上午好！保持好心情"
        hour < 14 -> "中午好！记得吃午饭"
        hour < 18 -> "下午好！来杯水休息一下吧"
        hour < 22 -> "晚上好！今天辛苦了"
        else -> "夜深了，早点休息吧"
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .padding(horizontal = adaptiveHorizontalPadding()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }

            // 1. 问候Banner卡片
            item {
                AnimatedCard(index = 0) {
                    GreetingBanner(
                        greeting = greeting,
                        overallScore = overallScore
                    )
                }
            }

            // 2. 今日签到卡片 - 心理维度横向滚动
            item {
                AnimatedCard(index = 1) {
                    DailyCheckInCard(
                        dimensions = dimensions,
                        onDimensionClick = { }
                    )
                }
            }

            // 3. 快捷入口卡片
            item {
                AnimatedCard(index = 2) {
                    QuickAccessCard(
                        onStartChatClick = onStartChatClick,
                        onBreathingClick = onBreathingClick,
                        onStressDetectionClick = onStressDetectionClick
                    )
                }
            }

            // 4. 今日目标卡片
            item {
                AnimatedCard(index = 3) {
                    val chatCount by usageStatsViewModel.chatCount.collectAsState()
                    val breathingCount by usageStatsViewModel.breathingCount.collectAsState()
                    val checkInCount by usageStatsViewModel.checkInCount.collectAsState()
                    DailyGoalsCard(
                        checkInCount = checkInCount,
                        chatCount = chatCount,
                        breathingCount = breathingCount
                    )
                }
            }

            // 5. 成就概览卡片
            item {
                AnimatedCard(index = 4) {
                    AchievementOverviewCard(
                        unlockedCount = unlockedCount,
                        totalCount = achievements.size,
                        onClick = onAchievementClick
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(130.dp)) }
        }

        // 右下角记录心情按钮
        Button(
            onClick = {
                showBottomSheet = true
                scope.launch { sheetState.show() }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 20.dp, bottom = 120.dp),
            shape = RoundedCornerShape(28.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "记录心情",
                style = MaterialTheme.typography.labelLarge
            )
        }

        if (showBottomSheet) {
            MoodBottomSheet(
                sheetState = sheetState,
                onDismiss = {
                    scope.launch { sheetState.hide() }
                    showBottomSheet = false
                },
                onConfirm = { mood, reason ->
                    moodViewModel.addMoodEntry(mood, reason)
                    scope.launch { sheetState.hide() }
                    showBottomSheet = false
                }
            )
        }
    }
}

// 问候Banner卡片
@Composable
private fun GreetingBanner(
    greeting: String,
    overallScore: Int
) {
    val currentDate = SimpleDateFormat("MM月dd日 E", Locale.getDefault()).format(Date())

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = currentDate,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                // 整体状态徽章
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$overallScore",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Text(
                            text = "心理健康分",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
}

// 今日签到卡片
@Composable
private fun DailyCheckInCard(
    dimensions: List<PsychologyDimension>,
    onDimensionClick: (PsychologyDimension) -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "我的状态",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "← 拖动查看全部 →",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                items(dimensions) { dimension ->
                    DimensionRingItem(
                        dimension = dimension,
                        onClick = { onDimensionClick(dimension) }
                    )
                }
            }
        }
    }
}

@Composable
private fun DimensionRingItem(
    dimension: PsychologyDimension,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(80.dp)
            .clickable(onClick = onClick)
    ) {
        GlowProgressRing(
            progress = dimension.progress,
            size = 70.dp,
            strokeWidth = 6.dp,
            glowColor = Color(dimension.colorHex)
        ) {
            Icon(
                imageVector = dimension.name.toDimensionIcon(),
                contentDescription = dimension.name,
                tint = Color(dimension.colorHex),
                modifier = Modifier.size(26.dp)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = dimension.name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        Text(
            text = "${(dimension.progress * 100).toInt()}%",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = Color(dimension.colorHex)
        )
    }
}

// 快捷入口卡片
@Composable
private fun QuickAccessCard(
    onStartChatClick: () -> Unit,
    onBreathingClick: () -> Unit,
    onStressDetectionClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Psychology,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "小晴建议",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickAccessButton(
                    icon = Icons.Default.Psychology,
                    text = "聊聊",
                    onClick = onStartChatClick,
                    modifier = Modifier.weight(1f),
                    iconTint = MaterialTheme.colorScheme.primary
                )
                QuickAccessButton(
                    icon = Icons.Default.Air,
                    text = "呼吸",
                    onClick = onBreathingClick,
                    modifier = Modifier.weight(1f),
                    iconTint = MaterialTheme.colorScheme.secondary
                )
                QuickAccessButton(
                    icon = Icons.Default.Favorite,
                    text = "压力检测",
                    onClick = onStressDetectionClick,
                    modifier = Modifier.weight(1f),
                    iconTint = MaterialTheme.colorScheme.tertiary
                )
            }
        }
    }
}

@Composable
private fun QuickAccessButton(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    iconTint: Color = MaterialTheme.colorScheme.primary
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

// 今日目标卡片
@Composable
private fun DailyGoalsCard(
    checkInCount: Int,
    chatCount: Int,
    breathingCount: Int
) {
    val hasCheckedIn = checkInCount > 0
    val hasChatted = chatCount > 0
    val hasBreathed = breathingCount > 0

    val goals = listOf(
        GoalItem("与小晴对话", hasChatted, Icons.Default.Psychology),
        GoalItem("呼吸练习", hasBreathed, Icons.Default.Air)
    )

    val completedCount = goals.count { it.isCompleted }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "今日目标",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "$completedCount/2 完成",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            GradientProgressBar(
                progress = completedCount / 3f,
                modifier = Modifier.fillMaxWidth(),
                height = 6.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

            goals.forEach { goal ->
                GoalItemRow(goal = goal)
                if (goal != goals.last()) {
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

private data class GoalItem(val text: String, val isCompleted: Boolean, val icon: ImageVector)

@Composable
private fun GoalItemRow(goal: GoalItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = goal.icon,
            contentDescription = null,
            tint = if (goal.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = goal.text,
            style = MaterialTheme.typography.bodyMedium,
            color = if (goal.isCompleted)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            else
                MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.weight(1f))
        if (goal.isCompleted) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "已完成",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// 成就概览卡片
@Composable
private fun AchievementOverviewCard(
    unlockedCount: Int,
    totalCount: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.tertiary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "本周成就",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "已解锁 $unlockedCount / $totalCount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                GradientProgressBar(
                    progress = if (totalCount > 0) unlockedCount.toFloat() / totalCount else 0f,
                    modifier = Modifier.fillMaxWidth(),
                    height = 6.dp,
                    gradientColors = listOf(
                        MaterialTheme.colorScheme.tertiary,
                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.7f)
                    )
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "查看成就",
                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

