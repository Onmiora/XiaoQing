package com.onmi.qing.ui.screens.history

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Analytics
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.onmi.qing.data.ChatSession
import com.onmi.qing.data.repository.ChatRepository
import com.onmi.qing.ui.components.AnalysisBottomSheet
import com.onmi.qing.ui.components.EmptyState
import com.onmi.qing.ui.components.GlowAvatarBubble
import com.onmi.qing.viewmodel.StateViewModel
import com.onmi.qing.data.demo.DemoModeManager
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onBackClick: () -> Unit,
    onSessionClick: (ChatSession) -> Unit,
    stateViewModel: StateViewModel,
    chatRepository: ChatRepository,
    demoModeManager: DemoModeManager,
    modifier: Modifier = Modifier
) {
    val isDemoMode by demoModeManager.isDemoMode.collectAsState()
    val demoSessions by demoModeManager.demoSessions.collectAsState()
    val sessions by chatRepository.getAllSessions().collectAsState(initial = emptyList())

    // 根据模式选择显示的会话列表
    val displaySessions = if (isDemoMode) demoSessions else sessions

    var searchQuery by remember { mutableStateOf("") }
    var showAnalysisSheet by remember { mutableStateOf(false) }
    var currentAnalyzeSessionId by remember { mutableStateOf<String?>(null) }
    val analysisState by stateViewModel.analysisState.collectAsState()

    // Delete confirmation dialog state
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    var sessionToDelete by remember { mutableStateOf<ChatSession?>(null) }

    // Analysis confirmation dialog state
    var showAnalysisConfirmDialog by remember { mutableStateOf(false) }
    var pendingAnalysisSession by remember { mutableStateOf<ChatSession?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val filteredSessions = if (searchQuery.isBlank()) {
        displaySessions
    } else {
        displaySessions.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.lastMessage.contains(searchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("对话历史")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (sessions.isNotEmpty()) {
                        IconButton(
                            onClick = { showDeleteAllDialog = true }
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeleteSweep,
                                contentDescription = "删除全部",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
//                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = {
                    Text("搜索对话...")
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(32.dp),
                singleLine = true
            )

            when {
                displaySessions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = Icons.Default.History,
                            title = if (isDemoMode) "演示模式" else "暂无对话历史",
                            description = if (isDemoMode) "这是演示数据" else "开始与小晴对话，记录你的心情"
                        )
                    }
                }
                filteredSessions.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        EmptyState(
                            icon = Icons.Default.Search,
                            title = "没有找到相关对话",
                            description = "尝试其他关键词搜索"
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        items(filteredSessions, key = { it.id }) { session ->
                            SessionCard(
                                session = session,
                                onClick = { onSessionClick(session) },
                                onAnalyzeClick = {
                                    // 检查分析次数是否已达上限
                                    if (session.analysisCount >= ChatSession.MAX_ANALYSIS_COUNT) {
                                        Toast.makeText(
                                            context,
                                            "该对话分析次数已达上限（${ChatSession.MAX_ANALYSIS_COUNT}次），不允许再次分析",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        return@SessionCard
                                    }
                                    // 记录待分析会话，显示确认对话框
                                    pendingAnalysisSession = session
                                    showAnalysisConfirmDialog = true
                                },
                                onDeleteClick = { sessionToDelete = session }
                            )
                        }
                    }
                }
            }
        }
    }

    // Delete single session confirmation dialog
    sessionToDelete?.let { session ->
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("删除对话") },
            text = { Text("确定要删除「${session.title}」吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            chatRepository.deleteSession(session.id)
                        }
                        sessionToDelete = null
                    }
                ) {
                    Text("删除", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) {
                    Text("取消")
                }
            }
        )
    }

    // Delete all sessions confirmation dialog
    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("删除全部对话") },
            text = { Text("确定要删除所有对话记录吗？此操作不可撤销。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            chatRepository.deleteAllData()
                        }
                        showDeleteAllDialog = false
                    }
                ) {
                    Text("删除全部", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("取消")
                }
            }
        )
    }

    // Analysis confirmation dialog
    if (showAnalysisConfirmDialog && pendingAnalysisSession != null) {
        val session = pendingAnalysisSession!!
        AlertDialog(
            onDismissRequest = {
                showAnalysisConfirmDialog = false
                pendingAnalysisSession = null
            },
            title = { Text("确认分析") },
            text = {
                if (session.analysisCount > 0) {
                    Text("该对话已进行过分析，为保证结果准确，不允许再次分析。")
                } else {
                    Text("为了保证分析结果准确，每次对话只能分析一次。确定要分析此对话吗？")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val sessionId = session.id
                        scope.launch {
                            chatRepository.incrementSessionAnalysisCount(sessionId)
                        }
                        currentAnalyzeSessionId = sessionId
                        stateViewModel.analyzeSession(sessionId)
                        showAnalysisSheet = true
                        showAnalysisConfirmDialog = false
                        pendingAnalysisSession = null
                    }
                ) {
                    Text("确认分析")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAnalysisConfirmDialog = false
                        pendingAnalysisSession = null
                    }
                ) {
                    Text("取消")
                }
            }
        )
    }

    // Analysis bottom sheet
    if (showAnalysisSheet) {
        AnalysisBottomSheet(
            analysisState = analysisState,
            onDismiss = {
                showAnalysisSheet = false
                stateViewModel.resetAnalysisState()
            },
            onConfirm = {
                showAnalysisSheet = false
                stateViewModel.resetAnalysisState()
            },
            onRetry = if (currentAnalyzeSessionId != null) {
                {
                    stateViewModel.analyzeSession(currentAnalyzeSessionId!!)
                }
            } else null
        )
    }
}

@Composable
private fun SessionCard(
    session: ChatSession,
    onClick: () -> Unit,
    onAnalyzeClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    // Generate accent color based on session id
    val colorIndex = session.id.hashCode() % 5
    val accentColors = listOf(
        MaterialTheme.colorScheme.primary,
        MaterialTheme.colorScheme.tertiary,
        MaterialTheme.colorScheme.secondary,
        Color(0xFF10B981),
        Color(0xFF3B82F6)
    )
    val accentColor = accentColors[kotlin.math.abs(colorIndex)]

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // AI Avatar
            GlowAvatarBubble(
                icon = Icons.Default.Psychology,
                size = 48.dp,
                containerColor = accentColor,
                iconSize = 24.dp
            )

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = session.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = session.getFormattedTime(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = session.lastMessage.ifEmpty { "开始对话吧..." },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "${session.messageCount} 条消息",
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor
                        )
                        if (session.analysisCount > 0) {
                            Text(
                                text = "已分析",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val isAnalysisDisabled = session.analysisCount >= ChatSession.MAX_ANALYSIS_COUNT
                        FilledTonalButton(
                            onClick = onAnalyzeClick,
                            modifier = Modifier.height(32.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                            shape = RoundedCornerShape(8.dp),
                            enabled = !isAnalysisDisabled
                        ) {
                            Icon(
                                imageVector = Icons.Default.Analytics,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isAnalysisDisabled) "已达上限" else "分析",
                                style = MaterialTheme.typography.labelMedium
                            )
                        }

                        IconButton(
                            onClick = onDeleteClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "删除",
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
