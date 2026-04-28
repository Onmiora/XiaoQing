package com.onmi.qing.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onmi.qing.ble.HeartRateManager
import com.onmi.qing.data.*
import com.onmi.qing.data.demo.DemoModeManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlin.math.sqrt

// 压力检测 ViewModel
class StressDetectionViewModel(
    private val context: Context,
    private val demoModeManager: DemoModeManager
) : ViewModel() {

    // 检测流程状态
    enum class DetectionStep {
        INIT,              // 初始状态
        BLE_SCANNING,      // BLE扫描
        DATA_COLLECTION,   // 并行: 心率采集 + 答题同时进行
        WAITING_FOR_COLLECTION, // 答题完成，等待采集完成
        RESULT             // 结果
    }

    // UI状态
    data class UiState(
        val currentStep: DetectionStep = DetectionStep.INIT,
        val isDemoMode: Boolean = false,
        // BLE状态
        val bleState: HeartRateManager.BleState = HeartRateManager.BleState.Idle,
        val discoveredDevices: List<BleDevice> = emptyList(),
        val currentHeartRate: Int? = null,
        val heartRateCollectionProgress: Float = 0f,  // 0-1
        val collectedHeartRateData: List<HeartRateData> = emptyList(),
        // 问卷状态
        val questionnaireProgress: Float = 0f,  // 0-1
        val answers: Map<Int, Int> = emptyMap(),  // questionId -> selectedOptionIndex
        val currentQuestionIndex: Int = 0,
        val questions: List<StressQuestion> = emptyList(),
        val isQuestionnaireCompleted: Boolean = false,  // 问卷是否已完成
        // 结果
        val stressResult: StressResult? = null,
        val isCalculating: Boolean = false
    )

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val heartRateManager = HeartRateManager(context)

    init {
        // 收集BLE状态
        viewModelScope.launch {
            combine(
                heartRateManager.bleState,
                heartRateManager.discoveredDevices,
                heartRateManager.currentHeartRate,
                heartRateManager.heartRateData
            ) { bleState, devices, hr, hrData ->
                val progress = heartRateManager.getCollectionProgress()
                val currentStep = _uiState.value.currentStep
                val isQuestionnaireDone = _uiState.value.isQuestionnaireCompleted

                // 如果处于等待状态且采集完成，自动跳转到结果
                if (currentStep == DetectionStep.WAITING_FOR_COLLECTION && progress >= 1f) {
                    calculateResult()
                }

                _uiState.update { state ->
                    state.copy(
                        bleState = bleState,
                        discoveredDevices = devices,
                        currentHeartRate = hr,
                        collectedHeartRateData = hrData,
                        heartRateCollectionProgress = progress
                    )
                }
            }.collect()
        }

        // 初始化问卷问题
        _uiState.update { it.copy(questions = getStressQuestions()) }
    }

// 开始扫描BLE设备
    fun startBleScan() {
        _uiState.update { it.copy(currentStep = DetectionStep.BLE_SCANNING) }
        heartRateManager.startScan()
    }

// 停止扫描
    fun stopBleScan() {
        heartRateManager.stopScan()
    }

    // 选择设备
    fun selectDevice(device: BleDevice) {
        heartRateManager.connectDevice(device)
        _uiState.update { it.copy(currentStep = DetectionStep.DATA_COLLECTION) }
    }

    // 跳过BLE，直接使用演示模式
    fun useDemoMode() {
        heartRateManager.startDemoMode()
        _uiState.update { it.copy(
            currentStep = DetectionStep.DATA_COLLECTION,
            isDemoMode = true
        ) }
    }

// 回答问题
    fun answerQuestion(questionId: Int, optionIndex: Int) {
        _uiState.update { state ->
            val newAnswers = state.answers.toMutableMap()
            newAnswers[questionId] = optionIndex
            val answeredCount = newAnswers.size
            val nextIndex = minOf(answeredCount, state.questions.size - 1)

            val allQuestionsAnswered = answeredCount == state.questions.size

            // 如果问卷完成但采集未完成，切换到等待状态
            val newStep = if (allQuestionsAnswered && state.heartRateCollectionProgress < 1f) {
                DetectionStep.WAITING_FOR_COLLECTION
            } else {
                state.currentStep
            }

            state.copy(
                answers = newAnswers,
                questionnaireProgress = answeredCount.toFloat() / state.questions.size,
                currentQuestionIndex = nextIndex,
                isQuestionnaireCompleted = allQuestionsAnswered,
                currentStep = newStep
            )
        }
    }

// 下一题
    fun nextQuestion() {
        _uiState.update { state ->
            val nextIndex = minOf(state.currentQuestionIndex + 1, state.questions.size - 1)
            state.copy(currentQuestionIndex = nextIndex)
        }
    }

// 计算压力结果
    fun calculateResult() {
        // 如果采集未完成，不允许计算结果
        if (_uiState.value.heartRateCollectionProgress < 1f) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isCalculating = true) }

            // 模拟计算过程
            delay(1500)

            val state = _uiState.value
            val hrData = state.collectedHeartRateData
            val answers = state.answers
            val questions = state.questions

            // 计算问卷得分 (0-100)
            val questionnaireScore = calculateQuestionnaireScore(questions, answers)

            // 计算心率得分 (0-100)
            val hrScore = calculateHeartRateScore(hrData)

            // 综合得分 (问卷60% + 心率40%)
            val finalScore = (questionnaireScore * 0.6 + hrScore * 0.4).toInt()

            val stressLevel = when {
                finalScore <= 35 -> StressLevel.LOW
                finalScore <= 65 -> StressLevel.MEDIUM
                else -> StressLevel.HIGH
            }

            val result = StressResult(
                stressScore = finalScore.coerceIn(0, 100),
                stressLevel = stressLevel,
                heartRateData = createHeartRateSummary(hrData),
                questionnaireScore = questionnaireScore,
                recommendations = generateRecommendations(stressLevel, hrData)
            )

            _uiState.update { it.copy(
                stressResult = result,
                currentStep = DetectionStep.RESULT,
                isCalculating = false
            ) }
        }
    }

