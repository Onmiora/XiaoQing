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
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao
    abstract fun moodDao(): MoodDao
    abstract fun achievementDao(): AchievementDao
}
