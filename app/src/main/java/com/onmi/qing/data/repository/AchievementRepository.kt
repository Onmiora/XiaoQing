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
