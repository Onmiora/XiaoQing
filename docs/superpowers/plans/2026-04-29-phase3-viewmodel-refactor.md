# Phase 3: ViewModel Refactoring Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Split StateViewModel (598 lines) into PsychologyViewModel, AchievementViewModel, and AnalysisViewModel. Refactor ChatViewModel to eliminate duplicated code and fix the streamingMessageId race condition. Remove all ViewModel-to-ViewModel dependencies.

**Architecture:** Each new ViewModel has a single responsibility and is `@HiltViewModel`. Shared state flows through Repository/Manager layers, not direct VM references.

**Tech Stack:** Hilt, Kotlin Coroutines, StateFlow, Mutex

**Prerequisite:** Phase 1 (Room) and Phase 2 (Hilt) must be complete.

---

### Task 3.1: Create PsychologyViewModel

**Files:**
- Create: `app/src/main/java/com/onmi/qing/viewmodel/PsychologyViewModel.kt`

- [ ] **Step 1: Create PsychologyViewModel**

```kotlin
package com.onmi.qing.viewmodel

import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onmi.qing.data.PsychologyDimension
import com.onmi.qing.data.datastore.PsychologyDimensions
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.demo.DemoModeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PsychologyViewModel @Inject constructor(
    private val dataStore: QingDataStore,
    private val demoModeManager: DemoModeManager
) : ViewModel() {

    enum class DimensionType(val key: String, val chineseName: String) {
        MoodStability("moodStability", "情绪稳定"),
        SelfAwareness("selfAwareness", "自我认知"),
        StressManagement("stressManagement", "压力管理"),
        SocialConfidence("socialConfidence", "社交信心"),
        SleepQuality("sleepQuality", "睡眠质量"),
        SelfCare("selfCare", "自我关怀")
    }

    val psychologyDimensions: StateFlow<PsychologyDimensions> = combine(
        demoModeManager.isDemoMode,
        demoModeManager.psychologyDimensions,
        dataStore.psychologyDimensions
    ) { isDemo, demoDims, userDims ->
        if (isDemo) demoDims else userDims
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PsychologyDimensions())

    fun boostDimension(dimension: DimensionType, delta: Float) {
        if (demoModeManager.isDemoMode.value) return
        viewModelScope.launch {
            val current = getCurrentValue(dimension)
            dataStore.updatePsychologyDimension(dimension.key, (current + delta).coerceIn(0f, 1f))
        }
    }

    fun setDimensionScore(dimension: DimensionType, value: Float) {
        if (demoModeManager.isDemoMode.value) return
        viewModelScope.launch {
            dataStore.updatePsychologyDimension(dimension.key, value.coerceIn(0f, 1f))
        }
    }

    fun updateDimensions(updates: Map<DimensionType, Float>) {
        if (demoModeManager.isDemoMode.value) return
        viewModelScope.launch {
            for ((dimension, value) in updates) {
                dataStore.updatePsychologyDimension(dimension.key, value.coerceIn(0f, 1f))
            }
        }
    }

    fun resetDimensions() {
        if (demoModeManager.isDemoMode.value) return
        viewModelScope.launch {
            dataStore.resetPsychologyDimensions()
        }
    }

    fun setDimensionProgress(dimensionKey: String, progress: Float) {
        if (demoModeManager.isDemoMode.value) return
        viewModelScope.launch {
            dataStore.updatePsychologyDimension(dimensionKey, progress.coerceIn(0f, 1f))
        }
    }

    fun getAllDimensions(): List<PsychologyDimension> {
        val dims = psychologyDimensions.value
        return listOf(
            PsychologyDimension("情绪稳定", "Mood Stability", dims.moodStability, 0xFF10B981),
            PsychologyDimension("自我认知", "Self-Awareness", dims.selfAwareness, 0xFF3B82F6),
            PsychologyDimension("压力管理", "Stress Management", dims.stressManagement, 0xFFF59E0B),
            PsychologyDimension("社交信心", "Social Confidence", dims.socialConfidence, 0xFF8B5CF6),
            PsychologyDimension("睡眠质量", "Sleep Quality", dims.sleepQuality, 0xFF06B6D4),
            PsychologyDimension("自我关怀", "Self-Care", dims.selfCare, 0xFFEC4899)
        )
    }

    private fun getCurrentValue(dimension: DimensionType): Float {
        val dims = psychologyDimensions.value
        return when (dimension) {
            DimensionType.MoodStability -> dims.moodStability
            DimensionType.SelfAwareness -> dims.selfAwareness
            DimensionType.StressManagement -> dims.stressManagement
            DimensionType.SocialConfidence -> dims.socialConfidence
            DimensionType.SleepQuality -> dims.sleepQuality
            DimensionType.SelfCare -> dims.selfCare
        }
    }
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL (PsychologyViewModel exists alongside old StateViewModel)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/onmi/qing/viewmodel/PsychologyViewModel.kt
git commit -m "feat: create PsychologyViewModel for dimension management"
```

