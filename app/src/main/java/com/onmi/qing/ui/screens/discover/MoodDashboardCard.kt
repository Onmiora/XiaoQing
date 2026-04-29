package com.onmi.qing.ui.screens.discover

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.MoodEntry
import com.onmi.qing.data.MoodType
import com.onmi.qing.ui.components.toMoodColor
import com.onmi.qing.ui.components.toMoodDisplayName
import com.onmi.qing.ui.components.toMoodIcon
import com.onmi.qing.ui.components.toMoodValue
import com.onmi.qing.ui.theme.MoodCalm
import com.onmi.qing.ui.theme.MoodHappy
import com.onmi.qing.ui.theme.MoodUnhappy

// 情绪仪表盘主卡片 - 展示当前情绪、近7天趋势和情绪分布
@Composable
fun MoodDashboardCard(
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
                            imageVector = latestMood?.mood.toMoodIcon(),
                            contentDescription = null,
                            tint = latestMood?.mood.toMoodColor(),
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
    val moodColor = latestMood?.mood.toMoodColor()
    val moodIcon = latestMood?.mood.toMoodIcon()
    val moodText = latestMood?.mood.toMoodDisplayName()

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
        dailyMoods.add(dayEntry?.let { it.mood.toMoodValue() })
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
