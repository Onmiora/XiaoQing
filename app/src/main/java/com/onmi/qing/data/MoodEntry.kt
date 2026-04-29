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
