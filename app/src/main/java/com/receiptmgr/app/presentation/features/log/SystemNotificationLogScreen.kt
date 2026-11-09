package com.receiptmgr.app.presentation.features.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.receiptmgr.app.data.storage.NotificationLogManager
import com.receiptmgr.app.domain.model.common.NotificationType
import com.receiptmgr.app.domain.model.log.NotificationLogEntry
import com.receiptmgr.app.presentation.components.PremiumConfigHeader
import com.receiptmgr.app.presentation.features.log.components.GenericLogViewerScaffold

/**
 * 系統通知日誌畫面 (SystemNotificationLogScreen)。
 * 1:1 對齊 LensWise 規範：
 * - 透過 GenericLogViewerScaffold 實現統一捲動佈局與毛玻璃懸浮垃圾桶。
 * - 支援長按拖曳日誌卡片至底部垃圾桶即時銷毀單筆。
 * - 底部垃圾桶點擊呼叫 SwipeToConfirmDrawer 雙段滑動抹除全數通知。
 */
@Composable
fun SystemNotificationLogScreen(
    notificationLogManager: NotificationLogManager,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val logs by notificationLogManager.logsFlow.collectAsState()

    GenericLogViewerScaffold(
        items = logs,
        onDeleteSingle = { notificationLogManager.clearLog(it.file) },
        onClearAll = { notificationLogManager.clearAllLogs() },
        clearDrawerTitle = "CLEAR SYSTEM LOGS",
        emptyMessage = "目前沒有系統通知日誌。",
        modifier = modifier,
        header = {
            PremiumConfigHeader(
                title = "系統通知日誌",
                subtitle = "SYSTEM NOTIFICATION LOG"
            )
        },
        itemContent = { entry, isDragging, itemModifier ->
            NotificationLogCardContent(
                entry = entry,
                isDark = isDark,
                modifier = itemModifier.alpha(if (isDragging) 0.3f else 1.0f)
            )
        }
    )
}

@Composable
private fun NotificationLogCardContent(
    entry: NotificationLogEntry,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val (typeColor, typeIcon) = when (entry.type) {
        NotificationType.SUCCESS -> Pair(Color(0xFF10B981), Icons.Outlined.CheckCircleOutline)
        NotificationType.ERROR -> Pair(Color(0xFFEF4444), Icons.Outlined.ErrorOutline)
        NotificationType.WARNING -> Pair(Color(0xFFF59E0B), Icons.Outlined.WarningAmber)
        NotificationType.INFO -> Pair(Color(0xFF3B82F6), Icons.Outlined.Info)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.06f),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.14f)
            )
            .clip(RoundedCornerShape(20.dp))
            .background(if (isDark) Color(0xFF1C1C1E) else Color.White)
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f),
                shape = RoundedCornerShape(20.dp)
            )
            .padding(20.dp)
    ) {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = entry.type.name,
                    tint = typeColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.type.name.uppercase(),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = typeColor
                    )

                    Text(
                        text = entry.timestampStr,
                        fontSize = 11.sp,
                        color = if (isDark) Color.White.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.35f)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = entry.message,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF1E293B)
                )
            }
        }
    }
}
