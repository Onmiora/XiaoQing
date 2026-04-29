package com.onmi.qing.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.onmi.qing.data.Achievement
import com.onmi.qing.data.AchievementList
import com.onmi.qing.data.ChatSession
import com.onmi.qing.data.Message
import com.onmi.qing.data.MoodEntry
import com.onmi.qing.data.MoodType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "qing_preferences")

// Qing 应用数据存储
class QingDataStore(private val context: Context) {

    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    companion object {
        // User Preferences Keys
        private val KEY_IS_DARK_THEME = booleanPreferencesKey("is_dark_theme")
        private val KEY_FOLLOW_SYSTEM_THEME = booleanPreferencesKey("follow_system_theme")
        private val KEY_API_URL = stringPreferencesKey("api_url")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_USER_DESCRIPTION = stringPreferencesKey("user_description")

        // Psychology Dimensions Keys
        private val KEY_MOOD_STABILITY = floatPreferencesKey("mood_stability")
        private val KEY_SELF_AWARENESS = floatPreferencesKey("self_awareness")
        private val KEY_STRESS_MANAGEMENT = floatPreferencesKey("stress_management")
        private val KEY_SOCIAL_CONFIDENCE = floatPreferencesKey("social_confidence")
        private val KEY_SLEEP_QUALITY = floatPreferencesKey("sleep_quality")
        private val KEY_SELF_CARE = floatPreferencesKey("self_care")

        // Usage Stats Keys
        private val KEY_CHAT_COUNT = intPreferencesKey("chat_count")
        private val KEY_BREATHING_COUNT = intPreferencesKey("breathing_count")
        private val KEY_CHECK_IN_COUNT = intPreferencesKey("check_in_count")

        // Daily Activity Tracking Keys
        private val KEY_LAST_ACTIVITY_DATE = stringPreferencesKey("last_activity_date")
        private val KEY_TODAY_CHECKIN = booleanPreferencesKey("today_checkin")
        private val KEY_TODAY_CHAT = booleanPreferencesKey("today_chat")
        private val KEY_TODAY_BREATHING = booleanPreferencesKey("today_breathing")
        private val KEY_EARLY_BIRD_UNLOCKED = booleanPreferencesKey("early_bird_unlocked")

        // Chat Sessions and Messages (JSON serialized)
        private val KEY_CHAT_SESSIONS = stringPreferencesKey("chat_sessions")
        private val KEY_MESSAGES = stringPreferencesKey("messages")

        // Achievements (JSON serialized)
        private val KEY_ACHIEVEMENTS = stringPreferencesKey("achievements")

        // Mood Entries (JSON serialized)
        private val KEY_MOOD_ENTRIES = stringPreferencesKey("mood_entries")

        // Default Values
        const val DEFAULT_API_URL = "https://api.xiaoqing.com"
        const val DEFAULT_USER_NAME = "小明同学"
        const val DEFAULT_USER_DESCRIPTION = "正在使用小晴心理健康助手"
    }

    // User Preferences

