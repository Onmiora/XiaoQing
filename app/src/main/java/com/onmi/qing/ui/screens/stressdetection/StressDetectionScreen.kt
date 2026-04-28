package com.onmi.qing.ui.screens.stressdetection

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onmi.qing.viewmodel.StressDetectionViewModel

// 压力检测主屏幕
@Composable
fun StressDetectionScreen(
    viewModel: StressDetectionViewModel,
    onBackClick: () -> Unit,
    onBreathingClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            targetState = uiState.currentStep,
            transitionSpec = {
                val direction = if (targetState.ordinal > initialState.ordinal) 1 else -1
                slideInHorizontally { direction * it } + fadeIn(animationSpec = tween(300)) togetherWith
                    slideOutHorizontally { -direction * it } + fadeOut(animationSpec = tween(300))
            },
            label = "stress_detection_step"
        ) { step ->
            when (step) {
                StressDetectionViewModel.DetectionStep.INIT,
                StressDetectionViewModel.DetectionStep.BLE_SCANNING -> {
                    BleScanningScreen(
                        bleState = uiState.bleState,
                        discoveredDevices = uiState.discoveredDevices,
                        onStartScan = { viewModel.startBleScan() },
                        onStopScan = { viewModel.stopBleScan() },
                        onDeviceSelected = { viewModel.selectDevice(it) },
                        onUseDemoMode = { viewModel.useDemoMode() },
                        onBackClick = onBackClick
                    )
                }
                StressDetectionViewModel.DetectionStep.DATA_COLLECTION -> {
                    // 并行显示：心率监测（紧凑模式）+ 问卷
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                    ) {
                        // 上方：心率监测（紧凑模式）
                        CompactHeartRateCard(
                            currentHeartRate = uiState.currentHeartRate,
                            progress = uiState.heartRateCollectionProgress,
                            isDemoMode = uiState.isDemoMode,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 12.dp)
                        )
                        QuestionnaireScreen(
                            questions = uiState.questions,
                            currentIndex = uiState.currentQuestionIndex,
                            answers = uiState.answers,
                            progress = uiState.questionnaireProgress,
                            heartRateProgress = uiState.heartRateCollectionProgress,
                            onAnswer = { qId, opt -> viewModel.answerQuestion(qId, opt) },
                            onNext = { viewModel.nextQuestion() },
                            onComplete = { viewModel.calculateResult() },
                            onBackClick = onBackClick,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                StressDetectionViewModel.DetectionStep.WAITING_FOR_COLLECTION -> {
                    WaitingCollectionScreen(
                        currentHeartRate = uiState.currentHeartRate,
                        progress = uiState.heartRateCollectionProgress,
                        isDemoMode = uiState.isDemoMode,
                        onBackClick = onBackClick
                    )
                }
                StressDetectionViewModel.DetectionStep.RESULT -> {
                    uiState.stressResult?.let { result ->
                        StressResultScreen(
                            result = result,
                            onDone = onBackClick,
                            onRetry = { viewModel.reset() },
                            onBreathingClick = onBreathingClick
                        )
                    }
                }
            }
        }

        // 计算中加载指示器
        if (uiState.isCalculating) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)),
                contentAlignment = androidx.compose.ui.Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "正在分析您的压力数据...",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

// 紧凑型心率显示卡片
@Composable
fun CompactHeartRateCard(
    currentHeartRate: Int?,
    progress: Float,
    isDemoMode: Boolean,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 心率图标和数值
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = currentHeartRate?.toString() ?: "--",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = "BPM",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // 采集进度
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .width(80.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                    color = MaterialTheme.colorScheme.primary
                )
                if (isDemoMode) {
                    Text(
                        text = "演示模式",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
        }
    }
}
