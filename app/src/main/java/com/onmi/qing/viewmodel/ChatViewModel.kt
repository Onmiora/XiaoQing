package com.onmi.qing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import com.onmi.qing.data.ChatSession
import com.onmi.qing.data.ChatMessage
import com.onmi.qing.data.MessagePart
import com.onmi.qing.data.MessageRole
import com.onmi.qing.data.Recommendation
import com.onmi.qing.data.CrisisIntervention
import com.onmi.qing.data.parseCrisisIntervention
import com.onmi.qing.data.parseRecommendation
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.data.remote.AnthropicMessage
import com.onmi.qing.data.remote.AnthropicRequest
import com.onmi.qing.data.remote.ApiServiceFactory
import com.onmi.qing.data.remote.ChatApiService
import com.onmi.qing.data.remote.SseEvent
import com.onmi.qing.data.remote.SseEventParser
import com.onmi.qing.data.repository.ChatRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 聊天页面 ViewModel
@HiltViewModel
class ChatViewModel @Inject constructor(
    private val dataStore: QingDataStore,
    private val chatRepository: ChatRepository,
    private val demoModeManager: DemoModeManager,
    private val usageStatsManager: com.onmi.qing.data.UsageStatsManager,
    private val apiServiceFactory: ApiServiceFactory
) : ViewModel() {

    private companion object {
        private const val ERROR_MESSAGE = "抱歉，服务出现了问题。请稍后再试。\n\n⚠️ 后端服务出现问题"
        private val GREETINGS = listOf(
            "你好呀！今天感觉怎么样？有什么想和我聊聊的吗？",
            "嗨，欢迎回来！今天过得如何？",
            "你好！看到你来我很开心。今天有什么心事想说吗？"
        )
    }

    private fun getSessionTitle(): String {
        val sdf = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _recommendation = MutableStateFlow<Recommendation?>(null)
    val recommendation: StateFlow<Recommendation?> = _recommendation.asStateFlow()

    private val _crisisIntervention = MutableStateFlow<CrisisIntervention?>(null)
    val crisisIntervention: StateFlow<CrisisIntervention?> = _crisisIntervention.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isAiTyping = MutableStateFlow(false)
    val isAiTyping: StateFlow<Boolean> = _isAiTyping.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    private val _currentSessionTitle = MutableStateFlow<String?>(null)

    private val chatApiService: ChatApiService by lazy {
        apiServiceFactory.create<ChatApiService>()
    }

    // Streaming state
    private var streamingMessageId: String? = null
    private val sseParser = SseEventParser()

    // 是否为演示模式
    private val isDemoMode: Boolean
        get() = demoModeManager.isDemoMode.value

    // 更新输入文本
    fun updateInputText(text: String) {
        _inputText.value = text
    }

    // 发送用户消息
    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return

        // 立即清空输入框，提供更好的 UX 反馈
        _inputText.value = ""

        // Lazy create session when user sends first message
        if (_currentSessionId.value == null) {
            viewModelScope.launch {
                val session = chatRepository.createSession(getSessionTitle())
                _currentSessionId.value = session.id
                _currentSessionTitle.value = session.title
                // Add greeting message first
                addAiMessage(GREETINGS.random())
                // 然后再发送用户消息
                doSendMessage(text)
            }
            return
        }

        doSendMessage(text)
    }

    // 构建消息上下文（供流式和非流式共用）
    private fun buildMessageContext(currentText: String): List<AnthropicMessage> {
        val messageHistory = _messages.value
            .sortedBy { it.timestamp }
            .drop(1) // Skip the first message (AI greeting)
            .map { msg ->
                AnthropicMessage(
                    role = if (msg.isFromUser) "user" else "assistant",
                    content = msg.textContent
                )
            }
        return messageHistory + AnthropicMessage(role = "user", content = currentText)
    }

    // 执行实际发送逻辑
    private fun doSendMessage(text: String) {
        val sessionId = _currentSessionId.value ?: return

        // Add user message (always use real API)
        addUserMessage(text)

        // Increment chat count for achievements (only in user mode)
        if (!isDemoMode) {
            viewModelScope.launch {
                usageStatsManager.incrementChatCount()
            }
        }

        // Show AI typing state
        _isAiTyping.value = true

        viewModelScope.launch {
            try {
                val streamed = streamChat(text)
                if (!streamed) {
                    // Fallback to non-streaming
                    Log.w("ChatViewModel", "Streaming failed, falling back to non-streaming")
                    nonStreamingChat(text)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Streaming error: ${e.message}", e)
                // 如果流式开始后失败，需要清理已添加的空消息占位符
                val placeholderId = streamingMessageId
                if (placeholderId != null) {
                    // 从 UI 移除
                    _messages.update { list -> list.filter { it.id != placeholderId } }
                    // 从 Room 删除占位符（避免重启后显示空消息）
                    chatRepository.deleteMessage(placeholderId, sessionId)
                }
                streamingMessageId = null
                _isAiTyping.value = false
                nonStreamingChat(text)
            }
        }
    }

    // 非流式聊天
    private suspend fun nonStreamingChat(text: String) {
        try {
            val prefs = dataStore.userPreferences.first()
            val allMessages = buildMessageContext(text)

            val request = AnthropicRequest(
                model = prefs.modelName,
                messages = allMessages,
                max_tokens = 4096,
                stream = false
            )
            val response = chatApiService.chat(request)
            _isAiTyping.value = false

            Log.d("ChatViewModel", "Response received: isSuccessful=${response.isSuccessful}, code=${response.code()}")

            if (response.isSuccessful) {
                val responseBody = response.body()
                Log.d("ChatViewModel", "Response body: $responseBody")
                Log.d("ChatViewModel", "Content blocks: ${responseBody?.content}")

                // 提取所有 text 类型的 content 块
                val contentBlocks = responseBody?.content
                val textContent = contentBlocks
                    ?.filter { it.type == "text" }
                    ?.mapNotNull { it.text }
                    ?.joinToString("")

                Log.d("ChatViewModel", "Extracted text content: $textContent")

                if (!textContent.isNullOrEmpty()) {
                    addAiMessage(textContent)
                    // Parse recommendation from response
                    val recommendation = parseRecommendation(textContent)
                    _recommendation.value = recommendation
                } else {
                    // 如果没有 text 内容，尝试获取任意非空内容
                    val anyContent = contentBlocks?.firstOrNull()?.text
                        ?: contentBlocks?.firstOrNull()?.thinking
                    if (!anyContent.isNullOrEmpty()) {
                        addAiMessage(anyContent)
                    } else {
                        Log.w("ChatViewModel", "No content found in response")
                        addAiMessage(ERROR_MESSAGE)
                    }
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Log.e("ChatViewModel", "API error ${response.code()}: $errorBody")
                addAiMessage(ERROR_MESSAGE)
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Network error: ${e.message}", e)
            _isAiTyping.value = false
            addAiMessage(ERROR_MESSAGE)
        }
    }

    // 流式聊天 - 使用 SSE
    private suspend fun streamChat(text: String): Boolean {
        try {
            val prefs = dataStore.userPreferences.first()
            val allMessages = buildMessageContext(text)

            val request = AnthropicRequest(
                model = prefs.modelName,
                messages = allMessages,
                max_tokens = 4096,
                stream = true  // Enable streaming
            )

            val response = chatApiService.chatStreaming(request)

            if (!response.isSuccessful) {
                Log.e("ChatViewModel", "Streaming request failed: ${response.code()}")
                return false
            }

            val responseBody = response.body() ?: run {
                Log.e("ChatViewModel", "Streaming response body is null")
                return false
            }

            Log.d("ChatViewModel", "Streaming started successfully")

            // Create in-progress AI message
            val sessionId = _currentSessionId.value ?: return false
            val savedMessage = chatRepository.addMessage(sessionId, "", MessageRole.ASSISTANT)
            streamingMessageId = savedMessage.id

            // Add to UI immediately (shows empty message)
            _messages.update { currentList -> currentList + savedMessage }

            // Parse SSE events
            var accumulatedContent = StringBuilder()
            var thinkingContent = StringBuilder()
            var isComplete = false
            var hasRecommendation = false

            sseParser.parseEvents(responseBody).collect { event ->
                when (event) {
                    is SseEvent.ContentBlockDelta -> {
                        accumulatedContent.append(event.text)
                        updateStreamingMessage(event.text)
                    }
                    is SseEvent.ThinkingDelta -> {
                        thinkingContent.append(event.thinking)
                        Log.d("ChatViewModel", "Thinking delta received: ${event.thinking.take(50)}...")
                    }
                    is SseEvent.MessageStop -> {
                        isComplete = true
                        Log.d("ChatViewModel", "Stream complete: $accumulatedContent")
                    }
                    is SseEvent.ToolUse -> {
                        if (event.name == "recommend_feature") {
                            val recommendation = parseRecommendation(event.input)
                            if (recommendation != null) {
                                _recommendation.value = recommendation
                                hasRecommendation = true
                            }
                            Log.d("ChatViewModel", "ToolUse recommend_feature: $recommendation")
                        } else if (event.name == "crisis_intervention") {
                            val crisis = parseCrisisIntervention(event.input)
                            if (crisis != null) {
                                _crisisIntervention.value = crisis
                            }
                            Log.d("ChatViewModel", "ToolUse crisis_intervention: $crisis")
                        }
                    }
                    is SseEvent.Ping -> {
                        // Ignore pings
                    }
                    is SseEvent.Error -> {
                        Log.e("ChatViewModel", "SSE Error: ${event.error}")
                    }
                    is SseEvent.ContentBlockStart,
                    is SseEvent.ContentBlockComplete,
                    is SseEvent.MessageDelta -> {
                        Log.d("ChatViewModel", "Received event: $event")
                    }
                    is SseEvent.Unknown -> {
                        Log.w("ChatViewModel", "Unknown event: ${event.event}")
                    }
                }
            }

            // Finalize the message
            _isAiTyping.value = false

            // Determine final content
            val finalContent = when {
                // If we have text content, use it
                accumulatedContent.isNotEmpty() && isComplete -> {
                    accumulatedContent.toString()
                }
                // If we only have thinking content, use it as the response
                thinkingContent.isNotEmpty() -> {
                    Log.d("ChatViewModel", "Using thinking content as response")
                    thinkingContent.toString()
                }
                // If we have a recommendation from tool_use but no text, provide a generic message
                hasRecommendation && isComplete -> {
                    Log.d("ChatViewModel", "Using generic message for tool_use recommendation")
                    "我为你推荐了这个功能，快去试试吧！"
                }
                // Otherwise use fallback
                else -> {
                    Log.w("ChatViewModel", "Stream incomplete, using fallback")
                    ERROR_MESSAGE
                }
            }

            // 保存必要的 ID（必须在清空 streamingMessageId 之前）
            val messageId = streamingMessageId

            // 清空流式状态
            streamingMessageId = null

            // 完成消息（复用已有的 sessionId）
            if (messageId != null) {
                finalizeStreamingMessage(messageId, sessionId, finalContent)
            }

            return true

        } catch (e: Exception) {
            Log.e("ChatViewModel", "Stream error: ${e.message}", e)
            _isAiTyping.value = false
            streamingMessageId = null
            throw e
        }
    }

    // 更新流式消息内容 (增量更新)
    private fun updateStreamingMessage(additionalText: String) {
        val messageId = streamingMessageId ?: return

        _messages.update { currentList ->
            currentList.map { message ->
                if (message.id == messageId) {
                    // Create new ChatMessage object with updated content to trigger Compose recomposition
                    message.copy(parts = listOf(MessagePart.Text(message.textContent + additionalText)))
                } else {
                    message
                }
            }
        }
    }

    // 完成流式消息 (最终更新)
    private suspend fun finalizeStreamingMessage(messageId: String, sessionId: String, finalContent: String) {

        // Only update recommendation if parseRecommendation returns non-null
        // This prevents overwriting a valid recommendation from tool_use with null from fallback text
        val recommendation = parseRecommendation(finalContent)
        if (recommendation != null) {
            _recommendation.value = recommendation
        }

        // Update the message content in the list
        _messages.update { currentList ->
            currentList.map { message ->
                if (message.id == messageId) {
                    // Create new ChatMessage object with final content
                    message.copy(parts = listOf(MessagePart.Text(finalContent)))
                } else {
                    message
                }
            }
        }

        // Persist final content to Room
        chatRepository.updateMessageContent(messageId, finalContent)
    }

    // 添加用户消息
    private fun addUserMessage(content: String) {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            val message = chatRepository.addMessage(sessionId, content, MessageRole.USER)
            _messages.update { currentList -> currentList + message }
        }
    }

    // 添加 AI 消息
    private fun addAiMessage(content: String) {
        val sessionId = _currentSessionId.value ?: return
        viewModelScope.launch {
            val message = chatRepository.addMessage(sessionId, content, MessageRole.ASSISTANT)
            _messages.update { currentList -> currentList + message }
        }
    }

    // 加载历史会话
    fun loadSession(session: ChatSession) {
        _currentSessionId.value = session.id
        _currentSessionTitle.value = session.title
        _recommendation.value = null  // Clear recommendation when loading history
        // 注意: session.messages 永远为空，消息需要从 Room 单独加载

        if (isDemoMode) {
            // 演示模式下从内存加载
            _messages.value = demoModeManager.getDemoMessagesForSession(session.id)
        } else {
            // Load messages from Room - use getMessagesForSessionOnce for one-shot load
            viewModelScope.launch {
                _messages.value = chatRepository.getMessagesForSessionOnce(session.id)
            }
        }
    }

    // 创建新会话
    fun createNewSession() {
        _currentSessionId.value = null
        _currentSessionTitle.value = null
        _messages.value = emptyList()
        _recommendation.value = null  // Clear recommendation on new session
        // Greeting will be added when user sends first message
    }

    // 关闭推荐卡片
    fun dismissRecommendation() {
        _recommendation.value = null
    }

    // 关闭危机干预卡片
    fun dismissCrisisIntervention() {
        _crisisIntervention.value = null
    }

}
