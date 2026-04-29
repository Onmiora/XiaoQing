package com.onmi.qing.data.repository

import com.onmi.qing.data.MoodEntry
import com.onmi.qing.data.MoodType
import com.onmi.qing.data.local.dao.MoodDao
import com.onmi.qing.data.local.entity.MoodEntryEntity
import com.onmi.qing.data.local.toDomain
import com.onmi.qing.data.local.toEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class MoodRepository(private val moodDao: MoodDao) {

    fun getAllEntries(): Flow<List<MoodEntry>> =
        moodDao.getAllEntries().map { list -> list.map { it.toDomain() } }

    fun getLatestEntry(): Flow<MoodEntry?> =
        moodDao.getLatestEntry().map { it?.toDomain() }

    suspend fun addEntry(mood: MoodType, reason: String): MoodEntry {
        val entry = MoodEntry(
            mood = mood,
            reason = reason,
            timestamp = System.currentTimeMillis()
        )
        moodDao.insert(entry.toEntity())
        return entry
    }

    suspend fun updateEntry(entryId: String, mood: MoodType, reason: String) {
        val existing = moodDao.getById(entryId) ?: return
        moodDao.update(existing.copy(mood = mood.name, reason = reason))
    }

    suspend fun deleteEntry(entryId: String) {
        moodDao.deleteById(entryId)
    }

    suspend fun insertDirect(entity: MoodEntryEntity) {
        moodDao.insert(entity)
    }

    suspend fun getEntryCount(): Int = moodDao.getEntryCount()
}
