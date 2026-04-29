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
