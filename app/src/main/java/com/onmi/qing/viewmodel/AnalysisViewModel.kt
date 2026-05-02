package com.onmi.qing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onmi.qing.data.AnalysisResult
import com.onmi.qing.data.AnalysisState
import com.onmi.qing.data.InsightfulTitles
import com.onmi.qing.data.MockAnalysisGenerator
import com.onmi.qing.data.NoChangeMessages
import com.onmi.qing.data.repository.ChatRepository
import com.onmi.qing.data.datastore.PsychologyDimensions
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.data.remote.AnalyzeApiService
import com.onmi.qing.data.remote.AnalyzeRequest
import com.onmi.qing.data.remote.AnthropicMessage
import com.onmi.qing.data.remote.ApiServiceFactory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val dataStore: QingDataStore,
    private val chatRepository: ChatRepository,
    private val demoModeManager: DemoModeManager,
    private val apiServiceFactory: ApiServiceFactory
) : ViewModel() {

    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    private val _lastAnalysisResult = MutableStateFlow<AnalysisResult?>(null)
    val lastAnalysisResult: StateFlow<AnalysisResult?> = _lastAnalysisResult.asStateFlow()

    fun analyzeSession(sessionId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _analysisState.value = AnalysisState.Analyzing

            try {
                if (demoModeManager.isDemoMode.value) {
                    delay(2000)
                    val dims = dataStore.psychologyDimensions.first()
                    val dimMap = mapOf(
                        "情绪稳定" to dims.moodStability,
                        "自我认知" to dims.selfAwareness,
                        "压力管理" to dims.stressManagement,
                        "社交信心" to dims.socialConfidence,
                        "睡眠质量" to dims.sleepQuality,
                        "自我关怀" to dims.selfCare
                    )
                    val result = MockAnalysisGenerator.generate(dimMap)
                    _lastAnalysisResult.value = result
                    _analysisState.value = AnalysisState.Completed(result)
                    onComplete()
                    return@launch
                }

                val analyzeApiService = apiServiceFactory.create<AnalyzeApiService>()
                val messages = chatRepository.getMessagesForSessionOnce(sessionId)

                val apiMessages = messages.map { msg ->
                    AnthropicMessage(
                        role = if (msg.isFromUser) "user" else "assistant",
                        content = msg.textContent
                    )
                }

                val dims = dataStore.psychologyDimensions.first()
                val currentScores = mapOf(
                    "moodStability" to dims.moodStability,
                    "selfAwareness" to dims.selfAwareness,
                    "stressManagement" to dims.stressManagement,
                    "socialConfidence" to dims.socialConfidence,
                    "sleepQuality" to dims.sleepQuality,
                    "selfCare" to dims.selfCare
                )

                val request = AnalyzeRequest(messages = apiMessages, currentScores = currentScores)
                val response = analyzeApiService.analyze(request)

                if (response.isSuccessful) {
                    val analyzeResponse = response.body()
                    if (analyzeResponse != null) {
                        val latestDims = dataStore.psychologyDimensions.first()
                        processAnalyzeResponse(analyzeResponse, latestDims)
                    } else {
                        val noChangeResult = AnalysisResult(
                            title = InsightfulTitles.random(),
                            subtitle = "基于这段对话，小晴对你的心理状态有了新的发现",
                            dimensionUpdates = emptyList(),
                            summary = null
                        )
                        _lastAnalysisResult.value = noChangeResult
                        _analysisState.value = AnalysisState.Completed(noChangeResult)
                    }
                } else {
                    val errorMessage = when (response.code()) {
                        400 -> "请求参数有误"
                        401 -> "认证失败，请检查API设置"
                        403 -> "没有权限访问"
                        404 -> "分析服务未找到"
                        500 -> "服务器内部错误"
                        else -> "分析失败 (${response.code()})"
                    }
                    _analysisState.value = AnalysisState.Error(errorMessage)
                }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("Unable to resolve host") == true -> "网络连接失败，请检查网络"
                    e.message?.contains("timeout") == true -> "请求超时，请重试"
                    e.message?.contains("connection") == true -> "网络连接失败，请检查网络"
                    else -> "网络错误: ${e.message ?: "未知错误"}"
                }
                _analysisState.value = AnalysisState.Error(errorMessage)
            }

            onComplete()
        }
    }

    private suspend fun processAnalyzeResponse(
        response: com.onmi.qing.data.remote.AnalyzeResponse,
        currentDims: PsychologyDimensions
    ) {
        val deltas = response.deltas
        val updates = mutableListOf<com.onmi.qing.data.DimensionUpdate>()
        val changeThreshold = 0.01f

        val keyToChineseName = mapOf(
            "moodStability" to "情绪稳定",
            "selfAwareness" to "自我认知",
            "stressManagement" to "压力管理",
            "socialConfidence" to "社交信心",
            "sleepQuality" to "睡眠质量",
            "selfCare" to "自我关怀"
        )

        val currentScoresMap = mapOf(
            "moodStability" to currentDims.moodStability,
            "selfAwareness" to currentDims.selfAwareness,
            "stressManagement" to currentDims.stressManagement,
            "socialConfidence" to currentDims.socialConfidence,
            "sleepQuality" to currentDims.sleepQuality,
            "selfCare" to currentDims.selfCare
        )

        val hasSignificantChange = deltas.values.any { kotlin.math.abs(it) >= changeThreshold }

        if (hasSignificantChange) {
            for ((key, delta) in deltas) {
                val currentScore = currentScoresMap[key] ?: continue
                val newScore = (currentScore + delta).coerceIn(0f, 1f)
                val chineseName = keyToChineseName[key] ?: continue

                val changeIcon = when {
                    newScore > currentScore + changeThreshold -> "up"
                    newScore < currentScore - changeThreshold -> "down"
                    else -> "stable"
                }

                if (changeIcon != "stable") {
                    updates.add(
                        com.onmi.qing.data.DimensionUpdate(
                            dimensionName = chineseName,
                            oldScore = currentScore * 100f,
                            newScore = newScore * 100f,
                            changeIcon = changeIcon
                        )
                    )
                    dataStore.updatePsychologyDimension(key, newScore)
                }
            }
        }

        val result = AnalysisResult(
            title = if (updates.isNotEmpty()) InsightfulTitles.random() else NoChangeMessages.random(),
            subtitle = if (updates.isNotEmpty()) {
                "基于这段对话，小晴对你的心理状态有了新的发现"
            } else {
                "小晴在静静倾听中..."
            },
            dimensionUpdates = updates,
            summary = response.summary
        )

        _lastAnalysisResult.value = result
        _analysisState.value = AnalysisState.Completed(result)
    }

    fun resetAnalysisState() {
        _analysisState.value = AnalysisState.Idle
    }
}
