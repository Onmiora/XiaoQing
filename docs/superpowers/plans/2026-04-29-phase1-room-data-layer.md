# Phase 1: Room Data Layer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace QingDataStore's JSON-in-Preferences storage for structured collections (chat sessions, messages, mood entries, achievements) with Room database, while slimming QingDataStore down to a PreferencesManager for simple key-value preferences only.

**Architecture:** Room entities map 1:1 to domain models via extension functions. Repository classes wrap DAOs and expose domain-model Flows. A one-time migration reads existing JSON from DataStore and inserts into Room. QingDataStore is renamed to PreferencesManager, losing all collection-related methods.

**Tech Stack:** Room (KSP), Gson (existing), DataStore Preferences (existing), Kotlin Coroutines/Flow

---

## File Structure

### New files
| File | Responsibility |
|---|---|
| `data/local/entity/ChatSessionEntity.kt` | Room entity for chat sessions |
| `data/local/entity/MessageEntity.kt` | Room entity for messages |
| `data/local/entity/MoodEntryEntity.kt` | Room entity for mood entries |
| `data/local/entity/AchievementEntity.kt` | Room entity for achievements |
| `data/local/dao/ChatDao.kt` | DAO for chat sessions + messages |
| `data/local/dao/MoodDao.kt` | DAO for mood entries |
| `data/local/dao/AchievementDao.kt` | DAO for achievements |
| `data/local/AppDatabase.kt` | Room database definition |
| `data/local/Converters.kt` | Type converters (if needed) |
| `data/repository/ChatRepository.kt` | Chat data operations |
| `data/repository/MoodRepository.kt` | Mood data operations |
| `data/repository/AchievementRepository.kt` | Achievement data operations |
| `data/local/DataMigration.kt` | One-time DataStore → Room migration |

### Modified files
| File | Changes |
|---|---|
| `gradle/libs.versions.toml` | Add Room, KSP, Hilt versions |
| `build.gradle.kts` (root) | Add KSP plugin |
| `app/build.gradle.kts` | Add Room, KSP dependencies |
| `data/datastore/QingDataStore.kt` | Remove all collection methods, rename to PreferencesManager |
| `data/Message.kt` | Change `content` from `var` to `val` |
| `data/MoodEntry.kt` | Remove duplicate MoodEntryEntity and conversion functions |
| `data/PsychologyDimension.kt` | Change Color to Long |
| `QingApplication.kt` | Add Room database initialization |
| All ViewModels | Update imports (will be done in Phase 2 with Hilt, but old QingDataStore references must still compile) |

### Deleted files
| File | Reason |
|---|---|
| (none in this phase) | Cleanup happens after all ViewModels are migrated in Phase 2 |

---

### Task 1.1: Add Room and KSP Dependencies

**Files:**
- Modify: `gradle/libs.versions.toml`
- Modify: `build.gradle.kts` (root)
- Modify: `app/build.gradle.kts`

- [ ] **Step 1: Update version catalog**

In `gradle/libs.versions.toml`, add versions and libraries:

```toml
# Add to [versions]
room = "2.7.1"
ksp = "2.2.10-1.0.31"

# Add to [libraries]
androidx-room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
androidx-room-ktx = { group = "androidx.room", name = "room-ktx", version.ref = "room" }
androidx-room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }

# Add to [plugins]
ksp = { id = "com.google.devtools.ksp", version.ref = "ksp" }
```

- [ ] **Step 2: Add KSP plugin to root build.gradle.kts**

In the root `build.gradle.kts`, add to the `plugins` block (if it exists) or ensure KSP is available. The current root build file uses AGP and Kotlin plugins declared in the version catalog. KSP is applied at the app level.

- [ ] **Step 3: Update app/build.gradle.kts**

Add the KSP plugin and Room dependencies:

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}
```

Add to the `dependencies` block:

```kotlin
    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
