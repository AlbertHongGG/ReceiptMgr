package com.receiptmgr.app.presentation.features.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Timer
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.receiptmgr.app.data.storage.ApiLogManager
import com.receiptmgr.app.domain.model.log.ApiLogModel
import com.receiptmgr.app.presentation.components.ImmersiveScaffold
import com.receiptmgr.app.presentation.components.NavigationBarsSafeSpacer
import com.receiptmgr.app.presentation.components.PremiumConfigHeader
import com.receiptmgr.app.presentation.components.PremiumEmptyState
import com.receiptmgr.app.presentation.components.SwipeToConfirmDrawer
import com.receiptmgr.app.presentation.navigation.contract.LocalCupertinoNavigator
import com.receiptmgr.app.presentation.navigation.model.AppRoute
import com.receiptmgr.app.presentation.theme.AppColors
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Composable
fun ApiLogViewerScreen(
    apiLogManager: ApiLogManager
) {
    val navigator = LocalCupertinoNavigator.current
    val scope = rememberCoroutineScope()
    val isDark = isSystemInDarkTheme()
    val logs by apiLogManager.logsFlow.collectAsState()
    var showClearDrawer by remember { mutableStateOf(false) }

    ImmersiveScaffold {
        Column(modifier = Modifier.fillMaxSize()) {
            // 右滑手勢退棧，無上一頁按鈕
            PremiumConfigHeader(
                title = "API 請求紀錄",
                subtitle = "API AUDIT LOGS",
                actions = {
                    if (logs.isNotEmpty()) {
                        IconButton(onClick = { showClearDrawer = true }) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "清空紀錄",
                                tint = AppColors.danger
                            )
                        }
                    }
                }
            )

            if (logs.isEmpty()) {
                PremiumEmptyState(
                    title = "暫無 API 紀錄",
                    subtitle = "發送登入或查詢請求後將自動收錄於此",
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
                        ApiLogItemCard(
                            log = item,
                            onClick = {
                                val rawJson = Json.encodeToString(item.record)
                                navigator.push(AppRoute.ApiLogDetail(rawJson))
                            },
                            onDelete = {
                                scope.launch { apiLogManager.clearLog(item.file) }
                            }
                        )
                    }
                }
            }

            NavigationBarsSafeSpacer()
        }

        if (showClearDrawer) {
            SwipeToConfirmDrawer(
                title = "清空 API 紀錄",
                description = "此操作將刪除本機所有已儲存之 API 請求與回應稽核檔案。",
                confirmText = "確認清空",
                onConfirm = {
                    scope.launch {
                        apiLogManager.clearAllLogs()
                        showClearDrawer = false
                    }
                },
                onDismiss = { showClearDrawer = false }
            )
        }
    }
}

@Composable
private fun ApiLogItemCard(
    log: ApiLogModel,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(14.dp)
    val record = log.record
    val isSuccess = record.isSuccess && record.status == 200

    val statusBg = if (isSuccess) {
        if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f)
    } else {
        AppColors.danger.copy(alpha = 0.14f)
    }
    val statusColor = if (isSuccess) {
        if (isDark) Color.White else AppColors.textPrimaryLight
    } else {
        AppColors.danger
    }
    val cardBg = if (isDark) AppColors.surfaceDark else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBg)
            .border(1.dp, borderColor, shape)
            .clickable(onClick = onClick)
            .padding(14.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 狀態碼 Badge (灰階高對比)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (record.status > 0) "${record.status}" else "ERR",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = record.endpoint,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (isDark) Color.White else AppColors.textPrimaryLight,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.DeleteOutline,
                        contentDescription = "刪除",
                        tint = AppColors.secondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = AppColors.secondary,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${record.durationMs} ms",
                    fontSize = 11.sp,
                    color = AppColors.secondary
                )

                Spacer(modifier = Modifier.weight(1f))

                Text(
                    text = log.timestampStr,
                    fontSize = 11.sp,
                    color = AppColors.secondary
                )
            }
        }
    }
}
