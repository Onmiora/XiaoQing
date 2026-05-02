package com.onmi.qing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
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
import com.onmi.qing.data.remote.SseEvent
import com.onmi.qing.data.remote.SseEventParser
import com.onmi.qing.data.repository.ChatRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import android.util.Log
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@HiltViewModel(assistedFactory = ChatViewModel.ChatViewModelFactory::class)
class ChatViewModel @AssistedInject constructor(
    private val dataStore: QingDataStore,
    private val chatRepository: ChatRepository,
    private val demoModeManager: DemoModeManager,
    private val usageStatsManager: com.onmi.qing.data.UsageStatsManager,
    private val apiServiceFactory: ApiServiceFactory,
    @Assisted private val sessionId: String?
) : ViewModel() {

    @AssistedFactory
    interface ChatViewModelFactory {
        fun create(sessionId: String?): ChatViewModel
    }

    private companion object {
        private const val ERROR_MESSAGE = "抱歉，服务出现了问题。请稍后再试。\n\n⚠️ 后端服务出现问题"
        private const val STREAMING_THROTTLE_MS = 50L
        private val GREETINGS = listOf(
            "你好呀！今天感觉怎么样？有什么想和我聊聊的吗？",
            "嗨，欢迎回来！今天过得如何？",
            "你好！看到你来我很开心。今天有什么心事想说吗？"
        )
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

    // Real-time thinking state
    private val _currentThinking = MutableStateFlow<String?>(null)
    val currentThinking: StateFlow<String?> = _currentThinking.asStateFlow()

    private var streamingMessageId: String? = null
    private val sseParser = SseEventParser()

    // Streaming delta buffer for throttled UI updates
    private val streamingDeltaChannel = Channel<String>(Channel.UNLIMITED)

    private val isDemoMode: Boolean
        get() = demoModeManager.isDemoMode.value

    init {
        // Load session if sessionId was provided via navigation
        if (sessionId != null && sessionId != "new") {
            loadSessionById(sessionId)
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isEmpty()) return
        _inputText.value = ""

        if (_currentSessionId.value == null) {
            viewModelScope.launch {
                val session = chatRepository.createSession(getSessionTitle())
                _currentSessionId.value = session.id
                _currentSessionTitle.value = session.title
                addAiMessage(GREETINGS.random())
                doSendMessage(text)
            }
            return
        }

        doSendMessage(text)
    }

    private fun getSessionTitle(): String {
        val sdf = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun buildMessageContext(currentText: String): List<AnthropicMessage> {
        val messageHistory = _messages.value
            .sortedBy { it.timestamp }
            .drop(1) // Skip greeting
            .map { msg ->
                AnthropicMessage(
                    role = if (msg.isFromUser) "user" else "assistant",
                    content = msg.textContent
                )
            }
        return messageHistory + AnthropicMessage(role = "user", content = currentText)
    }

    private fun doSendMessage(text: String) {
        val sid = _currentSessionId.value ?: return

        addUserMessage(text)

        if (!isDemoMode) {
            viewModelScope.launch { usageStatsManager.incrementChatCount() }
        }

        _isAiTyping.value = true
        _currentThinking.value = null

        viewModelScope.launch {
            try {
                val streamed = streamChat(text)
                if (!streamed) {
                    Log.w("ChatViewModel", "Streaming failed, falling back to non-streaming")
                    nonStreamingChat(text)
                }
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Streaming error: ${e.message}", e)
                val placeholderId = streamingMessageId
                if (placeholderId != null) {
                    _messages.update { list -> list.filter { it.id != placeholderId } }
                    chatRepository.deleteMessage(placeholderId, sid)
                }
                streamingMessageId = null
                _isAiTyping.value = false
                nonStreamingChat(text)
            }
        }
    }

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
            val chatApiService = apiServiceFactory.createChatApiService()
            val response = chatApiService.chat(request)
            _isAiTyping.value = false

            if (response.isSuccessful) {
                val responseBody = response.body()
                val textContent = responseBody?.content
                    ?.filter { it.type == "text" }
                    ?.mapNotNull { it.text }
                    ?.joinToString("")

                if (!textContent.isNullOrEmpty()) {
                    addAiMessage(textContent)
                    val recommendation = parseRecommendation(textContent)
                    _recommendation.value = recommendation
                } else {
                    val anyContent = responseBody?.content?.firstOrNull()?.text
                        ?: responseBody?.content?.firstOrNull()?.thinking
                    if (!anyContent.isNullOrEmpty()) {
                        addAiMessage(anyContent)
                    } else {
                        addAiMessage(ERROR_MESSAGE)
                    }
                }
            } else {
                addAiMessage(ERROR_MESSAGE)
            }
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Network error: ${e.message}", e)
            _isAiTyping.value = false
            addAiMessage(ERROR_MESSAGE)
        }
    }

    private suspend fun streamChat(text: String): Boolean {
        try {
            val prefs = dataStore.userPreferences.first()
            val allMessages = buildMessageContext(text)
            val request = AnthropicRequest(
                model = prefs.modelName,
                messages = allMessages,
                max_tokens = 4096,
                stream = true
            )

            val streamingApiService = apiServiceFactory.createStreamingChatApiService()
            val response = streamingApiService.chatStreaming(request)

            if (!response.isSuccessful) {
                Log.e("ChatViewModel", "Streaming request failed: ${response.code()}")
                return false
            }

            val responseBody = response.body() ?: return false

            // Create placeholder message
            val sid = _currentSessionId.value ?: return false
            val savedMessage = chatRepository.addMessage(sid, "", MessageRole.ASSISTANT)
            streamingMessageId = savedMessage.id
            _messages.update { currentList -> currentList + savedMessage }

            // Start throttled UI update collector on a background dispatcher
            // so it can run concurrently with the Main-dispatched event collection
            var accumulatedContentSnapshot = ""
            val throttleJob = viewModelScope.launch(Dispatchers.Default) {
                var lastContent = ""
                while (isActive) {
                    delay(STREAMING_THROTTLE_MS)
                    val current = accumulatedContentSnapshot
                    if (current != lastContent) {
                        lastContent = current
                        updateStreamingMessageDirect(current)
                    }
                }
            }

            val thinkingContent = StringBuilder()
            var thinkingStartTime: Long? = null
            var isComplete = false
            var hasRecommendation = false

            sseParser.parseEvents(responseBody).collect { event ->
                when (event) {
                    is SseEvent.ContentBlockDelta -> {
                        accumulatedContentSnapshot += event.text
                    }
                    is SseEvent.ThinkingDelta -> {
                        if (thinkingStartTime == null) thinkingStartTime = System.currentTimeMillis()
                        thinkingContent.append(event.thinking)
                        _currentThinking.value = thinkingContent.toString()
                    }
                    is SseEvent.MessageStop -> {
                        isComplete = true
                    }
                    is SseEvent.ToolUse -> {
                        if (event.name == "recommend_feature") {
                            val recommendation = parseRecommendation(event.input)
                            if (recommendation != null) {
                                _recommendation.value = recommendation
                                hasRecommendation = true
                            }
                        } else if (event.name == "crisis_intervention") {
                            val crisis = parseCrisisIntervention(event.input)
                            if (crisis != null) {
                                _crisisIntervention.value = crisis
                            }
                        }
                    }
                    is SseEvent.Error -> {
                        Log.e("ChatViewModel", "SSE Error: ${event.error}")
                    }
                    else -> {}
                }
            }

            throttleJob.cancel()

            // Final update
            _isAiTyping.value = false
            _currentThinking.value = null

            val finalContent = when {
                accumulatedContentSnapshot.isNotEmpty() && isComplete -> accumulatedContentSnapshot
                thinkingContent.isNotEmpty() -> thinkingContent.toString()
                hasRecommendation && isComplete -> "我为你推荐了这个功能，快去试试吧！"
                else -> ERROR_MESSAGE
            }

            // Build final parts with thinking
            val finalParts = mutableListOf<MessagePart>()
            if (thinkingContent.isNotBlank()) {
                val duration = thinkingStartTime?.let { System.currentTimeMillis() - it }
                finalParts.add(MessagePart.Thinking(thinkingContent.toString(), duration))
            }
            finalParts.add(MessagePart.Text(finalContent))

            val messageId = streamingMessageId
            streamingMessageId = null

            if (messageId != null) {
                finalizeStreamingMessage(messageId, sid, finalParts, finalContent)
            }

            return true
        } catch (e: Exception) {
            Log.e("ChatViewModel", "Stream error: ${e.message}", e)
            _isAiTyping.value = false
            streamingMessageId = null
            throw e
        }
    }

    private fun updateStreamingMessageDirect(content: String) {
        val messageId = streamingMessageId ?: return
        _messages.update { currentList ->
            currentList.map { message ->
                if (message.id == messageId) {
                    message.copy(parts = listOf(MessagePart.Text(content)))
                } else {
                    message
                }
            }
        }
    }

    private suspend fun finalizeStreamingMessage(
        messageId: String,
        sessionId: String,
        finalParts: List<MessagePart>,
        finalContent: String
    ) {
        val recommendation = parseRecommendation(finalContent)
        if (recommendation != null) {
            _recommendation.value = recommendation
        }

        _messages.update { currentList ->
            currentList.map { message ->
                if (message.id == messageId) {
                    message.copy(parts = finalParts)
                } else {
                    message
                }
            }
        }

        chatRepository.updateMessageParts(messageId, finalParts)
    }

    // --- Message Actions ---

    fun regenerateLastMessage() {
        viewModelScope.launch {
            val msgs = _messages.value
            val lastAiIndex = msgs.indexOfLast { it.role == MessageRole.ASSISTANT }
            if (lastAiIndex < 0) return@launch

            val lastAi = msgs[lastAiIndex]
            val userMsgBefore = msgs.take(lastAiIndex).lastOrNull { it.role == MessageRole.USER }
                ?: return@launch

            val sid = _currentSessionId.value ?: return@launch

            // Remove old AI message
            _messages.update { it.filterIndexed { i, _ -> i != lastAiIndex } }
            chatRepository.deleteMessage(lastAi.id, sid)

            _isAiTyping.value = true
            _currentThinking.value = null
            try {
                val streamed = streamChat(userMsgBefore.textContent)
                if (!streamed) nonStreamingChat(userMsgBefore.textContent)
            } catch (e: Exception) {
                Log.e("ChatViewModel", "Regenerate error: ${e.message}", e)
                _isAiTyping.value = false
                nonStreamingChat(userMsgBefore.textContent)
            }
        }
    }

    fun editMessage(messageId: String, newText: String) {
        viewModelScope.launch {
            val sid = _currentSessionId.value ?: return@launch

            // Delete this message and all after it
            chatRepository.deleteMessageAndAfter(messageId, sid)

            // Update UI
            val msgs = _messages.value
            val targetIndex = msgs.indexOfFirst { it.id == messageId }
            if (targetIndex >= 0) {
                _messages.update { it.take(targetIndex) }
            }

            // Re-send with edited text
            doSendMessage(newText)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            val sid = _currentSessionId.value ?: return@launch
            chatRepository.deleteMessage(messageId, sid)
            _messages.update { it.filter { msg -> msg.id != messageId } }
        }
    }

    // --- Session Management ---

    private fun loadSessionById(id: String) {
        _currentSessionId.value = id
        if (isDemoMode) {
            _messages.value = demoModeManager.getDemoMessagesForSession(id)
        } else {
            viewModelScope.launch {
                _messages.value = chatRepository.getMessagesForSessionOnce(id)
            }
        }
    }

    fun loadSession(session: ChatSession) {
        _currentSessionId.value = session.id
        _currentSessionTitle.value = session.title
        _recommendation.value = null
        if (isDemoMode) {
            _messages.value = demoModeManager.getDemoMessagesForSession(session.id)
        } else {
            viewModelScope.launch {
                _messages.value = chatRepository.getMessagesForSessionOnce(session.id)
            }
        }
    }

    fun createNewSession() {
        _currentSessionId.value = null
        _currentSessionTitle.value = null
        _messages.value = emptyList()
        _recommendation.value = null
        _crisisIntervention.value = null
        _inputText.value = ""
        _currentThinking.value = null
    }

    fun dismissRecommendation() {
        _recommendation.value = null
    }

    fun dismissCrisisIntervention() {
        _crisisIntervention.value = null
    }

    private fun addUserMessage(content: String) {
        val sid = _currentSessionId.value ?: return
        viewModelScope.launch {
            val message = chatRepository.addMessage(sid, content, MessageRole.USER)
            _messages.update { currentList -> currentList + message }
        }
    }

    private fun addAiMessage(content: String) {
        val sid = _currentSessionId.value ?: return
        viewModelScope.launch {
            val message = chatRepository.addMessage(sid, content, MessageRole.ASSISTANT)
            _messages.update { currentList -> currentList + message }
        }
    }
}