```

- [ ] **Step 4: Sync and verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL (no Room usage yet, just dependency resolution)

- [ ] **Step 5: Commit**

```bash
git add gradle/libs.versions.toml build.gradle.kts app/build.gradle.kts
git commit -m "build: add Room and KSP dependencies"
```

---

### Task 1.2: Create Room Entities

**Files:**
- Create: `app/src/main/java/com/onmi/qing/data/local/entity/ChatSessionEntity.kt`
- Create: `app/src/main/java/com/onmi/qing/data/local/entity/MessageEntity.kt`
- Create: `app/src/main/java/com/onmi/qing/data/local/entity/MoodEntryEntity.kt`
- Create: `app/src/main/java/com/onmi/qing/data/local/entity/AchievementEntity.kt`

- [ ] **Step 1: Create ChatSessionEntity**

```kotlin
package com.onmi.qing.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_sessions")
data class ChatSessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val lastMessage: String,
    val timestamp: Long,
    val messageCount: Int,
    val analysisCount: Int = 0
)
```

- [ ] **Step 2: Create MessageEntity**

```kotlin
package com.onmi.qing.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "messages",
    foreignKeys = [ForeignKey(
        entity = ChatSessionEntity::class,
        parentColumns = ["id"],
        childColumns = ["sessionId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index(value = ["sessionId"])]
)
data class MessageEntity(
    @PrimaryKey val id: String,
    val sessionId: String,
    val content: String,
    val isFromUser: Boolean,
    val timestamp: Long
)
```

- [ ] **Step 3: Create MoodEntryEntity**

```kotlin
package com.onmi.qing.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mood_entries")
data class MoodEntryEntity(
    @PrimaryKey val id: String,
    val mood: String,
    val reason: String,
    val timestamp: Long
)
```

- [ ] **Step 4: Create AchievementEntity**

```kotlin
package com.onmi.qing.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "achievements")
data class AchievementEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String,
    val iconName: String,
    val isUnlocked: Boolean = false,
    val unlockedDate: String? = null
)
```

- [ ] **Step 5: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/onmi/qing/data/local/entity/
git commit -m "feat: add Room entity classes"
```

---

### Task 1.3: Create DAOs

**Files:**
- Create: `app/src/main/java/com/onmi/qing/data/local/dao/ChatDao.kt`
- Create: `app/src/main/java/com/onmi/qing/data/local/dao/MoodDao.kt`
- Create: `app/src/main/java/com/onmi/qing/data/local/dao/AchievementDao.kt`

- [ ] **Step 1: Create ChatDao**

```kotlin
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
}
```

- [ ] **Step 2: Create MoodDao**

```kotlin
package com.onmi.qing.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.onmi.qing.data.local.entity.MoodEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<MoodEntryEntity>>

    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC LIMIT 1")
    fun getLatestEntry(): Flow<MoodEntryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MoodEntryEntity)

    @Update
    suspend fun update(entry: MoodEntryEntity)

    @Delete
    suspend fun delete(entry: MoodEntryEntity)

    @Query("SELECT * FROM mood_entries WHERE id = :entryId LIMIT 1")
    suspend fun getById(entryId: String): MoodEntryEntity?

    @Query("DELETE FROM mood_entries WHERE id = :entryId")
    suspend fun deleteById(entryId: String)

    @Query("SELECT COUNT(*) FROM mood_entries")
    suspend fun getEntryCount(): Int
}
```

- [ ] **Step 3: Create AchievementDao**

```kotlin
package com.onmi.qing.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.onmi.qing.data.local.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {
    @Query("SELECT * FROM achievements ORDER BY name ASC")
    fun getAll(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievements WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): AchievementEntity?

    @Update
    suspend fun update(achievement: AchievementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(achievements: List<AchievementEntity>)

    @Query("SELECT COUNT(*) FROM achievements")
    suspend fun getCount(): Int

    @Query("SELECT COUNT(*) FROM achievements WHERE isUnlocked = 1")
    fun getUnlockedCount(): Flow<Int>
}
```

