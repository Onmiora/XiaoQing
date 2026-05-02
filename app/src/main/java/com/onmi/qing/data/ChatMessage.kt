package com.onmi.qing.data

enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

sealed class MessagePart {
    data class Text(val text: String) : MessagePart()
    data class Thinking(
        val thinking: String,
        val durationMs: Long? = null
    ) : MessagePart()
}

data class ChatMessage(
    val id: String,
    val role: MessageRole,
    val parts: List<MessagePart>,
    val timestamp: Long = System.currentTimeMillis(),
    val regenerationIndex: Int = 0,
    val isRegenerating: Boolean = false
) {
    val isFromUser: Boolean get() = role == MessageRole.USER
    val textContent: String
        get() = parts.filterIsInstance<MessagePart.Text>().joinToString("") { it.text }
    val thinkingContent: String?
        get() = parts.filterIsInstance<MessagePart.Thinking>().firstOrNull()?.thinking
    val hasThinking: Boolean get() = parts.any { it is MessagePart.Thinking }
}
