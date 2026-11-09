package com.receiptmgr.app.presentation.features.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.receiptmgr.app.data.storage.NotificationLogManager
import com.receiptmgr.app.domain.model.common.NotificationType
import com.receiptmgr.app.domain.model.log.NotificationLogEntry
import com.receiptmgr.app.presentation.components.ImmersiveScaffold
import com.receiptmgr.app.presentation.components.NavigationBarsSafeSpacer
import com.receiptmgr.app.presentation.components.PremiumConfigHeader
import com.receiptmgr.app.presentation.components.PremiumEmptyState
import com.receiptmgr.app.presentation.components.SwipeToConfirmDrawer
import com.receiptmgr.app.presentation.navigation.contract.LocalCupertinoNavigator
import kotlinx.coroutines.launch

@Composable
fun SystemNotificationLogScreen(
    notificationLogManager: NotificationLogManager
) {
    val navigator = LocalCupertinoNavigator.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val logs by notificationLogManager.logsFlow.collectAsState()
    var showClearDrawer by remember { mutableStateOf(false) }

    val textPrimary = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)

    ImmersiveScaffold {
        Column(modifier = Modifier.fillMaxSize()) {
            PremiumConfigHeader(
                title = "系統通知日誌",
                subtitle = "歷史提示與系統訊息",
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = textPrimary
                        )
                    }
                },
                actions = {
                    if (logs.isNotEmpty()) {
                        IconButton(onClick = { showClearDrawer = true }) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "清空紀錄",
                                tint = Color(0xFFEF4444)
                            )
                        }
                    }
                }
            )

            if (logs.isEmpty()) {
                PremiumEmptyState(
                    title = "暫無通知日誌",
                    subtitle = "系統發出通知時將自動收錄於此",
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(logs, key = { it.id }) { item ->
                        NotificationLogItemCard(
                            log = item,
                            onDelete = {
                                scope.launch { notificationLogManager.clearLog(item.file) }
                            }
                        )
                    }
                }
            }

            NavigationBarsSafeSpacer()
        }

        if (showClearDrawer) {
            SwipeToConfirmDrawer(
                title = "清空通知日誌",
                description = "此操作將刪除所有過往收錄的系統通知歷史檔案。",
                confirmText = "確認清空",
                onConfirm = {
                    scope.launch {
                        notificationLogManager.clearAllLogs()
                        showClearDrawer = false
                    }
                },
                onDismiss = { showClearDrawer = false }
            )
        }
    }
}

@Composable
private fun NotificationLogItemCard(
    log: NotificationLogEntry,
    onDelete: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(14.dp)
    val cardBg = if (isDark) Color(0xFF161922) else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    val (typeColor, typeIcon) = when (log.type) {
        NotificationType.SUCCESS -> Pair(Color(0xFF10B981), Icons.Outlined.CheckCircleOutline)
        NotificationType.ERROR -> Pair(Color(0xFFEF4444), Icons.Outlined.ErrorOutline)
        NotificationType.WARNING -> Pair(Color(0xFFF59E0B), Icons.Outlined.WarningAmber)
        NotificationType.INFO -> Pair(Color(0xFF3B82F6), Icons.Outlined.Info)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBg)
            .border(1.dp, borderColor, shape)
            .padding(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = typeIcon,
                contentDescription = log.type.name,
                tint = typeColor,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = log.message,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (isDark) Color.White.copy(alpha = 0.9f) else Color(0xDE000000)
                )
                Text(
                    text = log.timestampStr,
                    fontSize = 11.sp,
                    color = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "刪除",
                    tint = if (isDark) Color(0xFF64748B) else Color(0xFF94A3B8),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}
