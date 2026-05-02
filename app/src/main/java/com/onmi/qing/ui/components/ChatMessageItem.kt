package com.onmi.qing.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.ChatMessage
import com.onmi.qing.data.MessagePart
import com.onmi.qing.data.MessageRole
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ChatMessageItem(
    message: ChatMessage,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    isStreaming: Boolean = false,
    modifier: Modifier = Modifier
) {
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val timeStr = timeFormat.format(Date(message.timestamp))

    when (message.role) {
        MessageRole.ASSISTANT -> AiMessageLayout(
            message = message,
            timeStr = timeStr,
            onCopy = onCopy,
            onRegenerate = onRegenerate,
            onDelete = onDelete,
            isStreaming = isStreaming,
            modifier = modifier
        )
        MessageRole.USER -> UserMessageLayout(
            message = message,
            timeStr = timeStr,
            onCopy = onCopy,
            onEdit = onEdit,
            onDelete = onDelete,
            modifier = modifier
        )
        MessageRole.SYSTEM -> {}
    }
}

@Composable
private fun AiMessageLayout(
    message: ChatMessage,
    timeStr: String,
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
    isStreaming: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Psychology,
                contentDescription = "小晴",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "小晴 · $timeStr",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))

            val thinking = message.parts.filterIsInstance<MessagePart.Thinking>().firstOrNull()
            if (thinking != null) {
                ThinkingBlock(
                    thinking = thinking.thinking,
                    durationMs = thinking.durationMs
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            val textParts = message.parts.filterIsInstance<MessagePart.Text>()
            textParts.forEach { part ->
                if (part.text.isNotBlank()) {
                    if (isStreaming) {
                        // Plain text during streaming to avoid markdown async parsing flicker
                        Text(
                            text = part.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    } else {
                        MarkdownText(
                            markdown = part.text,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Hide action bar during streaming
            if (!isStreaming) {
                AiMessageActions(
                    onCopy = onCopy,
                    onRegenerate = onRegenerate,
                    onDelete = onDelete
                )
            }
        }
    }
}

@Composable
private fun UserMessageLayout(
    message: ChatMessage,
    timeStr: String,
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Spacer(modifier = Modifier.weight(1f))

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = timeStr,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                shape = MaterialTheme.shapes.medium
            ) {
                Text(
                    text = message.textContent,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            UserMessageActions(
                onCopy = onCopy,
                onEdit = onEdit,
                onDelete = onDelete
            )
        }

        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "我",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}
