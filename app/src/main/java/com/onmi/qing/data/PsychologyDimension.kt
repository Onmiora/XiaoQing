package com.onmi.qing.data

// 心理学维度数据类
data class PsychologyDimension(
    val name: String,
    val englishName: String,
    val progress: Float,
    val colorHex: Long  // was Color, now Long for serialization safety
)