package com.onmi.qing.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// 返回自适应水平内边距
@Composable
fun adaptiveHorizontalPadding(): Dp {
    val screenWidth = LocalConfiguration.current.screenWidthDp
    return when {
        screenWidth < 600 -> 20.dp   // Compact
        screenWidth < 840 -> 32.dp   // Medium
        else -> 48.dp                // Expanded
    }
}

// 判断当前窗口宽度是否为紧凑（手机）
@Composable
fun isCompactWidth(): Boolean {
    return LocalConfiguration.current.screenWidthDp < 600
}