- [ ] **Step 4: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/onmi/qing/data/local/dao/
git commit -m "feat: add Room DAO interfaces"
```

---

### Task 1.4: Create AppDatabase

**Files:**
- Create: `app/src/main/java/com/onmi/qing/data/local/AppDatabase.kt`

- [ ] **Step 1: Create AppDatabase**

```kotlin
package com.onmi.qing.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.onmi.qing.data.local.dao.AchievementDao
import com.onmi.qing.data.local.dao.ChatDao
import com.onmi.qing.data.local.dao.MoodDao
import com.onmi.qing.data.local.entity.AchievementEntity
import com.onmi.qing.data.local.entity.ChatSessionEntity
import com.onmi.qing.data.local.entity.MessageEntity
import com.onmi.qing.data.local.entity.MoodEntryEntity

@Database(
    entities = [
        ChatSessionEntity::class,
        MessageEntity::class,
        MoodEntryEntity::class,
        AchievementEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun moodDao(): MoodDao
    abstract fun achievementDao(): AchievementDao
}
```

- [ ] **Step 2: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add app/src/main/java/com/onmi/qing/data/local/AppDatabase.kt
git commit -m "feat: add Room AppDatabase"
```

---

### Task 1.5: Create Domain Model Mappers

**Files:**
- Create: `app/src/main/java/com/onmi/qing/data/local/Mappers.kt`
- Modify: `app/src/main/java/com/onmi/qing/data/Message.kt`
- Modify: `app/src/main/java/com/onmi/qing/data/MoodEntry.kt`

- [ ] **Step 1: Fix Message.content to val**

In `app/src/main/java/com/onmi/qing/data/Message.kt`, change line 10:

```kotlin
// Before:
var content: String,         // 消息内容 (mutable for streaming support)

// After:
val content: String,         // 消息内容 (immutable, use copy() for streaming updates)
```

- [ ] **Step 2: Remove duplicate MoodEntryEntity from MoodEntry.kt**

In `app/src/main/java/com/onmi/qing/data/MoodEntry.kt`, remove lines 20-42 (the `MoodEntryEntity` data class, `toEntity()`, and `toMoodEntry()` extension functions). Keep only the `MoodType` enum and `MoodEntry` data class.

The file should become:

```kotlin
package com.onmi.qing.data

import java.util.UUID

// 心情类型枚举
enum class MoodType {
    HAPPY,  // 开心
    CALM,   // 平静
    UNHAPPY // 不开心
}

// 心情记录数据模型
data class MoodEntry(
    val id: String = UUID.randomUUID().toString(),
    val mood: MoodType,
    val reason: String,         // 发生的原因
    val timestamp: Long = System.currentTimeMillis()
)
```

- [ ] **Step 3: Create Mappers.kt**

```kotlin
package com.onmi.qing.data.local

import com.onmi.qing.data.Achievement
import com.onmi.qing.data.ChatSession
import com.onmi.qing.data.Message
import com.onmi.qing.data.MoodEntry
import com.onmi.qing.data.MoodType
import com.onmi.qing.data.local.entity.AchievementEntity
import com.onmi.qing.data.local.entity.ChatSessionEntity
import com.onmi.qing.data.local.entity.MessageEntity
import com.onmi.qing.data.local.entity.MoodEntryEntity

// ChatSession <-> ChatSessionEntity
fun ChatSession.toEntity() = ChatSessionEntity(
    id = id,
    title = title,
    lastMessage = lastMessage,
    timestamp = timestamp,
    messageCount = messageCount,
    analysisCount = analysisCount
)

fun ChatSessionEntity.toDomain() = ChatSession(
    id = id,
    title = title,
    lastMessage = lastMessage,
    timestamp = timestamp,
    messageCount = messageCount,
    analysisCount = analysisCount
)

// Message <-> MessageEntity
fun Message.toEntity(sessionId: String) = MessageEntity(
    id = id,
    sessionId = sessionId,
    content = content,
    isFromUser = isFromUser,
    timestamp = timestamp
)

fun MessageEntity.toDomain() = Message(
    id = id,
    content = content,
    isFromUser = isFromUser,
    timestamp = timestamp
)

// MoodEntry <-> MoodEntryEntity
fun MoodEntry.toEntity() = MoodEntryEntity(
    id = id,
    mood = mood.name,
    reason = reason,
    timestamp = timestamp
)

fun MoodEntryEntity.toDomain() = MoodEntry(
    id = id,
    mood = MoodType.valueOf(mood),
    reason = reason,
    timestamp = timestamp
)

// Achievement <-> AchievementEntity
fun Achievement.toEntity() = AchievementEntity(
    id = id,
    name = name,
    description = description,
    iconName = iconName,
    isUnlocked = isUnlocked,
    unlockedDate = unlockedDate
)

fun AchievementEntity.toDomain() = Achievement(
    id = id,
    name = name,
    description = description,
    iconName = iconName,
    isUnlocked = isUnlocked,
    unlockedDate = unlockedDate
)
```

- [ ] **Step 4: Fix compilation errors from removing MoodEntryEntity**

Search for any imports of `com.onmi.qing.data.MoodEntryEntity`, `com.onmi.qing.data.toEntity`, or `com.onmi.qing.data.toMoodEntry` in other files and update them to use the new mappers from `com.onmi.qing.data.local`.

The only file that imports these is `QingDataStore.kt` (lines 20-21), which will be refactored in Task 1.7. For now, temporarily keep the imports working by adding the new mapper imports.

- [ ] **Step 5: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL (QingDataStore still uses its own private entity classes, so removing the public ones won't break it)

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/onmi/qing/data/Message.kt \
       app/src/main/java/com/onmi/qing/data/MoodEntry.kt \
       app/src/main/java/com/onmi/qing/data/local/Mappers.kt
git commit -m "refactor: add Room mappers, fix Message.content immutability, remove duplicate MoodEntryEntity"
```

---

### Task 1.6: Create Repository Layer

**Files:**
- Create: `app/src/main/java/com/onmi/qing/data/repository/ChatRepository.kt`
- Create: `app/src/main/java/com/onmi/qing/data/repository/MoodRepository.kt`
- Create: `app/src/main/java/com/onmi/qing/data/repository/AchievementRepository.kt`

- [ ] **Step 1: Create ChatRepository**

```kotlin
package com.onmi.qing.data.repository

import com.onmi.qing.data.ChatSession
import com.onmi.qing.data.Message
import com.onmi.qing.data.local.dao.ChatDao
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

        // Update session
        val sessionEntity = chatDao.getMessageById(message.id)?.let {
            // session exists, update it
            null // we need the session, not the message
        }
        // Get session from messages query - actually we need a session query
        updateSessionAfterMessage(sessionId, message, isFromUser)

        return message
    }

    private suspend fun updateSessionAfterMessage(sessionId: String, message: Message, isFromUser: Boolean) {
        // We need to get the session. Since we don't have a direct query, we'll use a workaround
        // by reading from the sessions flow. But for now, let's add a direct query.
        // Actually, let's just insert/update the session directly.
        // We need to get the existing session first. Let's add a suspend query for this.
        // For now, we'll update using a raw approach - get all sessions and find ours.
        // This is a bit inefficient but correct. We can optimize later.
        val sessions = chatDao.getAllSessions() // This returns Flow, not what we want
        // Let's use a different approach - add a getSessionById query
        // For now, let's use a workaround: insert a new session entity with updated fields
        // Actually the simplest is to just call updateSession with the new values
        // We need to fetch the session first. Let me add a query.
        // Since we can't easily get a suspend result from a Flow, let's add a direct query.
        // For now, this will be handled by adding getSessionById to the DAO.
        // TODO: This is handled in the next step by adding getSessionById to ChatDao
    }

    suspend fun updateMessageContent(messageId: String, newContent: String) {
        val message = chatDao.getMessageById(messageId) ?: return
        chatDao.updateMessage(message.copy(content = newContent))
    }

    suspend fun deleteMessage(messageId: String, sessionId: String) {
        val message = chatDao.getMessageById(messageId) ?: return
        chatDao.deleteMessage(message)
    }

    suspend fun deleteSession(sessionId: String) {
        chatDao.deleteMessagesBySession(sessionId)
        // We need to get the session entity to delete it
        // Add getSessionById to DAO
    }

    suspend fun deleteAllData() {
        chatDao.deleteAllMessages()
        chatDao.deleteAllSessions()
    }

    suspend fun getSessionCount(): Int = chatDao.getSessionCount()
}
```

- [ ] **Step 2: Add missing queries to ChatDao**

Add these queries to `app/src/main/java/com/onmi/qing/data/local/dao/ChatDao.kt`:

```kotlin
    @Query("SELECT * FROM chat_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSessionById(sessionId: String): ChatSessionEntity?
```

- [ ] **Step 3: Update ChatRepository to use getSessionById**

Replace the `ChatRepository` with the corrected version:

```kotlin
package com.onmi.qing.data.repository

import com.onmi.qing.data.ChatSession
import com.onmi.qing.data.Message
import com.onmi.qing.data.local.dao.ChatDao
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

    suspend fun deleteAllData() {
        chatDao.deleteAllMessages()
        chatDao.deleteAllSessions()
    }

    suspend fun getSessionCount(): Int = chatDao.getSessionCount()
}
```

- [ ] **Step 4: Create MoodRepository**

```kotlin
package com.onmi.qing.data.repository

