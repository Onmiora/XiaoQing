package com.onmi.qing.ui.screens.breathing

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onmi.qing.ui.components.ConstrainedWidthContainer
import com.onmi.qing.ui.components.GradientProgressBar
import com.onmi.qing.viewmodel.UsageStatsViewModel
import com.onmi.qing.data.demo.DemoModeManager
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

@Composable
fun BreathingScreen(
    usageStatsViewModel: UsageStatsViewModel,
    demoModeManager: DemoModeManager,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isRunning by remember { mutableStateOf(false) }
    var currentPhase by remember { mutableStateOf(BreathingPhase.INHALE) }
    var currentCycle by remember { mutableIntStateOf(1) }
    var phaseProgress by remember { mutableStateOf(0f) }
    var resetTrigger by remember { mutableIntStateOf(0) }
    val circleScale = remember { Animatable(0.6f) }

    val totalCycles = 4

    LaunchedEffect(resetTrigger) {
        circleScale.snapTo(0.6f)
    }

    LaunchedEffect(isRunning) {
        if (isRunning) {
            while (isRunning) {
                // INHALE: 动画和进度条并行执行
                currentPhase = BreathingPhase.INHALE
                coroutineScope {
                    val animationJob = async {
                        circleScale.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(
                                durationMillis = 4000,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }
                    val progressJob = async {
                        for (i in 1..10) {
                            if (!isRunning) break
                            phaseProgress = i / 10f
                            delay(400)
                        }
                    }
                    animationJob.await()
                    progressJob.cancel()
                }

                if (!isRunning) break

                // HOLD: 只有进度条
                currentPhase = BreathingPhase.HOLD
                for (i in 1..14) {
                    if (!isRunning) break
                    phaseProgress = i / 14f
                    delay(500)
                }

                if (!isRunning) break

                // EXHALE: 动画和进度条并行执行
                currentPhase = BreathingPhase.EXHALE
                coroutineScope {
                    val animationJob = async {
                        circleScale.animateTo(
                            targetValue = 0.6f,
                            animationSpec = tween(
                                durationMillis = 8000,
                                easing = FastOutSlowInEasing
                            )
                        )
                    }
                    val progressJob = async {
                        for (i in 1..16) {
                            if (!isRunning) break
                            phaseProgress = i / 16f
                            delay(500)
                        }
                    }
                    animationJob.await()
                    progressJob.cancel()
                }

                if (!isRunning) break

                currentCycle++
                if (currentCycle > totalCycles) {
                    usageStatsViewModel.incrementBreathingCount()
                    isRunning = false
                    currentPhase = BreathingPhase.COMPLETE
                }
            }
        }
    }

    fun reset() {
        isRunning = false
        currentPhase = BreathingPhase.INHALE
        currentCycle = 1
        phaseProgress = 0f
        resetTrigger++
    }

    ConstrainedWidthContainer(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // 顶部导航
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "返回",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "呼吸练习",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.size(48.dp))
            }

            // 循环计数
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "第 $currentCycle / $totalCycles 个循环",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                )
            }

            // 呼吸动画区域 - Material 3 风格
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                // 使用 Material 3 的 surfaceContainer 色彩层次
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.3f))
                )

                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .scale(circleScale.value)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Air,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = currentPhase.displayText,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            if (isRunning) {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${currentPhase.duration}秒",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                                )
                            }
                        }
                    }
                }
            }

            // 指导文字区域
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isRunning) currentPhase.instruction else "点击开始按钮进行呼吸练习",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (isRunning) {
                        Spacer(modifier = Modifier.height(14.dp))
                        GradientProgressBar(
                            progress = phaseProgress,
                            modifier = Modifier.fillMaxWidth(),
                            height = 8.dp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 控制按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedButton(
                    onClick = { reset() },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "重置",
                        fontWeight = FontWeight.Medium
                    )
                }

                Button(
                    onClick = {
                        if (currentPhase == BreathingPhase.COMPLETE) {
                            reset()
                        } else {
                            isRunning = !isRunning
                        }
                    },
                    modifier = Modifier.weight(2f),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            currentPhase == BreathingPhase.COMPLETE -> "再来一次"
                            isRunning -> "暂停"
                            else -> "开始"
                        },
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
        }
    }
}

private enum class BreathingPhase(
    val displayText: String,
    val duration: Int,
    val instruction: String
) {
    INHALE("吸气", 4, "慢慢地吸气，让空气充满你的肺部"),
    HOLD("屏息", 7, "保持呼吸，感受身体的平静"),
    EXHALE("呼气", 8, "缓缓地呼气，释放所有的紧张"),
    COMPLETE("完成", 0, "太棒了！你完成了一次呼吸练习")
}
