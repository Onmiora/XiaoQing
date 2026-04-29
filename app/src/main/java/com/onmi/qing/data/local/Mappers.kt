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
