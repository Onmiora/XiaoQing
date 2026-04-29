package com.onmi.qing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onmi.qing.data.UsageStatsManager
import com.onmi.qing.data.datastore.UsageStats
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UsageStatsViewModel @Inject constructor(
    private val usageStatsManager: UsageStatsManager
) : ViewModel() {

    val usageStats: StateFlow<UsageStats> = usageStatsManager.usageStats
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UsageStats())

    val chatCount: StateFlow<Int> = usageStats.map { it.chatCount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val breathingCount: StateFlow<Int> = usageStats.map { it.breathingCount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val checkInCount: StateFlow<Int> = usageStats.map { it.checkInCount }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    fun incrementBreathingCount() {
        viewModelScope.launch {
            usageStatsManager.incrementBreathingCount()
        }
    }

    fun incrementChatCount() {
        viewModelScope.launch {
            usageStatsManager.incrementChatCount()
        }
    }

    fun incrementCheckInCount(hour: Int = -1) {
        viewModelScope.launch {
            usageStatsManager.incrementCheckInCount(hour)
        }
    }
}
