package com.onmi.qing.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.MoodEntry
import com.onmi.qing.data.MoodType
import com.onmi.qing.ui.theme.MoodCalm
import com.onmi.qing.ui.theme.MoodHappy
import com.onmi.qing.ui.theme.MoodUnhappy

// 心情类型数据类
data class MoodOption(
    val type: MoodType,
    val label: String,
    val icon: ImageVector,
    val color: Color
)

// 心情选项列表
val moodOptions = listOf(
    MoodOption(MoodType.HAPPY, "开心", Icons.Default.SentimentSatisfied, MoodHappy),
    MoodOption(MoodType.CALM, "平静", Icons.Default.SentimentNeutral, MoodCalm),
    MoodOption(MoodType.UNHAPPY, "不开心", Icons.Default.SentimentDissatisfied, MoodUnhappy)
)

// 可复用的心情记录底部动作条 - 使用 Material Design 3 ModalBottomSheet
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MoodBottomSheet(
    sheetState: SheetState,
    editingEntry: MoodEntry? = null,
    onDismiss: () -> Unit,
    onConfirm: (MoodType, String) -> Unit,
    onDelete: ((String) -> Unit)? = null
) {
    var selectedMood by remember { mutableStateOf<MoodType?>(editingEntry?.mood) }
    var reason by remember { mutableStateOf(editingEntry?.reason ?: "") }

    val isEditing = editingEntry != null

    ModalBottomSheet(
        onDismissRequest = {
            selectedMood = null
            reason = ""
            onDismiss()
        },
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Column(
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                androidx.compose.material3.HorizontalDivider(
                    modifier = Modifier.size(width = 32.dp, height = 4.dp),
                    thickness = 4.dp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 32.dp)
        ) {
            // 标题
            Text(
                text = if (isEditing) "编辑心情" else "记录心情",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = if (isEditing) "修改你的心情记录" else "此刻你的心情如何？",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 心情选择
            Text(
                text = "选择心情",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                moodOptions.forEach { option ->
                    MoodChip(
                        option = option,
                        selected = selectedMood == option.type,
                        onClick = { selectedMood = option.type }
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 原因输入
            Text(
                text = "发生了什么？",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "记录下此刻的心情来源...",
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                },
                minLines = 3,
                maxLines = 5,
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                )
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 删除按钮（仅编辑模式）
                if (isEditing && onDelete != null) {
                    Button(
                        onClick = {
                            editingEntry?.let { onDelete(it.id) }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "删除",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }

                TextButton(
                    onClick = {
                        selectedMood = null
                        reason = ""
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "取消",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Button(
                    onClick = {
                        selectedMood?.let { mood ->
                            onConfirm(mood, reason)
                            selectedMood = null
                            reason = ""
                        }
                    },
                    enabled = selectedMood != null,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = if (isEditing) "更新" else "保存",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun MoodChip(
    option: MoodOption,
    selected: Boolean,
    onClick: () -> Unit
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = {
            Text(
                text = option.label,
                style = MaterialTheme.typography.labelLarge
            )
        },
        leadingIcon = {
            Icon(
                imageVector = option.icon,
                contentDescription = option.label,
                modifier = Modifier.size(20.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            selectedContainerColor = option.color.copy(alpha = 0.2f),
            selectedLabelColor = option.color,
            selectedLeadingIconColor = option.color,
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            iconColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        border = FilterChipDefaults.filterChipBorder(
            enabled = true,
            selected = selected,
            borderColor = if (selected) option.color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
            selectedBorderColor = option.color,
            borderWidth = 1.dp,
            selectedBorderWidth = 2.dp
        ),
        shape = RoundedCornerShape(20.dp)
    )
}
