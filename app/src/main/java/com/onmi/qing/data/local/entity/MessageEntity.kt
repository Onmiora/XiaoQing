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
