package com.onmi.qing.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 环形进度组件 - Material Design 3，使用 Material 3 标准的 CircularProgressIndicator
@Composable
fun GlowProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 80.dp,
    strokeWidth: Dp = 8.dp,
    glowColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.primary,
    trackColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
    content: @Composable () -> Unit = {}
) {
    // 使用动画使进度变化更平滑
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "progress_animation"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier.size(size),
            color = glowColor,
            strokeWidth = strokeWidth,
            trackColor = trackColor,
            strokeCap = StrokeCap.Round
        )
        content()
    }
}

// 进度条 - Material Design 3，使用 Material 3 标准的 LinearProgressIndicator，支持动画
@Composable
fun GradientProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp,
    gradientColors: List<androidx.compose.ui.graphics.Color>? = null
) {
    // 使用主题色而非硬编码渐变
    val barColor = gradientColors?.firstOrNull() ?: MaterialTheme.colorScheme.primary

    // 使用动画使进度变化更平滑
    val animatedProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 300),
        label = "progress_animation"
    )

    LinearProgressIndicator(
        progress = { animatedProgress },
        modifier = modifier,
        color = barColor,
        trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        strokeCap = StrokeCap.Round
    )
}