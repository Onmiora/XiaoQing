package com.onmi.qing.ui.screens.mbti

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
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.MbtiQuestion
import com.onmi.qing.viewmodel.MbtiViewModel

// MBTI测试主屏幕
@Composable
fun MbtiTestScreen(
    viewModel: MbtiViewModel,
    onBackClick: () -> Unit,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentQuestion = viewModel.getCurrentQuestion()
    val currentAnswer = viewModel.getCurrentAnswer()
    val scrollState = rememberScrollState()

    // 防止初始加载时触发自动跳转
    var isInitial by remember { mutableStateOf(true) }

    // 当选择答案时自动跳转下一题
    LaunchedEffect(currentAnswer) {
        if (isInitial) {
            isInitial = false
            return@LaunchedEffect
        }
        if (currentAnswer != null && !viewModel.isLastQuestion()) {
            kotlinx.coroutines.delay(300)
            viewModel.nextQuestion()
        }
    }

    // 监听 currentIndex 变化，重置 isInitial 标志
    LaunchedEffect(uiState.currentQuestionIndex) {
        isInitial = true
    }

    // 显示结果页面
    if (uiState.isComplete && uiState.result != null) {
        MbtiResultScreen(
            mbtiType = uiState.result!!,
            description = uiState.resultDescription,
            onRetake = {
                viewModel.reset()
            },
            onBackClick = {
                viewModel.exitTest()
                onBackClick()
            },
            modifier = modifier
        )
        return
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
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
                text = "MBTI测试",
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
            progress = { viewModel.getProgress() },
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
                text = "问题 ${uiState.currentQuestionIndex + 1} / ${uiState.questions.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${viewModel.getProgressPercent()}%",
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

                // 维度标签
                viewModel.getCurrentDimension()?.let { dimension ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = getDimensionText(dimension),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // 选项E
                val isESelected = currentAnswer == 0
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.answerQuestion(question.id, 0) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isESelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface,
                    border = if (isESelected) null else androidx.compose.foundation.BorderStroke(
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
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isESelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isESelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = "E",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = question.optionE,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isESelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // 选项I
                val isISelected = currentAnswer == 1
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .clickable { viewModel.answerQuestion(question.id, 1) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (isISelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surface,
                    border = if (isISelected) null else androidx.compose.foundation.BorderStroke(
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
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isISelected)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.surfaceVariant
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isISelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            } else {
                                Text(
                                    text = "I",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = question.optionI,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isISelected)
                                MaterialTheme.colorScheme.onPrimaryContainer
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
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
                onClick = { viewModel.previousQuestion() },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                enabled = !viewModel.isFirstQuestion()
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
                    if (viewModel.isLastQuestion()) {
                        viewModel.completeTest()
                    } else {
                        viewModel.nextQuestion()
                    }
                },
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp),
                enabled = currentAnswer != null || viewModel.isLastQuestion()
            ) {
                Text(
                    text = if (viewModel.isLastQuestion()) "查看结果" else "下一题",
                    style = MaterialTheme.typography.labelLarge
                )
                if (!viewModel.isLastQuestion()) {
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

    // 显示加载状态
    if (uiState.isCalculating) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
        ) {
            Card(
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "正在分析您的答案...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

// 获取维度的中文描述
private fun getDimensionText(dimension: String): String {
    return when (dimension) {
        "E_I" -> "外向/内向"
        "S_N" -> "感觉/直觉"
        "T_F" -> "思考/情感"
        "J_P" -> "判断/知觉"
        else -> dimension
    }
}
