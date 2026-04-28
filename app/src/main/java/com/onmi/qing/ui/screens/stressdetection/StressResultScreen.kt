package com.onmi.qing.ui.screens.stressdetection

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.StressLevel
import com.onmi.qing.data.StressResult
import com.onmi.qing.data.getRandomHighStressReminder
import com.onmi.qing.ui.theme.DimensionStress
import com.onmi.qing.ui.theme.MoodCalm
import com.onmi.qing.ui.theme.MoodHappy
import kotlinx.coroutines.delay

// 压力检测结果屏幕
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StressResultScreen(
    result: StressResult,
    onDone: () -> Unit,
    onRetry: () -> Unit,
    onBreathingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stressColor = when (result.stressLevel) {
        StressLevel.LOW -> MoodHappy
        StressLevel.MEDIUM -> DimensionStress
        StressLevel.HIGH -> MaterialTheme.colorScheme.error
    }

    val stressText = when (result.stressLevel) {
        StressLevel.LOW -> "压力较低"
        StressLevel.MEDIUM -> "中等压力"
        StressLevel.HIGH -> "压力较高"
    }

    val stressEmoji = when (result.stressLevel) {
        StressLevel.LOW -> "😊"
        StressLevel.MEDIUM -> "😐"
        StressLevel.HIGH -> "😰"
    }

    // 高压力提醒BottomSheet
    var showHighStressSheet by remember { mutableStateOf(result.stressLevel == StressLevel.HIGH) }
    val sheetState = rememberModalBottomSheetState()

    // 卡片入场动画状态
    var cardVisible by remember { mutableStateOf(false) }
    // 圆环动画触发状态
    var ringAnimationTrigger by remember { mutableStateOf(false) }

    // 页面进入时触发动画
    LaunchedEffect(Unit) {
        // 先让卡片入场
        cardVisible = true
        // 等待卡片入场动画完成（400ms + 延迟）后触发圆环动画
        delay(500)
        ringAnimationTrigger = true
    }

    if (showHighStressSheet && result.stressLevel == StressLevel.HIGH) {
        ModalBottomSheet(
            onDismissRequest = { showHighStressSheet = false },
            sheetState = sheetState,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            HighStressReminderContent(
                onBreathingClick = {
                    showHighStressSheet = false
                    onBreathingClick()
                },
                onDismiss = {
                    showHighStressSheet = false
                }
            )
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            // 操作按钮 - 固定在底部
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onRetry,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("重新检测")
                }
                Button(
                    onClick = onDone,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Text("完成")
                }
            }
        }
    ) { paddingValues ->
        // 内容入场动画
        val alpha by animateFloatAsState(
            targetValue = if (cardVisible) 1f else 0f,
            animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
            label = "content_alpha"
        )
        val offsetY by animateFloatAsState(
            targetValue = if (cardVisible) 0f else 50f,
            animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
            label = "content_offset"
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState())
                .graphicsLayer {
                    this.alpha = alpha
                    this.translationY = offsetY
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            // 标题
            Text(
                text = "检测结果",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 免责声明
            DisclaimerCard()

            Spacer(modifier = Modifier.height(24.dp))

            // 压力分数圆环
            Box(
                modifier = Modifier.size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                StressScoreRing(
                    score = result.stressScore,
                    color = stressColor,
                    animate = ringAnimationTrigger,
                    modifier = Modifier.fillMaxSize()
                )
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.offset(y = (-8).dp)
                ) {
                    AnimatedScoreText(
                        score = result.stressScore,
                        color = stressColor,
                        animate = ringAnimationTrigger
                    )
                    Text(
                        text = stressText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 压力等级卡片
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(stressColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stressEmoji,
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = "您的压力等级",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stressText,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = stressColor
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 心率摘要卡片
            result.heartRateData?.let { hrData ->
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "心率数据",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            HeartRateStatItem(label = "平均", value = "${hrData.averageHR}", unit = "BPM")
                            HeartRateStatItem(label = "最低", value = "${hrData.minHR}", unit = "BPM")
                            HeartRateStatItem(label = "最高", value = "${hrData.maxHR}", unit = "BPM")
                        }

                        hrData.hrvMetric?.let { hrv ->
                            Spacer(modifier = Modifier.height(12.dp))
                            Divider(color = MaterialTheme.colorScheme.outlineVariant)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "HRV指标: ",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = String.format("%.1f", hrv),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = " ms",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))


            // 问卷得分
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "问卷得分",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "${result.questionnaireScore}%",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 分析说明
            result.heartRateData?.let { hrData ->
                AnalysisExplanationCard(
                    hrScore = hrData.hrvScore ?: 50,
                    questionnaireScore = result.questionnaireScore,
                    averageHR = hrData.averageHR,
                    hrvMetric = hrData.hrvMetric
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 建议列表
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "个性化建议",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                result.recommendations.forEach { recommendation ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MoodHappy,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = recommendation,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StressScoreRing(
    score: Int,
    color: Color,
    animate: Boolean = true,
    modifier: Modifier = Modifier
) {
    // 进度动画：从 0 到目标分数
    val animatedProgress by animateFloatAsState(
        targetValue = if (animate) score.toFloat() else 0f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = FastOutSlowInEasing
        ),
        label = "score"
    )

    Canvas(modifier = modifier) {
        val strokeWidth = 16.dp.toPx()
        val radius = (size.minDimension - strokeWidth) / 2
        val center = Offset(size.width / 2, size.height / 2)

        // 背景圆环
        drawArc(
            color = Color.Gray.copy(alpha = 0.2f),
            startAngle = -90f,
            sweepAngle = 360f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )

        // 进度圆环
        drawArc(
            color = color,
            startAngle = -90f,
            sweepAngle = 360f * (animatedProgress / 100f),
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = Size(radius * 2, radius * 2),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
        )
    }
}

// 数字动画文本 - 数字从0增长到目标值
@Composable
private fun AnimatedScoreText(
    score: Int,
    color: Color,
    animate: Boolean,
    modifier: Modifier = Modifier
) {
    // 数字动画
    val animatedValue by animateFloatAsState(
        targetValue = if (animate) score.toFloat() else 0f,
        animationSpec = tween(
            durationMillis = 1500,
            easing = FastOutSlowInEasing
        ),
        label = "score_number"
    )

    Text(
        text = animatedValue.toInt().toString(),
        style = MaterialTheme.typography.headlineLarge,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
    )
}

@Composable
private fun HeartRateStatItem(
    label: String,
    value: String,
    unit: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = unit,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// 分析说明卡片 - 解释心率和问卷结果如何得出最终结论
@Composable
private fun AnalysisExplanationCard(
    hrScore: Int,
    questionnaireScore: Int,
    averageHR: Int,
    hrvMetric: Float?
) {
    val hrInterpretation = when {
        hrScore <= 35 -> "心率变异性较好，身体处于放松状态"
        hrScore <= 65 -> "心率变异性正常，身体状态平稳"
        else -> "心率变异性偏低，身体可能处于紧张状态"
    }

    val hrDetail = buildString {
        append("平均心率为 ${averageHR} BPM")
        hrvMetric?.let {
            append("，HRV指标为 ${String.format("%.1f", it)} ms")
        }
    }

    val questionnaireInterpretation = when {
        questionnaireScore <= 35 -> "问卷结果显示您的主观压力感受较轻"
        questionnaireScore <= 65 -> "问卷结果显示您的主观压力感受适中"
        else -> "问卷结果显示您的主观压力感受较明显"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Insights,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "结果分析",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 心率分析
            Text(
                text = "心率分析：$hrInterpretation",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = hrDetail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 问卷分析
            Text(
                text = "问卷分析：$questionnaireInterpretation",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 综合说明
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "综合判定",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "问卷60% + 心率40%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "结合心率数据和问卷结果，系统综合评估您的压力状态",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HighStressReminderContent(
    onBreathingClick: () -> Unit,
    onDismiss: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .padding(bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 标题
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "💙",
                style = MaterialTheme.typography.headlineMedium
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "小晴关心你",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 提醒内容
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            )
        ) {
            Text(
                text = getRandomHighStressReminder(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 呼吸练习按钮
        Button(
            onClick = onBreathingClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            )
        ) {
            Icon(
                imageVector = Icons.Default.Air,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "前往呼吸练习",
                style = MaterialTheme.typography.labelLarge
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 稍后再说
        TextButton(onClick = onDismiss) {
            Text(
                text = "稍后再说",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
