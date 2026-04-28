package com.onmi.qing.ui.screens.mbti

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// 卡片交错入场动画组件
@Composable
private fun AnimatedResultCard(
    index: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    var startAnimation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(index * 80L)
        startAnimation = true
    }

    val alpha by animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "card_alpha"
    )

    val offsetY by animateFloatAsState(
        targetValue = if (startAnimation) 0f else 50f,
        animationSpec = tween(durationMillis = 450, easing = FastOutSlowInEasing),
        label = "card_offset"
    )

    Box(
        modifier = modifier.graphicsLayer {
            this.alpha = alpha
            this.translationY = offsetY
        }
    ) {
        content()
    }
}

// MBTI测试结果页面
@Composable
fun MbtiResultScreen(
    mbtiType: String,
    description: String,
    onRetake: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    var showContent by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        showContent = true
    }

    val contentAlpha by animateFloatAsState(
        targetValue = if (showContent) 1f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "content_alpha"
    )

    val contentOffset by animateFloatAsState(
        targetValue = if (showContent) 0f else 50f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "content_offset"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(24.dp)
    ) {
        // 顶部导航
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "返回"
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "测试结果",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.size(48.dp))
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // MBTI类型图标/图案 - 带缩放动画
            AnimatedResultCard(index = 0) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(if (showContent) 1f else 0.5f)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = mbtiType.take(2),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // MBTI类型全称
            AnimatedResultCard(index = 1) {
                Text(
                    text = getMbtiTypeName(mbtiType),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // MBTI代码
            AnimatedResultCard(index = 2) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "人格代码: $mbtiType",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 人格描述卡片
            AnimatedResultCard(index = 3) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "人格解读",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.5f
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 维度解析卡片
            AnimatedResultCard(index = 4) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "维度解析",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // 四个维度
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            DimensionBadge(
                                letter = mbtiType.getOrNull(0)?.toString() ?: "",
                                fullName = getDimensionFullName("E_I", mbtiType.getOrNull(0)?.toString() ?: "")
                            )
                            DimensionBadge(
                                letter = mbtiType.getOrNull(1)?.toString() ?: "",
                                fullName = getDimensionFullName("S_N", mbtiType.getOrNull(1)?.toString() ?: "")
                            )
                            DimensionBadge(
                                letter = mbtiType.getOrNull(2)?.toString() ?: "",
                                fullName = getDimensionFullName("T_F", mbtiType.getOrNull(2)?.toString() ?: "")
                            )
                            DimensionBadge(
                                letter = mbtiType.getOrNull(3)?.toString() ?: "",
                                fullName = getDimensionFullName("J_P", mbtiType.getOrNull(3)?.toString() ?: "")
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 温馨提示卡片
            AnimatedResultCard(index = 5) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp)
                    ) {
                        Text(
                            text = "温馨提示",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "MBTI测试仅供参考，不应作为判断个人能力的唯一依据。每个人都有独特的成长轨迹，持续自我探索能帮助你更好地了解自己。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }

        // 底部按钮 - 带动画
        AnimatedResultCard(index = 6) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "返回",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Button(
                    onClick = onRetake,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "重新测试",
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

// 维度徽章组件
@Composable
private fun DimensionBadge(
    letter: String,
    fullName: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(40.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = letter,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = fullName,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            textAlign = TextAlign.Center
        )
    }
}

// 获取MBTI类型名称
private fun getMbtiTypeName(type: String): String {
    return when (type) {
        "ISTJ" -> "检查者"
        "ISFJ" -> "守护者"
        "INFJ" -> "提倡者"
        "INTJ" -> "建筑师"
        "ISTP" -> "鉴赏家"
        "ISFP" -> "艺术家"
        "INFP" -> "调停者"
        "INTP" -> "逻辑学家"
        "ESTP" -> "企业家"
        "ESFP" -> "表演者"
        "ENFP" -> "竞选者"
        "ENTP" -> "辩论家"
        "ESTJ" -> "总经理"
        "ESFJ" -> "执行官"
        "ENFJ" -> "主人公"
        "ENTJ" -> "指挥官"
        else -> type
    }
}

// 获取维度的全名
private fun getDimensionFullName(dimension: String, letter: String): String {
    return when (dimension) {
        "E_I" -> if (letter == "E") "外向" else "内向"
        "S_N" -> if (letter == "S") "感觉" else "直觉"
        "T_F" -> if (letter == "T") "思考" else "情感"
        "J_P" -> if (letter == "J") "判断" else "知觉"
        else -> letter
    }
}