---

### Task 3.2: Create AchievementViewModel

**Files:**
- Create: `app/src/main/java/com/onmi/qing/viewmodel/AchievementViewModel.kt`

- [ ] **Step 1: Create AchievementViewModel**

```kotlin
package com.onmi.qing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onmi.qing.data.Achievement
import com.onmi.qing.data.UsageStatsManager
import com.onmi.qing.data.datastore.PsychologyDimensions
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.data.repository.AchievementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AchievementViewModel @Inject constructor(
    private val achievementRepository: AchievementRepository,
    private val dataStore: QingDataStore,
    private val demoModeManager: DemoModeManager,
    private val usageStatsManager: UsageStatsManager
) : ViewModel() {

    val achievements: StateFlow<List<Achievement>> = combine(
        demoModeManager.isDemoMode,
        demoModeManager.demoAchievements,
        achievementRepository.getAll()
    ) { isDemo, demoAchievements, userAchievements ->
        if (isDemo) demoAchievements else userAchievements
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unlockedCount: StateFlow<Int> = combine(
        demoModeManager.isDemoMode,
        demoModeManager.demoAchievements,
        achievementRepository.getUnlockedCount()
    ) { isDemo, demoAchievements, userCount ->
        if (isDemo) demoAchievements.count { it.isUnlocked } else userCount
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun unlockAchievement(achievementId: String) {
        if (demoModeManager.isDemoMode.value) return
        viewModelScope.launch {
            achievementRepository.unlock(achievementId)
        }
    }

    fun lockAchievement(achievementId: String) {
        if (demoModeManager.isDemoMode.value) return
        viewModelScope.launch {
            achievementRepository.lock(achievementId)
        }
    }

    fun resetAllAchievements() {
        if (demoModeManager.isDemoMode.value) return
        viewModelScope.launch {
            achievementRepository.resetAll()
        }
    }

    fun checkAndUnlockAchievements() {
        if (demoModeManager.isDemoMode.value) return

        viewModelScope.launch {
            val stats = dataStore.usageStats.first()
            val dims = dataStore.psychologyDimensions.first()

            // 签到类
            if (stats.checkInCount >= 1) unlockAchievement("checkin_3")
            if (stats.checkInCount >= 7) unlockAchievement("checkin_7")
            if (stats.checkInCount >= 30) unlockAchievement("checkin_30")

            // 聊天类
            if (stats.chatCount >= 10) unlockAchievement("chat_10")
            if (stats.chatCount >= 50) unlockAchievement("chat_50")
            if (stats.chatCount >= 100) unlockAchievement("chat_100")

            // 呼吸类
            if (stats.breathingCount >= 5) unlockAchievement("breathing_5")
            if (stats.breathingCount >= 20) unlockAchievement("breathing_20")
            if (stats.breathingCount >= 50) unlockAchievement("breathing_50")

            // 心理维度
            if (dims.moodStability > 0.7f) unlockAchievement("mood_good_7")
            if (dims.sleepQuality > 0.7f) unlockAchievement("sleep_good_7")
            if (dims.stressManagement > 0.7f) unlockAchievement("stress_low_7")

            // 全面发展
            if (dims.moodStability > 0.7f && dims.selfAwareness > 0.7f &&
                dims.stressManagement > 0.7f && dims.socialConfidence > 0.7f &&
                dims.sleepQuality > 0.7f && dims.selfCare > 0.7f) {
                unlockAchievement("all_round")
            }

            // 完美一天
            if (dataStore.isTodayPerfectDay()) {
                unlockAchievement("perfect_day")
            }
        }
    }

    fun getUnlockedCount(): Int = unlockedCount.value
    fun getTotalCount(): Int = achievements.value.size
}
```

