package com.onmi.qing.ui.components

import android.content.Intent
import android.net.Uri
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
import androidx.compose.material3.ElevatedCard
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.CrisisIntervention
import kotlinx.coroutines.delay

@Composable
fun CrisisInterventionCard(
    crisis: CrisisIntervention,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isLongPressing by remember { mutableStateOf(false) }
    var pressProgress by remember { mutableFloatStateOf(0f) }

    val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
        targetValue = if (isLongPressing) pressProgress else 0f,
        animationSpec = androidx.compose.animation.core.tween(50),
        label = "press_progress"
    )

    val errorColor = MaterialTheme.colorScheme.error

    LaunchedEffect(isLongPressing) {
        if (isLongPressing) {
            val totalDuration = 5000L
            val startTime = System.currentTimeMillis()
            while (isLongPressing) {
                val elapsed = System.currentTimeMillis() - startTime
                pressProgress = (elapsed.toFloat() / totalDuration).coerceIn(0f, 1f)
                if (pressProgress >= 1f) {
                    val intent = Intent(Intent.ACTION_DIAL).apply {
                        data = Uri.parse("tel:${crisis.phone}")
                    }
                    context.startActivity(intent)
                    isLongPressing = false
                    pressProgress = 0f
                    onDismiss()
                    break
                }
                delay(50)
            }
        } else {
            pressProgress = 0f
        }
    }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = androidx.compose.material3.CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
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
                        text = crisis.title,
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
                    text = crisis.phone,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = crisis.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.9f)
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                                try {
                                    awaitRelease()
                                } finally {
                                    isLongPressing = false
                                }
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
                            else -> crisis.action
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
