package com.receiptmgr.app.presentation.features.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.ToggleOn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.receiptmgr.app.domain.model.auth.SessionState
import com.receiptmgr.app.presentation.components.AppSwitch
import com.receiptmgr.app.presentation.components.ImmersiveScaffold
import com.receiptmgr.app.presentation.components.NavigationBarsSafeSpacer
import com.receiptmgr.app.presentation.components.PremiumConfigHeader
import com.receiptmgr.app.presentation.components.SettingsSection
import com.receiptmgr.app.presentation.components.SettingsTile
import com.receiptmgr.app.presentation.components.SwipeToConfirmDrawer
import com.receiptmgr.app.presentation.navigation.contract.LocalCupertinoNavigator
import com.receiptmgr.app.presentation.navigation.model.AppRoute
import com.receiptmgr.app.presentation.notification.NotificationController
import com.receiptmgr.app.presentation.theme.AppColors

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel
) {
    val navigator = LocalCupertinoNavigator.current
    val clipboardManager = LocalClipboardManager.current
    val sessionState by viewModel.sessionState.collectAsState()
    val darkModePref by viewModel.darkModeFlow.collectAsState()
    val apiLogging by viewModel.apiLoggingFlow.collectAsState()
    val notificationLogging by viewModel.notificationLoggingFlow.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val isSystemDark = isSystemInDarkTheme()

    var showClearSessionDrawer by remember { mutableStateOf(false) }

    val currentDarkMode = darkModePref ?: isSystemDark
    val textPrimary = if (currentDarkMode) Color(0xFFE2E8F0) else Color(0xFF0F172A)
    val textSecondary = if (currentDarkMode) Color(0xFF94A3B8) else Color(0xFF64748B)

    ImmersiveScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            PremiumConfigHeader(
                title = "系統設定",
                subtitle = "環境、日誌與會話憑證",
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = textPrimary
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
                // 1. 會話管理
                SettingsSection(title = "會話管理") {
                    when (val state = sessionState) {
                        is SessionState.Authenticated -> {
                            SettingsTile(
                                icon = Icons.Outlined.Key,
                                title = "會話憑證",
                                subtitle = "帳號 ${state.credentials.phone} (剩餘 ${state.credentials.remainingDays} 天)",
                                onClick = { navigator.push(AppRoute.Login) }
                            )
                            SettingsTile(
                                icon = Icons.Outlined.DeleteForever,
                                title = "登出憑證",
                                subtitle = "清除本機 ss_me 與會話資料",
                                onClick = { showClearSessionDrawer = true }
                            )
                        }

                        is SessionState.Expired -> {
                            SettingsTile(
                                icon = Icons.Outlined.Key,
                                title = "會話已過期",
                                subtitle = "點擊以前往重新驗證",
                                onClick = { navigator.push(AppRoute.Login) }
                            )
                        }

                        is SessionState.Unauthenticated -> {
                            SettingsTile(
                                icon = Icons.Outlined.Key,
                                title = "尚未授權",
                                subtitle = "登入以取得 ss_me 金鑰",
                                onClick = { navigator.push(AppRoute.Login) }
                            )
                        }
                    }
                }

                // 2. 系統日誌
                SettingsSection(title = "日誌稽核") {
                    SettingsTile(
                        icon = Icons.Outlined.DataObject,
                        title = "API 請求紀錄",
                        subtitle = "檢視登入與通訊之 Payload 紀錄",
                        onClick = { navigator.push(AppRoute.ApiLogs) }
                    )
                    SettingsTile(
                        icon = Icons.Outlined.ToggleOn,
                        title = "API 紀錄開關",
                        subtitle = "是否自動儲存通訊稽核封包",
                        trailing = {
                            AppSwitch(
                                checked = apiLogging,
                                onCheckedChange = { viewModel.setApiLogging(it) }
                            )
                        }
                    )
                    SettingsTile(
                        icon = Icons.Outlined.Notifications,
                        title = "系統通知紀錄",
                        subtitle = "檢視歷史系統通知與警示",
                        onClick = { navigator.push(AppRoute.NotificationLogs) }
                    )
                    SettingsTile(
                        icon = Icons.Outlined.ToggleOn,
                        title = "通知日誌開關",
                        subtitle = "是否寫入系統通知日誌",
                        trailing = {
                            AppSwitch(
                                checked = notificationLogging,
                                onCheckedChange = { viewModel.setNotificationLogging(it) }
                            )
                        }
                    )
                }

                // 3. 硬體指紋與環境
                SettingsSection(title = "終端環境") {
                    SettingsTile(
                        icon = Icons.Outlined.Fingerprint,
                        title = "Android UUID",
                        subtitle = uiState.deviceUuid,
                        trailing = {
                            IconButton(onClick = {
                                clipboardManager.setText(AnnotatedString(uiState.deviceUuid))
                                NotificationController.showSuccess("已複製 UUID")
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "複製",
                                    tint = textSecondary
                                )
                            }
                        }
                    )
                    SettingsTile(
                        icon = Icons.Outlined.PhoneAndroid,
                        title = "裝置型號",
                        subtitle = uiState.deviceModel
                    )
                    SettingsTile(
                        icon = Icons.Outlined.Smartphone,
                        title = "Android 版本",
                        subtitle = "Android ${uiState.osVersion}"
                    )
                    SettingsTile(
                        icon = Icons.Outlined.Info,
                        title = "App 版本",
                        subtitle = "v${uiState.appVersion}"
                    )
                }

                // 4. 外觀設定
                SettingsSection(title = "偏好外觀") {
                    SettingsTile(
                        icon = Icons.Outlined.DarkMode,
                        title = "深色模式",
                        subtitle = if (currentDarkMode) "已啟用" else "已關閉",
                        trailing = {
                            AppSwitch(
                                checked = currentDarkMode,
                                onCheckedChange = { viewModel.setDarkMode(it) }
                            )
                        }
                    )
                }

                NavigationBarsSafeSpacer()
            }
        }

        // 滑動確認清除抽屜
        if (showClearSessionDrawer) {
            SwipeToConfirmDrawer(
                title = "確認清除會話",
                description = "此操作將抹除本機已儲存之 ss_me 金鑰及憑證，需重新驗證帳號登入。",
                confirmText = "滑動清除",
                onConfirm = {
                    viewModel.clearSession()
                    showClearSessionDrawer = false
                },
                onDismiss = { showClearSessionDrawer = false }
            )
        }
    }
}
