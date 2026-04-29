package com.onmi.qing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.onmi.qing.data.PsychologyDimension
import com.onmi.qing.data.datastore.QingDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// 首页 ViewModel - 管理用户心理状态数据
class HomeViewModel(
    private val dataStore: QingDataStore
) : ViewModel() {

    // Psychology dimensions from DataStore
    val psychologyDimensions = dataStore.psychologyDimensions
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            com.onmi.qing.data.datastore.PsychologyDimensions()
        )

    // 获取所有心理维度列表（响应式）
    val allDimensions: StateFlow<List<PsychologyDimension>> = psychologyDimensions
        .map { dims ->
            listOf(
                PsychologyDimension("情绪稳定", "Mood Stability", dims.moodStability, 0xFF4CAF50),
                PsychologyDimension("自我认知", "Self-Awareness", dims.selfAwareness, 0xFF2196F3),
                PsychologyDimension("压力管理", "Stress Management", dims.stressManagement, 0xFFFF9800),
                PsychologyDimension("社交信心", "Social Confidence", dims.socialConfidence, 0xFF9C27B0),
                PsychologyDimension("睡眠质量", "Sleep Quality", dims.sleepQuality, 0xFF00BCD4),
                PsychologyDimension("自我关怀", "Self-Care", dims.selfCare, 0xFFE91E63)
            )
        }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    // 更新某个维度的进度
    fun updateDimension(dimension: String, progress: Float) {
        viewModelScope.launch {
            dataStore.updatePsychologyDimension(dimension, progress.coerceIn(0f, 1f))
        }
    }

    class Factory(
        private val dataStore: QingDataStore
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(dataStore) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
