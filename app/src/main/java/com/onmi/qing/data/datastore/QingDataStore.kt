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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "qing_preferences")

// Qing 应用偏好设置存储（仅键值对，结构化数据由 Room 管理）
class QingDataStore(private val context: Context) {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

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

    // Daily Activity Tracking

    suspend fun resetDailyActivitiesIfNewDay() {
        context.dataStore.edit { preferences ->
            val today = LocalDate.now().format(dateFormatter)
            val lastDate = preferences[KEY_LAST_ACTIVITY_DATE]
            if (lastDate != today) {
                preferences[KEY_LAST_ACTIVITY_DATE] = today
                preferences[KEY_TODAY_CHECKIN] = false
                preferences[KEY_TODAY_CHAT] = false
                preferences[KEY_TODAY_BREATHING] = false
            }
        }
    }

    suspend fun markTodayCheckin() {
        context.dataStore.edit { preferences ->
            preferences[KEY_TODAY_CHECKIN] = true
        }
    }

    suspend fun markTodayChat() {
        context.dataStore.edit { preferences ->
            preferences[KEY_TODAY_CHAT] = true
        }
    }

    suspend fun markTodayBreathing() {
        context.dataStore.edit { preferences ->
            preferences[KEY_TODAY_BREATHING] = true
        }
    }

    suspend fun isTodayPerfectDay(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[KEY_TODAY_CHECKIN] == true &&
                preferences[KEY_TODAY_CHAT] == true &&
                preferences[KEY_TODAY_BREATHING] == true
    }

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

    // Clear All Data
    suspend fun clearAllData() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }

    // Legacy accessors for DataStore migration (to be removed after migration)
    internal val rawDataStore: DataStore<Preferences> = context.dataStore
}