import com.onmi.qing.data.MoodEntry
import com.onmi.qing.data.MoodType
import com.onmi.qing.data.local.dao.MoodDao
import com.onmi.qing.data.local.toDomain
import com.onmi.qing.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MoodRepository(private val moodDao: MoodDao) {

    fun getAllEntries(): Flow<List<MoodEntry>> =
        moodDao.getAllEntries().map { list -> list.map { it.toDomain() } }

    fun getLatestEntry(): Flow<MoodEntry?> =
        moodDao.getLatestEntry().map { it?.toDomain() }

    suspend fun addEntry(mood: MoodType, reason: String): MoodEntry {
        val entry = MoodEntry(
            mood = mood,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )
        moodDao.insert(entry.toEntity())
        return entry
    }

    suspend fun updateEntry(entryId: String, mood: MoodType, reason: String) {
        val existing = moodDao.getById(entryId) ?: return
        moodDao.update(existing.copy(mood = mood.name, reason = reason))
    }

    suspend fun deleteEntry(entryId: String) {
        moodDao.deleteById(entryId)
    }

    suspend fun getEntryCount(): Int = moodDao.getEntryCount()
}
```

- [ ] **Step 5: Create AchievementRepository**

```kotlin
package com.onmi.qing.data.repository

import com.onmi.qing.data.Achievement
import com.onmi.qing.data.AchievementList
import com.onmi.qing.data.local.dao.AchievementDao
import com.onmi.qing.data.local.toDomain
import com.onmi.qing.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AchievementRepository(private val achievementDao: AchievementDao) {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    fun getAll(): Flow<List<Achievement>> =
        achievementDao.getAll().map { list -> list.map { it.toDomain() } }

    fun getUnlockedCount(): Flow<Int> = achievementDao.getUnlockedCount()

    suspend fun initializeDefaults() {
        if (achievementDao.getCount() == 0) {
            achievementDao.insertAll(AchievementList.achievements.map { it.toEntity() })
        }
    }

    suspend fun unlock(achievementId: String) {
        val entity = achievementDao.getById(achievementId) ?: return
        if (!entity.isUnlocked) {
            achievementDao.update(
                entity.copy(
                    isUnlocked = true,
                    unlockedDate = dateFormat.format(Date())
                )
            )
        }
    }

    suspend fun lock(achievementId: String) {
        val entity = achievementDao.getById(achievementId) ?: return
        if (entity.isUnlocked) {
            achievementDao.update(
                entity.copy(
                    isUnlocked = false,
                    unlockedDate = null
                )
            )
        }
    }

    suspend fun resetAll() {
        achievementDao.insertAll(AchievementList.achievements.map { it.toEntity() })
    }
}
```

- [ ] **Step 6: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 7: Commit**

```bash
git add app/src/main/java/com/onmi/qing/data/repository/ \
       app/src/main/java/com/onmi/qing/data/local/dao/ChatDao.kt
