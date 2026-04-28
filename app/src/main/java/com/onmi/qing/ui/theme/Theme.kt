package com.onmi.qing.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// 科技感浅色主题
private val TechLightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF1E1B4B),

    secondary = SecondaryLight,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFCCFBF1),
    onSecondaryContainer = Color(0xFF052E16),

    tertiary = TertiaryLight,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E8FF),
    onTertiaryContainer = Color(0xFF2E1065),

    error = ErrorLight,
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = Color(0xFF4A0000),

    background = BackgroundLight,
    onBackground = Color(0xFF0F172A),

    surface = SurfaceLight,
    onSurface = Color(0xFF0F172A),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF475569),

    outline = Color(0xFF94A3B8),
    outlineVariant = Color(0xFFCBD5E1)
)

// 科技感深色主题
private val TechDarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = Color(0xFF1E1B4B),
    primaryContainer = Color(0xFF3730A3),
    onPrimaryContainer = Color(0xFFE0E7FF),

    secondary = SecondaryDark,
    onSecondary = Color(0xFF052E16),
    secondaryContainer = Color(0xFF0F766E),
    onSecondaryContainer = Color(0xFFCCFBF1),

    tertiary = TertiaryDark,
    onTertiary = Color(0xFF2E1065),
    tertiaryContainer = Color(0xFF581C87),
    onTertiaryContainer = Color(0xFFF3E8FF),

    error = ErrorDark,
    onError = Color(0xFF4A0000),
    errorContainer = Color(0xFF6A1010),
    onErrorContainer = Color(0xFFFFEBEE),

    background = BackgroundDark,
    onBackground = Color(0xFFF8FAFC),

    surface = SurfaceDark,
    onSurface = Color(0xFFF8FAFC),
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = Color(0xFF94A3B8),

    outline = Color(0xFF64748B),
    outlineVariant = Color(0xFF334155)
)

@Composable
fun QingTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+ - 默认开启保留用户偏好
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Android 12+ 使用动态取色（用户壁纸颜色）
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // 深色主题 - 科技感深色
        darkTheme -> TechDarkColorScheme
        // 浅色主题 - 科技感浅色
        else -> TechLightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // 设置状态栏颜色
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = QingTypography,
        shapes = QingShapes,
        content = content
    )
}