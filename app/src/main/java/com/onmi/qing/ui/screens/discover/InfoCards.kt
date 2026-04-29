package com.onmi.qing.ui.screens.discover

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// 小晴心理Tips卡片
@Composable
internal fun TipCard() {
    val tips = listOf(
        "每天花10分钟进行深呼吸练习，可以显著降低皮质醇水平，减少焦虑感。",
        "保持规律作息有助于情绪稳定，每晚7-9小时的睡眠能让身心得到充分恢复。",
        "适度运动能够释放内啡肽，每天30分钟的散步或伸展都能改善心情。",
        "写日记是一种有效的情绪疏导方式，每天花几分钟记录感恩的事能提升幸福感。",
        "社交支持是心理健康的重要保护因素，与朋友或家人倾诉能缓解心理压力。",
        "正念冥想练习能帮助我们更好地觉察当下，减少对过去的后悔和对未来的担忧。",
        "保持充足的水分摄入有助于维持大脑功能，研究表明脱水会影响情绪和认知。",
        "给自己设定合理的目标，完成后及时肯定，能增强自我效能感和自信心。"
    )

    val randomTip = remember { tips.random() }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "小晴心理Tips",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = randomTip,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// 心理援助热线卡片 - 紧急情况下快速拨打12356寻求专业帮助
@Composable
internal fun HotlineCard() {
    val context = LocalContext.current
    var isLongPressing by remember { mutableStateOf(false) }
    var pressProgress by remember { mutableFloatStateOf(0f) }

    val animatedProgress by animateFloatAsState(
        targetValue = if (isLongPressing) pressProgress else 0f,
        animationSpec = tween(50),
        label = "press_progress"
    )

    val errorColor = MaterialTheme.colorScheme.error

    // LaunchedEffect to handle long press progress
    LaunchedEffect(isLongPressing) {
        if (isLongPressing) {
            val totalDuration = 5000L
            val startTime = System.currentTimeMillis()
            while (isLongPressing) {
                val elapsed = System.currentTimeMillis() - startTime
                pressProgress = (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
                if (pressProgress >= 1f) {
                    // Time's up, dial the number
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:12356")
                    }
                    context.startActivity(intent)
                    isLongPressing = false
                    pressProgress = 0f
                    break
                }
                delay(50)
            }
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onError,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "心理援助热线",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "24小时人工服务",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f)
                    )
                }

                Text(
                    text = "12356",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "如果您正处于心理困扰、情绪崩溃或有紧急心理援助需求，请立即拨打热线，专业的心理咨询师全天候为您服务。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 长按拨号按钮
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(errorColor.copy(alpha = 0.1f))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                isLongPressing = true
                                pressProgress = 0f
                                tryAwaitRelease()
                                isLongPressing = false
                                pressProgress = 0f
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (animatedProgress > 0f && isLongPressing) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            drawArc(
                                color = errorColor,
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = 3.dp.toPx())
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                    }
                    Icon(
                        imageVector = Icons.Default.Phone,
                        contentDescription = null,
                        tint = errorColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when {
                            isLongPressing && animatedProgress > 0f -> "${((1f - animatedProgress) * 5).toInt()}秒后拨打"
                            else -> "长按拨打热线"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = errorColor
                    )
                }
            }
        }
    }
}