git commit -m "feat: add Repository layer (Chat, Mood, Achievement)"
```

---

### Task 1.7: Slim Down QingDataStore to PreferencesManager

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/data/datastore/QingDataStore.kt`

This is the largest task. We need to:
1. Remove all collection-related methods (sessions, messages, achievements, mood entries)
2. Remove private entity classes and JSON serialization
3. Keep only preference-related methods
4. Keep the same class name for now (rename in Phase 2 when Hilt is introduced)

- [ ] **Step 1: Rewrite QingDataStore**

Replace the entire file content. The new version keeps only preferences, psychology dimensions, usage stats, and daily activity tracking. All collection storage is removed.

```kotlin
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
```

- [ ] **Step 2: Verify build — this will fail**

Run: `./gradlew assembleDebug`
Expected: FAIL — many files reference removed methods like `allSessions`, `addMessage`, `allAchievements`, `moodEntries`, etc.

This is expected. The compilation errors tell us exactly which files need to be updated to use the new Repository classes. We'll fix these in Task 1.8 and Phase 2.

- [ ] **Step 3: Document all compilation errors**

Run: `./gradlew assembleDebug 2>&1 | grep "error:" | head -50`

Save the output to understand which files need updating.

- [ ] **Step 4: Commit (WIP)**

```bash
git add app/src/main/java/com/onmi/qing/data/datastore/QingDataStore.kt
git commit -m "refactor: slim QingDataStore to preferences-only (WIP, breaks build)"
```

