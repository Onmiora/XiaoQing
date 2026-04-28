package com.onmi.qing.ui.screens.discover

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.onmi.qing.ui.components.TestSelectionBottomSheet

// 测试选择页面 - 显示测试选择BottomSheet，管理MBTI和SBTI的选择逻辑
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestSelectionScreen(
    onNavigateToSbti: () -> Unit,
    onNavigateToMbti: () -> Unit,
    onBackClick: () -> Unit
) {
    // skipPartiallyExpanded = true 使BottomSheet完全展开
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Empty column - the BottomSheet is the main content
        }
    }

    // 显示BottomSheet - 全高度展开
    ModalBottomSheet(
        onDismissRequest = onBackClick,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null, // 隐藏拖动把手，让底部栏看起来更完整
        scrimColor = MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            TestSelectionBottomSheet(
                onDismiss = onBackClick,
                onMbtiSelected = {
                    onNavigateToMbti()
                },
                onSbtiSelected = {
                    onNavigateToSbti()
                }
            )

            // 添加底部安全区域padding
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
