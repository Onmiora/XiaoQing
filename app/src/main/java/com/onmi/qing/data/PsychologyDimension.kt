package com.onmi.qing.data

import androidx.compose.ui.graphics.Color

// 心理学维度数据类
data class PsychologyDimension(
    val name: String,
    val englishName: String,
    val progress: Float,
    val color: Color
)