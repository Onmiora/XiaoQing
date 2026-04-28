package com.onmi.qing.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onmi.qing.data.Achievement
import com.onmi.qing.data.AnalysisResult
import com.onmi.qing.data.AnalysisState
import com.onmi.qing.data.InsightfulTitles
import com.onmi.qing.data.Message
import com.onmi.qing.data.MockAnalysisGenerator
import com.onmi.qing.data.NoChangeMessages
import com.onmi.qing.data.PsychologyDimension
import com.onmi.qing.data.datastore.PsychologyDimensions
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.datastore.UsageStats
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.data.remote.AnalyzeApiService
import com.onmi.qing.data.remote.AnalyzeRequest
import com.onmi.qing.data.remote.AnthropicMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

// 全局状态 ViewModel
class StateViewModel(
    private val dataStore: QingDataStore,
    private val demoModeManager: DemoModeManager
) : ViewModel() {

    // 当前是否为演示模式
    val isDemoMode: StateFlow<Boolean> = demoModeManager.isDemoMode

    // 分析 API 服务
    private var analyzeApiService: AnalyzeApiService? = null
    private var currentApiUrl: String? = null

    // 初始化分析 API 服务
    private fun initializeAnalyzeApiService() {
        viewModelScope.launch {
            val prefs = dataStore.userPreferences.first()
            val apiUrl = prefs.apiUrl
            if (apiUrl != currentApiUrl || analyzeApiService == null) {
                currentApiUrl = apiUrl
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                val anthropicInterceptor = Interceptor { chain ->
                    val request = chain.request().newBuilder()
                        .addHeader("anthropic-version", "2023-06-01")
                        .addHeader("Content-Type", "application/json")
                        .build()
                    chain.proceed(request)
                }
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(120, TimeUnit.SECONDS)
                    .writeTimeout(30, TimeUnit.SECONDS)
                    .addInterceptor(anthropicInterceptor)
                    .addInterceptor(loggingInterceptor)
                    .build()
                val baseUrl = if (apiUrl.endsWith("/")) apiUrl else "$apiUrl/"
                val retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(client)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                analyzeApiService = retrofit.create(AnalyzeApiService::class.java)
            }
        }
    }

    // Achievements - 根据模式切换数据源
    val achievements: StateFlow<List<Achievement>> = combine(
        demoModeManager.isDemoMode,
        demoModeManager.demoAchievements,
        dataStore.allAchievements
    ) { isDemo, demoAchievements, userAchievements ->
        if (isDemo) demoAchievements else userAchievements
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Usage stats - 根据模式切换数据源
    val usageStats: StateFlow<UsageStats> = combine(
        demoModeManager.isDemoMode,
        demoModeManager.usageStats,
        dataStore.usageStats
    ) { isDemo, demoStats, userStats ->
        if (isDemo) demoStats else userStats
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UsageStats())

    // Psychology dimensions - 根据模式切换数据源
    val psychologyDimensions: StateFlow<PsychologyDimensions> = combine(
        demoModeManager.isDemoMode,
        demoModeManager.psychologyDimensions,
        dataStore.psychologyDimensions
    ) { isDemo, demoDims, userDims ->
        if (isDemo) demoDims else userDims
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PsychologyDimensions())

    // Unlocked count
    val unlockedCount: StateFlow<Int> = combine(
        demoModeManager.isDemoMode,
        demoModeManager.demoAchievements,
        dataStore.unlockedCount
    ) { isDemo, demoAchievements, userUnlockedCount ->
        if (isDemo) {
            demoAchievements.count { it.isUnlocked }
        } else {
            userUnlockedCount
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Legacy counters for UI compatibility (derived from usageStats)
    val totalChatCount: StateFlow<Int> = MutableStateFlow(0).also { flow ->
        viewModelScope.launch {
            usageStats.collect { stats ->
                flow.value = stats.chatCount
            }
        }
    }

    val totalBreathingCount: StateFlow<Int> = MutableStateFlow(0).also { flow ->
        viewModelScope.launch {
            usageStats.collect { stats ->
                flow.value = stats.breathingCount
            }
        }
    }

    val totalCheckInCount: StateFlow<Int> = MutableStateFlow(0).also { flow ->
        viewModelScope.launch {
            usageStats.collect { stats ->
                flow.value = stats.checkInCount
            }
        }
    }

    // Analysis state
    private val _analysisState = MutableStateFlow<AnalysisState>(AnalysisState.Idle)
    val analysisState: StateFlow<AnalysisState> = _analysisState.asStateFlow()

    // Analysis result
    private val _lastAnalysisResult = MutableStateFlow<AnalysisResult?>(null)
    val lastAnalysisResult: StateFlow<AnalysisResult?> = _lastAnalysisResult.asStateFlow()

    // 心理维度枚举
    enum class PsychologyDimensionType {
        MoodStability,      // 情绪稳定
        SelfAwareness,      // 自我认知
        StressManagement,   // 压力管理
        SocialConfidence,   // 社交信心
        SleepQuality,       // 睡眠质量
        SelfCare            // 自我关怀
    }

    private fun PsychologyDimensionType.toKey(): String = when (this) {
        PsychologyDimensionType.MoodStability -> "moodStability"
        PsychologyDimensionType.SelfAwareness -> "selfAwareness"
        PsychologyDimensionType.StressManagement -> "stressManagement"
        PsychologyDimensionType.SocialConfidence -> "socialConfidence"
        PsychologyDimensionType.SleepQuality -> "sleepQuality"
        PsychologyDimensionType.SelfCare -> "selfCare"
    }

    private fun PsychologyDimensionType.toChineseName(): String = when (this) {
        PsychologyDimensionType.MoodStability -> "情绪稳定"
        PsychologyDimensionType.SelfAwareness -> "自我认知"
        PsychologyDimensionType.StressManagement -> "压力管理"
        PsychologyDimensionType.SocialConfidence -> "社交信心"
        PsychologyDimensionType.SleepQuality -> "睡眠质量"
        PsychologyDimensionType.SelfCare -> "自我关怀"
    }

    // 增量更新单个维度分数
    fun boostDimension(dimension: PsychologyDimensionType, delta: Float) {
        if (isDemoMode.value) return
        viewModelScope.launch {
            val currentValue = getCurrentDimensionValue(dimension)
            val newValue = (currentValue + delta).coerceIn(0f, 1f)
            dataStore.updatePsychologyDimension(dimension.toKey(), newValue)
        }
    }

    // 直接设置维度分数
    fun setDimensionScore(dimension: PsychologyDimensionType, value: Float) {
        if (isDemoMode.value) return
        viewModelScope.launch {
            dataStore.updatePsychologyDimension(dimension.toKey(), value.coerceIn(0f, 1f))
        }
    }

    // 批量更新多个维度分数
    fun updateDimensions(updates: Map<PsychologyDimensionType, Float>) {
        if (isDemoMode.value) return
        viewModelScope.launch {
            for ((dimension, value) in updates) {
                dataStore.updatePsychologyDimension(dimension.toKey(), value.coerceIn(0f, 1f))
            }
        }
    }

    private fun getCurrentDimensionValue(dimension: PsychologyDimensionType): Float {
        val dims = psychologyDimensions.value
        return when (dimension) {
            PsychologyDimensionType.MoodStability -> dims.moodStability
            PsychologyDimensionType.SelfAwareness -> dims.selfAwareness
            PsychologyDimensionType.StressManagement -> dims.stressManagement
            PsychologyDimensionType.SocialConfidence -> dims.socialConfidence
            PsychologyDimensionType.SleepQuality -> dims.sleepQuality
            PsychologyDimensionType.SelfCare -> dims.selfCare
        }
    }

    // 获取所有心理维度列表（用于首页展示）
    fun getAllDimensions(): List<PsychologyDimension> {
        val dims = psychologyDimensions.value
        return listOf(
            PsychologyDimension("情绪稳定", "Mood Stability", dims.moodStability, Color(0xFF10B981)),
            PsychologyDimension("自我认知", "Self-Awareness", dims.selfAwareness, Color(0xFF3B82F6)),
            PsychologyDimension("压力管理", "Stress Management", dims.stressManagement, Color(0xFFF59E0B)),
            PsychologyDimension("社交信心", "Social Confidence", dims.socialConfidence, Color(0xFF8B5CF6)),
            PsychologyDimension("睡眠质量", "Sleep Quality", dims.sleepQuality, Color(0xFF06B6D4)),
            PsychologyDimension("自我关怀", "Self-Care", dims.selfCare, Color(0xFFEC4899))
        )
    }

    // 执行会话分析（真实API调用）
    fun analyzeSession(sessionId: String, onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            _analysisState.value = AnalysisState.Analyzing

            try {
                // 初始化API服务
                initializeAnalyzeApiService()

                // 演示模式使用Mock
                if (isDemoMode.value) {
                    delay(2000)
                    // 直接从DataStore获取最新基准分，避免StateFlow缓存导致的问题
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

                // 获取会话消息
                val messages = dataStore.getMessagesForSession(sessionId).first()

                // 构建API请求格式
                val apiMessages = messages.map { msg ->
                    AnthropicMessage(
                        role = if (msg.isFromUser) "user" else "assistant",
                        content = msg.content
                    )
                }

                // 直接从DataStore获取当前维度分数，避免StateFlow缓存导致的问题
                val dims = dataStore.psychologyDimensions.first()
                val currentScores = mapOf(
                    "moodStability" to dims.moodStability,
                    "selfAwareness" to dims.selfAwareness,
                    "stressManagement" to dims.stressManagement,
                    "socialConfidence" to dims.socialConfidence,
                    "sleepQuality" to dims.sleepQuality,
                    "selfCare" to dims.selfCare
                )

                // 构建请求
                val request = AnalyzeRequest(
                    messages = apiMessages,
                    currentScores = currentScores
                )

                // 调用API
                val response = analyzeApiService?.analyze(request)

                if (response?.isSuccessful == true) {
                    val analyzeResponse = response.body()
                    if (analyzeResponse != null) {
                        // 再次获取最新维度分数，确保使用更新后的数据
                        val latestDims = dataStore.psychologyDimensions.first()
                        processAnalyzeResponse(analyzeResponse, latestDims)
                    } else {
                        // 空响应，使用无变化提示
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
                    // API错误，返回错误状态
                    val errorMessage = when (response?.code()) {
                        400 -> "请求参数有误"
                        401 -> "认证失败，请检查API设置"
                        403 -> "没有权限访问"
                        404 -> "分析服务未找到"
                        500 -> "服务器内部错误"
                        else -> "分析失败 (${response?.code()})"
                    }
                    _analysisState.value = AnalysisState.Error(errorMessage)
                }
            } catch (e: Exception) {
                // 异常情况，返回错误状态
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

    // 处理分析响应
    private fun processAnalyzeResponse(
        response: com.onmi.qing.data.remote.AnalyzeResponse,
        currentDims: PsychologyDimensions
    ) {
        val deltas = response.deltas
        val updates = mutableListOf<com.onmi.qing.data.DimensionUpdate>()

        // 变化阈值
        val changeThreshold = 0.01f

        // 英文key到中文名的映射
        val keyToChineseName = mapOf(
            "moodStability" to "情绪稳定",
            "selfAwareness" to "自我认知",
            "stressManagement" to "压力管理",
            "socialConfidence" to "社交信心",
            "sleepQuality" to "睡眠质量",
            "selfCare" to "自我关怀"
        )

        // 获取当前分数（0.0-1.0范围）
        val currentScoresMap = mapOf(
            "moodStability" to currentDims.moodStability,
            "selfAwareness" to currentDims.selfAwareness,
            "stressManagement" to currentDims.stressManagement,
            "socialConfidence" to currentDims.socialConfidence,
            "sleepQuality" to currentDims.sleepQuality,
            "selfCare" to currentDims.selfCare
        )

        // 检查是否所有变化都很小（无变化情况）
        val hasSignificantChange = deltas.values.any { kotlin.math.abs(it) >= changeThreshold }

        if (hasSignificantChange) {
            // 有明显变化，处理每个维度的更新
            for ((key, delta) in deltas) {
                val currentScore = currentScoresMap[key] ?: continue
                val newScore = (currentScore + delta).coerceIn(0f, 1f)
                val chineseName = keyToChineseName[key] ?: continue

                // 判断变化图标
                val changeIcon = when {
                    newScore > currentScore + changeThreshold -> "up"
                    newScore < currentScore - changeThreshold -> "down"
                    else -> "stable"
                }

                // 只有有实际变化的才添加
                if (changeIcon != "stable") {
                    updates.add(
                        com.onmi.qing.data.DimensionUpdate(
                            dimensionName = chineseName,
                            oldScore = currentScore * 100f,
                            newScore = newScore * 100f,
                            changeIcon = changeIcon
                        )
                    )

                    // 更新到DataStore (key是英文key)
                    updateDimensionByKey(key, newScore)
                }
            }
        }

        // 构建结果
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

    // 更新单个维度分数（演示模式下不保存）
    private fun updateDimension(dimensionName: String, newProgress: Float) {
        if (isDemoMode.value) return // 演示模式下不保存

        viewModelScope.launch {
            val dimensionKey = when (dimensionName) {
                "情绪稳定" -> "moodStability"
                "自我认知" -> "selfAwareness"
                "压力管理" -> "stressManagement"
                "社交信心" -> "socialConfidence"
                "睡眠质量" -> "sleepQuality"
                "自我关怀" -> "selfCare"
                else -> return@launch
            }
            dataStore.updatePsychologyDimension(dimensionKey, newProgress)
        }
    }

        // 通过英文key更新维度分数（演示模式下不保存）
    private fun updateDimensionByKey(dimensionKey: String, newProgress: Float) {
        if (isDemoMode.value) return // 演示模式下不保存

        viewModelScope.launch {
            dataStore.updatePsychologyDimension(dimensionKey, newProgress)
        }
    }

    // 重置分析状态
    fun resetAnalysisState() {
        _analysisState.value = AnalysisState.Idle
    }

    // 解锁成就（演示模式下不保存）
    fun unlockAchievement(achievementId: String) {
        if (isDemoMode.value) return // 演示模式下不保存
        
        viewModelScope.launch {
            dataStore.unlockAchievement(achievementId)
        }
    }

    // 锁定成就（开发者选项 - 演示模式下不保存）
    fun lockAchievement(achievementId: String) {
        if (isDemoMode.value) return // 演示模式下不保存
        
        viewModelScope.launch {
            dataStore.lockAchievement(achievementId)
        }
    }

    // 手动设置维度分数（开发者选项 - 演示模式下不保存）
    fun setDimensionProgress(dimension: String, progress: Float) {
        if (isDemoMode.value) return // 演示模式下不保存
        
        viewModelScope.launch {
            dataStore.updatePsychologyDimension(dimension, progress.coerceIn(0f, 1f))
        }
    }

    // 检查并解锁所有可完成的成就
    private fun checkAndUnlockAchievements() {
        if (isDemoMode.value) return // 演示模式下不自动解锁
        
        val stats = usageStats.value
        val dims = psychologyDimensions.value

        // 签到类成就（用总次数代替连续天数）
        if (stats.checkInCount >= 1) unlockAchievement("checkin_3")
        if (stats.checkInCount >= 7) unlockAchievement("checkin_7")
        if (stats.checkInCount >= 30) unlockAchievement("checkin_30")

        // 聊天类成就
        if (stats.chatCount >= 10) unlockAchievement("chat_10")
        if (stats.chatCount >= 50) unlockAchievement("chat_50")
        if (stats.chatCount >= 100) unlockAchievement("chat_100")

        // 呼吸类成就
        if (stats.breathingCount >= 5) unlockAchievement("breathing_5")
        if (stats.breathingCount >= 20) unlockAchievement("breathing_20")
        if (stats.breathingCount >= 50) unlockAchievement("breathing_50")

        // 心理维度成就（首次达标即解锁）
        if (dims.moodStability > 0.7f) unlockAchievement("mood_good_7")
        if (dims.sleepQuality > 0.7f) unlockAchievement("sleep_good_7")
        if (dims.stressManagement > 0.7f) unlockAchievement("stress_low_7")

        // 全面发展成就（所有维度都超过70%）
        if (dims.moodStability > 0.7f && dims.selfAwareness > 0.7f &&
            dims.stressManagement > 0.7f && dims.socialConfidence > 0.7f &&
            dims.sleepQuality > 0.7f && dims.selfCare > 0.7f) {
            unlockAchievement("all_round")
        }

        // 完美一天（同一日历天内三项都完成）
        viewModelScope.launch {
            if (dataStore.isTodayPerfectDay()) {
                unlockAchievement("perfect_day")
            }
        }
    }

    // 增加聊天次数（演示模式下不保存）
    fun incrementChatCount() {
        if (isDemoMode.value) return // 演示模式下不保存
        
        viewModelScope.launch {
            dataStore.incrementChatCount()
            dataStore.resetDailyActivitiesIfNewDay()
            dataStore.markTodayChat()
            checkAndUnlockAchievements()
        }
    }

    // 增加呼吸练习次数（演示模式下不保存）
    fun incrementBreathingCount() {
        if (isDemoMode.value) return // 演示模式下不保存
        
        viewModelScope.launch {
            dataStore.incrementBreathingCount()
            dataStore.resetDailyActivitiesIfNewDay()
            dataStore.markTodayBreathing()
            checkAndUnlockAchievements()
        }
    }

// 增加签到次数（演示模式下不保存）
    fun incrementCheckInCount(hour: Int = -1) {
        if (isDemoMode.value) return // 演示模式下不保存
        
        viewModelScope.launch {
            dataStore.incrementCheckInCount()
            dataStore.resetDailyActivitiesIfNewDay()
            dataStore.markTodayCheckin()
            // 检查早起鸟成就
            if (hour in 0..7) {
                dataStore.checkAndUnlockEarlyBird(hour)
                unlockAchievement("early_bird")
            }
            checkAndUnlockAchievements()
        }
    }

// 获取已解锁成就数量
    fun getUnlockedCount(): Int {
        return unlockedCount.value
    }

// 获取总成就数量
    fun getTotalCount(): Int {
        return achievements.value.size
    }

// 清除所有数据
    fun clearAllData() {
        viewModelScope.launch {
            dataStore.clearAllData()
        }
    }

    class Factory(
        private val dataStore: QingDataStore,
        private val demoModeManager: DemoModeManager
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StateViewModel::class.java)) {
                return StateViewModel(dataStore, demoModeManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