---

### Task 1.8: Wire Repositories into QingApplication and Fix Compilation

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/QingApplication.kt`
- Modify: All ViewModels that reference removed QingDataStore methods
- Modify: `app/src/main/java/com/onmi/qing/ui/navigation/Navigation.kt`

This is the most complex task. We need to make the build pass by wiring the repositories through the application class and updating all consumers.

- [ ] **Step 1: Update QingApplication to initialize Room and Repositories**

```kotlin
package com.onmi.qing

import android.app.Application
import androidx.room.Room
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.data.local.AppDatabase
import com.onmi.qing.data.repository.AchievementRepository
import com.onmi.qing.data.repository.ChatRepository
import com.onmi.qing.data.repository.MoodRepository
import com.onmi.qing.data.local.DataMigration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QingApplication : Application() {

    lateinit var dataStore: QingDataStore
        private set

    lateinit var demoModeManager: DemoModeManager
        private set

    lateinit var database: AppDatabase
        private set

    lateinit var chatRepository: ChatRepository
        private set

    lateinit var moodRepository: MoodRepository
        private set

    lateinit var achievementRepository: AchievementRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        dataStore = QingDataStore(this)
        demoModeManager = DemoModeManager(this)

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "qing_database"
        ).build()

        chatRepository = ChatRepository(database.chatDao())
        moodRepository = MoodRepository(database.moodDao())
        achievementRepository = AchievementRepository(database.achievementDao())

        // Initialize default achievements and run migration
        applicationScope.launch {
            achievementRepository.initializeDefaults()
            DataMigration.migrateIfNeeded(dataStore, chatRepository, moodRepository, achievementRepository)
        }
    }
}
```

- [ ] **Step 2: Create DataMigration utility**

```kotlin
package com.onmi.qing.data.local

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.onmi.qing.data.Achievement
import com.onmi.qing.data.ChatSession
import com.onmi.qing.data.Message
import com.onmi.qing.data.MoodEntry
import com.onmi.qing.data.MoodType
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.local.entity.AchievementEntity
import com.onmi.qing.data.local.entity.ChatSessionEntity
import com.onmi.qing.data.local.entity.MessageEntity
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
                        val entity = ChatSessionEntity(
                            id = old.id,
                            title = old.title,
                            lastMessage = old.lastMessage,
                            timestamp = old.timestamp,
                            messageCount = old.messageCount,
                            analysisCount = old.analysisCount
                        )
                        // Insert directly via DAO to avoid re-generating IDs
                        // We need access to the DAO. Let's pass it through or use the repository.
                        // Actually, we can add a raw insert method to the repository.
                        // For simplicity, let's use the repository's createSession equivalent.
                        // But that generates new IDs. We need to preserve old IDs.
                        // Solution: add insertSession to ChatRepository that takes a ChatSession.
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
                        val entity = MessageEntity(
                            id = old.id,
                            sessionId = old.sessionId,
                            content = old.content,
                            isFromUser = old.isFromUser,
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
```

- [ ] **Step 3: Add direct insert methods to Repositories**

Add to `ChatRepository`:

```kotlin
    suspend fun insertSessionDirect(entity: com.onmi.qing.data.local.entity.ChatSessionEntity) {
        chatDao.insertSession(entity)
    }

    suspend fun insertMessageDirect(entity: com.onmi.qing.data.local.entity.MessageEntity) {
        chatDao.insertMessage(entity)
    }
```

Add to `MoodRepository`:

```kotlin
    suspend fun insertDirect(entity: com.onmi.qing.data.local.entity.MoodEntryEntity) {
        moodDao.insert(entity)
    }
```

- [ ] **Step 4: Update ViewModels to use Repositories**

For each ViewModel, replace `dataStore.xxx` collection calls with the appropriate repository call. The key changes:

**StateViewModel** — Change constructor to accept repositories:
```kotlin
class StateViewModel(
    private val dataStore: QingDataStore,
    private val demoModeManager: DemoModeManager,
    private val achievementRepository: AchievementRepository
) : ViewModel() {
```

Replace `dataStore.allAchievements` → `achievementRepository.getAll()`
Replace `dataStore.unlockedCount` → `achievementRepository.getUnlockedCount()`
Replace `dataStore.unlockAchievement(id)` → `achievementRepository.unlock(id)`
Replace `dataStore.lockAchievement(id)` → `achievementRepository.lock(id)`
Replace `dataStore.getMessagesForSession(id)` → `chatRepository.getMessagesForSessionOnce(id)`

Update Factory:
```kotlin
    class Factory(
        private val dataStore: QingDataStore,
        private val demoModeManager: DemoModeManager,
        private val achievementRepository: AchievementRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StateViewModel::class.java)) {
                return StateViewModel(dataStore, demoModeManager, achievementRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
```

**ChatViewModel** — Change constructor to accept ChatRepository:
```kotlin
class ChatViewModel(
    private val dataStore: QingDataStore,
    private val chatRepository: ChatRepository,
    private val stateViewModel: StateViewModel? = null,
    private val demoModeManager: DemoModeManager? = null
) : ViewModel() {
```

Replace all `dataStore.createSession(...)` → `chatRepository.createSession(...)`
Replace all `dataStore.addMessage(...)` → `chatRepository.addMessage(...)`
Replace all `dataStore.updateMessageContent(...)` → `chatRepository.updateMessageContent(...)`
Replace all `dataStore.deleteMessage(...)` → `chatRepository.deleteMessage(...)`
Replace all `dataStore.getMessagesForSession(...)` → `chatRepository.getMessagesForSession(...)`

Update Factory similarly.

**MoodViewModel** — Change constructor to accept MoodRepository:
Replace all `dataStore.moodEntries` → `moodRepository.getAllEntries()`
Replace all `dataStore.latestMood` → `moodRepository.getLatestEntry()`
Replace all `dataStore.addMoodEntry(...)` → `moodRepository.addEntry(...)`
Replace all `dataStore.deleteMoodEntry(...)` → `moodRepository.deleteEntry(...)`
Replace all `dataStore.updateMoodEntry(...)` → `moodRepository.updateEntry(...)`

Update Factory similarly.

**HomeViewModel** — Update to use repositories for any collection access.

- [ ] **Step 5: Update Navigation.kt and MainActivity.kt**

Update the ViewModel Factory calls in `MainActivity.kt` to pass repositories:

```kotlin
    val stateViewModel: StateViewModel = viewModel(
        factory = StateViewModel.Factory(application.dataStore, application.demoModeManager, application.achievementRepository)
    )
    val chatViewModel: ChatViewModel = viewModel(
        factory = ChatViewModel.Factory(application.dataStore, application.chatRepository, stateViewModel, application.demoModeManager)
    )
    val moodViewModel: MoodViewModel = viewModel(
        factory = MoodViewModel.Factory(application.dataStore, application.moodRepository, application.demoModeManager)
    )
```

Update `Navigation.kt` — HistoryScreen needs `chatRepository` instead of `dataStore`:
```kotlin
        composable(Screen.History.route) {
            HistoryScreen(
                onBackClick = { navController.popBackStack() },
                onSessionClick = { session ->
                    chatViewModel.loadSession(session)
                    navController.navigate(Screen.Chat.route)
                },
                stateViewModel = stateViewModel,
                chatRepository = application.chatRepository,
                demoModeManager = application.demoModeManager
            )
        }
```

- [ ] **Step 6: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

If there are remaining compilation errors, fix them one by one.

- [ ] **Step 7: Commit**

```bash
git add -A
git commit -m "refactor: wire Room repositories into ViewModels and Application"
```

---

### Task 1.9: Fix PsychologyDimension Color→Long and Update Consumers

**Files:**
- Modify: `app/src/main/java/com/onmi/qing/data/PsychologyDimension.kt`
- Modify: `app/src/main/java/com/onmi/qing/viewmodel/StateViewModel.kt`
- Modify: Any screen that reads `PsychologyDimension.color`

- [ ] **Step 1: Update PsychologyDimension to use Long**

```kotlin
package com.onmi.qing.data

// 心理学维度数据类
data class PsychologyDimension(
    val name: String,
    val englishName: String,
    val progress: Float,
    val colorHex: Long  // was Color, now Long for serialization safety
)
```

- [ ] **Step 2: Update StateViewModel.getAllDimensions()**

```kotlin
    fun getAllDimensions(): List<PsychologyDimension> {
        val dims = psychologyDimensions.value
        return listOf(
            PsychologyDimension("情绪稳定", "Mood Stability", dims.moodStability, 0xFF10B981),
            PsychologyDimension("自我认知", "Self-Awareness", dims.selfAwareness, 0xFF3B82F6),
            PsychologyDimension("压力管理", "Stress Management", dims.stressManagement, 0xFFF59E0B),
            PsychologyDimension("社交信心", "Social Confidence", dims.socialConfidence, 0xFF8B5CF6),
            PsychologyDimension("睡眠质量", "Sleep Quality", dims.sleepQuality, 0xFF06B6D4),
            PsychologyDimension("自我关怀", "Self-Care", dims.selfCare, 0xFFEC4899)
        )
    }
```

- [ ] **Step 3: Update all consumers of PsychologyDimension.color**

Search for `.color` on PsychologyDimension instances and replace with `Color(dim.colorHex)`:

```bash
grep -rn "\.color" app/src/main/java/com/onmi/qing/ --include="*.kt" | grep -i "dimension\|psychology"
```

Update each occurrence to use `Color(dim.colorHex)` instead of `dim.color`.

- [ ] **Step 4: Verify build**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

- [ ] **Step 5: Commit**

```bash
git add -A
git commit -m "refactor: change PsychologyDimension.color from Color to Long"
```

---

### Task 1.10: Final Cleanup and Verification

**Files:**
- Various files with minor fixes

- [ ] **Step 1: Remove unused imports**

Search for any remaining imports of removed classes:
```bash
grep -rn "import com.onmi.qing.data.toEntity\|import com.onmi.qing.data.toMoodEntry\|import com.onmi.qing.data.MoodEntryEntity" app/src/main/java/ --include="*.kt"
```

Remove any that are found.

- [ ] **Step 2: Verify no remaining references to removed QingDataStore methods**

```bash
grep -rn "dataStore\.allSessions\|dataStore\.addMessage\|dataStore\.allAchievements\|dataStore\.moodEntries\|dataStore\.createSession\|dataStore\.getMessagesForSession\|dataStore\.unlockAchievement\|dataStore\.deleteSession\|dataStore\.deleteMessage\|dataStore\.addMoodEntry\|dataStore\.deleteMoodEntry\|dataStore\.updateMoodEntry\|dataStore\.lockAchievement\|dataStore\.resetAllAchievements" app/src/main/java/ --include="*.kt"
```

All should be replaced with repository calls.

- [ ] **Step 3: Full build and test**

Run: `./gradlew assembleDebug`
Expected: BUILD SUCCESSFUL

Run: `./gradlew test`
Expected: All unit tests pass

- [ ] **Step 4: Final commit**

```bash
git add -A
git commit -m "chore: Phase 1 complete — Room data layer with migration"
```
