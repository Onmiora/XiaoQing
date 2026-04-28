package com.onmi.qing.data.datastore

// 心理学维度数据类
data class PsychologyDimensions(
    val moodStability: Float = 0.50f,
    val selfAwareness: Float = 0.50f,
    val stressManagement: Float = 0.50f,
    val socialConfidence: Float = 0.50f,
    val sleepQuality: Float = 0.50f,
    val selfCare: Float = 0.50f
)
