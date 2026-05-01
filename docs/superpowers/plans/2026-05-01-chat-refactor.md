# Chat Refactor Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Refactor the chat system with a polymorphic message model, fix SSE streaming, add message actions (regenerate/edit/delete), and redesign the UI in RikkaHub style while preserving MD3 dynamic color and tool call cards.

**Architecture:** MVVM with new `ChatMessage` + `MessagePart` sealed class replacing the flat `Message` model. Flat message list with `regenerationIndex` for regeneration support. Dual OkHttpClient for streaming fix. Third-party Markdown library.

**Tech Stack:** Kotlin, Jetpack Compose, Room, Hilt, Retrofit + OkHttp, Material 3

**Design Spec:** `docs/superpowers/specs/2026-05-01-chat-refactor-design.md`

---

## File Structure

### New Files
| File | Responsibility |
|------|---------------|
| `data/ChatMessage.kt` | `ChatMessage`, `MessageRole`, `MessagePart` domain models |
| `ui/components/ToolCallCard.kt` | Recommendation + CrisisIntervention cards with entrance animation |
| `ui/components/ChatMessageItem.kt` | Single message composable with ThinkingBlock + InlineActionBar |
| `ui/components/ThinkingBlock.kt` | Collapsible thinking/reasoning display |
| `ui/components/InlineActionBar.kt` | Copy/regenerate/edit/delete action buttons |

### Modified Files
| File | Changes |
|------|---------|
| `data/local/entity/MessageEntity.kt` | `role` + `parts` JSON + `regenerationIndex`, remove `isFromUser` |
| `data/local/dao/ChatDao.kt` | Adjust queries for new schema |
| `data/local/AppDatabase.kt` | Version bump, rebuild |
| `data/local/Mappers.kt` | New entity <-> domain mapping |
| `data/repository/ChatRepository.kt` | Adapt to new model, add regenerate/delete-after |
| `data/remote/SseEventParser.kt` | Add `flowOn(Dispatchers.IO)` |
| `di/NetworkModule.kt` | Dual OkHttpClient |
| `data/remote/ApiServiceFactory.kt` | Accept client parameter |
| `viewmodel/ChatViewModel.kt` | Full refactor: new model + streaming fix + actions |
| `ui/navigation/Screen.kt` | Chat route adds `sessionId` parameter |
| `ui/navigation/Navigation.kt` | Pass `sessionId` to ChatViewModel |
| `ui/screens/chat/ChatScreen.kt` | Full rewrite: new layout + actions |
| `ui/components/MarkdownText.kt` | Replace with third-party library wrapper |
| `data/demo/DemoModeManager.kt` | Adapt to `ChatMessage` model |
| `gradle/libs.versions.toml` | Add Markdown library |
| `app/build.gradle.kts` | Add Markdown library dependency |

---

## Task 1: Data Models

**Files:**
- Create: `app/src/main/java/com/onmi/qing/data/ChatMessage.kt`
- Delete: `app/src/main/java/com/onmi/qing/data/Message.kt` (contents moved)

- [ ] **Step 1: Create `ChatMessage.kt` with domain models**

```kotlin
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
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL (existing `Message.kt` still exists, no references broken yet)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/onmi/qing/data/ChatMessage.kt
git commit -m "feat: add ChatMessage and MessagePart domain models"
```

---

