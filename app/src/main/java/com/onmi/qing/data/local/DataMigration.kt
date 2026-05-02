package com.onmi.qing.data.local

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.onmi.qing.data.Achievement
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.local.entity.MoodEntryEntity
import com.onmi.qing.data.repository.AchievementRepository
import com.onmi.qing.data.repository.ChatRepository
import com.onmi.qing.data.repository.MoodRepository
import kotlinx.coroutines.flow.first

object DataMigration {
    private const val TAG = "DataMigration"
    private const val PREF_KEY_CHAT_SESSIONS = "chat_sessions"
    private const val PREF_KEY_MESSAGES = "messages"
    private const val PREF_KEY_ACHIEVEMENTS = "achievements"
    private const val PREF_KEY_MOOD_ENTRIES = "mood_entries"

    suspend fun migrateIfNeeded(
        dataStore: QingDataStore,
        chatRepository: ChatRepository,
        moodRepository: MoodRepository,
        achievementRepository: AchievementRepository
    ) {
        try {
            // Check if Room already has data
            if (chatRepository.getSessionCount() > 0) {
                Log.d(TAG, "Room already has data, skipping migration")
                return
            }

            // Read old JSON data from DataStore
            val prefs = dataStore.rawDataStore.data.first()
            val gson = Gson()

            // Migrate chat sessions
            val sessionsJson = prefs[androidx.datastore.preferences.core.stringPreferencesKey(PREF_KEY_CHAT_SESSIONS)]
            if (!sessionsJson.isNullOrEmpty() && sessionsJson != "[]") {
                try {
                    val type = object : TypeToken<List<OldChatSessionEntity>>() {}.type
                    val oldSessions: List<OldChatSessionEntity> = gson.fromJson(sessionsJson, type)
                    for (old in oldSessions) {
                        val entity = com.onmi.qing.data.local.entity.ChatSessionEntity(
                            id = old.id,
                            title = old.title,
                            lastMessage = old.lastMessage,
                            timestamp = old.timestamp,
                            messageCount = old.messageCount,
                            analysisCount = old.analysisCount
                        )
                        chatRepository.insertSessionDirect(entity)
                    }
                    Log.d(TAG, "Migrated ${oldSessions.size} chat sessions")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to migrate chat sessions", e)
                }
            }

            // Migrate messages
            val messagesJson = prefs[androidx.datastore.preferences.core.stringPreferencesKey(PREF_KEY_MESSAGES)]
            if (!messagesJson.isNullOrEmpty() && messagesJson != "[]") {
                try {
                    val type = object : TypeToken<List<OldMessageEntity>>() {}.type
                    val oldMessages: List<OldMessageEntity> = gson.fromJson(messagesJson, type)
                    for (old in oldMessages) {
                        val role = if (old.isFromUser) "USER" else "ASSISTANT"
                        val partsJson = """[{"type":"Text","text":"${old.content.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")}"}]"""
                        val entity = com.onmi.qing.data.local.entity.MessageEntity(
                            id = old.id,
                            sessionId = old.sessionId,
                            role = role,
                            partsJson = partsJson,
                            timestamp = old.timestamp
                        )
                        chatRepository.insertMessageDirect(entity)
                    }
                    Log.d(TAG, "Migrated ${oldMessages.size} messages")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to migrate messages", e)
                }
            }

            // Migrate achievements (unlock states)
            val achievementsJson = prefs[androidx.datastore.preferences.core.stringPreferencesKey(PREF_KEY_ACHIEVEMENTS)]
            if (!achievementsJson.isNullOrEmpty()) {
                try {
                    val type = object : TypeToken<List<Achievement>>() {}.type
                    val oldAchievements: List<Achievement> = gson.fromJson(achievementsJson, type)
                    for (old in oldAchievements) {
                        if (old.isUnlocked) {
                            achievementRepository.unlock(old.id)
                        }
                    }
                    Log.d(TAG, "Migrated achievement unlock states")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to migrate achievements", e)
                }
            }

            // Migrate mood entries
            val moodJson = prefs[androidx.datastore.preferences.core.stringPreferencesKey(PREF_KEY_MOOD_ENTRIES)]
            if (!moodJson.isNullOrEmpty() && moodJson != "[]") {
                try {
                    val type = object : TypeToken<List<OldMoodEntryEntity>>() {}.type
                    val oldEntries: List<OldMoodEntryEntity> = gson.fromJson(moodJson, type)
                    for (old in oldEntries) {
                        val entity = MoodEntryEntity(
                            id = old.id,
                            mood = old.mood,
                            reason = old.reason,
                            timestamp = old.timestamp
                        )
                        moodRepository.insertDirect(entity)
                    }
                    Log.d(TAG, "Migrated ${oldEntries.size} mood entries")
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to migrate mood entries", e)
                }
            }

            Log.d(TAG, "Data migration completed successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Data migration failed", e)
        }
    }

    // Old entity classes matching the JSON structure in DataStore
    private data class OldChatSessionEntity(
        val id: String,
        val title: String,
        val lastMessage: String,
        val timestamp: Long,
        val messageCount: Int,
        val analysisCount: Int = 0
    )

    private data class OldMessageEntity(
        val id: String,
        val sessionId: String,
        val content: String,
        val isFromUser: Boolean,
        val timestamp: Long
    )

    private data class OldMoodEntryEntity(
        val id: String,
        val mood: String,
        val reason: String,
        val timestamp: Long
    )
}
