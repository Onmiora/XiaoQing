package com.onmi.qing.ui.screens.stressdetection

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.StressQuestion

// 压力问卷屏幕 - 显示压力相关问题（PSS-10风格）
@Composable
fun QuestionnaireScreen(
    questions: List<StressQuestion>,
    currentIndex: Int,
    answers: Map<Int, Int>,
    progress: Float,
    heartRateProgress: Float = 1f,
    onAnswer: (Int, Int) -> Unit,
    onNext: () -> Unit,
    onComplete: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentQuestion = questions.getOrNull(currentIndex)
    val scrollState = rememberScrollState()
    val isLastQuestion = currentIndex == questions.size - 1
    val currentAnswer = currentQuestion?.let { answers[it.id] }
    val canViewResult = isLastQuestion && currentAnswer != null && heartRateProgress >= 1f

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        // 顶部导航和进度
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
                text = "压力评估",
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

                // 问题分类标签
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ) {
                    Text(
                        text = getCategoryText(question.category),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
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

        // 底部按钮
        Button(
            onClick = {
                if (isLastQuestion) {
                    onComplete()
                } else {
                    onNext()
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(20.dp),
            enabled = currentAnswer != null
        ) {
            Text(
                text = when {
                    isLastQuestion && heartRateProgress < 1f -> "等待采集完成..."
                    isLastQuestion -> "查看结果"
                    else -> "下一题"
                },
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}

private fun getCategoryText(category: com.onmi.qing.data.QuestionCategory): String {
    return when (category) {
        com.onmi.qing.data.QuestionCategory.PERCEIVED_STRESS -> "感知压力"
        com.onmi.qing.data.QuestionCategory.EMOTIONAL_DISTRESS -> "情绪困扰"
        com.onmi.qing.data.QuestionCategory.PHYSICAL_SYMPTOMS -> "身体症状"
        com.onmi.qing.data.QuestionCategory.SLEEP_QUALITY -> "睡眠质量"
    }
}