## Task 2: Room Schema Refactor

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/data/local/entity/MessageEntity.kt`
- Modify: `app/src/main/java/com/onmi/qing/data/local/AppDatabase.kt`
- Modify: `app/src/main/java/com/onmi/qing/data/local/dao/ChatDao.kt`
- Modify: `app/src/main/java/com/onmi/qing/data/local/Mappers.kt`
- Modify: `app/src/main/java/com/onmi/qing/data/remote/ChatApiService.kt` (AnthropicMessage content type)

- [ ] **Step 1: Rewrite `MessageEntity.kt`**

Replace the entire file:

```kotlin
package com.onmi.qing.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [
        ForeignKey(
            entity = ChatSessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionId"])]
)
data class MessageEntity(
    @PrimaryKey
    val id: String,
    val sessionId: String,
    val role: String,           // "USER", "ASSISTANT", "SYSTEM"
    val partsJson: String,      // JSON-serialized List<MessagePart>
    val timestamp: Long,
    val regenerationIndex: Int = 0
)
```

- [ ] **Step 2: Update `AppDatabase.kt` version**

In `AppDatabase.kt`, change:
```kotlin
@Database(
    entities = [ChatSessionEntity::class, MessageEntity::class, MoodEntryEntity::class, AchievementEntity::class],
    version = 2,  // was 1
    exportSchema = false
)
```

Add `fallbackToDestructiveMigration()` to the builder (since we're rebuilding, not migrating):
```kotlin
Room.databaseBuilder(context, AppDatabase::class.java, "qing_database")
    .fallbackToDestructiveMigration()
    .build()
```

- [ ] **Step 3: Update `ChatDao.kt` queries**

Replace `MessageEntity` field references. The key change is `isFromUser` no longer exists — queries that filter by user/AI need to use `role`. Read the current DAO and update:

```kotlin
// In ChatDao.kt, update any query that references isFromUser or content
// For example, if there's a query like:
//   @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY timestamp ASC")
// This stays the same since it doesn't reference isFromUser.
// But any query referencing isFromUser or content column needs updating to use role and partsJson.
```

Review all queries in `ChatDao.kt` and update column references. The `addMessage` insert method signature changes — remove `content` and `isFromUser` params, add `role` and `partsJson`.

- [ ] **Step 4: Rewrite `Mappers.kt` message mappers**

Add Gson imports and new mappers. Keep existing session/mood/achievement mappers unchanged. Add:

```kotlin
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.onmi.qing.data.ChatMessage
import com.onmi.qing.data.MessagePart
import com.onmi.qing.data.MessageRole

private val gson = Gson()
private val messagePartListType = object : TypeToken<List<MessagePart>>() {}.type

fun ChatMessage.toEntity(sessionId: String): MessageEntity = MessageEntity(
    id = id,
    sessionId = sessionId,
    role = role.name,
    partsJson = gson.toJson(parts),
    timestamp = timestamp,
    regenerationIndex = regenerationIndex
)

fun MessageEntity.toDomain(): ChatMessage = ChatMessage(
    id = id,
    role = MessageRole.valueOf(role),
    parts = gson.fromJson(partsJson, messagePartListType),
    timestamp = timestamp,
    regenerationIndex = regenerationIndex
)
```

Remove the old `Message.toEntity()` and `MessageEntity.toDomain()` that used `isFromUser` and `content`.

- [ ] **Step 5: Verify build compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL (ViewModel and UI still reference old Message — that's OK for now)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/onmi/qing/data/local/entity/MessageEntity.kt \
      app/src/main/java/com/onmi/qing/data/local/AppDatabase.kt \
      app/src/main/java/com/onmi/qing/data/local/dao/ChatDao.kt \
      app/src/main/java/com/onmi/qing/data/local/Mappers.kt
git commit -m "refactor: Room schema for ChatMessage with role + parts JSON"
```

---

## Task 3: Repository + DemoModeManager

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/data/repository/ChatRepository.kt`
- Modify: `app/src/main/java/com/onmi/qing/data/demo/DemoModeManager.kt`

- [ ] **Step 1: Rewrite `ChatRepository.kt`**

Replace the entire file. Key changes:
- All methods use `ChatMessage` instead of `Message`
- `addMessage` takes `role` + `parts` instead of `isFromUser` + `content`
- New: `deleteMessageAndAfter(messageId, sessionId)` for edit functionality
- New: `updateMessageParts(messageId, parts)` for streaming finalization
- Keep `getAllSessions`, `createSession`, `deleteSession`, `deleteAllData`, `incrementSessionAnalysisCount` as-is

```kotlin
package com.onmi.qing.data.repository

import com.onmi.qing.data.ChatMessage
import com.onmi.qing.data.MessagePart
import com.onmi.qing.data.MessageRole
import com.onmi.qing.data.ChatSession
import com.onmi.qing.data.local.dao.ChatDao
import com.onmi.qing.data.local.toDomain
import com.onmi.qing.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val chatDao: ChatDao
) {
    fun getAllSessions(): Flow<List<ChatSession>> =
        chatDao.getAllSessions().map { entities ->
            entities.map { it.toDomain() }
        }

    fun getMessagesForSession(sessionId: String): Flow<List<ChatMessage>> =
        chatDao.getMessagesForSession(sessionId).map { entities ->
            entities.map { it.toDomain() }
        }

    suspend fun getMessagesForSessionOnce(sessionId: String): List<ChatMessage> =
        chatDao.getMessagesForSessionOnce(sessionId).map { it.toDomain() }

    suspend fun createSession(title: String): ChatSession {
        val session = ChatSession(
            id = UUID.randomUUID().toString(),
            title = title,
            lastMessage = "",
            timestamp = System.currentTimeMillis(),
            messageCount = 0
        )
        chatDao.insertSession(session.toEntity())
        return session
    }

    suspend fun addMessage(sessionId: String, message: ChatMessage) {
        chatDao.insertMessage(message.toEntity(sessionId))
        chatDao.updateSessionLastMessage(
            sessionId = sessionId,
            lastMessage = message.textContent.take(100),
            timestamp = message.timestamp,
            messageCount = chatDao.getMessageCount(sessionId)
        )
    }

    suspend fun updateMessageParts(messageId: String, parts: List<MessagePart>) {
        val gson = com.google.gson.Gson()
        chatDao.updateMessageParts(messageId, gson.toJson(parts))
    }

    suspend fun deleteMessage(messageId: String, sessionId: String) {
        chatDao.deleteMessage(messageId)
        chatDao.updateSessionMessageCount(sessionId, chatDao.getMessageCount(sessionId))
    }

    suspend fun deleteMessageAndAfter(messageId: String, sessionId: String) {
        val messages = chatDao.getMessagesForSessionOnce(sessionId)
        val targetIndex = messages.indexOfFirst { it.id == messageId }
        if (targetIndex >= 0) {
            val idsToDelete = messages.drop(targetIndex).map { it.id }
            idsToDelete.forEach { chatDao.deleteMessage(it) }
            chatDao.updateSessionMessageCount(sessionId, chatDao.getMessageCount(sessionId))
        }
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteMessagesForSession(sessionId)
        chatDao.deleteSession(sessionId)
    }

    suspend fun incrementSessionAnalysisCount(sessionId: String): Int {
        val current = chatDao.getAnalysisCount(sessionId) ?: 0
        val newCount = current + 1
        chatDao.updateAnalysisCount(sessionId, newCount)
        return newCount
    }

    suspend fun deleteAllData() {
        chatDao.deleteAllMessages()
        chatDao.deleteAllSessions()
    }
}
```

- [ ] **Step 2: Update `DemoModeManager.kt`**

Replace `Message` imports with `ChatMessage`. Update `demoMessages` from `MutableStateFlow<Map<String, List<Message>>>` to `MutableStateFlow<Map<String, List<ChatMessage>>>`. Convert all demo message creation to use `ChatMessage`:

```kotlin
// Change all Message(...) constructions to:
ChatMessage(
    id = UUID.randomUUID().toString(),
    role = if (isFromUser) MessageRole.USER else MessageRole.ASSISTANT,
    parts = listOf(MessagePart.Text(content)),
    timestamp = timestamp
)
```

- [ ] **Step 3: Delete old `Message.kt`**

Remove `app/src/main/java/com/onmi/qing/data/Message.kt` — its contents are now in `ChatMessage.kt` and the `ChatSession` data class should be moved there too (or kept in a separate file).

Before deleting, ensure `ChatSession` is preserved. Move it to `ChatMessage.kt` or a new `ChatSession.kt`:

```kotlin
// Add to ChatMessage.kt or create ChatSession.kt:
data class ChatSession(
    val id: String,
    val title: String,
    val lastMessage: String,
    val timestamp: Long,
    val messageCount: Int,
    val analysisCount: Int = 0,
    val messages: List<ChatMessage> = emptyList()
) {
    companion object {
        const val MAX_ANALYSIS_COUNT = 1
    }

    fun getFormattedTime(): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp
        val calendar = java.util.Calendar.getInstance().apply { timeInMillis = timestamp }
        val today = java.util.Calendar.getInstance()

        return when {
            diff < 60_000 -> "刚刚"
            diff < 3600_000 -> "${diff / 60_000}分钟前"
            calendar.get(java.util.Calendar.YEAR) == today.get(java.util.Calendar.YEAR) &&
                calendar.get(java.util.Calendar.DAY_OF_YEAR) == today.get(java.util.Calendar.DAY_OF_YEAR) ->
                "今天 ${String.format("%02d:%02d", calendar.get(java.util.Calendar.HOUR_OF_DAY), calendar.get(java.util.Calendar.MINUTE))}"
            else -> "${calendar.get(java.util.Calendar.MONTH) + 1}/${calendar.get(java.util.Calendar.DAY_OF_MONTH)}"
        }
    }
}
```

- [ ] **Step 4: Verify build compiles**

Run: `./gradlew compileDebugKotlin`
Expected: Errors in ViewModel and UI (expected — they still reference old `Message`)

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/onmi/qing/data/repository/ChatRepository.kt \
      app/src/main/java/com/onmi/qing/data/demo/DemoModeManager.kt \
      app/src/main/java/com/onmi/qing/data/ChatMessage.kt \
      app/src/main/java/com/onmi/qing/data/Message.kt
git commit -m "refactor: migrate Repository and DemoModeManager to ChatMessage model"
```

