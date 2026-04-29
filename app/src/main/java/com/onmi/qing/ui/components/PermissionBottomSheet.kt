package com.onmi.qing.ui.components

import android.Manifest
import android.os.Build
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// 权限信息数据类
data class PermissionInfo(
    val icon: ImageVector,
    val title: String,
    val description: String,
    val permission: String
)

// 获取 BLE 心率监测所需的权限列表
fun getRequiredPermissions(): List<PermissionInfo> {
    val permissions = mutableListOf<PermissionInfo>()

    // 蓝牙扫描权限
    permissions.add(
        PermissionInfo(
            icon = Icons.Default.Bluetooth,
            title = "蓝牙权限",
            description = "用于扫描和连接心率设备，以获取准确的压力检测数据",
            permission = Manifest.permission.BLUETOOTH_SCAN
        )
    )

    // 蓝牙连接权限
    permissions.add(
        PermissionInfo(
            icon = Icons.Default.Bluetooth,
            title = "蓝牙连接权限",
            description = "用于与心率设备建立连接并接收心率数据",
            permission = Manifest.permission.BLUETOOTH_CONNECT
        )
    )

    // 始终显示位置权限，因为测试发现在高版本 Android 上也需要位置权限才能扫描 BLE 设备
    permissions.add(
        PermissionInfo(
            icon = Icons.Default.LocationOn,
            title = "位置权限",
            description = "蓝牙扫描需要位置权限来发现附近设备",
            permission = Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    return permissions
}

// 权限申请底部动作条 - 首次启动时向用户说明权限用途并申请权限
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionBottomSheet(
    onDismiss: () -> Unit,
    onPermissionsGranted: () -> Unit
) {
    val permissions = getRequiredPermissions()
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = {
            Column(
                modifier = Modifier.padding(top = 12.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                HorizontalDivider(
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
                .padding(bottom = 16.dp)
        ) {
            // 标题
            Text(
                text = "权限申请",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "为了提供准确的压力检测服务，需要获取以下权限",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 权限列表
            permissions.forEach { permission ->
                PermissionItem(
                    icon = permission.icon,
                    title = permission.title,
                    description = permission.description
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // 说明文字
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "您可以在系统设置中随时管理这些权限。如果拒绝授予权限，部分功能将无法正常使用。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 按钮
            Button(
                onClick = onPermissionsGranted,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(20.dp)
            ) {
                Text(
                    text = "授予权限",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "稍后再说",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PermissionItem(
    icon: ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
