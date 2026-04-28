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

// 心情记录实体类（用于JSON序列化）
data class MoodEntryEntity(
    val id: String,
    val mood: String,
    val reason: String,
    val timestamp: Long
)

// MoodEntry 到 MoodEntryEntity 转换
fun MoodEntry.toEntity() = MoodEntryEntity(
    id = id,
    mood = mood.name,
    reason = reason,
    timestamp = timestamp
)

// MoodEntryEntity 到 MoodEntry 转换
fun MoodEntryEntity.toMoodEntry() = MoodEntry(
    id = id,
    mood = MoodType.valueOf(mood),
    reason = reason,
    timestamp = timestamp
)
