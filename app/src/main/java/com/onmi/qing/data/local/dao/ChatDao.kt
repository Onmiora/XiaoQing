package com.onmi.qing.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.onmi.qing.data.local.entity.ChatSessionEntity
import com.onmi.qing.data.local.entity.MessageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatDao {
    @Query("SELECT * FROM chat_sessions ORDER BY timestamp DESC")
    fun getAllSessions(): Flow<List<ChatSessionEntity>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getMessagesForSession(sessionId: String): Flow<List<MessageEntity>>

    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getMessagesForSessionOnce(sessionId: String): List<MessageEntity>

    @Query("SELECT * FROM messages WHERE id = :messageId LIMIT 1")
    suspend fun getMessageById(messageId: String): MessageEntity?

    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): ChatSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: ChatSessionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: MessageEntity)

    @Update
    suspend fun updateSession(session: ChatSessionEntity)

    @Update
    suspend fun updateMessage(message: MessageEntity)

    @Delete
    suspend fun deleteMessage(message: MessageEntity)

    @Delete
    suspend fun deleteSession(session: ChatSessionEntity)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesBySession(sessionId: String)

    @Query("DELETE FROM chat_sessions")
    suspend fun deleteAllSessions()

    @Query("DELETE FROM messages")
    suspend fun deleteAllMessages()

    @Query("SELECT COUNT(*) FROM chat_sessions")
    suspend fun getSessionCount(): Int

    @Query("DELETE FROM messages WHERE id = :messageId")
    suspend fun deleteMessageById(messageId: String)

    @Query("SELECT COUNT(*) FROM messages WHERE sessionId = :sessionId")
    suspend fun getMessageCount(sessionId: String): Int

    @Query("UPDATE chat_sessions SET lastMessage = :lastMessage, timestamp = :timestamp, messageCount = :messageCount WHERE id = :sessionId")
    suspend fun updateSessionLastMessage(sessionId: String, lastMessage: String, timestamp: Long, messageCount: Int)

    @Query("UPDATE chat_sessions SET messageCount = :count WHERE id = :sessionId")
    suspend fun updateSessionMessageCount(sessionId: String, count: Int)

    @Query("SELECT analysisCount FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getAnalysisCount(sessionId: String): Int?

    @Query("UPDATE chat_sessions SET analysisCount = :count WHERE id = :sessionId")
    suspend fun updateAnalysisCount(sessionId: String, count: Int)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteMessagesForSession(sessionId: String)

    @Query("UPDATE messages SET partsJson = :partsJson WHERE id = :messageId")
    suspend fun updateMessageParts(messageId: String, partsJson: String)
}
