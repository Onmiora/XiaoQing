package com.onmi.qing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onmi.qing.data.Achievement
import com.onmi.qing.data.UsageStatsManager
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.data.repository.AchievementRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AchievementViewModel @Inject constructor(
    private val achievementRepository: AchievementRepository,
    private val dataStore: QingDataStore,
    private val demoModeManager: DemoModeManager,
    private val usageStatsManager: UsageStatsManager
) : ViewModel() {

    val achievements: StateFlow<List<Achievement>> = combine(
        demoModeManager.isDemoMode,
        demoModeManager.demoAchievements,
        achievementRepository.getAll()
    ) { isDemo, demoAchievements, userAchievements ->
        if (isDemo) demoAchievements else userAchievements
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unlockedCount: StateFlow<Int> = combine(
        demoModeManager.isDemoMode,
        demoModeManager.demoAchievements,
        achievementRepository.getUnlockedCount()
    ) { isDemo, demoAchievements, userCount ->
        if (isDemo) demoAchievements.count { it.isUnlocked } else userCount
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun unlockAchievement(achievementId: String) {
        if (demoModeManager.isDemoMode.value) return
        viewModelScope.launch {
            achievementRepository.unlock(achievementId)
        }
    }

    fun lockAchievement(achievementId: String) {
        if (demoModeManager.isDemoMode.value) return
        viewModelScope.launch {
            achievementRepository.lock(achievementId)
        }
    }

    fun resetAllAchievements() {
        if (demoModeManager.isDemoMode.value) return
        viewModelScope.launch {
            achievementRepository.resetAll()
        }
    }

    fun checkAndUnlockAchievements() {
        if (demoModeManager.isDemoMode.value) return

        viewModelScope.launch {
            val stats = dataStore.usageStats.first()
            val dims = dataStore.psychologyDimensions.first()

            // 签到类
            if (stats.checkInCount >= 1) unlockAchievement("checkin_3")
            if (stats.checkInCount >= 7) unlockAchievement("checkin_7")
            if (stats.checkInCount >= 30) unlockAchievement("checkin_30")

            // 聊天类
            if (stats.chatCount >= 10) unlockAchievement("chat_10")
            if (stats.chatCount >= 50) unlockAchievement("chat_50")
            if (stats.chatCount >= 100) unlockAchievement("chat_100")

            // 呼吸类
            if (stats.breathingCount >= 5) unlockAchievement("breathing_5")
            if (stats.breathingCount >= 20) unlockAchievement("breathing_20")
            if (stats.breathingCount >= 50) unlockAchievement("breathing_50")

            // 心理维度
            if (dims.moodStability > 0.7f) unlockAchievement("mood_good_7")
            if (dims.sleepQuality > 0.7f) unlockAchievement("sleep_good_7")
            if (dims.stressManagement > 0.7f) unlockAchievement("stress_low_7")

            // 全面发展
            if (dims.moodStability > 0.7f && dims.selfAwareness > 0.7f &&
                dims.stressManagement > 0.7f && dims.socialConfidence > 0.7f &&
                dims.sleepQuality > 0.7f && dims.selfCare > 0.7f) {
                unlockAchievement("all_round")
            }

            // 完美一天
            if (dataStore.isTodayPerfectDay()) {
                unlockAchievement("perfect_day")
            }
        }
    }

    fun getUnlockedCount(): Int = unlockedCount.value
    fun getTotalCount(): Int = achievements.value.size
}
