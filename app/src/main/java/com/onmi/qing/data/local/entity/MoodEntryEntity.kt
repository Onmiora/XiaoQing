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
