package com.onmi.qing.data.local

import com.onmi.qing.data.Achievement
import com.onmi.qing.data.ChatMessage
import com.onmi.qing.data.ChatSession
import com.onmi.qing.data.MessagePart
import com.onmi.qing.data.MessageRole
import com.onmi.qing.data.MoodEntry
import com.onmi.qing.data.MoodType
import com.onmi.qing.data.local.entity.AchievementEntity
import com.onmi.qing.data.local.entity.ChatSessionEntity
import com.onmi.qing.data.local.entity.MessageEntity
import com.onmi.qing.data.local.entity.MoodEntryEntity
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import java.lang.reflect.Type

private val messagePartAdapter = object : JsonSerializer<MessagePart>, JsonDeserializer<MessagePart> {
    override fun serialize(src: MessagePart, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
        return context.serialize(src)
    }

    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): MessagePart {
        val obj = json.asJsonObject
        // Gson serializes Kotlin sealed classes with a "type" field based on the class name
        // We detect by checking which fields are present
        return when {
            obj.has("thinking") -> MessagePart.Thinking(
                thinking = obj.get("thinking").asString,
                durationMs = if (obj.has("durationMs") && !obj.get("durationMs").isJsonNull) obj.get("durationMs").asLong else null
            )
            obj.has("text") -> MessagePart.Text(
                text = obj.get("text").asString
            )
            else -> MessagePart.Text(text = json.toString())
        }
    }
}

private val gson: Gson = GsonBuilder()
    .registerTypeAdapter(MessagePart::class.java, messagePartAdapter)
    .create()
private val messagePartListType = object : TypeToken<List<MessagePart>>() {}.type

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

// ChatMessage <-> MessageEntity
fun ChatMessage.toEntity(sessionId: String) = MessageEntity(
    id = id,
    sessionId = sessionId,
    role = role.name,
    partsJson = gson.toJson(parts),
    timestamp = timestamp,
    regenerationIndex = regenerationIndex
)

fun MessageEntity.toDomain() = ChatMessage(
    id = id,
    role = MessageRole.valueOf(role),
    parts = gson.fromJson(partsJson, messagePartListType),
    timestamp = timestamp,
    regenerationIndex = regenerationIndex
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
