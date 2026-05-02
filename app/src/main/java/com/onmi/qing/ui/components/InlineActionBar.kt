package com.onmi.qing.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun AiMessageActions(
    onCopy: () -> Unit,
    onRegenerate: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ActionButton(icon = Icons.Default.ContentCopy, label = "复制", onClick = onCopy)
        ActionButton(icon = Icons.Default.Refresh, label = "重新生成", onClick = onRegenerate)
        ActionButton(icon = Icons.Default.Delete, label = "删除", onClick = onDelete)
    }
}

@Composable
fun UserMessageActions(
    onCopy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ActionButton(icon = Icons.Default.ContentCopy, label = "复制", onClick = onCopy)
        ActionButton(icon = Icons.Default.Edit, label = "编辑", onClick = onEdit)
        ActionButton(icon = Icons.Default.Delete, label = "删除", onClick = onDelete)
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.size(16.dp)
        )
    }
}
