package com.onmi.qing.data.repository

import com.onmi.qing.data.ChatSession
import com.onmi.qing.data.Message
import com.onmi.qing.data.local.dao.ChatDao
import com.onmi.qing.data.local.entity.ChatSessionEntity
import com.onmi.qing.data.local.entity.MessageEntity
import com.onmi.qing.data.local.toDomain
import com.onmi.qing.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class ChatRepository(private val chatDao: ChatDao) {

    fun getAllSessions(): Flow<List<ChatSession>> =
        chatDao.getAllSessions().map { list -> list.map { it.toDomain() } }

    fun getMessagesForSession(sessionId: String): Flow<List<Message>> =
        chatDao.getMessagesForSession(sessionId).map { list -> list.map { it.toDomain() } }

    suspend fun getMessagesForSessionOnce(sessionId: String): List<Message> =
        chatDao.getMessagesForSessionOnce(sessionId).map { it.toDomain() }

    suspend fun createSession(title: String): ChatSession {
        val session = ChatSession(
            id = "session_${UUID.randomUUID()}",
            title = title,
            lastMessage = "",
            timestamp = System.currentTimeMillis(),
            messageCount = 0
        )
        chatDao.insertSession(session.toEntity())
        return session
    }

    suspend fun addMessage(sessionId: String, content: String, isFromUser: Boolean): Message {
        val message = Message(
            id = "msg_${UUID.randomUUID()}",
            content = content,
            isFromUser = isFromUser,
            timestamp = System.currentTimeMillis()
        )
        chatDao.insertMessage(message.toEntity(sessionId))

        // Update session's lastMessage and messageCount
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            val prefix = if (isFromUser) "我: " else "小晴: "
            chatDao.updateSession(
                session.copy(
                    lastMessage = prefix + content,
                    messageCount = session.messageCount + 1,
                    timestamp = message.timestamp
                )
            )
        }

        return message
    }

    suspend fun updateMessageContent(messageId: String, newContent: String) {
        val message = chatDao.getMessageById(messageId) ?: return
        chatDao.updateMessage(message.copy(content = newContent))

        // Also update session's lastMessage
        val session = chatDao.getSessionById(message.sessionId)
        if (session != null) {
            chatDao.updateSession(
                session.copy(
                    lastMessage = "小晴: " + newContent.take(50),
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    suspend fun deleteMessage(messageId: String, sessionId: String) {
        val message = chatDao.getMessageById(messageId) ?: return
        chatDao.deleteMessage(message)

        // Update session message count
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            chatDao.updateSession(
                session.copy(
                    messageCount = (session.messageCount - 1).coerceAtLeast(0)
                )
            )
        }
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteMessagesBySession(sessionId)
        val session = chatDao.getSessionById(sessionId)
        if (session != null) {
            chatDao.deleteSession(session)
        }
    }

    suspend fun incrementSessionAnalysisCount(sessionId: String): Int? {
        val session = chatDao.getSessionById(sessionId) ?: return null
        val newCount = session.analysisCount + 1
        chatDao.updateSession(session.copy(analysisCount = newCount))
        return newCount
    }

    suspend fun getSessionAnalysisCount(sessionId: String): Int {
        return chatDao.getSessionById(sessionId)?.analysisCount ?: 0
    }

    suspend fun insertSessionDirect(entity: ChatSessionEntity) {
        chatDao.insertSession(entity)
    }

    suspend fun insertMessageDirect(entity: MessageEntity) {
        chatDao.insertMessage(entity)
    }

    suspend fun deleteAllData() {
        chatDao.deleteAllMessages()
        chatDao.deleteAllSessions()
    }

    suspend fun getSessionCount(): Int = chatDao.getSessionCount()
}
