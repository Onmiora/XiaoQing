package com.onmi.qing.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.onmi.qing.ui.navigation.Screen

data class FloatingNavItem(
    val route: String,
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

@Composable
fun FloatingPillNavBar(
    visible: Boolean,
    currentRoute: String,
    navItems: List<FloatingNavItem>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 根据主题自适应颜色
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // 胶囊宽度，参考 OMaster 的 260.dp
    val pillWidth = 260.dp
    val pillHeight = 64.dp

    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            initialOffsetY = { it },
            animationSpec = tween(durationMillis = 300)
        ),
        exit = slideOutVertically(
            targetOffsetY = { it },
            animationSpec = tween(durationMillis = 300)
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            // 外层柔和阴影
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.4f else 0.15f),
                        spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.6f else 0.25f)
                    )
            ) {
                // 磨砂玻璃背景层 - 自适应深浅色
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(pillHeight)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDarkTheme) {
                                    listOf(
                                        Color.Black.copy(alpha = 0.75f),
                                        Color.Black.copy(alpha = 0.65f)
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.85f),
                                        Color.White.copy(alpha = 0.75f)
                                    )
                                }
                            )
                        )
                        .blur(8.dp)
                )

                // 顶部高光线条 - 模拟玻璃反光
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(pillHeight)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDarkTheme) {
                                    listOf(
                                        Color.White.copy(alpha = 0.18f),
                                        Color.White.copy(alpha = 0.06f),
                                        Color.Transparent
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.9f),
                                        Color.White.copy(alpha = 0.5f),
                                        Color.Transparent
                                    )
                                }
                            )
                        )
                )

                // 边框层
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(pillHeight)
                        .clip(RoundedCornerShape(32.dp))
                        .border(
                            width = 1.dp,
                            brush = Brush.verticalGradient(
                                colors = if (isDarkTheme) {
                                    listOf(
                                        Color.White.copy(alpha = 0.25f),
                                        Color.White.copy(alpha = 0.08f)
                                    )
                                } else {
                                    listOf(
                                        Color.Black.copy(alpha = 0.08f),
                                        Color.Black.copy(alpha = 0.04f)
                                    )
                                }
                            ),
                            shape = RoundedCornerShape(32.dp)
                        )
                )

                // 内部背景层
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(pillHeight)
                        .padding(1.dp)
                        .clip(RoundedCornerShape(31.dp))
                        .background(
                            brush = Brush.verticalGradient(
                                colors = if (isDarkTheme) {
                                    listOf(
                                        Color.Black.copy(alpha = 0.8f),
                                        Color.Black.copy(alpha = 0.7f)
                                    )
                                } else {
                                    listOf(
                                        Color.White.copy(alpha = 0.9f),
                                        Color.White.copy(alpha = 0.8f)
                                    )
                                }
                            )
                        )
                )

                // 导航项容器
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(pillHeight)
                        .padding(horizontal = 8.dp, vertical = 8.dp)
                ) {
                    // 滑动选中胶囊背景
                    val selectedIndex = navItems.indexOfFirst { it.route == currentRoute }
                    val itemWidth = 244f / navItems.size // (260dp - 16dp padding) / items
                    val capsuleOffset by animateFloatAsState(
                        targetValue = if (selectedIndex >= 0) selectedIndex * itemWidth else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "capsuleOffset"
                    )

                    // 选中胶囊背景 - 带发光效果
                    if (selectedIndex >= 0) {
                        // 发光层
                        Box(
                            modifier = Modifier
                                .width((244f / navItems.size).dp)
                                .height(48.dp)
                                .offset(x = capsuleOffset.dp + 2.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                            Color.Transparent
                                        )
                                    )
                                )
                        )

                        // 选中胶囊主体
                        Box(
                            modifier = Modifier
                                .width((244f / navItems.size).dp)
                                .height(48.dp)
                                .offset(x = capsuleOffset.dp)
                                .clip(RoundedCornerShape(24.dp))
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = if (isDarkTheme) {
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                            )
                                        } else {
                                            listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                            )
                                        }
                                    )
                                )
                                .border(
                                    width = 1.dp,
                                    brush = Brush.verticalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        )
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                )
                                .background(
                                    brush = Brush.verticalGradient(
                                        colors = if (isDarkTheme) {
                                            listOf(
                                                Color.White.copy(alpha = 0.15f),
                                                Color.Transparent
                                            )
                                        } else {
                                            listOf(
                                                Color.White.copy(alpha = 0.7f),
                                                Color.Transparent
                                            )
                                        }
                                    ),
                                    shape = RoundedCornerShape(24.dp)
                                )
                        )
                    }

                    // 导航按钮
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(pillHeight),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        navItems.forEach { item ->
                            val selected = currentRoute == item.route

                            FloatingNavItemButton(
                                item = item,
                                selected = selected,
                                isDarkTheme = isDarkTheme,
                                onClick = { onNavigate(item.route) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FloatingNavItemButton(
    item: FloatingNavItem,
    selected: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val contentColor = when {
        selected -> MaterialTheme.colorScheme.primary
        isDarkTheme -> Color.White.copy(alpha = 0.75f)
        else -> Color.Black.copy(alpha = 0.65f)
    }

    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    Column(
        modifier = Modifier
            .width(60.dp)
            .height(48.dp)
            .scale(scale)
            .clip(RoundedCornerShape(24.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.title,
            modifier = Modifier
                .size(if (selected) 22.dp else 20.dp)
                .scale(iconScale),
            tint = contentColor
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor
        )
    }
}

// 从 Screen.bottomNavItems 转换为 FloatingNavItem
fun Screen.Companion.toFloatingNavItems(): List<FloatingNavItem> {
    return bottomNavItems.map { screen ->
        FloatingNavItem(
            route = screen.route,
            title = screen.title,
            selectedIcon = screen.selectedIcon,
            unselectedIcon = screen.unselectedIcon
        )
    }
}

// 计算颜色亮度
private fun Color.luminance(): Float {
    val red = this.red
    val green = this.green
    val blue = this.blue
    return 0.299f * red + 0.587f * green + 0.114f * blue
}
