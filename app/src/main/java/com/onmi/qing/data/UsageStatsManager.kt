package com.onmi.qing.data

import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.datastore.UsageStats
import com.onmi.qing.data.demo.DemoModeManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageStatsManager @Inject constructor(
    private val dataStore: QingDataStore,
    private val demoModeManager: DemoModeManager
) {
    val usageStats: Flow<UsageStats> = combine(
        demoModeManager.isDemoMode,
        demoModeManager.usageStats,
        dataStore.usageStats
    ) { isDemo, demoStats, userStats ->
        if (isDemo) demoStats else userStats
    }

    suspend fun incrementChatCount() {
        if (demoModeManager.isDemoMode.value) return
        dataStore.incrementChatCount()
        dataStore.resetDailyActivitiesIfNewDay()
        dataStore.markTodayChat()
    }

    suspend fun incrementBreathingCount() {
        if (demoModeManager.isDemoMode.value) return
        dataStore.incrementBreathingCount()
        dataStore.resetDailyActivitiesIfNewDay()
        dataStore.markTodayBreathing()
    }

    suspend fun incrementCheckInCount(hour: Int = -1) {
        if (demoModeManager.isDemoMode.value) return
        dataStore.incrementCheckInCount()
        dataStore.resetDailyActivitiesIfNewDay()
        dataStore.markTodayCheckin()
        if (hour in 0..7) {
            dataStore.checkAndUnlockEarlyBird(hour)
        }
    }
}
