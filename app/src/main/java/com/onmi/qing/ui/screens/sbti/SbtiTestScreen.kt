package com.onmi.qing.ui.screens.sbti

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onmi.qing.ui.screens.quiz.QuizScreen
import com.onmi.qing.viewmodel.SbtiViewModel

// SBTI测试主屏幕 - 管理测试的各个阶段（筛选门、正式测试、结果）
@Composable
fun SbtiTestScreen(
    viewModel: SbtiViewModel,
    onBackClick: () -> Unit,
    onComplete: (isRetake: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    // 根据当前步骤显示不同内容
    when (uiState.currentStep) {
        SbtiViewModel.SbtiStep.GATE_QUESTIONS -> {
            // 筛选门问题
            val currentGateQuestion = uiState.gateQuestions.getOrNull(uiState.gateQuestionIndex)
            val isFirstGateQuestion = uiState.gateQuestionIndex == 0
            val isLastGateQuestion = uiState.gateQuestionIndex == uiState.gateQuestions.size - 1

            QuizScreen(
                title = "个人测试",
                questions = uiState.gateQuestions,
                currentIndex = uiState.gateQuestionIndex,
                answers = uiState.answers,
                progress = if (uiState.gateQuestions.isNotEmpty()) {
                    (uiState.gateQuestionIndex + 1).toFloat() / uiState.gateQuestions.size
                } else 0f,
                currentQuestion = currentGateQuestion,
                isFirstQuestion = isFirstGateQuestion,
                isLastQuestion = isLastGateQuestion,
                completeButtonText = "开始测试",
                onAnswer = { questionId, optionIndex ->
                    viewModel.answerGateQuestion(optionIndex)
                },
                onNext = {
                    // 筛选门问题不需要手动下一题，自动跳转
                },
                onPrevious = {
                    // 筛选门问题返回
                    if (!isFirstGateQuestion) {
                        // 简化处理：直接退出到发现页
                        viewModel.exitTest()
                        onBackClick()
                    }
                },
                onComplete = {
                    // 进入正式测试
                    viewModel.reset()
                    viewModel.answerGateQuestion(uiState.answers[-2] ?: 0)
                },
                onBackClick = {
                    viewModel.exitTest()
                    onBackClick()
                },
                modifier = modifier
            )
        }

        SbtiViewModel.SbtiStep.FORMAL_QUESTIONS -> {
            // 正式测试问题
            val currentQuestion = viewModel.getCurrentFormalQuestion()

            QuizScreen(
                title = "SBTI个人测试",
                questions = uiState.formalQuestions,
                currentIndex = uiState.currentQuestionIndex,
                answers = uiState.answers,
                progress = viewModel.getFormalProgress(),
                currentQuestion = currentQuestion,
                isFirstQuestion = viewModel.isFirstQuestion(),
                isLastQuestion = viewModel.isLastQuestion(),
                completeButtonText = "查看结果",
                onAnswer = { questionId, optionIndex ->
                    viewModel.answerQuestion(questionId, optionIndex)
                },
                onNext = {
                    viewModel.nextQuestion()
                },
                onPrevious = {
                    viewModel.previousQuestion()
                },
                onComplete = {
                    viewModel.completeTest()
                },
                onBackClick = {
                    viewModel.exitTest()
                    onBackClick()
                },
                modifier = modifier
            )
        }

        SbtiViewModel.SbtiStep.RESULT -> {
            // 显示结果
            uiState.result?.let { personalityType ->
                SbtiResultScreen(
                    personalityType = personalityType,
                    similarity = uiState.similarity,
                    onRetake = {
                        viewModel.reset()
                    },
                    onBackClick = {
                        viewModel.exitTest()
                        onBackClick()
                    },
                    modifier = modifier
                )
            } ?: run {
                // 如果没有结果，显示加载状态
                Box(
                    modifier = modifier.fillMaxSize(),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
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
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Card(
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally
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