---

## Task 4: Streaming Infrastructure Fix

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/di/NetworkModule.kt`
- Modify: `app/src/main/java/com/onmi/qing/data/remote/ApiServiceFactory.kt`
- Modify: `app/src/main/java/com/onmi/qing/data/remote/SseEventParser.kt`

- [ ] **Step 1: Update `NetworkModule.kt` — dual OkHttpClient**

Replace the entire `NetworkModule.kt`:

```kotlin
package com.onmi.qing.di

import com.onmi.qing.BuildConfig
import com.onmi.qing.data.remote.ApiServiceFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY
                    else HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideStreamingOkHttpClient(): OkHttpClient {
        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.HEADERS
                    else HttpLoggingInterceptor.Level.NONE
        }

        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("anthropic-version", "2023-06-01")
                    .addHeader("Content-Type", "application/json")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(loggingInterceptor)
            .build()
    }

    @Provides
    @Singleton
    fun provideApiServiceFactory(
        okHttpClient: OkHttpClient,
        streamingOkHttpClient: OkHttpClient
    ): ApiServiceFactory {
        return ApiServiceFactory(okHttpClient, streamingOkHttpClient)
    }
}
```

- [ ] **Step 2: Update `ApiServiceFactory.kt` to accept dual clients**

Replace the entire file:

```kotlin
package com.onmi.qing.data.remote

