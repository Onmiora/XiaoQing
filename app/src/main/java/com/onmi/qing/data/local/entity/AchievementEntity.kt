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
