package com.onmi.qing.ui.screens.discover

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import android.content.Intent
import android.net.Uri
import com.onmi.qing.ui.components.adaptiveHorizontalPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.MoodEntry
import com.onmi.qing.data.MoodType
import com.onmi.qing.ui.components.GlowProgressRing
import com.onmi.qing.ui.theme.MoodCalm
import com.onmi.qing.ui.theme.MoodHappy
import com.onmi.qing.ui.theme.MoodUnhappy
import com.onmi.qing.viewmodel.MoodViewModel
import com.onmi.qing.viewmodel.StateViewModel
import com.onmi.qing.data.demo.DemoModeManager

@Composable
fun DiscoverScreen(
    stateViewModel: StateViewModel,
    moodViewModel: MoodViewModel,
    demoModeManager: DemoModeManager,
    onBreathingClick: () -> Unit,
    onAchievementClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onStressDetectionClick: () -> Unit,
    onFunTestClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val achievements by stateViewModel.achievements.collectAsState()
    val breathingCount = stateViewModel.totalBreathingCount.collectAsState().value
    val unlockedCount = stateViewModel.getUnlockedCount()
    val totalCount = stateViewModel.getTotalCount()
    val moodEntries by moodViewModel.moodEntries.collectAsState()
    val latestMood by moodViewModel.latestMood.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 40.dp, bottom = 20.dp)
        ) {
            Text(
                text = "发现",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // Determine layout based on screen width
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        // Calculate max span based on actual columns that fit
        val maxSpan = if (screenWidth >= 560) 2 else 1
        val useSideBySideCards = screenWidth >= 600

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 280.dp),
            contentPadding = PaddingValues(
                horizontal = adaptiveHorizontalPadding(),
                vertical = 8.dp
            ),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // 小晴心理Tips - 置顶显示
            item(span = { GridItemSpan(maxSpan) }) {
                AnimatedCard(index = 0) {
                    TipCard()
                }
            }

            // 情绪仪表盘
            item(span = { GridItemSpan(maxSpan) }) {
                AnimatedCard(index = 1) {
                    MoodDashboardCard(
                        moodEntries = moodEntries,
                        latestMood = latestMood
                    )
                }
            }

            item(span = { GridItemSpan(maxSpan) }) {
                AnimatedCard(index = 2) {
                    StressDetectionCard(
                        onClick = onStressDetectionClick
                    )
                }
            }

            item(span = { GridItemSpan(maxSpan) }) {
                AnimatedCard(index = 3) {
                    BreathingExerciseCard(
                        breathingCount = breathingCount,
                        onClick = onBreathingClick
                    )
                }
            }

            item(span = { GridItemSpan(maxSpan) }) {
                AnimatedCard(index = 4) {
                    FunTestCard(
                        onClick = onFunTestClick
                    )
                }
            }

            if (useSideBySideCards) {
                // Wide screen: show Achievement and History side by side in a Row
                item(span = { GridItemSpan(maxSpan) }) {
                    AnimatedCard(index = 5) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AchievementStatCard(
                                unlockedCount = unlockedCount,
                                totalCount = totalCount,
                                onClick = onAchievementClick,
                                modifier = Modifier.weight(1f)
                            )
                            HistoryStatCard(
                                onClick = onHistoryClick,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            } else {
                // Narrow screen: show Achievement and History separately
                item(span = { GridItemSpan(maxSpan) }) {
                    AnimatedCard(index = 4) {
                        AchievementStatCard(
                            unlockedCount = unlockedCount,
                            totalCount = totalCount,
                            onClick = onAchievementClick
                        )
                    }
                }

                item(span = { GridItemSpan(maxSpan) }) {
                    AnimatedCard(index = 5) {
                        HistoryStatCard(onClick = onHistoryClick)
                    }
                }
            }

            item(span = { GridItemSpan(maxSpan) }) {
                AnimatedCard(index = 6) {
                    HotlineCard()
                }
            }

            // 底部间距，避免被浮动导航栏遮挡
            item(span = { GridItemSpan(maxSpan) }) {
                Spacer(modifier = Modifier.height(130.dp))
            }
        }
    }
}

