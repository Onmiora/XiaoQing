package com.onmi.qing.ui.screens.mooddiary

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.MoodEntry
import com.onmi.qing.data.MoodType
import com.onmi.qing.ui.components.MoodBottomSheet
import com.onmi.qing.ui.components.toMoodColor
import com.onmi.qing.ui.components.toMoodIcon
import com.onmi.qing.ui.components.toMoodDisplayName
import com.onmi.qing.ui.theme.MoodCalm
import com.onmi.qing.ui.theme.MoodHappy
import com.onmi.qing.ui.theme.MoodUnhappy
import com.onmi.qing.viewmodel.MoodViewModel
import com.onmi.qing.data.demo.DemoModeManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

// 心情日记页面 - Material Design 3，查看最新心情、浏览心情记录历史、添加/编辑/删除心情记录
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoodDiaryScreen(
    viewModel: MoodViewModel,
    demoModeManager: DemoModeManager,
    modifier: Modifier = Modifier
) {
    val moodEntries by viewModel.moodEntries.collectAsState()
    val latestMood by viewModel.latestMood.collectAsState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showBottomSheet by remember { mutableStateOf(false) }
    var editingEntry by remember { mutableStateOf<MoodEntry?>(null) }
    var entryToDelete by remember { mutableStateOf<MoodEntry?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        topBar = {
            TopAppBar(
                title = {
                    Box(modifier = Modifier.padding(top = 40.dp, bottom = 20.dp)) {
                        Text(
                            text = "心情日记",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingEntry = null
                    showBottomSheet = true
                    scope.launch { sheetState.show() }
                },
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(bottom = 96.dp)
                    .semantics {
                        contentDescription = "添加心情记录"
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(innerPadding),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 最新心情卡片
            item {
                if (latestMood != null) {
                    LatestMoodCard(
                        moodEntry = latestMood!!,
                        onClick = {
                            editingEntry = latestMood
                            showBottomSheet = true
                            scope.launch { sheetState.show() }
                        }
                    )
                } else {
                    EmptyMoodCard()
                }
            }

            // 心情记录标题
            item {
                Text(
                    text = "心情记录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // 心情记录列表
            if (moodEntries.isEmpty()) {
                item { EmptyStateCard() }
            } else {
                items(moodEntries, key = { it.id }) { entry ->
                    MoodEntryCard(
                        entry = entry,
                        onClick = {
                            editingEntry = entry
                            showBottomSheet = true
                            scope.launch { sheetState.show() }
                        },
                        onDeleteClick = {
                            entryToDelete = entry
                        }
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(130.dp)) }
        }

        // 编辑/新建 BottomSheet
        if (showBottomSheet) {
            MoodBottomSheet(
                sheetState = sheetState,
                editingEntry = editingEntry,
                onDismiss = {
                    scope.launch { sheetState.hide() }
                    showBottomSheet = false
                    editingEntry = null
                },
                onConfirm = { mood, reason ->
                    if (editingEntry != null) {
                        viewModel.updateMoodEntry(editingEntry!!.id, mood, reason)
                    } else {
                        viewModel.addMoodEntry(mood, reason)
                    }
                    scope.launch { sheetState.hide() }
                    showBottomSheet = false
                    editingEntry = null
                },
                onDelete = { entryId ->
                    viewModel.deleteMoodEntry(entryId)
                    scope.launch { sheetState.hide() }
                    showBottomSheet = false
                    editingEntry = null
                }
            )
        }

        // 删除确认对话框
        entryToDelete?.let { entry ->
            DeleteConfirmDialog(
                moodText = entry.mood.toMoodDisplayName(),
                onConfirm = {
                    viewModel.deleteMoodEntry(entry.id)
                    entryToDelete = null
                },
                onDismiss = {
                    entryToDelete = null
                }
            )
        }
    }
}

// 最新心情卡片
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LatestMoodCard(
    moodEntry: MoodEntry,
    onClick: () -> Unit
) {
    val moodColor = moodEntry.mood.toMoodColor()
    val moodIcon = moodEntry.mood.toMoodIcon()
    val moodText = moodEntry.mood.toMoodDisplayName()
    val timeFormat = SimpleDateFormat("MM月dd日 HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(moodEntry.timestamp))

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                    contentDescription = "最新心情：${moodText}，记录于${formattedTime}，点击编辑"
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 心情图标
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(moodColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = moodIcon,
                    contentDescription = null,
                    tint = moodColor,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "最新心情",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = moodText,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = moodColor
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = formattedTime,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// 空状态卡片
@Composable
private fun EmptyMoodCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "暂无心情记录"
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.SentimentNeutral,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = "还没有记录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "点击右下角按钮记录你的心情",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// 心情记录卡片
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoodEntryCard(
    entry: MoodEntry,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    val moodColor = entry.mood.toMoodColor()
    val moodIcon = entry.mood.toMoodIcon()
    val moodText = entry.mood.toMoodDisplayName()
    val relativeTime = getRelativeTime(entry.timestamp)

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(20.dp)
            )
            .semantics {
                contentDescription = "${moodText}心情，$relativeTime${if (entry.reason.isNotBlank()) "，原因：${entry.reason}" else ""}，点击编辑"
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.Top
        ) {
            // 心情图标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(moodColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = moodIcon,
                    contentDescription = null,
                    tint = moodColor,
                    modifier = Modifier.size(26.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = moodText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = moodColor
                    )
                    Text(
                        text = relativeTime,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (entry.reason.isNotBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = entry.reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 删除按钮 - 48dp 触摸目标
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onDeleteClick)
                    .semantics {
                        contentDescription = "删除${moodText}记录"
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

// 空状态展示卡片
@Composable
private fun EmptyStateCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "暂无心情记录，开始记录你的情绪变化吧"
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.SentimentNeutral,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "暂无心情记录",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "开始记录你的情绪变化吧",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }
    }
}

// 删除确认对话框 - MD3 AlertDialog
@Composable
private fun DeleteConfirmDialog(
    moodText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "删除记录",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Text(
                text = "确定要删除这条${moodText}的心情记录吗？此操作无法撤销。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "删除",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "取消",
                    color = MaterialTheme.colorScheme.primary
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(32.dp)
    )
}


// 获取相对时间
private fun getRelativeTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp

    return when {
        diff < TimeUnit.MINUTES.toMillis(1) -> "刚刚"
        diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)}分钟前"
        diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)}小时前"
        diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)}天前"
        else -> SimpleDateFormat("MM/dd", Locale.getDefault()).format(Date(timestamp))
    }
}
