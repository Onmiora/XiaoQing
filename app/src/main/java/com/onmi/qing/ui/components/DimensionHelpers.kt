package com.onmi.qing.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SocialDistance
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

fun String.toDimensionColor(): Color = when (this) {
    "情绪稳定" -> Color(0xFF10B981)
    "自我认知" -> Color(0xFF3B82F6)
    "压力管理" -> Color(0xFFF59E0B)
    "社交信心" -> Color(0xFF8B5CF6)
    "睡眠质量" -> Color(0xFF06B6D4)
    "自我关怀" -> Color(0xFFEC4899)
    else -> Color.Gray
}

fun String.toDimensionIcon(): ImageVector = when (this) {
    "情绪稳定" -> Icons.Filled.Mood
    "自我认知" -> Icons.Filled.Psychology
    "压力管理" -> Icons.Filled.Air
    "社交信心" -> Icons.Filled.Favorite
    "睡眠质量" -> Icons.Filled.CheckCircle
    "自我关怀" -> Icons.Filled.EmojiEvents
    else -> Icons.Filled.Psychology
}