Note: Add `import kotlinx.coroutines.flow.first` to imports.

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/onmi/qing/viewmodel/AchievementViewModel.kt
git commit -m "feat: create AchievementViewModel for achievement management"
```

---

### Task 3.3: Create AnalysisViewModel

**Files:**
- Create: `app/src/main/java/com/onmi/qing/viewmodel/AnalysisViewModel.kt`

- [ ] **Step 1: Create AnalysisViewModel**

```kotlin
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
                val messages = dataStore.getMessagesForSession(sessionId).first()

                val apiMessages = messages.map { msg ->
                    AnthropicMessage(
                        role = if (msg.isFromUser) "user" else "assistant",
                        content = msg.content
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
```

Note: The `dataStore.getMessagesForSession()` call here needs to be replaced with `chatRepository.getMessagesForSessionOnce()` since we removed that method from QingDataStore in Phase 1. Add `chatRepository` as a constructor dependency.

Updated constructor:
```kotlin
@HiltViewModel
class AnalysisViewModel @Inject constructor(
    private val dataStore: QingDataStore,
    private val chatRepository: ChatRepository,
    private val demoModeManager: DemoModeManager,
    private val apiServiceFactory: ApiServiceFactory
) : ViewModel() {
```

And replace:
```kotlin
val messages = dataStore.getMessagesForSession(sessionId).first()
```
with:
```kotlin
val messages = chatRepository.getMessagesForSessionOnce(sessionId)
```

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/onmi/qing/viewmodel/AnalysisViewModel.kt
git commit -m "feat: create AnalysisViewModel for AI analysis"
```

---

### Task 3.4: Refactor ChatViewModel

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/viewmodel/ChatViewModel.kt`

- [ ] **Step 1: Extract buildMessageContext()**

Add a private method to eliminate the duplicated message-building logic:

```kotlin
    private fun buildMessageContext(currentText: String): List<AnthropicMessage> {
        val messageHistory = _messages.value
            .sortedBy { it.timestamp }
            .drop(1) // Skip the first message (AI greeting)
            .map { msg ->
                AnthropicMessage(
                    role = if (msg.isFromUser) "user" else "assistant",
                    content = msg.content
                )
            }
        return messageHistory + AnthropicMessage(role = "user", content = currentText)
    }
```

Replace the inline message-building in both `nonStreamingChat` and `streamChat` with:
```kotlin
val allMessages = buildMessageContext(text)
```

- [ ] **Step 2: Replace stateViewModel dependency with UsageStatsManager**

Change constructor:
```kotlin
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val dataStore: QingDataStore,
    private val chatRepository: ChatRepository,
    private val apiServiceFactory: ApiServiceFactory,
    private val usageStatsManager: UsageStatsManager,
    private val demoModeManager: DemoModeManager
) : ViewModel() {
```

Replace `stateViewModel?.incrementChatCount()` with `usageStatsManager.incrementChatCount()`.

- [ ] **Step 3: Use ApiServiceFactory instead of manual Retrofit creation**

Replace the `initializeApiService()` method and `chatApiService` field:

```kotlin
    // Remove these:
    // private var chatApiService: ChatApiService? = null
    // private var currentApiUrl: String? = null
    // private fun initializeApiService() { ... }
    // init { viewModelScope.launch { initializeApiService() } }

    // Use this instead:
    private val chatApiService: ChatApiService by lazy {
        apiServiceFactory.create<ChatApiService>()
    }
```

Update all `chatApiService?.chat(request)` to `chatApiService.chat(request)` (no null check needed).

- [ ] **Step 4: Fix streamingMessageId race condition**

Wrap `streamingMessageId` access with a Mutex:

```kotlin
    private val streamingMutex = kotlinx.coroutines.sync.Mutex()
    private var streamingMessageId: String? = null
```

In `doSendMessage`, when setting `streamingMessageId`:
```kotlin
    streamingMutex.withLock {
        streamingMessageId = savedMessage.id
    }
```

In `updateStreamingMessage`:
```kotlin
    private fun updateStreamingMessage(additionalText: String) {
        val messageId = streamingMessageId ?: return  // Read is safe here (single coroutine)
        // ... rest of update logic
    }
```

Actually, since `updateStreamingMessage` is called from within the same coroutine that sets the ID (the SSE collect loop), the mutex is mainly needed for the error cleanup path. A simpler approach: ensure all `streamingMessageId` access happens on the `viewModelScope` dispatcher (which is `Dispatchers.Main.immediate` by default), making it single-threaded.

The real fix: move the error-cleanup `streamingMessageId = null` into the same coroutine:

```kotlin
    viewModelScope.launch {
        try {
            val streamed = streamChat(text)
            if (!streamed) {
                nonStreamingChat(text)
            }
        } catch (e: Exception) {
            // Cleanup is now in the same coroutine
            val placeholderId = streamingMessageId
            val sid = _currentSessionId.value
            if (placeholderId != null && sid != null) {
                _messages.update { list -> list.filter { it.id != placeholderId } }
                chatRepository.deleteMessage(placeholderId, sid)
            }
            streamingMessageId = null
            _isAiTyping.value = false
            nonStreamingChat(text)
        }
    }
```

Remove the nested `viewModelScope.launch` inside the catch block (the `deleteMessage` call).

- [ ] **Step 5: Make model name configurable**

Replace hardcoded `"glm-4.5-air"` with a setting:

```kotlin
    // In buildMessageContext or wherever the request is built:
    val prefs = dataStore.userPreferences.first()  // or use a cached value
    val model = prefs.modelName ?: "glm-4.5-air"
```

For now, since `UserPreferences` doesn't have a `modelName` field, add it:

In `UserPreferences.kt`:
```kotlin
data class UserPreferences(
    // ... existing fields
    val modelName: String = "glm-4.5-air"
)
```

In `QingDataStore`:
```kotlin
    private val KEY_MODEL_NAME = stringPreferencesKey("model_name")

    // In userPreferences flow:
    modelName = preferences[KEY_MODEL_NAME] ?: "glm-4.5-air"

    // Add setter:
    suspend fun updateModelName(name: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MODEL_NAME] = name
        }
    }
```

- [ ] **Step 6: Remove AIResponses fallback — use error state instead**

Replace `addAiMessage(AIResponses.getResponse(text))` with a proper error message:

```kotlin
    // In the catch/error paths:
    addAiMessage("抱歉，服务出现了问题。请稍后再试。\n\n⚠️ 后端服务出现问题")
```

- [ ] **Step 7: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 8: Commit**

```bash
git add -A
git commit -m "refactor: clean up ChatViewModel — extract shared code, fix race condition, use ApiServiceFactory"
```

---

### Task 3.5: Wire New ViewModels and Remove Old StateViewModel

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/ui/navigation/Navigation.kt`
- Modify: `app/src/main/java/com/onmi/qing/MainActivity.kt`
- Modify: All screens that use StateViewModel

- [ ] **Step 1: Update Navigation.kt to use new ViewModels**

Replace `stateViewModel` parameter with individual VMs:

```kotlin
@Composable
fun QingNavHost(
    navController: NavHostController,
    isDarkTheme: Boolean,
    followSystemTheme: Boolean,
    onThemeChange: (Boolean) -> Unit,
    onFollowSystemChange: (Boolean) -> Unit,
    demoModeManager: DemoModeManager,
    paddingValues: PaddingValues,
    modifier: Modifier = Modifier
) {
    NavHost(...) {
        composable(Screen.Home.route) {
            val psychologyViewModel = hiltViewModel<PsychologyViewModel>()
            val achievementViewModel = hiltViewModel<AchievementViewModel>()
            val moodViewModel = hiltViewModel<MoodViewModel>()
            HomeScreen(
                psychologyViewModel = psychologyViewModel,
                achievementViewModel = achievementViewModel,
                moodViewModel = moodViewModel,
                demoModeManager = demoModeManager,
                // ... navigation callbacks
            )
        }

        composable(Screen.Discover.route) {
            val psychologyViewModel = hiltViewModel<PsychologyViewModel>()
            val achievementViewModel = hiltViewModel<AchievementViewModel>()
            val moodViewModel = hiltViewModel<MoodViewModel>()
            DiscoverScreen(
                psychologyViewModel = psychologyViewModel,
                achievementViewModel = achievementViewModel,
                moodViewModel = moodViewModel,
                demoModeManager = demoModeManager,
                // ... navigation callbacks
            )
        }

        composable(Screen.Achievement.route) {
            val achievementViewModel = hiltViewModel<AchievementViewModel>()
            AchievementScreen(
                viewModel = achievementViewModel,
                demoModeManager = demoModeManager,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.Breathing.route) {
            val achievementViewModel = hiltViewModel<AchievementViewModel>()
            BreathingScreen(
                achievementViewModel = achievementViewModel,
                demoModeManager = demoModeManager,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(Screen.History.route) {
            val psychologyViewModel = hiltViewModel<PsychologyViewModel>()
            val chatViewModel = hiltViewModel<ChatViewModel>()
            HistoryScreen(
                onBackClick = { navController.popBackStack() },
                onSessionClick = { session ->
                    chatViewModel.loadSession(session)
                    navController.navigate(Screen.Chat.route)
                },
                psychologyViewModel = psychologyViewModel,
                chatRepository = chatViewModel.chatRepository, // expose if needed
                demoModeManager = demoModeManager
            )
        }

        composable(Screen.Settings.route) {
            val settingsViewModel = hiltViewModel<SettingsViewModel>()
            val psychologyViewModel = hiltViewModel<PsychologyViewModel>()
            SettingsScreen(
                settingsViewModel = settingsViewModel,
                psychologyViewModel = psychologyViewModel,
                // ... rest
            )
        }

        composable(Screen.Developer.route) {
            val psychologyViewModel = hiltViewModel<PsychologyViewModel>()
            val achievementViewModel = hiltViewModel<AchievementViewModel>()
            val settingsViewModel = hiltViewModel<SettingsViewModel>()
            DeveloperScreen(
                psychologyViewModel = psychologyViewModel,
                achievementViewModel = achievementViewModel,
                settingsViewModel = settingsViewModel,
                demoModeManager = demoModeManager,
                onBackClick = { navController.popBackStack() }
            )
        }

        // ... other routes
    }
}
```

- [ ] **Step 2: Update screen composables to accept new VM types**

For each screen that previously took `stateViewModel: StateViewModel`, change the parameter to the specific VM(s) it needs.

For example, `HomeScreen`:
```kotlin
@Composable
fun HomeScreen(
    psychologyViewModel: PsychologyViewModel,
    achievementViewModel: AchievementViewModel,
    moodViewModel: MoodViewModel,
    demoModeManager: DemoModeManager,
    onStartChatClick: () -> Unit,
    onBreathingClick: () -> Unit,
    onAchievementClick: () -> Unit,
    onStressDetectionClick: () -> Unit
)
```

Replace all `stateViewModel.psychologyDimensions` → `psychologyViewModel.psychologyDimensions`
Replace all `stateViewModel.achievements` → `achievementViewModel.achievements`
Replace all `stateViewModel.getAllDimensions()` → `psychologyViewModel.getAllDimensions()`
Replace all `stateViewModel.unlockAchievement()` → `achievementViewModel.unlockAchievement()`
Replace all `stateViewModel.incrementChatCount()` → (removed, handled by ChatViewModel)
Replace all `stateViewModel.analyzeSession()` → `analysisViewModel.analyzeSession()`

- [ ] **Step 3: Delete old StateViewModel**

Once all consumers are updated, delete `app/src/main/java/com/onmi/qing/viewmodel/StateViewModel.kt`.

- [ ] **Step 4: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: replace StateViewModel with Psychology/Achievement/Analysis ViewModels"
```

---

### Task 3.6: Final Cleanup and Verification

- [ ] **Step 1: Remove unused AIResponses object**

In `Message.kt`, remove the entire `AIResponses` object (lines 36-57) since it's no longer used.

- [ ] **Step 2: Remove unused imports across all files**

```bash
grep -rn "import com.onmi.qing.viewmodel.StateViewModel" app/src/main/java/ --include="*.kt"
```

All should be gone. If any remain, update them.

- [ ] **Step 3: Full build and test**

Run: `./gradlew assembleDebug`
Run: `./gradlew test`
Expected: All passes

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "chore: Phase 3 complete — ViewModel refactoring"
```
