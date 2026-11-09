package com.receiptmgr.app.presentation.features.settings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.DataObject
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Smartphone
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.receiptmgr.app.domain.model.auth.SessionState
import com.receiptmgr.app.presentation.components.AppSwitch
import com.receiptmgr.app.presentation.components.ImmersiveScaffold
import com.receiptmgr.app.presentation.components.NavigationBarsSafeSpacer
import com.receiptmgr.app.presentation.components.PremiumConfigHeader
import com.receiptmgr.app.presentation.components.SettingsDivider
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

    ImmersiveScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 頂部大標題（右滑手勢退棧，零上一頁按鈕）
            PremiumConfigHeader(
                title = "偏好設定",
                subtitle = "SYSTEM CONFIGURATION"
            )

            // 第一分組：會話憑證
            SettingsSection(title = "會話管理") {
                when (val state = sessionState) {
                    is SessionState.Authenticated -> {
                        SettingsTile(
                            icon = Icons.Default.AccountCircle,
                            iconColor = AppColors.primary,
                            title = "載具帳號",
                            subtitle = "${state.credentials.phone} (剩餘 ${state.credentials.remainingDays} 天)",
                            onTap = { navigator.push(AppRoute.Login) }
                        )
                        SettingsDivider()
                        SettingsTile(
                            icon = Icons.Default.Key,
                            iconColor = AppColors.primary,
                            title = "會話憑證",
                            subtitle = "檢視 ss_me 與 Response Payload",
                            onTap = { navigator.push(AppRoute.Login) }
                        )
                        SettingsDivider()
                        SettingsTile(
                            icon = Icons.Default.DeleteOutline,
                            iconColor = AppColors.danger,
                            title = "清除會話",
                            subtitle = "抹除本機 ss_me 金鑰憑證",
                            isDestructive = true,
                            onTap = { showClearSessionDrawer = true }
                        )
                    }

                    is SessionState.Expired -> {
                        SettingsTile(
                            icon = Icons.Default.Key,
                            iconColor = AppColors.primary,
                            title = "會話已過期",
                            subtitle = "帳號 ${state.credentials.phone}，點擊重新登入",
                            onTap = { navigator.push(AppRoute.Login) }
                        )
                    }

                    is SessionState.Unauthenticated -> {
                        SettingsTile(
                            icon = Icons.Default.Key,
                            iconColor = AppColors.primary,
                            title = "尚未授權",
                            subtitle = "登入以取得 ss_me 會話簽章金鑰",
                            onTap = { navigator.push(AppRoute.Login) }
                        )
                    }
                }
            }

            // 第二分組：日誌稽核 (1:1 整合開關與跳轉)
            SettingsSection(title = "日誌稽核") {
                SettingsTile(
                    icon = Icons.Default.History,
                    iconColor = AppColors.primary,
                    title = "通知日誌",
                    subtitle = "檢視歷史系統通知與提示歷程",
                    onTap = { navigator.push(AppRoute.NotificationLogs) },
                    trailing = {
                        AppSwitch(
                            checked = notificationLogging,
                            onCheckedChange = { viewModel.setNotificationLogging(it) }
                        )
                    }
                )
                SettingsDivider()
                SettingsTile(
                    icon = Icons.Default.DataObject,
                    iconColor = AppColors.primary,
                    title = "API 日誌",
                    subtitle = "檢視通訊封包與伺服器回應",
                    onTap = { navigator.push(AppRoute.ApiLogs) },
                    trailing = {
                        AppSwitch(
                            checked = apiLogging,
                            onCheckedChange = { viewModel.setApiLogging(it) }
                        )
                    }
                )
            }

            // 第三分組：終端環境
            SettingsSection(title = "終端環境") {
                SettingsTile(
                    icon = Icons.Default.Fingerprint,
                    iconColor = AppColors.primary,
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
                                tint = AppColors.secondary
                            )
                        }
                    }
                )
                SettingsDivider()
                SettingsTile(
                    icon = Icons.Default.PhoneAndroid,
                    iconColor = AppColors.primary,
                    title = "裝置型號",
                    subtitle = uiState.deviceModel
                )
                SettingsDivider()
                SettingsTile(
                    icon = Icons.Default.Smartphone,
                    iconColor = AppColors.primary,
                    title = "系統版本",
                    subtitle = "Android ${uiState.osVersion}"
                )
                SettingsDivider()
                SettingsTile(
                    icon = Icons.Default.Info,
                    iconColor = AppColors.primary,
                    title = "應用程式版本",
                    subtitle = "v${uiState.appVersion}"
                )
            }

            // 第四分組：外觀設定
            SettingsSection(title = "系統外觀") {
                SettingsTile(
                    icon = Icons.Default.DarkMode,
                    iconColor = AppColors.primary,
                    title = "深色模式",
                    subtitle = if (currentDarkMode) "已啟用" else "跟隨系統",
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

        // 滑動確認清除抽屜
        if (showClearSessionDrawer) {
            SwipeToConfirmDrawer(
                title = "確認清除會話",
                description = "此操作將抹除本機儲存之 ss_me 金鑰及憑證，需重新驗證帳號登入。",
                sliderTitle = "SLIDE TO CLEAR",
                onConfirm = {
                    viewModel.clearSession()
                    showClearSessionDrawer = false
                },
                onDismiss = { showClearSessionDrawer = false }
            )
        }
    }
}