    val userPreferences: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            isDarkTheme = preferences[KEY_IS_DARK_THEME] ?: false,
            followSystemTheme = preferences[KEY_FOLLOW_SYSTEM_THEME] ?: true,
            apiUrl = preferences[KEY_API_URL] ?: DEFAULT_API_URL,
            userName = preferences[KEY_USER_NAME] ?: DEFAULT_USER_NAME,
            userDescription = preferences[KEY_USER_DESCRIPTION] ?: DEFAULT_USER_DESCRIPTION
        )
    }

    suspend fun updateTheme(isDark: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_IS_DARK_THEME] = isDark
        }
    }

    suspend fun updateFollowSystemTheme(follow: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_FOLLOW_SYSTEM_THEME] = follow
        }
    }

    suspend fun updateApiUrl(url: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_API_URL] = url
        }
    }

    suspend fun updateUserProfile(name: String, description: String) {
        context.dataStore.edit { preferences ->
            preferences[KEY_USER_NAME] = name
            preferences[KEY_USER_DESCRIPTION] = description
        }
    }

    // Psychology Dimensions

    val psychologyDimensions: Flow<PsychologyDimensions> = context.dataStore.data.map { preferences ->
        PsychologyDimensions(
            moodStability = preferences[KEY_MOOD_STABILITY] ?: 0.50f,
            selfAwareness = preferences[KEY_SELF_AWARENESS] ?: 0.50f,
            stressManagement = preferences[KEY_STRESS_MANAGEMENT] ?: 0.50f,
            socialConfidence = preferences[KEY_SOCIAL_CONFIDENCE] ?: 0.50f,
            sleepQuality = preferences[KEY_SLEEP_QUALITY] ?: 0.50f,
            selfCare = preferences[KEY_SELF_CARE] ?: 0.50f
        )
    }

    suspend fun updatePsychologyDimension(dimension: String, value: Float) {
        context.dataStore.edit { preferences ->
            when (dimension) {
                "moodStability" -> preferences[KEY_MOOD_STABILITY] = value.coerceIn(0f, 1f)
                "selfAwareness" -> preferences[KEY_SELF_AWARENESS] = value.coerceIn(0f, 1f)
                "stressManagement" -> preferences[KEY_STRESS_MANAGEMENT] = value.coerceIn(0f, 1f)
                "socialConfidence" -> preferences[KEY_SOCIAL_CONFIDENCE] = value.coerceIn(0f, 1f)
                "sleepQuality" -> preferences[KEY_SLEEP_QUALITY] = value.coerceIn(0f, 1f)
                "selfCare" -> preferences[KEY_SELF_CARE] = value.coerceIn(0f, 1f)
            }
        }
    }

    // 重置所有心理学维度为默认值
    suspend fun resetPsychologyDimensions() {
        context.dataStore.edit { preferences ->
            preferences[KEY_MOOD_STABILITY] = 0.50f
            preferences[KEY_SELF_AWARENESS] = 0.50f
            preferences[KEY_STRESS_MANAGEMENT] = 0.50f
            preferences[KEY_SOCIAL_CONFIDENCE] = 0.50f
            preferences[KEY_SLEEP_QUALITY] = 0.50f
            preferences[KEY_SELF_CARE] = 0.50f
        }
    }

    // Usage Stats

    val usageStats: Flow<UsageStats> = context.dataStore.data.map { preferences ->
        UsageStats(
            chatCount = preferences[KEY_CHAT_COUNT] ?: 0,
            breathingCount = preferences[KEY_BREATHING_COUNT] ?: 0,
            checkInCount = preferences[KEY_CHECK_IN_COUNT] ?: 0
        )
    }

    suspend fun incrementChatCount() {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_CHAT_COUNT] ?: 0
            preferences[KEY_CHAT_COUNT] = current + 1
        }
    }

    suspend fun incrementBreathingCount() {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_BREATHING_COUNT] ?: 0
            preferences[KEY_BREATHING_COUNT] = current + 1
        }
    }

    suspend fun incrementCheckInCount() {
        context.dataStore.edit { preferences ->
            val current = preferences[KEY_CHECK_IN_COUNT] ?: 0
            preferences[KEY_CHECK_IN_COUNT] = current + 1
        }
    }

    // Chat Sessions

    val allSessions: Flow<List<ChatSession>> = context.dataStore.data.map { preferences ->
        val json = preferences[KEY_CHAT_SESSIONS] ?: "[]"
        try {
            val type = object : TypeToken<List<ChatSessionEntity>>() {}.type
            val entities: List<ChatSessionEntity> = gson.fromJson(json, type)
            entities.map { it.toChatSession() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun createSession(title: String): ChatSession {
        val session = ChatSession(
            id = "session_${UUID.randomUUID()}",
            title = title,
            lastMessage = "",
            timestamp = System.currentTimeMillis(),
            messageCount = 0
        )
        val sessions = getCurrentSessions().toMutableList()
        sessions.add(0, session.toEntity())
        saveSessions(sessions)
        return session
    }

    suspend fun addMessage(sessionId: String, content: String, isFromUser: Boolean): Message {
        val message = Message(
            id = "msg_${UUID.randomUUID()}",
            content = content,
            isFromUser = isFromUser,
            timestamp = System.currentTimeMillis()
        )

        // Save message
        val messages = getCurrentMessages().toMutableList()
        messages.add(message.toEntity(sessionId))
        saveMessages(messages)

        // Update session
        val sessions = getCurrentSessions().toMutableList()
        val sessionIndex = sessions.indexOfFirst { it.id == sessionId }
        if (sessionIndex >= 0) {
            val session = sessions[sessionIndex]
            val prefix = if (isFromUser) "我: " else "小晴: "
            sessions[sessionIndex] = session.copy(
                lastMessage = prefix + content,
                messageCount = session.messageCount + 1,
                timestamp = message.timestamp
            )
            saveSessions(sessions)
        }

        return message
    }

    // 更新消息内容 (用于流式消息)
    suspend fun updateMessageContent(sessionId: String, messageId: String, newContent: String) {
        val messages = getCurrentMessages().toMutableList()
        val index = messages.indexOfFirst { it.id == messageId && it.sessionId == sessionId }
        if (index >= 0) {
            messages[index] = messages[index].copy(content = newContent)
            saveMessages(messages)

            // Also update session's lastMessage
            val sessions = getCurrentSessions().toMutableList()
            val sessionIndex = sessions.indexOfFirst { it.id == sessionId }
            if (sessionIndex >= 0) {
                val session = sessions[sessionIndex]
                sessions[sessionIndex] = session.copy(
                    lastMessage = "小晴: " + newContent.take(50),
                    timestamp = System.currentTimeMillis()
                )
                saveSessions(sessions)
            }
        }
    }

    suspend fun deleteSession(sessionId: String) {
        val sessions = getCurrentSessions().toMutableList()
        sessions.removeAll { it.id == sessionId }
        saveSessions(sessions)

        // Also delete messages for this session
        val messages = getCurrentMessages().toMutableList()
        messages.removeAll { it.sessionId == sessionId }
        saveMessages(messages)
    }

    // 增加会话的分析次数
    suspend fun incrementSessionAnalysisCount(sessionId: String): Int? {
        val sessions = getCurrentSessions().toMutableList()
        val sessionIndex = sessions.indexOfFirst { it.id == sessionId }
        if (sessionIndex < 0) return null

        val session = sessions[sessionIndex]
        val newAnalysisCount = session.analysisCount + 1
        sessions[sessionIndex] = session.copy(analysisCount = newAnalysisCount)
        saveSessions(sessions)
        return newAnalysisCount
    }

    // 获取会话的分析次数
    suspend fun getSessionAnalysisCount(sessionId: String): Int {
        val sessions = getCurrentSessions().toMutableList()
        val session = sessions.find { it.id == sessionId }
        return session?.analysisCount ?: 0
    }

    // 删除指定消息
    suspend fun deleteMessage(messageId: String, sessionId: String) {
        val messages = getCurrentMessages().toMutableList()
        messages.removeAll { it.id == messageId && it.sessionId == sessionId }
        saveMessages(messages)

        // Also update session's message count
        val sessions = getCurrentSessions().toMutableList()
        val sessionIndex = sessions.indexOfFirst { it.id == sessionId }
        if (sessionIndex >= 0) {
            val session = sessions[sessionIndex]
            sessions[sessionIndex] = session.copy(
                messageCount = (session.messageCount - 1).coerceAtLeast(0)
            )
            saveSessions(sessions)
        }
    }

    suspend fun deleteAllSessions() {
        context.dataStore.edit { preferences ->
            preferences[KEY_CHAT_SESSIONS] = "[]"
            preferences[KEY_MESSAGES] = "[]"
        }
    }

    fun getMessagesForSession(sessionId: String): Flow<List<Message>> = context.dataStore.data.map { preferences ->
        val json = preferences[KEY_MESSAGES] ?: "[]"
        try {
            val type = object : TypeToken<List<MessageEntity>>() {}.type
            val entities: List<MessageEntity> = gson.fromJson(json, type)
            entities.filter { it.sessionId == sessionId }.map { it.toMessage() }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun getCurrentSessions(): List<ChatSessionEntity> {
        val preferences = context.dataStore.data.first()
        val json = preferences[KEY_CHAT_SESSIONS] ?: "[]"
        return try {
            val type = object : TypeToken<List<ChatSessionEntity>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun saveSessions(sessions: List<ChatSessionEntity>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_CHAT_SESSIONS] = gson.toJson(sessions)
        }
    }

    private suspend fun getCurrentMessages(): List<MessageEntity> {
        val preferences = context.dataStore.data.first()
        val json = preferences[KEY_MESSAGES] ?: "[]"
        return try {
            val type = object : TypeToken<List<MessageEntity>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun saveMessages(messages: List<MessageEntity>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MESSAGES] = gson.toJson(messages)
        }
    }

    // Achievements

    val allAchievements: Flow<List<Achievement>> = context.dataStore.data.map { preferences ->
        val json = preferences[KEY_ACHIEVEMENTS]
        if (json.isNullOrEmpty()) {
            // First launch - return default achievements
            AchievementList.achievements
        } else {
            try {
                val type = object : TypeToken<List<Achievement>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                AchievementList.achievements
            }
        }
    }

    val unlockedCount: Flow<Int> = context.dataStore.data.map { preferences ->
        val json = preferences[KEY_ACHIEVEMENTS]
        if (json.isNullOrEmpty()) {
            AchievementList.achievements.count { it.isUnlocked }
        } else {
            try {
                val type = object : TypeToken<List<Achievement>>() {}.type
                val achievements: List<Achievement> = gson.fromJson(json, type)
                achievements.count { it.isUnlocked }
            } catch (e: Exception) {
                0
            }
        }
    }

    suspend fun unlockAchievement(achievementId: String) {
        val achievements = getCurrentAchievements().toMutableList()
        val index = achievements.indexOfFirst { it.id == achievementId }
        if (index >= 0 && !achievements[index].isUnlocked) {
            achievements[index] = achievements[index].copy(
                isUnlocked = true,
                unlockedDate = dateFormat.format(Date())
            )
            saveAchievements(achievements)
        }
    }

    suspend fun lockAchievement(achievementId: String) {
        val achievements = getCurrentAchievements().toMutableList()
        val index = achievements.indexOfFirst { it.id == achievementId }
        if (index >= 0 && achievements[index].isUnlocked) {
            achievements[index] = achievements[index].copy(
                isUnlocked = false,
                unlockedDate = null
            )
            saveAchievements(achievements)
        }
    }

    suspend fun resetAllAchievements() {
        saveAchievements(AchievementList.achievements)
    }

    private suspend fun getCurrentAchievements(): List<Achievement> {
        val preferences = context.dataStore.data.first()
        val json = preferences[KEY_ACHIEVEMENTS]
        return if (json.isNullOrEmpty()) {
            AchievementList.achievements
        } else {
            try {
                val type = object : TypeToken<List<Achievement>>() {}.type
                gson.fromJson(json, type)
            } catch (e: Exception) {
                AchievementList.achievements
            }
        }
    }

    private suspend fun saveAchievements(achievements: List<Achievement>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_ACHIEVEMENTS] = gson.toJson(achievements)
        }
    }

    // Mood Entries

    val moodEntries: Flow<List<MoodEntry>> = context.dataStore.data.map { preferences ->
        val json = preferences[KEY_MOOD_ENTRIES] ?: "[]"
        try {
            val type = object : TypeToken<List<MoodEntryEntity>>() {}.type
            val entities: List<MoodEntryEntity> = gson.fromJson(json, type)
            entities.map { it.toMoodEntry() }.sortedByDescending { it.timestamp }
        } catch (e: Exception) {
            emptyList()
        }
    }

    val latestMood: Flow<MoodEntry?> = context.dataStore.data.map { preferences ->
        val json = preferences[KEY_MOOD_ENTRIES] ?: "[]"
        try {
            val type = object : TypeToken<List<MoodEntryEntity>>() {}.type
            val entities: List<MoodEntryEntity> = gson.fromJson(json, type)
            entities.map { it.toMoodEntry() }.maxByOrNull { it.timestamp }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun addMoodEntry(mood: MoodType, reason: String) {
        val entry = MoodEntry(
            mood = mood,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )
        val entries = getCurrentMoodEntries().toMutableList()
        entries.add(0, entry.toEntity())
        saveMoodEntries(entries)
    }

    suspend fun deleteMoodEntry(entryId: String) {
        val entries = getCurrentMoodEntries().toMutableList()
        entries.removeAll { it.id == entryId }
        saveMoodEntries(entries)
    }

    suspend fun updateMoodEntry(entryId: String, mood: MoodType, reason: String) {
        val entries = getCurrentMoodEntries().toMutableList()
        val index = entries.indexOfFirst { it.id == entryId }
        if (index >= 0) {
            entries[index] = entries[index].copy(
                mood = mood.name,
                reason = reason
            )
            saveMoodEntries(entries)
        }
    }

    private suspend fun getCurrentMoodEntries(): List<MoodEntryEntity> {
        val preferences = context.dataStore.data.first()
        val json = preferences[KEY_MOOD_ENTRIES] ?: "[]"
        return try {
            val type = object : TypeToken<List<MoodEntryEntity>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun saveMoodEntries(entries: List<MoodEntryEntity>) {
        context.dataStore.edit { preferences ->
            preferences[KEY_MOOD_ENTRIES] = gson.toJson(entries)
        }
    }

    // Clear All Data

    suspend fun clearAllData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    // Daily Activity Tracking

    // 重置新的一天（跨天时调用）
    suspend fun resetDailyActivitiesIfNewDay() {
        context.dataStore.edit { preferences ->
            val today = dateFormat.format(Date())
            val lastDate = preferences[KEY_LAST_ACTIVITY_DATE]
            if (lastDate != today) {
                preferences[KEY_LAST_ACTIVITY_DATE] = today
                preferences[KEY_TODAY_CHECKIN] = false
                preferences[KEY_TODAY_CHAT] = false
                preferences[KEY_TODAY_BREATHING] = false
            }
        }
    }

    // 标记今日已完成签到
    suspend fun markTodayCheckin() {
        context.dataStore.edit { preferences ->
            preferences[KEY_TODAY_CHECKIN] = true
        }
    }

    // 标记今日已完成聊天
    suspend fun markTodayChat() {
        context.dataStore.edit { preferences ->
            preferences[KEY_TODAY_CHAT] = true
        }
    }

    // 标记今日已完成呼吸练习
    suspend fun markTodayBreathing() {
        context.dataStore.edit { preferences ->
            preferences[KEY_TODAY_BREATHING] = true
        }
    }

    // 检查今日是否三项活动都完成
    suspend fun isTodayPerfectDay(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[KEY_TODAY_CHECKIN] == true &&
                preferences[KEY_TODAY_CHAT] == true &&
                preferences[KEY_TODAY_BREATHING] == true
    }

    // 检查并解锁早起鸟成就（8点前签到）
    suspend fun checkAndUnlockEarlyBird(hour: Int): Boolean {
        if (hour < 8) {
            val preferences = context.dataStore.data.first()
            if (preferences[KEY_EARLY_BIRD_UNLOCKED] != true) {
                context.dataStore.edit { prefs ->
                    prefs[KEY_EARLY_BIRD_UNLOCKED] = true
                }
                return true
            }
        }
        return false
    }

    // 获取今日活动状态
    suspend fun getTodayActivities(): TodayActivities {
        val preferences = context.dataStore.data.first()
        return TodayActivities(
            checkin = preferences[KEY_TODAY_CHECKIN] ?: false,
            chat = preferences[KEY_TODAY_CHAT] ?: false,
            breathing = preferences[KEY_TODAY_BREATHING] ?: false
        )
    }

    data class TodayActivities(
        val checkin: Boolean,
        val chat: Boolean,
        val breathing: Boolean
    )

    // Entity Classes for JSON serialization

    private data class ChatSessionEntity(
        val id: String,
        val title: String,
        val lastMessage: String,
        val timestamp: Long,
        val messageCount: Int,
        val analysisCount: Int = 0
    )

    private data class MessageEntity(
        val id: String,
        val sessionId: String,
        val content: String,
        val isFromUser: Boolean,
        val timestamp: Long
    )

    private fun ChatSession.toEntity() = ChatSessionEntity(
        id = id,
        title = title,
        lastMessage = lastMessage,
        timestamp = timestamp,
        messageCount = messageCount,
        analysisCount = analysisCount
    )

    private fun ChatSessionEntity.toChatSession() = ChatSession(
        id = id,
        title = title,
        lastMessage = lastMessage,
        timestamp = timestamp,
        messageCount = messageCount,
        analysisCount = analysisCount
    )

    private fun Message.toEntity(sessionId: String) = MessageEntity(
        id = id,
        sessionId = sessionId,
        content = content,
        isFromUser = isFromUser,
        timestamp = timestamp
    )

    private fun MessageEntity.toMessage() = Message(
        id = id,
        content = content,
        isFromUser = isFromUser,
        timestamp = timestamp
    )

    private data class MoodEntryEntity(
        val id: String,
        val mood: String,
        val reason: String,
        val timestamp: Long
    )

    private fun MoodEntry.toEntity() = MoodEntryEntity(
        id = id,
        mood = mood.name,
        reason = reason,
        timestamp = timestamp
    )

    private fun MoodEntryEntity.toMoodEntry() = MoodEntry(
        id = id,
        mood = MoodType.valueOf(mood),
        reason = reason,
        timestamp = timestamp
    )
}
