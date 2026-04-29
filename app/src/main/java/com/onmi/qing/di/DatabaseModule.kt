package com.onmi.qing.di

import android.content.Context
import androidx.room.Room
import com.onmi.qing.data.local.AppDatabase
import com.onmi.qing.data.local.dao.AchievementDao
import com.onmi.qing.data.local.dao.ChatDao
import com.onmi.qing.data.local.dao.MoodDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "qing_database"
        ).build()
    }

    @Provides
    fun provideChatDao(database: AppDatabase): ChatDao {
        return database.chatDao()
    }

    @Provides
    fun provideMoodDao(database: AppDatabase): MoodDao {
        return database.moodDao()
    }

    @Provides
    fun provideAchievementDao(database: AppDatabase): AchievementDao {
        return database.achievementDao()
    }
}
