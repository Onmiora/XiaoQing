package com.onmi.qing.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.onmi.qing.data.local.entity.MoodEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MoodDao {
    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC")
    fun getAllEntries(): Flow<List<MoodEntryEntity>>

    @Query("SELECT * FROM mood_entries ORDER BY timestamp DESC LIMIT 1")
    fun getLatestEntry(): Flow<MoodEntryEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: MoodEntryEntity)

    @Update
    suspend fun update(entry: MoodEntryEntity)

    @Delete
    suspend fun delete(entry: MoodEntryEntity)

    @Query("SELECT * FROM mood_entries WHERE id = :entryId LIMIT 1")
    suspend fun getById(entryId: String): MoodEntryEntity?

    @Query("DELETE FROM mood_entries WHERE id = :entryId")
    suspend fun deleteById(entryId: String)

    @Query("SELECT COUNT(*) FROM mood_entries")
    suspend fun getEntryCount(): Int
}