import com.onmi.qing.data.datastore.QingDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ApiServiceFactory @Inject constructor(
    private val regularClient: OkHttpClient,
    private val streamingClient: OkHttpClient
) {
    private var currentBaseUrl: String? = null
    private var regularRetrofit: Retrofit? = null
    private var streamingRetrofit: Retrofit? = null

    private fun getBaseUrl(dataStore: QingDataStore): String {
        return runBlocking {
            val prefs = dataStore.userPreferences.first()
            val url = prefs.apiUrl.ifBlank { "https://api.xiaoqing.com" }
            if (url.endsWith("/")) url else "$url/"
        }
    }

    fun createChatApiService(dataStore: QingDataStore): ChatApiService {
        val baseUrl = getBaseUrl(dataStore)
        if (regularRetrofit == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl
            regularRetrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(regularClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return regularRetrofit!!.create(ChatApiService::class.java)
    }

    fun createStreamingChatApiService(dataStore: QingDataStore): ChatApiService {
        val baseUrl = getBaseUrl(dataStore)
        if (streamingRetrofit == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl
            streamingRetrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(streamingClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return streamingRetrofit!!.create(ChatApiService::class.java)
    }

    fun createAnalyzeApiService(dataStore: QingDataStore): AnalyzeApiService {
        val baseUrl = getBaseUrl(dataStore)
        if (regularRetrofit == null || currentBaseUrl != baseUrl) {
            currentBaseUrl = baseUrl
            regularRetrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(regularClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        }
        return regularRetrofit!!.create(AnalyzeApiService::class.java)
    }
}
```

- [ ] **Step 3: Update `SseEventParser.kt` — add `flowOn`**

In `SseEventParser.kt`, add `flowOn(Dispatchers.IO)` to the `parseEvents` flow. At the end of the `flow { ... }` block, chain `.flowOn(Dispatchers.IO)`:

```kotlin
fun parseEvents(body: ResponseBody): Flow<SseEvent> = flow {
    // ... existing implementation stays the same ...
}.flowOn(Dispatchers.IO)
```

Add the import:
```kotlin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
```

- [ ] **Step 4: Verify build compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/onmi/qing/di/NetworkModule.kt \
      app/src/main/java/com/onmi/qing/data/remote/ApiServiceFactory.kt \
      app/src/main/java/com/onmi/qing/data/remote/SseEventParser.kt
git commit -m "fix: SSE streaming — dual OkHttpClient + flowOn(IO) for parser"
```

---

## Task 5: Navigation — Session ID Parameter

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/ui/navigation/Screen.kt`
- Modify: `app/src/main/java/com/onmi/qing/ui/navigation/Navigation.kt`

- [ ] **Step 1: Update `Screen.kt` — Chat route with sessionId**

Change the `Chat` object from a simple route to one with a parameter:

```kotlin
// In Screen.kt, change:
//   data object Chat : Screen("chat", "小晴")
// To:
data object Chat : Screen("chat/{sessionId}", "小晴") {
    fun createRoute(sessionId: String? = null): String {
        return if (sessionId != null) "chat/$sessionId" else "chat/new"
    }
}
```

- [ ] **Step 2: Update `Navigation.kt` — pass sessionId**

In `Navigation.kt`, update the Chat composable destination:

```kotlin
// Change the composable registration for Chat to include navArgument:
composable(
    route = Screen.Chat.route,
    arguments = listOf(
        navArgument("sessionId") {
            type = NavType.StringType
            defaultValue = "new"
        }
    )
) { backStackEntry ->
    val sessionId = backStackEntry.arguments?.getString("sessionId")
    val chatViewModel: ChatViewModel = hiltViewModel(
        creationExtras = defaultViewModelCreationExtras().apply {
            set(ChatViewModel.SESSION_ID_KEY, sessionId)
        }
    )
    ChatScreen(
        viewModel = chatViewModel,
        demoModeManager = demoModeManager,
        onHistoryClick = { navController.navigate(Screen.History.route) },
        onNewChatClick = { chatViewModel.createNewSession() },
        onBackClick = null,
        onBreathingClick = { navController.navigate(Screen.Breathing.route) },
        onFunTestClick = { navController.navigate(Screen.StressDetection.route) }
    )
}
```

Update the History screen's `onSessionClick`:
```kotlin
// Change:
//   onSessionClick = { navController.navigate(Screen.Chat.route) }
// To:
onSessionClick = { session -> navController.navigate(Screen.Chat.createRoute(session.id)) }
```

Update all other navigation to Chat:
```kotlin
// Change all instances of:
//   navController.navigate(Screen.Chat.route)
// To:
//   navController.navigate(Screen.Chat.createRoute())
```

- [ ] **Step 3: Verify build compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL (ChatViewModel Factory change is in Task 6)

- [ ] **Step 4: Commit**

```bash
git add app/src/main/java/com/onmi/qing/ui/navigation/Screen.kt \
      app/src/main/java/com/onmi/qing/ui/navigation/Navigation.kt
git commit -m "feat: add sessionId nav parameter for chat history navigation"
```

---

## Task 6: ChatViewModel Refactor

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/viewmodel/ChatViewModel.kt`

- [ ] **Step 1: Rewrite `ChatViewModel.kt`**

This is the largest change. Replace the entire file. Key changes:
- Use `ChatMessage` + `MessagePart` instead of `Message`
- Use `streamingChatApiService` (from streaming OkHttpClient) for streaming calls
- Use `regularChatApiService` for non-streaming fallback
- Add throttled streaming UI updates (buffer + 50ms throttle)
- Add `regenerateLastMessage()`, `editMessage(messageId, newText)`, `deleteMessage(messageId)`
- Accept `sessionId` in Factory, load session on init
- Real-time `_currentThinking` state for ThinkingBlock

```kotlin
package com.onmi.qing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onmi.qing.data.*
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.data.remote.*
import com.onmi.qing.data.repository.ChatRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

@HiltViewModel(assistedFactory = ChatViewModel.ChatViewModelFactory::class)
class ChatViewModel @AssistedInject constructor(
    private val dataStore: QingDataStore,
    private val chatRepository: ChatRepository,
    private val demoModeManager: DemoModeManager,
    private val apiServiceFactory: ApiServiceFactory,
    @Assisted private val sessionId: String?
) : ViewModel() {

    @dagger.assisted.AssistedFactory
    interface ChatViewModelFactory {
        fun create(sessionId: String?): ChatViewModel
    }

    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _inputText = MutableStateFlow("")
    val inputText: StateFlow<String> = _inputText.asStateFlow()

    private val _isAiTyping = MutableStateFlow(false)
    val isAiTyping: StateFlow<Boolean> = _isAiTyping.asStateFlow()

    private val _recommendation = MutableStateFlow<Recommendation?>(null)
    val recommendation: StateFlow<Recommendation?> = _recommendation.asStateFlow()

    private val _crisisIntervention = MutableStateFlow<CrisisIntervention?>(null)
    val crisisIntervention: StateFlow<CrisisIntervention?> = _crisisIntervention.asStateFlow()

    private val _currentSessionId = MutableStateFlow<String?>(null)
    private val _currentThinking = MutableStateFlow<String?>(null)
    val currentThinking: StateFlow<String?> = _currentThinking.asStateFlow()

    private var streamingMessageId: String? = null
    private val streamingBuffer = Channel<String>(Channel.UNLIMITED)

    // Streaming throttle: buffer deltas, emit UI updates every 50ms
    private val _streamingContent = MutableStateFlow("")
    val streamingContent: StateFlow<String> = _streamingContent.asStateFlow()

    init {
        if (sessionId != null && sessionId != "new") {
            loadSession(sessionId)
        }
    }

    fun updateInputText(text: String) {
        _inputText.value = text
    }

    fun sendMessage() {
        val text = _inputText.value.trim()
        if (text.isBlank()) return
        _inputText.value = ""

        viewModelScope.launch {
            if (_currentSessionId.value == null) {
                val session = chatRepository.createSession(text.take(20))
                _currentSessionId.value = session.id
                // Add greeting
                val greeting = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.ASSISTANT,
                    parts = listOf(MessagePart.Text(getRandomGreeting())),
                    timestamp = System.currentTimeMillis()
                )
                chatRepository.addMessage(session.id, greeting)
                _messages.update { it + greeting }
            }
            doSendMessage(text)
        }
    }

    private suspend fun doSendMessage(text: String) {
        val sessionId = _currentSessionId.value ?: return

        // Add user message
        val userMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.USER,
            parts = listOf(MessagePart.Text(text)),
            timestamp = System.currentTimeMillis()
        )
        chatRepository.addMessage(sessionId, userMessage)
        _messages.update { it + userMessage }

        // Send to API
        _isAiTyping.value = true
        _currentThinking.value = null
        try {
            streamChat(text)
        } catch (e: Exception) {
            try {
                nonStreamingChat(text)
            } catch (e2: Exception) {
                val errorMessage = ChatMessage(
                    id = UUID.randomUUID().toString(),
                    role = MessageRole.ASSISTANT,
                    parts = listOf(MessagePart.Text("抱歉，发生了错误：${e2.message}")),
                    timestamp = System.currentTimeMillis()
                )
                chatRepository.addMessage(sessionId, errorMessage)
                _messages.update { it + errorMessage }
            }
        } finally {
            _isAiTyping.value = false
            _currentThinking.value = null
        }
    }

    private suspend fun streamChat(text: String) {
        val sessionId = _currentSessionId.value ?: return
        val chatApiService = apiServiceFactory.createStreamingChatApiService(dataStore)

        val request = buildAnthropicRequest(text, stream = true)
        val response = chatApiService.chatStreaming(request)

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}")
        }

        val responseBody = response.body() ?: throw Exception("Empty response body")

        // Create placeholder message
        val placeholderId = UUID.randomUUID().toString()
        streamingMessageId = placeholderId
        val placeholder = ChatMessage(
            id = placeholderId,
            role = MessageRole.ASSISTANT,
            parts = listOf(MessagePart.Text("")),
            timestamp = System.currentTimeMillis()
        )
        chatRepository.addMessage(sessionId, placeholder)
        _messages.update { it + placeholder }

        var accumulatedContent = ""
        var thinkingContent = ""
        var thinkingStartTime: Long? = null

        // Start throttle collector
        viewModelScope.launch {
            var lastContent = ""
            while (_isAiTyping.value) {
                kotlinx.coroutines.delay(50)
                val current = accumulatedContent
                if (current != lastContent) {
                    lastContent = current
                    updateStreamingMessage(placeholderId, current)
                }
            }
        }

        SseEventParser.parseEvents(responseBody).collect { event ->
            when (event) {
                is SseEvent.ContentBlockDelta -> {
                    accumulatedContent += event.text
                }
                is SseEvent.ThinkingDelta -> {
                    if (thinkingStartTime == null) thinkingStartTime = System.currentTimeMillis()
                    thinkingContent += event.thinking
                    _currentThinking.value = thinkingContent
                }
                is SseEvent.MessageStop -> {
                    // Finalize
                }
                is SseEvent.ToolUse -> {
                    handleToolUse(event)
                }
                is SseEvent.Error -> {
                    throw Exception(event.error)
                }
                else -> {}
            }
        }

        // Final update
        val finalParts = mutableListOf<MessagePart>()
        if (thinkingContent.isNotBlank()) {
            val duration = thinkingStartTime?.let { System.currentTimeMillis() - it }
            finalParts.add(MessagePart.Thinking(thinkingContent, duration))
        }
        finalParts.add(MessagePart.Text(accumulatedContent))

        val finalMessage = placeholder.copy(parts = finalParts)
        chatRepository.updateMessageParts(placeholderId, finalParts)
        _messages.update { list ->
            list.map { if (it.id == placeholderId) finalMessage else it }
        }
        streamingMessageId = null
    }

    private fun updateStreamingMessage(messageId: String, content: String) {
        _messages.update { list ->
            list.map { msg ->
                if (msg.id == messageId) {
                    msg.copy(parts = listOf(MessagePart.Text(content)))
                } else msg
            }
        }
    }

    private suspend fun nonStreamingChat(text: String) {
        val sessionId = _currentSessionId.value ?: return
        val chatApiService = apiServiceFactory.createChatApiService(dataStore)

        val request = buildAnthropicRequest(text, stream = false)
        val response = chatApiService.chat(request)

        if (!response.isSuccessful) {
            throw Exception("HTTP ${response.code()}: ${response.errorBody()?.string()}")
        }

        val body = response.body() ?: throw Exception("Empty response")
        val content = body.content?.firstOrNull { it.type == "text" }?.text ?: "无响应内容"

        val aiMessage = ChatMessage(
            id = UUID.randomUUID().toString(),
            role = MessageRole.ASSISTANT,
            parts = listOf(MessagePart.Text(content)),
            timestamp = System.currentTimeMillis()
        )
        chatRepository.addMessage(sessionId, aiMessage)
        _messages.update { it + aiMessage }
    }

    private fun handleToolUse(event: SseEvent.ToolUse) {
        when (event.name) {
            "recommend_feature" -> {
                _recommendation.value = parseRecommendationFromToolUse(event.input)
            }
            "crisis_intervention" -> {
                _crisisIntervention.value = parseCrisisInterventionFromToolUse(event.input)
            }
        }
    }

    fun regenerateLastMessage() {
        viewModelScope.launch {
            val msgs = _messages.value
            val lastAiIndex = msgs.indexOfLast { it.role == MessageRole.ASSISTANT }
            if (lastAiIndex < 0) return@launch

            val lastAi = msgs[lastAiIndex]
            val newRegenIndex = lastAi.regenerationIndex + 1

            // Find the user message before it
            val userMsgBefore = msgs.take(lastAiIndex).lastOrNull { it.role == MessageRole.USER }
            if (userMsgBefore == null) return@launch

            // Remove the old AI message from UI
            _messages.update { it.filterIndexed { i, _ -> i != lastAiIndex } }

            // Delete from DB
            val sessionId = _currentSessionId.value ?: return@launch
            chatRepository.deleteMessage(lastAi.id, sessionId)

            // Re-send
            _isAiTyping.value = true
            try {
                streamChat(userMsgBefore.textContent)
            } catch (e: Exception) {
                // Fallback
                try { nonStreamingChat(userMsgBefore.textContent) } catch (_: Exception) {}
            } finally {
                _isAiTyping.value = false
            }
        }
    }

    fun editMessage(messageId: String, newText: String) {
        viewModelScope.launch {
            val sessionId = _currentSessionId.value ?: return@launch
            val msgs = _messages.value
            val targetIndex = msgs.indexOfFirst { it.id == messageId }
            if (targetIndex < 0) return@launch

            // Delete this message and all after it from DB
            chatRepository.deleteMessageAndAfter(messageId, sessionId)

            // Update UI
            _messages.update { it.take(targetIndex) }

            // Re-send with edited text
            doSendMessage(newText)
        }
    }

    fun deleteMessage(messageId: String) {
        viewModelScope.launch {
            val sessionId = _currentSessionId.value ?: return@launch
            chatRepository.deleteMessage(messageId, sessionId)
            _messages.update { it.filter { msg -> msg.id != messageId } }
        }
    }

    fun loadSession(sessionId: String) {
        viewModelScope.launch {
            _currentSessionId.value = sessionId
            val msgs = chatRepository.getMessagesForSessionOnce(sessionId)
            _messages.value = msgs
        }
    }

    fun createNewSession() {
        _currentSessionId.value = null
        _messages.value = emptyList()
        _recommendation.value = null
        _crisisIntervention.value = null
        _inputText.value = ""
        _currentThinking.value = null
    }

    fun clearRecommendation() { _recommendation.value = null }
    fun clearCrisisIntervention() { _crisisIntervention.value = null }

    private fun buildAnthropicRequest(text: String, stream: Boolean): AnthropicRequest {
        val contextMessages = _messages.value
            .filter { it.role == MessageRole.USER || it.role == MessageRole.ASSISTANT }
            .dropWhile { it.role == MessageRole.ASSISTANT } // drop greeting
            .map { msg ->
                AnthropicMessage(
                    role = if (msg.role == MessageRole.USER) "user" else "assistant",
                    content = msg.textContent
                )
            }

        return AnthropicRequest(
            model = "minimax-m2.7",
            messages = contextMessages,
            max_tokens = 4096,
            stream = stream,
            temperature = 1.0
        )
    }

    private fun getRandomGreeting(): String {
        val greetings = listOf(
            "你好！我是小晴，你的AI心理陪伴助手。有什么想聊的吗？",
            "嗨！很高兴见到你。今天心情怎么样？",
            "你好呀！我是小晴，随时准备倾听你的心声。",
        )
        return greetings.random()
    }
}
```

Note: The `parseRecommendationFromToolUse` and `parseCrisisInterventionFromToolUse` helper functions should be extracted from the existing `Recommendation.kt` parsing logic. Move them to a utility or keep them in the ViewModel.

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL (UI files may have errors from old Message references — fixed in Task 9)

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/onmi/qing/viewmodel/ChatViewModel.kt
git commit -m "refactor: ChatViewModel with ChatMessage model, streaming fix, regenerate/edit/delete"
```

---

## Task 7: Markdown Library + Regex Fix

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/com/onmi/qing/ui/components/MarkdownText.kt`

- [ ] **Step 1: Add Markdown library to `libs.versions.toml`**

Add to `[versions]`:
```toml
richeditor = "1.0.0"
```

Add to `[libraries]`:
```toml
richeditor-compose = { group = "com.mohamedrejeb.richeditor", name = "richeditor-compose", version.ref = "richeditor" }
```

- [ ] **Step 2: Add dependency to `app/build.gradle.kts`**

Add to dependencies:
```kotlin
implementation(libs.richeditor.compose)
```

- [ ] **Step 3: Rewrite `MarkdownText.kt`**

Replace with a wrapper around the RichText library. Keep the same `@Composable fun MarkdownText(text: String, ...)` signature so callers don't need to change:

```kotlin
package com.onmi.qing.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.mohamedrejeb.richeditor.model.rememberRichTextState
import com.mohamedrejeb.richeditor.ui.material3.RichText

@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurface,
    style: TextStyle = MaterialTheme.typography.bodyLarge
) {
    val state = rememberRichTextState()
    state.setMarkdown(text)

    RichText(
        state = state,
        modifier = modifier,
        color = color,
        style = style
    )
}
```

- [ ] **Step 4: Sync Gradle and verify build**

Run: `./gradlew --refresh-dependencies compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml app/build.gradle.kts app/src/main/java/com/onmi/qing/ui/components/MarkdownText.kt
git commit -m "feat: replace custom MarkdownText with richeditor-compose library"
```

---

## Task 8: UI Components — ToolCallCard, ThinkingBlock, ChatMessageItem, InlineActionBar

**Files:**
- Create: `app/src/main/java/com/onmi/qing/ui/components/ToolCallCard.kt`
- Create: `app/src/main/java/com/onmi/qing/ui/components/ThinkingBlock.kt`
- Create: `app/src/main/java/com/onmi/qing/ui/components/InlineActionBar.kt`
- Create: `app/src/main/java/com/onmi/qing/ui/components/ChatMessageItem.kt`

- [ ] **Step 1: Create `ToolCallCard.kt`**

Extract recommendation and crisis intervention cards from ChatScreen with entrance animation. Use MD3 dynamic color and match DiscoverScreen style:

```kotlin
package com.onmi.qing.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.CrisisIntervention
import com.onmi.qing.data.Recommendation

@Composable
fun RecommendationCard(
    recommendation: Recommendation,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedToolCard(modifier) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = getRecommendationIcon(recommendation.type),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = recommendation.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = recommendation.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onAction,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(recommendation.action)
                }
            }
        }
    }
}

@Composable
fun CrisisInterventionCard(
    crisis: CrisisIntervention,
    onCall: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedToolCard(modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = crisis.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = crisis.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = crisis.phone,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun AnimatedToolCard(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnimation = true }

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing)
    )
    val offsetY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 40f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                this.alpha = alpha
                this.translationY = offsetY
            }
    ) {
        content()
    }
}

private fun getRecommendationIcon(type: String): ImageVector = when (type) {
    "breathing_exercise" -> Icons.Default.Air
    "stress_detection" -> Icons.Default.Favorite
    "mood_diary" -> Icons.Default.MenuBook
    "personal_test" -> Icons.Default.Quiz
    else -> Icons.Default.Lightbulb
}
```

- [ ] **Step 2: Create `ThinkingBlock.kt`**

```kotlin
package com.onmi.qing.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun ThinkingBlock(
    thinking: String,
    durationMs: Long? = null,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f),
                shape = RoundedCornerShape(8.dp)
            )
            .clickable { expanded = !expanded }
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "💭",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "思考过程",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.tertiary
                )
                if (durationMs != null) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${durationMs / 1000.0}s",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = if (expanded) "收起" else "展开",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(20.dp)
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically()
        ) {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = thinking,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

- [ ] **Step 3: Create `InlineActionBar.kt`**

```kotlin
package com.onmi.qing.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AiMessageActions(
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ActionButton(icon = Icons.Default.ContentCopy, label = "复制", onClick = onCopy)
        ActionButton(icon = Icons.Default.Refresh, label = "重新生成", onClick = onRegenerate)
        ActionButton(icon = Icons.Default.Delete, label = "删除", onClick = onDelete)
    }
}

@Composable
fun UserMessageActions(
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ActionButton(icon = Icons.Default.ContentCopy, label = "复制", onClick = onCopy)
        ActionButton(icon = Icons.Default.Edit, label = "编辑", onClick = onEdit)
        ActionButton(icon = Icons.Default.Delete, label = "删除", onClick = onDelete)
    }
}

@Composable
private fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
    }
}
```

- [ ] **Step 4: Create `ChatMessageItem.kt`**

```kotlin
package com.onmi.qing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.ChatMessage
import com.onmi.qing.data.MessagePart
import com.onmi.qing.data.MessageRole
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(Date(message.timestamp))

    when (message.role) {
        MessageRole.ASSISTANT -> AiMessageLayout(
            message = message,
            timeStr = timeStr,
            onCopy = onCopy,
            onRegenerate = onRegenerate,
            onDelete = onDelete,
            modifier = modifier
        )
        MessageRole.USER -> UserMessageLayout(
            message = message,
            timeStr = timeStr,
            onCopy = onCopy,
            onEdit = onEdit,
            onDelete = onDelete,
            modifier = modifier
        )
        MessageRole.SYSTEM -> {} // not displayed
    }
}