// 重置检测流程
    fun reset() {
        heartRateManager.stopDemoMode()
        heartRateManager.disconnect()
        _uiState.value = UiState(questions = getStressQuestions())
    }

// 计算问卷得分
    private fun calculateQuestionnaireScore(questions: List<StressQuestion>, answers: Map<Int, Int>): Int {
        if (answers.isEmpty()) return 0

        var totalWeight = 0
        var maxWeight = 0

        questions.forEach { question ->
            answers[question.id]?.let { selectedIndex ->
                if (selectedIndex < question.weights.size) {
                    totalWeight += question.weights[selectedIndex]
                    maxWeight += question.weights.maxOrNull() ?: 4
                }
            }
        }

        return if (maxWeight > 0) {
            ((totalWeight.toFloat() / maxWeight) * 100).toInt()
        } else {
            0
        }
    }

    // 计算心率得分 (基于HRV分析)
    private fun calculateHeartRateScore(hrData: List<HeartRateData>): Int {
        if (hrData.size < 30) {
            // 数据不足，使用中等分数
            return 50
        }

        // 计算RMSSD
        val rmssd = calculateRMSSD(hrData)

        // RMSSD越低表示压力越大（迷走神经张力降低）
        return when {
            rmssd > 50 -> 15  // 低压力 - RMSSD高，放松状态
            rmssd > 40 -> 30  // 中低压力
            rmssd > 30 -> 45  // 中等压力
            rmssd > 20 -> 65  // 中高压力
            rmssd > 10 -> 80  // 高压力 - RMSSD低，紧张状态
            else -> 95        // 极高压力 - RMSSD非常低
        }
    }

    // 计算RMSSD
    private fun calculateRMSSD(hrData: List<HeartRateData>): Float {
        if (hrData.size < 2) return 30f

        val hrvValues = hrData.mapNotNull { it.hrv }.filter { it > 0 }
        if (hrvValues.size < 2) {
            // 如果没有HRV数据，使用心率变异性估算
            val hrValues = hrData.map { it.heartRate }
            val diffs = hrValues.zipWithNext { a, b -> kotlin.math.abs(a - b) }
            val avgDiff = diffs.average().toFloat()
            return avgDiff * 10 // 估算RMSSD
        }

        val successiveDiffs = hrvValues.zipWithNext { a, b -> (a - b).toFloat() }
            .map { it * it }
            .average()

        return sqrt(successiveDiffs).toFloat()
    }

    // 创建心率数据摘要
    private fun createHeartRateSummary(hrData: List<HeartRateData>): HeartRateSummary? {
        if (hrData.isEmpty()) return null

        val heartRates = hrData.map { it.heartRate }
        val hrvValues = hrData.mapNotNull { it.hrv }.filter { it > 0 }

        return HeartRateSummary(
            averageHR = heartRates.average().toInt(),
            minHR = heartRates.minOrNull() ?: 0,
            maxHR = heartRates.maxOrNull() ?: 0,
            hrvMetric = if (hrvValues.isNotEmpty()) calculateRMSSD(hrData) else null,
            hrvScore = if (hrvValues.isNotEmpty()) calculateHeartRateScore(hrData) else null
        )
    }

    // 生成建议
    private fun generateRecommendations(stressLevel: StressLevel, hrData: List<HeartRateData>): List<String> {
        return when (stressLevel) {
            StressLevel.LOW -> listOf(
                "继续保持良好的生活习惯",
                "定期进行呼吸练习有助于维持身心健康",
                "建议保持规律的運動习惯"
            )
            StressLevel.MEDIUM -> listOf(
                "建议每天花10-15分钟进行深呼吸练习",
                "保持规律的运动习惯有助于缓解压力",
                "尝试记录情绪日记，关注压力来源",
                "保证充足的睡眠时间（7-8小时）"
            )
            StressLevel.HIGH -> listOf(
                "建议寻求专业心理咨询支持",
                "每天进行呼吸练习可以有效缓解焦虑",
                "尝试与朋友或家人倾诉分享",
                "考虑减少工作/学习压力源",
                "如持续感到压力过大，请及时就医"
            )
        }
    }

    class Factory(
        private val context: Context,
        private val demoModeManager: DemoModeManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StressDetectionViewModel(context, demoModeManager) as T
        }
    }
}
