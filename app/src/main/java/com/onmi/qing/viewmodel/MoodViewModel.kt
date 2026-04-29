package com.onmi.qing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onmi.qing.data.MoodEntry
import com.onmi.qing.data.MoodType
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.demo.DemoModeManager
import com.onmi.qing.data.repository.MoodRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 心情日记 ViewModel
class MoodViewModel(
    private val dataStore: QingDataStore,
    private val moodRepository: MoodRepository,
    private val demoModeManager: DemoModeManager? = null
) : ViewModel() {

    // 是否为演示模式
    private val isDemoMode: Boolean
        get() = demoModeManager?.isDemoMode?.value == true

    // 所有心情记录列表 - 根据模式切换数据源
    val moodEntries: StateFlow<List<MoodEntry>> = combine(
        demoModeManager?.isDemoMode ?: kotlinx.coroutines.flow.flowOf(false),
        demoModeManager?.demoMoodEntries ?: kotlinx.coroutines.flow.flowOf(emptyList()),
        moodRepository.getAllEntries()
    ) { isDemo, demoEntries, userEntries ->
        if (isDemo) demoEntries else userEntries
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        emptyList()
    )

    // 最新一条心情记录 - 根据模式切换数据源
    val latestMood: StateFlow<MoodEntry?> = combine(
        demoModeManager?.isDemoMode ?: kotlinx.coroutines.flow.flowOf(false),
        demoModeManager?.demoMoodEntries ?: kotlinx.coroutines.flow.flowOf(emptyList()),
        moodRepository.getLatestEntry()
    ) { isDemo, demoEntries, userLatest ->
        if (isDemo) demoEntries.firstOrNull() else userLatest
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    // 演示模式下内存中的心情记录
    private val _demoMoodEntriesInternal = demoModeManager?.demoMoodEntries
        ?: kotlinx.coroutines.flow.flowOf(emptyList())

// 添加心情记录
    fun addMoodEntry(mood: MoodType, reason: String) {
        viewModelScope.launch {
            if (isDemoMode) {
                // 演示模式下不保存
                return@launch
            }
            moodRepository.addEntry(mood, reason)
        }
    }

// 删除心情记录
    fun deleteMoodEntry(entryId: String) {
        viewModelScope.launch {
            if (isDemoMode) {
                // 演示模式下不保存
                return@launch
            }
            moodRepository.deleteEntry(entryId)
        }
    }

// 更新心情记录
    fun updateMoodEntry(entryId: String, mood: MoodType, reason: String) {
        viewModelScope.launch {
            if (isDemoMode) {
                // 演示模式下不保存
                return@launch
            }
            moodRepository.updateEntry(entryId, mood, reason)
        }
    }

    class Factory(
        private val dataStore: QingDataStore,
        private val moodRepository: MoodRepository,
        private val demoModeManager: DemoModeManager? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MoodViewModel::class.java)) {
                return MoodViewModel(dataStore, moodRepository, demoModeManager) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
