package com.onmi.qing.ui.screens.quiz

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.SbtiQuestion

// 可复用的测试答题页面组件 - 支持筛选门问题和正式测试问题
@Composable
fun QuizScreen(
    title: String,
    questions: List<SbtiQuestion>,
    currentIndex: Int,
    answers: Map<Int, Int>,
    progress: Float,
    currentQuestion: SbtiQuestion?,
    isFirstQuestion: Boolean,
    isLastQuestion: Boolean,
    completeButtonText: String = "查看结果",
    onAnswer: (Int, Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onComplete: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentAnswer = currentQuestion?.let { answers[it.id] }
    val scrollState = rememberScrollState()

    // 防止初始加载时触发自动跳转
    var isInitial by remember { mutableStateOf(true) }

    // 当选择答案时自动跳转下一题（但不处理初始加载的情况）
    LaunchedEffect(currentAnswer) {
        if (isInitial) {
            isInitial = false
            return@LaunchedEffect
        }
        if (currentAnswer != null && !isLastQuestion) {
            // 延迟一小段时间让用户看到选择效果
            kotlinx.coroutines.delay(300)
            onNext()
        }
    }

    // 监听 currentIndex 变化，重置 isInitial 标志
    LaunchedEffect(currentIndex) {
        isInitial = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        // 顶部导航和标题
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回"
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(48.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 进度条
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "问题 ${currentIndex + 1} / ${questions.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${(progress * 100).toInt()}%",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 问题内容
        currentQuestion?.let { question ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(scrollState)
            ) {
                // 问题文本
                Text(
                    text = question.question,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 问题分类标签（如果是正式问题）
                if (!question.isGateQuestion) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = getDimensionText(question.dimension.name),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 选项列表
                question.options.forEachIndexed { index, option ->
                    val isSelected = currentAnswer == index

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onAnswer(question.id, index) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surface,
                        border = if (isSelected) null else androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 选项指示器
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.surfaceVariant
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 底部按钮行
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 上一题按钮
            OutlinedButton(
                onClick = onPrevious,
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                enabled = !isFirstQuestion
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "上一题",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            // 下一题/查看结果按钮
            Button(
                onClick = {
                    if (isLastQuestion) {
                        onComplete()
                    } else {
                        onNext()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                enabled = currentAnswer != null || isLastQuestion
            ) {
                Text(
                    text = if (isLastQuestion) completeButtonText else "下一题",
                    style = MaterialTheme.typography.labelLarge
                )
                if (!isLastQuestion) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier
                            .size(20.dp)
                            .graphicsLayer { scaleX = -1f }
                    )
                }
            }
        }
    }
}

// 获取维度的中文描述
private fun getDimensionText(dimensionCode: String): String {
    return when (dimensionCode) {
        "S1" -> "自尊自信"
        "S2" -> "自我清晰度"
        "S3" -> "核心价值"
        "E1" -> "依恋安全感"
        "E2" -> "情感投入度"
        "E3" -> "边界与依赖"
        "A1" -> "世界观倾向"
        "A2" -> "规则与灵活度"
        "A3" -> "人生意义感"
        "Ac1" -> "动机导向"
        "Ac2" -> "决策风格"
        "Ac3" -> "执行模式"
        "So1" -> "社交主动性"
        "So2" -> "人际边界感"
        "So3" -> "表达与真实度"
        else -> dimensionCode
    }
}
