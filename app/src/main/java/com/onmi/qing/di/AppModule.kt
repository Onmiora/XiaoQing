package com.onmi.qing.di

import android.content.Context
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.demo.DemoModeManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideQingDataStore(@ApplicationContext context: Context): QingDataStore {
        return QingDataStore(context)
    }

    @Provides
    @Singleton
    fun provideDemoModeManager(@ApplicationContext context: Context): DemoModeManager {
        return DemoModeManager(context)
    }
}
