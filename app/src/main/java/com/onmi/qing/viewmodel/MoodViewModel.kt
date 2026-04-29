package com.onmi.qing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
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
@HiltViewModel
class MoodViewModel @Inject constructor(
    private val dataStore: QingDataStore,
    private val moodRepository: MoodRepository,
    private val demoModeManager: DemoModeManager
) : ViewModel() {

    // 是否为演示模式
    private val isDemoMode: Boolean
        get() = demoModeManager.isDemoMode.value

    // 所有心情记录列表 - 根据模式切换数据源
    val moodEntries: StateFlow<List<MoodEntry>> = combine(
        demoModeManager.isDemoMode,
        demoModeManager.demoMoodEntries,
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
        demoModeManager.isDemoMode,
        demoModeManager.demoMoodEntries,
        moodRepository.getLatestEntry()
    ) { isDemo, demoEntries, userLatest ->
        if (isDemo) demoEntries.firstOrNull() else userLatest
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        null
    )

    // 演示模式下内存中的心情记录
    private val _demoMoodEntriesInternal = demoModeManager.demoMoodEntries

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

}
