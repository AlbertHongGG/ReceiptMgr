package com.receiptmgr.app.presentation.features.log

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.receiptmgr.app.domain.model.log.ApiLogRecord
import com.receiptmgr.app.presentation.components.ImmersiveScaffold
import com.receiptmgr.app.presentation.components.JsonTreeViewer
import com.receiptmgr.app.presentation.components.NavigationBarsSafeSpacer
import com.receiptmgr.app.presentation.components.PremiumConfigHeader
import com.receiptmgr.app.presentation.components.SettingsSection
import com.receiptmgr.app.presentation.navigation.contract.LocalCupertinoNavigator
import com.receiptmgr.app.presentation.notification.NotificationController
import com.receiptmgr.app.presentation.theme.AppColors
import kotlinx.serialization.json.Json

@Composable
fun ApiLogDetailScreen(
    rawJson: String
) {
    val navigator = LocalCupertinoNavigator.current
    val clipboardManager = LocalClipboardManager.current
    val isDark = isSystemInDarkTheme()

    val record = remember(rawJson) {
        try {
            Json { ignoreUnknownKeys = true }.decodeFromString<ApiLogRecord>(rawJson)
        } catch (_: Exception) {
            ApiLogRecord(endpoint = "解析失敗", status = 0)
        }
    }

    val textPrimary = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    ImmersiveScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            PremiumConfigHeader(
                title = record.endpoint,
                subtitle = "狀態碼: ${record.status} • 耗時: ${record.durationMs}ms",
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
                    IconButton(onClick = {
                        clipboardManager.setText(AnnotatedString(rawJson))
                        NotificationController.showSuccess("已複製日誌 JSON")
                    }) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = "複製",
                            tint = textSecondary
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 解密 Payload (若有)
                if (!record.decryptedPayload.isNullOrBlank()) {
                    SettingsSection(
                        title = "解密 Payload",
                        action = {
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(record.decryptedPayload))
                                NotificationController.showSuccess("已複製解密 Payload")
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "複製",
                                    tint = AppColors.AccentTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            JsonTreeViewer(
                                data = record.decryptedPayload,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 2. 伺服器原始回應 (Raw Response)
                if (record.responseBody.isNotBlank()) {
                    SettingsSection(
                        title = "原始回應 (Response Body)",
                        action = {
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(record.responseBody))
                                NotificationController.showSuccess("已複製回應內容")
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "複製",
                                    tint = AppColors.AccentTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    ) {
                        Box(modifier = Modifier.padding(12.dp)) {
                            JsonTreeViewer(
                                data = record.responseBody,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 3. 請求 Body
                if (record.requestBody.isNotBlank()) {
                    SettingsSection(title = "請求內容 (Request Body)") {
                        Box(modifier = Modifier.padding(12.dp)) {
                            JsonTreeViewer(
                                data = record.requestBody,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // 4. 錯誤資訊 (若有)
                if (!record.error.isNullOrBlank()) {
                    SettingsSection(title = "錯誤訊息") {
                        Text(
                            text = record.error,
                            fontSize = 13.sp,
                            color = Color(0xFFEF4444),
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }

                NavigationBarsSafeSpacer()
            }
        }
    }
}
