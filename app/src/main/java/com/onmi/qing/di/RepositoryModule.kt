package com.onmi.qing.di

import com.onmi.qing.data.local.dao.AchievementDao
import com.onmi.qing.data.local.dao.ChatDao
import com.onmi.qing.data.local.dao.MoodDao
import com.onmi.qing.data.repository.AchievementRepository
import com.onmi.qing.data.repository.ChatRepository
import com.onmi.qing.data.repository.MoodRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideChatRepository(chatDao: ChatDao): ChatRepository {
        return ChatRepository(chatDao)
    }

    @Provides
    @Singleton
    fun provideMoodRepository(moodDao: MoodDao): MoodRepository {
        return MoodRepository(moodDao)
    }

    @Provides
    @Singleton
    fun provideAchievementRepository(achievementDao: AchievementDao): AchievementRepository {
        return AchievementRepository(achievementDao)
    }
}
