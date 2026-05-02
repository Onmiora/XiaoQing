package com.onmi.qing.data.demo

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.onmi.qing.data.Achievement
import com.onmi.qing.data.ChatSession
import com.onmi.qing.data.ChatMessage
import com.onmi.qing.data.MessagePart
import com.onmi.qing.data.MessageRole
import com.onmi.qing.data.MoodEntry
import com.onmi.qing.data.datastore.PsychologyDimensions
import com.onmi.qing.data.datastore.UsageStats
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

private val Context.demoDataStore: DataStore<Preferences> by preferencesDataStore(name = "demo_mode_preferences")

// 演示模式管理器
class DemoModeManager(private val context: Context) {

    companion object {
        private val KEY_IS_DEMO_MODE = booleanPreferencesKey("is_demo_mode")
        private val KEY_HAS_USER_DATA = booleanPreferencesKey("has_user_data")
    }

    // 当前是否为演示模式
    private val _isDemoMode = MutableStateFlow(false)
    val isDemoMode: StateFlow<Boolean> = _isDemoMode.asStateFlow()

    // 是否有用户数据（用于切换回用户模式时判断）
    private val _hasUserData = MutableStateFlow(false)
    val hasUserData: StateFlow<Boolean> = _hasUserData.asStateFlow()

    // 演示数据槽位（内存）
    private val _demoSessions = MutableStateFlow<List<ChatSession>>(emptyList())
    val demoSessions: StateFlow<List<ChatSession>> = _demoSessions.asStateFlow()

    private val _demoMessages = MutableStateFlow<Map<String, List<ChatMessage>>>(emptyMap())
    val demoMessages: StateFlow<Map<String, List<ChatMessage>>> = _demoMessages.asStateFlow()

    private val _demoMoodEntries = MutableStateFlow<List<MoodEntry>>(emptyList())
    val demoMoodEntries: StateFlow<List<MoodEntry>> = _demoMoodEntries.asStateFlow()

    private val _demoAchievements = MutableStateFlow<List<Achievement>>(emptyList())
    val demoAchievements: StateFlow<List<Achievement>> = _demoAchievements.asStateFlow()

    private val _demoPsychologyDimensions = MutableStateFlow(PsychologyDimensions())
    val psychologyDimensions: StateFlow<PsychologyDimensions> = _demoPsychologyDimensions.asStateFlow()

    private val _demoUsageStats = MutableStateFlow(UsageStats())
    val usageStats: StateFlow<UsageStats> = _demoUsageStats.asStateFlow()

    init {
        // 初始化时从 DataStore 读取状态
        loadModeState()
    }

// 从 DataStore 加载模式状态
    private fun loadModeState() {
        // 使用 runBlocking 确保在 init 块中同步读取状态
        runBlocking {
            try {
                val preferences = context.demoDataStore.data.first()
                val wasInDemoMode = preferences[KEY_IS_DEMO_MODE] ?: false
                
                if (wasInDemoMode) {
                    // 如果之前是演示模式，恢复状态
                    _isDemoMode.value = true
                    loadDemoData()
                }
            } catch (e: Exception) {
                // 读取失败时保持默认状态（用户模式）
                _isDemoMode.value = false
            }
        }
    }

    // 收集演示模式状态（Flow 版本，用于 DataStore 集成）
    val demoModeFlow: Flow<Boolean> = context.demoDataStore.data.map { preferences ->
        preferences[KEY_IS_DEMO_MODE] ?: false
    }

    // 收集是否有用户数据的状态
    val userDataExistsFlow: Flow<Boolean> = context.demoDataStore.data.map { preferences ->
        preferences[KEY_HAS_USER_DATA] ?: false
    }

// 切换到演示模式
    suspend fun enableDemoMode() {
        // 标记有用户数据存在（即使当前是空的）
        context.demoDataStore.edit { preferences ->
            preferences[KEY_HAS_USER_DATA] = true
        }

        // 切换到演示模式
        _isDemoMode.value = true

        // 加载演示数据
        loadDemoData()

        // 保存状态到 DataStore
        context.demoDataStore.edit { preferences ->
            preferences[KEY_IS_DEMO_MODE] = true
        }
    }

    // 切换回用户模式
    suspend fun disableDemoMode() {
        // 切换回用户模式
        _isDemoMode.value = false

        // 清空演示数据
        clearDemoData()

        // 保存状态到 DataStore
        context.demoDataStore.edit { preferences ->
            preferences[KEY_IS_DEMO_MODE] = false
        }
    }

// 加载演示数据到内存
    private fun loadDemoData() {
        // 加载聊天会话
        val sessions = DemoData.getDemoSessions()
        _demoSessions.value = sessions

        // 加载会话消息映射
        val messagesMap = sessions.associate { session ->
            session.id to session.messages
        }
        _demoMessages.value = messagesMap

        // 加载心情记录
        _demoMoodEntries.value = DemoData.getDemoMoodEntries()

        // 加载成就
        _demoAchievements.value = DemoData.getDemoAchievements()

        // 加载心理维度
        _demoPsychologyDimensions.value = DemoData.psychologyDimensions

        // 加载使用统计
        _demoUsageStats.value = DemoData.usageStats
    }

// 清空演示数据
    private fun clearDemoData() {
        _demoSessions.value = emptyList()
        _demoMessages.value = emptyMap()
        _demoMoodEntries.value = emptyList()
        _demoAchievements.value = emptyList()
        _demoPsychologyDimensions.value = PsychologyDimensions()
        _demoUsageStats.value = UsageStats()
    }

    // 在演示模式下添加消息（仅存储在内存中）
    fun addDemoMessage(sessionId: String, content: String, isFromUser: Boolean): ChatMessage {
        val role = if (isFromUser) MessageRole.USER else MessageRole.ASSISTANT
        val message = ChatMessage(
            id = "demo_msg_${System.currentTimeMillis()}",
            role = role,
            parts = listOf(MessagePart.Text(content)),
            timestamp = System.currentTimeMillis()
        )

        // 更新内存中的消息
        val currentMessages = _demoMessages.value.toMutableMap()
        val sessionMessages = currentMessages[sessionId]?.toMutableList() ?: mutableListOf()
        sessionMessages.add(message)
        currentMessages[sessionId] = sessionMessages
        _demoMessages.value = currentMessages

        // 更新会话的最后消息
        val sessions = _demoSessions.value.toMutableList()
        val sessionIndex = sessions.indexOfFirst { it.id == sessionId }
        if (sessionIndex >= 0) {
            val session = sessions[sessionIndex]
            val prefix = if (isFromUser) "我: " else "小晴: "
            sessions[sessionIndex] = session.copy(
                lastMessage = prefix + content.take(50),
                messageCount = session.messageCount + 1,
                timestamp = message.timestamp,
                messages = sessionMessages
            )
            _demoSessions.value = sessions
        }

        return message
    }

    // 获取演示模式下的会话消息
    fun getDemoMessagesForSession(sessionId: String): List<ChatMessage> {
        return _demoMessages.value[sessionId] ?: emptyList()
    }

    // 获取已解锁成就数量
    fun getUnlockedAchievementCount(): Int {
        return _demoAchievements.value.count { it.isUnlocked }
    }

    // 检查是否有用户数据
    suspend fun checkHasUserData(): Boolean {
        // 这个方法用于检查是否曾经有过用户数据
        // 在实际实现中，可能需要检查 DataStore 中是否有用户相关的数据
        val preferences = context.demoDataStore.data
        return preferences.map { it[KEY_HAS_USER_DATA] ?: false }.let { flow ->
            var result = false
            flow.collect { result = it; return@collect }
            result
        }
    }
}
