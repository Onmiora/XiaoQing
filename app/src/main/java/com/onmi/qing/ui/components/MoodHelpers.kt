package com.onmi.qing.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.onmi.qing.data.MoodType
import com.onmi.qing.ui.theme.MoodCalm
import com.onmi.qing.ui.theme.MoodHappy
import com.onmi.qing.ui.theme.MoodUnhappy

fun MoodType?.toMoodColor(): Color = when (this) {
    MoodType.HAPPY -> MoodHappy
    MoodType.CALM -> MoodCalm
    MoodType.UNHAPPY -> MoodUnhappy
    null -> Color.Gray
}

fun MoodType?.toMoodIcon(): ImageVector = when (this) {
    MoodType.HAPPY -> Icons.Filled.SentimentSatisfied
    MoodType.CALM -> Icons.Filled.SentimentNeutral
    MoodType.UNHAPPY -> Icons.Filled.SentimentDissatisfied
    null -> Icons.Filled.SentimentNeutral
}

fun MoodType?.toMoodDisplayName(): String = when (this) {
    MoodType.HAPPY -> "开心"
    MoodType.CALM -> "平静"
    MoodType.UNHAPPY -> "不开心"
    null -> "未记录"
}

fun MoodType.toMoodValue(): Float = when (this) {
    MoodType.HAPPY -> 1.0f
    MoodType.CALM -> 0.5f
    MoodType.UNHAPPY -> 0.0f
}
