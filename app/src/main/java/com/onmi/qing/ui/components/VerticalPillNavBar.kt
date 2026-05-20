package com.onmi.qing.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
fun VerticalPillNavBar(
    visible: Boolean,
    currentRoute: String,
    navItems: List<FloatingNavItem>,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    // Expand/collapse state
    var isExpanded by remember { mutableStateOf(false) }

    // Auto-collapse after idle
    LaunchedEffect(isExpanded) {
        if (isExpanded) {
            delay(3000) // Wait 3s before collapsing
            isExpanded = false
        }
    }

    val collapsedWidth = 64.dp
    val expandedWidth = 120.dp
    val pillWidth by animateDpAsState(
        targetValue = if (isExpanded) expandedWidth else collapsedWidth,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "pillWidth"
    )

    val itemHeight = 56.dp
    val toggleButtonHeight = 40.dp
    val totalHeight = (itemHeight * navItems.size) + toggleButtonHeight + 24.dp

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(
            initialOffsetX = { -it },
            animationSpec = tween(durationMillis = 300)
        ),
        exit = slideOutHorizontally(
            targetOffsetX = { -it },
            animationSpec = tween(durationMillis = 300)
        ),
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(pillWidth + 24.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Outer shadow
            Box(
                modifier = Modifier
                    .shadow(
                        elevation = 24.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = Color.Black.copy(alpha = if (isDarkTheme) 0.4f else 0.15f),
                        spotColor = Color.Black.copy(alpha = if (isDarkTheme) 0.6f else 0.25f)
                    )
            ) {
                // Frosted glass background
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(totalHeight)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            brush = Brush.horizontalGradient(
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

                // Highlight strip
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(totalHeight)
                        .clip(RoundedCornerShape(32.dp))
                        .background(
                            brush = Brush.horizontalGradient(
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

                // Border
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(totalHeight)
                        .clip(RoundedCornerShape(32.dp))
                        .border(
                            width = 1.dp,
                            brush = Brush.horizontalGradient(
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

                // Inner background
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(totalHeight)
                        .padding(1.dp)
                        .clip(RoundedCornerShape(31.dp))
                        .background(
                            brush = Brush.horizontalGradient(
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

                // Content container
                Column(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(totalHeight)
                        .padding(horizontal = 8.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Toggle button at top
                    ExpandToggleButton(
                        isExpanded = isExpanded,
                        isDarkTheme = isDarkTheme,
                        onClick = { isExpanded = !isExpanded }
                    )

                    // Nav items
                    navItems.forEach { item ->
                        VerticalNavItemButton(
                            item = item,
                            selected = currentRoute == item.route,
                            isDarkTheme = isDarkTheme,
                            isExpanded = isExpanded,
                            onClick = {
                                onNavigate(item.route)
                                // Collapse after navigation
                                isExpanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ExpandToggleButton(
    isExpanded: Boolean,
    isDarkTheme: Boolean,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.9f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    val contentColor = if (isDarkTheme) {
        Color.White.copy(alpha = 0.75f)
    } else {
        Color.Black.copy(alpha = 0.65f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(40.dp)
            .scale(scale)
            .clip(RoundedCornerShape(20.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isExpanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
            contentDescription = if (isExpanded) "收起" else "展开",
            modifier = Modifier.size(20.dp),
            tint = contentColor
        )
    }
}

@Composable
private fun VerticalNavItemButton(
    item: FloatingNavItem,
    selected: Boolean,
    isDarkTheme: Boolean,
    isExpanded: Boolean,
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

    val selectedBackground = when {
        selected && isDarkTheme -> Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
            )
        )
        selected -> Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
            )
        )
        else -> Brush.verticalGradient(listOf(Color.Transparent, Color.Transparent))
    }

    val selectedBorder = if (selected) {
        1.dp
    } else {
        0.dp
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(scale)
            .clip(RoundedCornerShape(28.dp))
            .background(selectedBackground)
            .then(
                if (selected) {
                    Modifier.border(
                        width = selectedBorder,
                        brush = Brush.verticalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            )
                        ),
                        shape = RoundedCornerShape(28.dp)
                    )
                } else {
                    Modifier
                }
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = if (isExpanded) Arrangement.spacedBy(10.dp) else Arrangement.Center
    ) {
        Icon(
            imageVector = if (selected) item.selectedIcon else item.unselectedIcon,
            contentDescription = item.title,
            modifier = Modifier
                .size(if (selected) 22.dp else 20.dp)
                .scale(iconScale),
            tint = contentColor
        )

        // Show text only when expanded
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.labelMedium,
                color = contentColor,
                maxLines = 1
            )
        }
    }
}
