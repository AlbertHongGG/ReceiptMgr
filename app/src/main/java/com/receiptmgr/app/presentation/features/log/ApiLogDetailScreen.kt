package com.receiptmgr.app.presentation.features.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.receiptmgr.app.domain.model.log.ApiLogRecord
import com.receiptmgr.app.presentation.components.ImmersiveScaffold
import com.receiptmgr.app.presentation.components.JsonTreeViewer
import com.receiptmgr.app.presentation.components.NavigationBarsSafeSpacer
import com.receiptmgr.app.presentation.components.SettingsSection
import com.receiptmgr.app.presentation.notification.NotificationController
import com.receiptmgr.app.presentation.theme.AppColors
import kotlinx.serialization.json.Json

private val detailJson = Json { ignoreUnknownKeys = true }

/**
 * API 日誌詳細資料畫面 (ApiLogDetailScreen)。
 * 遵循右滑返回手勢，包含端點標題、耗時與時間晶片、複製 JSON 與可摺疊的 JSON 樹狀檢視。
 */
@Composable
fun ApiLogDetailScreen(
    rawJson: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val isDark = isSystemInDarkTheme()

    val record = remember(rawJson) {
        try {
            detailJson.decodeFromString<ApiLogRecord>(rawJson)
        } catch (_: Exception) {
            ApiLogRecord(endpoint = "API 詳情", status = 0)
        }
    }

    val metaIconColor = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.54f)
    val metaTextColor = if (isDark) Color.White.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.87f)

    ImmersiveScaffold(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 頂部看板 (右滑返回，無上一頁按鈕)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = record.endpoint,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1E293B)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        val (statusBg, statusFg) = if (record.isSuccess) {
                            Pair(
                                Color(0xFF10B981).copy(alpha = if (isDark) 0.20f else 0.14f),
                                if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                            )
                        } else {
                            Pair(
                                Color(0xFFEF4444).copy(alpha = if (isDark) 0.22f else 0.14f),
                                Color(0xFFEF4444)
                            )
                        }

                        // Status Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${record.status}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusFg
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = metaIconColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${record.durationMs} ms",
                            fontSize = 12.sp,
                            color = metaTextColor
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = metaIconColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = record.timestamp.replace("T", " "),
                            fontSize = 12.sp,
                            color = metaTextColor
                        )
                    }
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(rawJson))
                        NotificationController.showSuccess("已複製日誌 JSON")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "複製完整日誌",
                        tint = if (isDark) Color.White.copy(alpha = 0.7f) else AppColors.primary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 錯誤訊息 (若有)
                if (!record.error.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "錯誤: ${record.error}",
                            fontSize = 13.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 2. 解密 Payload (若有)
                if (!record.decryptedPayload.isNullOrBlank()) {
                    SettingsSection(
                        title = "解密 Payload"
                    ) {
                        Box(modifier = Modifier.padding(14.dp)) {
                            JsonTreeViewer(
                                data = record.decryptedPayload,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 3. 伺服器原始回應 (Raw Response)
                if (record.responseBody.isNotBlank()) {
                    SettingsSection(
                        title = "伺服器原始回應"
                    ) {
                        Box(modifier = Modifier.padding(14.dp)) {
                            JsonTreeViewer(
                                data = record.responseBody,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 4. 請求內容 (Request Body)
                if (record.requestBody.isNotBlank()) {
                    SettingsSection(
                        title = "請求 Payload"
                    ) {
                        Box(modifier = Modifier.padding(14.dp)) {
                            JsonTreeViewer(
                                data = record.requestBody,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 5. 請求標頭 (Request Headers)
                if (record.requestHeaders.isNotEmpty()) {
                    SettingsSection(
                        title = "請求標頭 (Headers)"
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            record.requestHeaders.forEach { (key, value) ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = key,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = if (isDark) Color.White.copy(alpha = 0.7f) else Color(0xFF334155)
                                    )
                                    Text(
                                        text = value,
                                        fontSize = 12.sp,
                                        color = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            NavigationBarsSafeSpacer()
        }
    }
}
