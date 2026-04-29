package com.onmi.qing

import android.app.Application
import androidx.room.Room
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.data.local.AppDatabase
import com.onmi.qing.data.repository.AchievementRepository
import com.onmi.qing.data.repository.ChatRepository
import com.onmi.qing.data.repository.MoodRepository
import com.onmi.qing.data.local.DataMigration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QingApplication : Application() {

    lateinit var dataStore: QingDataStore
        private set

    lateinit var demoModeManager: DemoModeManager
        private set

    lateinit var database: AppDatabase
        private set

    lateinit var chatRepository: ChatRepository
        private set

    lateinit var moodRepository: MoodRepository
        private set

    lateinit var achievementRepository: AchievementRepository
        private set

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        dataStore = QingDataStore(this)
        demoModeManager = DemoModeManager(this)

        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "qing_database"
        ).build()

        chatRepository = ChatRepository(database.chatDao())
        moodRepository = MoodRepository(database.moodDao())
        achievementRepository = AchievementRepository(database.achievementDao())

        // Initialize default achievements and run migration
        applicationScope.launch {
            achievementRepository.initializeDefaults()
            DataMigration.migrateIfNeeded(dataStore, chatRepository, moodRepository, achievementRepository)
        }
    }
}