@Composable
private fun AiMessageLayout(
    message: ChatMessage,
    timeStr: String,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = "小晴",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "小晴 · $timeStr",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            // Thinking block
            val thinking = message.parts.filterIsInstance<MessagePart.Thinking>().firstOrNull()
            if (thinking != null) {
                ThinkingBlock(
                    thinking = thinking.thinking,
                    durationMs = thinking.durationMs
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Text content
            val textParts = message.parts.filterIsInstance<MessagePart.Text>()
            textParts.forEach { part ->
                if (part.text.isNotBlank()) {
                    MarkdownText(
                        text = part.text,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Actions
            AiMessageActions(
                onCopy = onCopy,
                onRegenerate = onRegenerate,
                onDelete = onDelete
            )
        }
    }
}

@Composable
private fun UserMessageLayout(
    message: ChatMessage,
    timeStr: String,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$timeStr",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = message.textContent,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            UserMessageActions(
                onCopy = onCopy,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }

        // User avatar
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "我",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}
```

- [ ] **Step 5: Verify build compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/onmi/qing/ui/components/ToolCallCard.kt \
      app/src/main/java/com/onmi/qing/ui/components/ThinkingBlock.kt \
      app/src/main/java/com/onmi/qing/ui/components/InlineActionBar.kt \
      app/src/main/java/com/onmi/qing/ui/components/ChatMessageItem.kt
git commit -m "feat: add ChatMessageItem, ThinkingBlock, InlineActionBar, ToolCallCard components"
```

---

## Task 9: ChatScreen Rewrite

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/chat/ChatScreen.kt`

- [ ] **Step 1: Rewrite `ChatScreen.kt`**

Replace the entire file. Key changes:
- Use `ChatMessage` instead of `Message`
- Use `ChatMessageItem` composable for each message
- Use `ToolCallCard` composables for recommendation/crisis
- Fix auto-scroll key to trigger during streaming
- Copy/edit/delete actions wired to ViewModel
- All colors from `MaterialTheme.colorScheme`

```kotlin
package com.onmi.qing.ui.screens.chat

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.ChatMessage
import com.onmi.qing.data.MessageRole
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.ui.components.*
import com.onmi.qing.viewmodel.ChatViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: ChatViewModel,
    demoModeManager: DemoModeManager,
    onHistoryClick: () -> Unit,
    onNewChatClick: () -> Unit,
    onBackClick: (() -> Unit)? = null,
    onBreathingClick: () -> Unit,
    onFunTestClick: () -> Unit
) {
    val messages by viewModel.messages.collectAsState()
    val inputText by viewModel.inputText.collectAsState()
    val isAiTyping by viewModel.isAiTyping.collectAsState()
    val recommendation by viewModel.recommendation.collectAsState()
    val crisisIntervention by viewModel.crisisIntervention.collectAsState()
    val currentThinking by viewModel.currentThinking.collectAsState()
    val context = LocalContext.current

    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll: trigger on content change during streaming
    val lastMessageContent = messages.lastOrNull()?.textContent
    LaunchedEffect(messages.size, lastMessageContent, isAiTyping) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Scaffold(
        topBar = {
            ChatTopAppBar(
                onBackClick = onBackClick,
                onNewChatClick = onNewChatClick,
                onHistoryClick = onHistoryClick
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Message list
            Box(modifier = Modifier.weight(1f)) {
                if (messages.isEmpty()) {
                    EmptyChatState(
                        onSuggestionClick = { suggestion ->
                            viewModel.updateInputText(suggestion)
                            viewModel.sendMessage()
                        }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(
                            items = messages,
                            key = { it.id }
                        ) { message ->
                            // Date separator
                            val index = messages.indexOf(message)
                            if (index == 0 || !isSameDay(messages[index - 1].timestamp, message.timestamp)) {
                                DateSeparator(message.timestamp)
                            }

                            ChatMessageItem(
                                message = message,
                                onCopy = { copyToClipboard(context, message.textContent) },
                                onRegenerate = { viewModel.regenerateLastMessage() },
                                onEdit = { /* show edit dialog */ },
                                onDelete = { viewModel.deleteMessage(message.id) }
                            )
                        }

                        // Typing indicator
                        if (isAiTyping) {
                            item(key = "typing") {
                                TypingIndicator(thinking = currentThinking)
                            }
                        }
                    }
                }
            }

            // Recommendation card
            AnimatedVisibility(
                visible = recommendation != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                recommendation?.let { rec ->
                    RecommendationCard(
                        recommendation = rec,
                        onAction = {
                            when (rec.type) {
                                "breathing_exercise" -> onBreathingClick()
                                "personal_test" -> onFunTestClick()
                            }
                            viewModel.clearRecommendation()
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Crisis intervention card
            AnimatedVisibility(
                visible = crisisIntervention != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                crisisIntervention?.let { crisis ->
                    CrisisInterventionCard(
                        crisis = crisis,
                        onCall = { viewModel.clearCrisisIntervention() },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }

            // Input area
            ChatInputArea(
                text = inputText,
                onTextChange = { viewModel.updateInputText(it) },
                onSendClick = { viewModel.sendMessage() },
                isSending = isAiTyping
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatTopAppBar(
    onBackClick: (() -> Unit)?,
    onNewChatClick: () -> Unit,
    onHistoryClick: () -> Unit
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("AI 心理陪伴助手")
            }
        },
        navigationIcon = {
            if (onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                }
            }
        },
        actions = {
            IconButton(onClick = onNewChatClick) {
                Icon(Icons.Default.Add, contentDescription = "新对话")
            }
            IconButton(onClick = onHistoryClick) {
                Icon(Icons.Default.History, contentDescription = "历史记录")
            }
        }
    )
}

@Composable
private fun EmptyChatState(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(80.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(48.dp)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "你好，我是小晴",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "你的 AI 心理陪伴助手，随时可以和我聊天",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(24.dp))

        val suggestions = listOf(
            "最近有点焦虑",
            "想聊聊学习方法",
            "感觉压力好大"
        )
        suggestions.forEach { suggestion ->
            SuggestionChip(
                onClick = { onSuggestionClick(suggestion) },
                label = { Text(suggestion) },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun DateSeparator(timestamp: Long) {
    val calendar = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance()
    val text = when {
        isSameDay(timestamp, System.currentTimeMillis()) -> "今天"
        isSameDay(timestamp, System.currentTimeMillis() - 86400000) -> "昨天"
        else -> "${calendar.get(Calendar.MONTH) + 1}/${calendar.get(Calendar.DAY_OF_MONTH)}"
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = MaterialTheme.shapes.small
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
            )
        }
    }
}

@Composable
private fun TypingIndicator(thinking: String? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(32.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary
            )
        }
        if (thinking != null) {
            Text(
                text = "正在思考...",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
    val cal1 = Calendar.getInstance().apply { timeInMillis = timestamp1 }
    val cal2 = Calendar.getInstance().apply { timeInMillis = timestamp2 }
    return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
        cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("message", text))
}
```

- [ ] **Step 2: Verify build compiles**

Run: `./gradlew compileDebugKotlin`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/onmi/qing/ui/screens/chat/ChatScreen.kt
git commit -m "feat: rewrite ChatScreen with RikkaHub-style layout and message actions"
```

---

## Task 10: ChatInputArea + Build Verification

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/ui/screens/chat/ChatScreen.kt` (ChatInputArea composable)

- [ ] **Step 1: Replace ChatInputArea in `ChatScreen.kt`**

Find the `ChatInputArea` composable at the bottom of the file and replace it:

```kotlin
@Composable
private fun ChatInputArea(
    text: String,
    onTextChange: (String) -> Unit,
    onSendClick: () -> Unit,
    isSending: Boolean = false,
    isEditing: Boolean = false,
    onCancelEdit: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column {
            // Edit mode indicator
            if (isEditing) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "正在编辑消息",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    TextButton(onClick = { onCancelEdit?.invoke() }) {
                        Text("取消")
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // Multi-line text field
                OutlinedTextField(
                    value = text,
                    onValueChange = onTextChange,
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp, max = 160.dp),
                    enabled = !isSending,
                    placeholder = {
                        Text(
                            "说点什么吧...",
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (!isSending && text.isNotBlank()) onSendClick()
                        }
                    ),
                    maxLines = 6
                )

                Spacer(modifier = Modifier.width(8.dp))

                // Send button
                FilledIconButton(
                    onClick = onSendClick,
                    enabled = !isSending && text.isNotBlank(),
                    colors = IconButtonDefaults.filledIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        disabledContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
                    ),
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Bottom)
                ) {
                    if (isSending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Send,
                            contentDescription = "发送",
                            tint = if (text.isNotBlank()) MaterialTheme.colorScheme.onPrimary
                                   else MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Full build verification**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL — APK produced at `app/build/outputs/apk/debug/app-debug.apk`

- [ ] **Step 3: Install and smoke test**

Run: `./gradlew installDebug`
Test on device/emulator:
1. Open app, navigate to chat
2. Type a message and send — verify streaming text appears in real-time
3. Verify thinking block shows during AI response
4. Tap "重新生成" on an AI message
5. Tap "编辑" on a user message
6. Tap "删除" on a message
7. Navigate to history, tap a session — verify it loads correctly
8. Navigate back to new chat — verify greeting appears
9. Test recommendation card appears with animation
10. Verify MD3 dynamic color applies to all elements

- [ ] **Step 4: Final commit**

```bash
git add app/src/main/java/com/onmi/qing/ui/screens/chat/ChatScreen.kt
git commit -m "feat: multi-line ChatInputArea with auto-expand and edit mode"
```

---

## Summary

| Task | Description | Key Files |
|------|-------------|-----------|
| 1 | Data models | `ChatMessage.kt` (new) |
| 2 | Room schema | `MessageEntity.kt`, `AppDatabase.kt`, `ChatDao.kt`, `Mappers.kt` |
| 3 | Repository + Demo | `ChatRepository.kt`, `DemoModeManager.kt` |
| 4 | Streaming fix | `NetworkModule.kt`, `ApiServiceFactory.kt`, `SseEventParser.kt` |
| 5 | Navigation | `Screen.kt`, `Navigation.kt` |
| 6 | ViewModel | `ChatViewModel.kt` |
| 7 | Markdown lib | `libs.versions.toml`, `build.gradle.kts`, `MarkdownText.kt` |
| 8 | UI components | `ToolCallCard.kt`, `ThinkingBlock.kt`, `InlineActionBar.kt`, `ChatMessageItem.kt` |
| 9 | ChatScreen | `ChatScreen.kt` |
| 10 | InputArea + verify | `ChatScreen.kt` + full build + smoke test |
