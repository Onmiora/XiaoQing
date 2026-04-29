package com.onmi.qing.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.onmi.qing.data.PsychologyDimension
import com.onmi.qing.data.datastore.PsychologyDimensions
import com.onmi.qing.data.datastore.QingDataStore
import com.onmi.qing.data.demo.DemoModeManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PsychologyViewModel @Inject constructor(
    private val dataStore: QingDataStore,
    private val demoModeManager: DemoModeManager
) : ViewModel() {

    enum class DimensionType(val key: String, val chineseName: String) {
        MoodStability("moodStability", "情绪稳定"),
        SelfAwareness("selfAwareness", "自我认知"),
        StressManagement("stressManagement", "压力管理"),
        SocialConfidence("socialConfidence", "社交信心"),
        SleepQuality("sleepQuality", "睡眠质量"),
        SelfCare("selfCare", "自我关怀")
    }

    val psychologyDimensions: StateFlow<PsychologyDimensions> = combine(
        demoModeManager.isDemoMode,
        demoModeManager.psychologyDimensions,
        dataStore.psychologyDimensions
    ) { isDemo, demoDims, userDims ->
        if (isDemo) demoDims else userDims
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PsychologyDimensions())

    fun boostDimension(dimension: DimensionType, delta: Float) {
        if (demoModeManager.isDemoMode.value) return
        viewModelScope.launch {
            val current = getCurrentValue(dimension)
            dataStore.updatePsychologyDimension(dimension.key, (current + delta).coerceIn(0f, 1f))
        }
    }

    fun setDimensionScore(dimension: DimensionType, value: Float) {
        if (demoModeManager.isDemoMode.value) return
        viewModelScope.launch {
            dataStore.updatePsychologyDimension(dimension.key, value.coerceIn(0f, 1f))
        }
    }

    fun updateDimensions(updates: Map<DimensionType, Float>) {
        if (demoModeManager.isDemoMode.value) return
        viewModelScope.launch {
            for ((dimension, value) in updates) {
                dataStore.updatePsychologyDimension(dimension.key, value.coerceIn(0f, 1f))
            }
        }
    }

    fun resetDimensions() {
        if (demoModeManager.isDemoMode.value) return
        viewModelScope.launch {
            dataStore.resetPsychologyDimensions()
        }
    }

    fun setDimensionProgress(dimensionKey: String, progress: Float) {
        if (demoModeManager.isDemoMode.value) return
        viewModelScope.launch {
            dataStore.updatePsychologyDimension(dimensionKey, progress.coerceIn(0f, 1f))
        }
    }

    fun getAllDimensions(): List<PsychologyDimension> {
        val dims = psychologyDimensions.value
        return listOf(
            PsychologyDimension("情绪稳定", "Mood Stability", dims.moodStability, 0xFF10B981),
            PsychologyDimension("自我认知", "Self-Awareness", dims.selfAwareness, 0xFF3B82F6),
            PsychologyDimension("压力管理", "Stress Management", dims.stressManagement, 0xFFF59E0B),
            PsychologyDimension("社交信心", "Social Confidence", dims.socialConfidence, 0xFF8B5CF6),
            PsychologyDimension("睡眠质量", "Sleep Quality", dims.sleepQuality, 0xFF06B6D4),
            PsychologyDimension("自我关怀", "Self-Care", dims.selfCare, 0xFFEC4899)
        )
    }

    private fun getCurrentValue(dimension: DimensionType): Float {
        val dims = psychologyDimensions.value
        return when (dimension) {
            DimensionType.MoodStability -> dims.moodStability
            DimensionType.SelfAwareness -> dims.selfAwareness
            DimensionType.StressManagement -> dims.stressManagement
            DimensionType.SocialConfidence -> dims.socialConfidence
            DimensionType.SleepQuality -> dims.sleepQuality
            DimensionType.SelfCare -> dims.selfCare
        }
    }
}