@Composable
private fun BreathingExerciseCard(
    breathingCount: Int,
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Air,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "呼吸练习",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "4-7-8 放松呼吸法",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "科学的呼吸节奏，帮助你放松身心，缓解焦虑和压力",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "$breathingCount 次练习",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "开始练习",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun FunTestCard(
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Quiz,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "个人测试",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "探索你的性格特点",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "通过有趣的心理测试，发现你的人格特质和潜在优势",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SBTI震撼来袭...",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "开始测试",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun StressDetectionCard(
    onClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "压力检测",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = "科学压力评估",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "通过心率变异性和问卷综合评估您的压力水平",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "了解自身压力",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "开始检测",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementStatCard(
    unlockedCount: Int,
    totalCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlowProgressRing(
                progress = if (totalCount > 0) unlockedCount.toFloat() / totalCount else 0f,
                size = 80.dp,
                strokeWidth = 6.dp,
                glowColor = Color(0xFFFFD700)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = Color(0xFFFFD700),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "成就",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "$unlockedCount / $totalCount 已解锁",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HistoryStatCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(24.dp)
            ),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            GlowProgressRing(
                progress = 1f,
                size = 80.dp,
                strokeWidth = 6.dp,
                glowColor = MaterialTheme.colorScheme.secondary
            ) {
                Icon(
                    imageVector = Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "对话历史",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "查看过往",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TipCard() {
    val tips = listOf(
        "每天花10分钟进行深呼吸练习，可以显著降低皮质醇水平，减少焦虑感。",
        "保持规律作息有助于情绪稳定，每晚7-9小时的睡眠能让身心得到充分恢复。",
        "适度运动能够释放内啡肽，每天30分钟的散步或伸展都能改善心情。",
        "写日记是一种有效的情绪疏导方式，每天花几分钟记录感恩的事能提升幸福感。",
        "社交支持是心理健康的重要保护因素，与朋友或家人倾诉能缓解心理压力。",
        "正念冥想练习能帮助我们更好地觉察当下，减少对过去的后悔和对未来的担忧。",
        "保持充足的水分摄入有助于维持大脑功能，研究表明脱水会影响情绪和认知。",
        "给自己设定合理的目标，完成后及时肯定，能增强自我效能感和自信心。"
    )

    val randomTip = remember { tips.random() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "小晴心理Tips",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = randomTip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// 心理援助热线卡片 - 紧急情况下快速拨打12356寻求专业帮助
@Composable
private fun HotlineCard() {
    val context = androidx.compose.ui.platform.LocalContext.current
    var isLongPressing by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
    var pressProgress by androidx.compose.runtime.remember { androidx.compose.runtime.mutableFloatStateOf(0f) }

    val animatedProgress by animateFloatAsState(
        targetValue = if (isLongPressing) pressProgress else 0f,
        animationSpec = tween(50),
        label = "press_progress"
    )

    val errorColor = MaterialTheme.colorScheme.error

    // LaunchedEffect to handle long press progress
    androidx.compose.runtime.LaunchedEffect(isLongPressing) {
        if (isLongPressing) {
            val totalDuration = 5000L
            val startTime = System.currentTimeMillis()
            while (isLongPressing) {
                val elapsed = System.currentTimeMillis() - startTime
                pressProgress = (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
                if (pressProgress >= 1f) {
                    // Time's up, dial the number
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:12356")
                    }
                    context.startActivity(intent)
                    isLongPressing = false
                    pressProgress = 0f
                    break
                }
                kotlinx.coroutines.delay(50)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "心理援助热线",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "24小时人工服务",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }

                Text(
                    text = "12356",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "如果您正处于心理困扰、情绪崩溃或有紧急心理援助需求，请立即拨打热线，专业的心理咨询师全天候为您服务。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 长按拨号按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(errorColor.copy(alpha = 0.1f))
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                when (event.type) {
                                    androidx.compose.ui.input.pointer.PointerEventType.Press -> {
                                        isLongPressing = true
                                        pressProgress = 0f
                                    }
                                    androidx.compose.ui.input.pointer.PointerEventType.Release -> {
                                        isLongPressing = false
                                        pressProgress = 0f
                                    }
                                }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (animatedProgress > 0f && isLongPressing) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            drawArc(
                                color = errorColor,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = errorColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isLongPressing && animatedProgress > 0f -> "${((1f - animatedProgress) * 5).toInt()}秒后拨打"
                            else -> "长按拨打热线"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = errorColor
                    )
                }
            }
        }
    }
}

// 情绪仪表盘卡片

// 情绪仪表盘主卡片 - 展示当前情绪、近7天趋势和情绪分布
@Composable
private fun MoodDashboardCard(
    moodEntries: List<MoodEntry>,
    latestMood: MoodEntry?
) {
    // 计算近7天的数据
    val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
    val recentEntries = moodEntries.filter { it.timestamp >= sevenDaysAgo }
    
    // 情绪统计
    val happyCount = recentEntries.count { it.mood == MoodType.HAPPY }
    val calmCount = recentEntries.count { it.mood == MoodType.CALM }
    val unhappyCount = recentEntries.count { it.mood == MoodType.UNHAPPY }
    val totalCount = recentEntries.size

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // 标题行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = getMoodIcon(latestMood?.mood),
                            contentDescription = null,
                            tint = getMoodColor(latestMood?.mood),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "情绪仪表盘",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "近7天",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 主要内容区域 - 2x2布局
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 左侧 - 当前情绪
                CurrentMoodSection(
                    latestMood = latestMood,
                    modifier = Modifier.weight(1f)
                )

                // 右侧 - 近7天趋势
                MoodTrendSection(
                    moodEntries = moodEntries,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 左侧 - 记录次数
                MoodCountSection(
                    totalCount = totalCount,
                    modifier = Modifier.weight(1f)
                )

                // 右侧 - 情绪分布
                MoodDistributionSection(
                    happyCount = happyCount,
                    calmCount = calmCount,
                    unhappyCount = unhappyCount,
                    totalCount = totalCount,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// 当前情绪模块
@Composable
private fun CurrentMoodSection(
    latestMood: MoodEntry?,
    modifier: Modifier = Modifier
) {
    val moodColor = getMoodColor(latestMood?.mood)
    val moodIcon = getMoodIcon(latestMood?.mood)
    val moodText = getMoodText(latestMood?.mood)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = moodColor.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "当前情绪",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(moodColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = moodIcon,
                    contentDescription = null,
                    tint = moodColor,
                    modifier = Modifier.size(26.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = if (latestMood != null) moodText else "未记录",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = moodColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

// 近7天情绪趋势模块 - 迷你折线图
@Composable
private fun MoodTrendSection(
    moodEntries: List<MoodEntry>,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "情绪趋势",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            // 迷你趋势图
            MoodMiniTrendChart(
                moodEntries = moodEntries,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "← 近7天变化",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

// 情绪迷你趋势图
@Composable
private fun MoodMiniTrendChart(
    moodEntries: List<MoodEntry>,
    modifier: Modifier = Modifier
) {
    // 获取最近7天每天的情绪数据
    val sevenDaysAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L
    val today = System.currentTimeMillis()
    
    val dailyMoods = mutableListOf<Float?>()
    
    for (i in 6 downTo 0) {
        val dayStart = today - (i + 1) * 24 * 60 * 60 * 1000L
        val dayEnd = today - i * 24 * 60 * 60 * 1000L
        val dayEntry = moodEntries.find { it.timestamp in dayStart until dayEnd }
        dailyMoods.add(dayEntry?.let { moodToValue(it.mood) })
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val pointSpacing = width / 6f
        val maxY = height * 0.9f
        val minY = height * 0.1f

        // 绘制背景参考线
        drawLine(
            color = Color.Gray.copy(alpha = 0.2f),
            start = Offset(0f, height / 2),
            end = Offset(width, height / 2),
            strokeWidth = 1.dp.toPx()
        )

        // 绘制趋势线
        var lastX: Float? = null
        var lastY: Float? = null

        dailyMoods.forEachIndexed { index, value ->
            if (value != null) {
                val x = index * pointSpacing
                val y = maxY - (value * (maxY - minY))

                if (lastX != null && lastY != null) {
                    // 绘制连接线
                    drawLine(
                        color = Color(0xFF6366F1).copy(alpha = 0.8f),
                        start = Offset(lastX!!, lastY!!),
                        end = Offset(x, y),
                        strokeWidth = 2.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }

                // 绘制数据点
                drawCircle(
                    color = Color(0xFF6366F1),
                    radius = 4.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = Color.White,
                    radius = 2.dp.toPx(),
                    center = Offset(x, y)
                )

                lastX = x
                lastY = y
            }
        }
    }
}

// 记录次数模块
@Composable
private fun MoodCountSection(
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "记录次数",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "$totalCount",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "次",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// 情绪分布模块 - 环形图
@Composable
private fun MoodDistributionSection(
    happyCount: Int,
    calmCount: Int,
    unhappyCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            // 左侧 - 迷你环形图
            MoodDistributionRing(
                happyCount = happyCount,
                calmCount = calmCount,
                unhappyCount = unhappyCount,
                totalCount = totalCount,
                modifier = Modifier.size(48.dp)
            )

            // 右侧 - 图例
            Column {
                MoodLegendItem(
                    color = MoodHappy,
                    label = "开心",
                    count = happyCount
                )
                Spacer(modifier = Modifier.height(4.dp))
                MoodLegendItem(
                    color = MoodCalm,
                    label = "平静",
                    count = calmCount
                )
                Spacer(modifier = Modifier.height(4.dp))
                MoodLegendItem(
                    color = MoodUnhappy,
                    label = "不开心",
                    count = unhappyCount
                )
            }
        }
    }
}

// 情绪分布环形图
@Composable
private fun MoodDistributionRing(
    happyCount: Int,
    calmCount: Int,
    unhappyCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    val animatedHappy by animateFloatAsState(
        targetValue = if (totalCount > 0) happyCount.toFloat() / totalCount else 0f,
        animationSpec = tween(500),
        label = "happy_progress"
    )
    val animatedCalm by animateFloatAsState(
        targetValue = if (totalCount > 0) calmCount.toFloat() / totalCount else 0f,
        animationSpec = tween(500),
        label = "calm_progress"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 6.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        // 背景轨道
        drawCircle(
            color = Color.Gray.copy(alpha = 0.2f),
            radius = radius,
            center = center,
            style = Stroke(width = strokeWidth)
        )

        if (totalCount > 0) {
            // 开心部分
            val happySweep = 360f * animatedHappy
            drawArc(
                color = MoodHappy,
                startAngle = -90f,
                sweepAngle = happySweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 平静部分
            val calmSweep = 360f * animatedCalm
            drawArc(
                color = MoodCalm,
                startAngle = -90f + happySweep,
                sweepAngle = calmSweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            // 不开心部分
            val unhappySweep = 360f - happySweep - calmSweep
            drawArc(
                color = MoodUnhappy,
                startAngle = -90f + happySweep + calmSweep,
                sweepAngle = unhappySweep,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

// 情绪分布图例项
@Composable
private fun MoodLegendItem(
    color: Color,
    label: String,
    count: Int
) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$label $count",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 辅助函数

// 获取心情颜色
@Composable
private fun getMoodColor(mood: MoodType?): Color {
    return when (mood) {
        MoodType.HAPPY -> MoodHappy
        MoodType.CALM -> MoodCalm
        MoodType.UNHAPPY -> MoodUnhappy
        null -> Color.Gray
    }
}

// 获取心情图标
private fun getMoodIcon(mood: MoodType?): ImageVector {
    return when (mood) {
        MoodType.HAPPY -> Icons.Default.SentimentSatisfied
        MoodType.CALM -> Icons.Default.SentimentNeutral
        MoodType.UNHAPPY -> Icons.Default.SentimentDissatisfied
        null -> Icons.Default.SentimentNeutral
    }
}

// 获取心情文字
private fun getMoodText(mood: MoodType?): String {
    return when (mood) {
        MoodType.HAPPY -> "开心"
        MoodType.CALM -> "平静"
        MoodType.UNHAPPY -> "不开心"
        null -> "未记录"
    }
}

// 将心情类型转换为数值 (0-1)
private fun moodToValue(mood: MoodType): Float {
    return when (mood) {
        MoodType.HAPPY -> 1.0f
        MoodType.CALM -> 0.5f
        MoodType.UNHAPPY -> 0.0f
    }
}

// 卡片交错入场动画组件
@Composable
private fun AnimatedCard(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        // 每个卡片依次延迟 60ms 上浮（减少延迟避免卡顿）
        delay(index * 60L)
        startAnimation = true
    }

    // 使用 animateFloatAsState 实现淡入上滑，性能优于 AnimatedVisibility
    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(
            durationMillis = 350,
            easing = FastOutSlowInEasing
        ),
        label = "card_alpha"
    )

    val offsetY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 40f,
        animationSpec = tween(
            durationMillis = 400,
            easing = FastOutSlowInEasing
        ),
        label = "card_offset"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = offsetY
        }
    ) {
        content()
    }
}
