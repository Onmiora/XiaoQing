# Chat Refactor Design

**Date:** 2026-05-01
**Scope:** Chat conversation frontend + code logic refactor
**Reference:** [RikkaHub](https://github.com/rikkahub/rikkahub) (simplified for single backend)

## Goals

1. Adopt a polymorphic message model (`ChatMessage` + `MessagePart`) supporting text and thinking/reasoning display
2. Fix real-time SSE streaming display (currently broken due to logging interceptor buffering)
3. Add message actions: copy, regenerate, edit, delete
4. Replace custom `MarkdownText` with a third-party Markdown library
5. Fix history-to-chat navigation (session not passed)
6. Improve ChatInputArea UX (multi-line, auto-expand)
7. Preserve all existing features: tool call cards (recommendation + crisis intervention), demo mode, analysis
8. Maintain Material 3 dynamic color support throughout

## Approach

**Approach B: Incremental refactor** — adopt RikkaHub's `UIMessage`-style polymorphic model, keep flat message list with `regenerationIndex` for regeneration (simpler than `MessageNode` branching). No branch selector UI.

---

## 1. Data Model

### New `ChatMessage`

```kotlin
data class ChatMessage(
    val id: String,
    val role: MessageRole,           // USER, ASSISTANT, SYSTEM
    val parts: List<MessagePart>,    // polymorphic content
    val timestamp: Long,
    val regenerationIndex: Int = 0,  // which generation (for regenerate)
    val isRegenerating: Boolean = false
)

enum class MessageRole { USER, ASSISTANT, SYSTEM }

sealed class MessagePart {
    data class Text(val text: String) : MessagePart()
    data class Thinking(
        val thinking: String,
        val durationMs: Long? = null
    ) : MessagePart()
}
```

### Room Schema

**`MessageEntity` changes:**
- New columns: `role` (String), `parts` (JSON-serialized `List<MessagePart>`), `regenerationIndex` (Int)
- Remove: `isFromUser` (Boolean)
- `ChatSessionEntity` unchanged

**Database:** Rebuild (no migration). Version bump in `AppDatabase`.

### Key Decisions

- `regenerationIndex` supports regenerate: user clicks "regenerate" -> `regenerationIndex++` -> new content overwrites old (flat list, no history variants stored)
- Tool call cards (recommendation/crisis intervention) are NOT stored as `MessagePart`. They continue to be parsed from AI response text, preserving existing logic.

---

## 2. Streaming Fix

### Root Causes Found

| Priority | Issue | File | Impact |
|----------|-------|------|--------|
| CRITICAL | `HttpLoggingInterceptor.Level.BODY` buffers entire SSE stream | `NetworkModule.kt:24` | Streaming completely broken in debug builds |
| HIGH | Blocking `readLine()` on Main dispatcher | `ChatViewModel.kt:273` | UI thread blocked, janky |
| HIGH | No throttling on per-chunk UI updates + regex recompilation | `ChatViewModel.kt:371` / `MarkdownText.kt` | 50-100 recompositions/sec |
| MEDIUM | Auto-scroll key doesn't change during streaming | `ChatScreen.kt:106` | No auto-scroll during stream |
| LOW | Backend middleware eager body read | `main.py:52` | Minor (backend not changed) |

### Fixes

1. **`NetworkModule.kt`:** Create two `OkHttpClient` instances — one for regular requests (BODY logging), one for streaming (HEADERS logging only). `ApiServiceFactory` selects based on endpoint.

2. **`SseEventParser.kt`:** Add `flowOn(Dispatchers.IO)` to the `parseEvents` flow so `readLine()` runs on IO thread.

3. **`ChatViewModel.kt`:** Buffer streaming deltas and emit UI updates at most every 50ms using `buffer(Channel.CONFLATED)` + `collectLatest` with a delay-based throttle. Move `Regex` objects in `MarkdownText.kt` to `companion object` constants.

4. **`ChatScreen.kt`:** Change auto-scroll `LaunchedEffect` key from `messages.size` to `messages.lastOrNull()?.content` or a streaming content hash to trigger during streaming.

### Backend

No changes needed. SSE format is correct (`data: {json}\n\n` with proper headers).

---

## 3. ChatViewModel Refactor

### State

```kotlin
private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
private val _inputText = MutableStateFlow("")
private val _isAiTyping = MutableStateFlow(false)
private val _streamingMessageId = MutableStateFlow<String?>(null)
private val _recommendation = MutableStateFlow<Recommendation?>(null)
private val _crisisIntervention = MutableStateFlow<CrisisIntervention?>(null)
private val _currentSessionId = MutableStateFlow<String?>(null)
private val _currentThinking = MutableStateFlow<String?>(null)  // real-time thinking display
```

### Flows

**Send message:** trim -> clear input -> create session (if none) -> add user message -> stream API

**Streaming response:**
- `ContentBlockDelta` -> append text to streaming message's `Text` part
- `ThinkingDelta` -> update `_currentThinking` in real-time
- `MessageStop` -> seal thinking as `Thinking` part, persist to Room
- `ToolUse` -> keep existing parsing logic for recommendation/crisis cards

**Regenerate:** Find last AI message, `regenerationIndex++`, clear content, re-call streaming API

**Edit message:** Delete all messages after the edited one, re-send with edited content

**Delete message:** Remove from Room and in-memory list

### History Navigation Fix

- `Screen.Chat` route: `"chat/{sessionId}"` with optional `sessionId` argument (default null = new session)
- `ChatViewModel.Factory` receives `sessionId`, loads session messages on init if present
- `HistoryScreen.onSessionClick`: `navController.navigate("chat/${session.id}")`

---

## 4. UI Layer

### ChatScreen Layout

```
Scaffold
├── TopAppBar (MD3 themed)
├── MessageList (LazyColumn)
│   ├── DateSeparator
│   ├── ChatMessageItem (AI / user)
│   │   ├── Avatar row
│   │   ├── ThinkingBlock (collapsible, AnimatedVisibility)
│   │   ├── TextBlock (third-party Markdown)
│   │   └── InlineActionBar (copy/regenerate/edit/delete)
│   ├── ToolCallCard (recommendation/crisis, AnimatedCard entrance)
│   └── TypingIndicator
└── ChatInputArea
```

### Message Layout (RikkaHub style)

- No bubbles — messages displayed flat
- AI messages: left-aligned with avatar + role indicator
- User messages: right-aligned with avatar
- All colors from `MaterialTheme.colorScheme.*` (dynamic color)

### ThinkingBlock

- `AnimatedVisibility` for expand/collapse
- Border color: `MaterialTheme.colorScheme.tertiary` (follows dynamic color)
- Default collapsed, tap to expand
- Shows thinking duration if available

### InlineActionBar

- AI messages: copy, regenerate, delete
- User messages: copy, edit, delete
- Buttons: `IconButton` with `MaterialTheme.colorScheme.onSurfaceVariant`
- Default 50% opacity, highlights on press

### ToolCallCard Animations

- Reuse existing `AnimatedCard` pattern (fade in + slide up, 60ms stagger)
- Recommendation: `ElevatedCard` + `primaryContainer` (matches DiscoverScreen)
- Crisis intervention: `Card` + `errorContainer` (matches DiscoverScreen HotlineCard)

### Markdown Rendering

- Replace custom `MarkdownText` with a third-party library
- Candidates: `com.mikepenz:multiplatform-markdown-renderer` or `com.mohamedrejeb.richeditor:richtext-ui`
- Support: headings, bold/italic, code blocks, lists, links, blockquotes

### ChatInputArea Improvements

**Current problems:**
- `singleLine = true` prevents long text from wrapping
- Input + send button in same Row breaks layout on multi-line

**New design:**
```
Surface
├── Column
│   ├── EditModeBar (shown when editing: "Editing message" + cancel button)
│   └── Row (bottom-aligned)
│       ├── TextField (multi-line, maxLines=6, auto-expand)
│       └── SendButton (48dp, bottom-aligned)
```

- `singleLine = false`, `maxLines = 6`, auto-expanding height
- Use `BasicTextField` + custom container for better multi-line control
- Send button at bottom-right, aligned with last line
- Edit mode: indicator bar above input with `primaryContainer` background
- All colors follow MD3 dynamic color

---

## 5. File Structure Changes

| File | Change |
|------|--------|
| `data/ChatMessage.kt` | New: `ChatMessage` + `MessagePart` sealed class |
| `data/local/entity/MessageEntity.kt` | Refactor: `role` + `parts` JSON + `regenerationIndex` |
| `data/local/Mappers.kt` | Refactor: new entity <-> domain mapping |
| `data/local/ChatDao.kt` | Adjust queries for new schema |
| `data/repository/ChatRepository.kt` | Adapt to new model, add `regenerateMessage` / `deleteMessageAndAfter` |
| `viewmodel/ChatViewModel.kt` | Refactor: streaming fix + regenerate/edit/delete logic |
| `ui/screens/chat/ChatScreen.kt` | Refactor: new layout + message actions + ThinkingBlock |
| `ui/components/MarkdownText.kt` | Replace with third-party library wrapper |
| `ui/components/ToolCallCard.kt` | New: extract recommendation/crisis cards + animation |
| `ui/navigation/Screen.kt` | Chat route adds `sessionId` parameter |
| `ui/navigation/Navigation.kt` | Pass `sessionId` to ChatViewModel |
| `di/NetworkModule.kt` | Dual OkHttpClient (regular/streaming) |

## 6. Dependencies

- Add Markdown library (TBD: `multiplatform-markdown-renderer` or `richtext-ui`)
- No other new dependencies

## 7. Database

- `AppDatabase` version bump, rebuild (no data migration)

## 8. Backend

- No changes needed
