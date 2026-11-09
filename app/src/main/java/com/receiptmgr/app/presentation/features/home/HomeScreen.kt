package com.receiptmgr.app.presentation.features.home

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.receiptmgr.app.domain.model.auth.SessionCredentials
import com.receiptmgr.app.domain.model.auth.SessionState
import com.receiptmgr.app.presentation.components.ImmersiveScaffold
import com.receiptmgr.app.presentation.components.NavigationBarsSafeSpacer
import com.receiptmgr.app.presentation.components.PremiumConfigHeader
import com.receiptmgr.app.presentation.components.SettingsDivider
import com.receiptmgr.app.presentation.components.SettingsSection
import com.receiptmgr.app.presentation.components.SettingsTile
import com.receiptmgr.app.presentation.navigation.contract.LocalCupertinoNavigator
import com.receiptmgr.app.presentation.navigation.model.AppRoute
import com.receiptmgr.app.presentation.notification.NotificationController
import com.receiptmgr.app.presentation.theme.AppColors

@Composable
fun HomeScreen(
    viewModel: HomeViewModel
) {
    val navigator = LocalCupertinoNavigator.current
    val clipboardManager = LocalClipboardManager.current
    val sessionState by viewModel.sessionState.collectAsState()
    val isDark = isSystemInDarkTheme()

    val textPrimary = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    ImmersiveScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 頂部大標題（不使用 ThinkingOrb，純淨無上一頁按鈕）
            PremiumConfigHeader(
                title = "雲端發票",
                subtitle = "CARRIER & INVOICE HUB",
                showOrbWatermark = false,
                actions = {
                    IconButton(onClick = { navigator.push(AppRoute.Settings) }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "設定",
                            tint = textPrimary
                        )
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // 1. 核心載具條碼大卡 (Carrier Barcode Chassis) - 統一左右 16.dp 外距展開
            when (val state = sessionState) {
                is SessionState.Authenticated -> {
                    CarrierBarcodeCard(
                        session = state.credentials,
                        onCopyBarcode = {
                            clipboardManager.setText(AnnotatedString(state.credentials.barcode))
                            NotificationController.showSuccess("已複製載具條碼")
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                is SessionState.Expired -> {
                    UnauthenticatedBarcodeCard(
                        title = "會話已過期",
                        subtitle = "載具 ${state.credentials.barcode}，請重新登入",
                        onLogin = { navigator.push(AppRoute.Login) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                is SessionState.Unauthenticated -> {
                    UnauthenticatedBarcodeCard(
                        title = "尚未綁定載具",
                        subtitle = "登入以啟用載具條碼與會話憑證",
                        onLogin = { navigator.push(AppRoute.Login) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2. 發票業務服務插槽 (SettingsSection 自帶 horizontal = 16.dp，無第三個選項)
            SettingsSection(title = "發票業務") {
                SettingsTile(
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    iconColor = AppColors.primary,
                    title = "月分發票明細",
                    subtitle = "Phase 2 待續：由 ss_me 請求 UPI 發票清單",
                    onTap = {
                        if (sessionState is SessionState.Authenticated) {
                            NotificationController.showInfo("發票清單 API 將於 Phase 2 開展實作")
                        } else {
                            navigator.push(AppRoute.Login)
                        }
                    }
                )
                SettingsDivider()
                SettingsTile(
                    icon = Icons.Default.Pin,
                    iconColor = AppColors.primary,
                    title = "開獎號碼兌獎",
                    subtitle = "Phase 2 待續：自動比對當期統一發票中獎號碼",
                    onTap = {
                        NotificationController.showInfo("開獎號碼比對功能將於 Phase 2 推出")
                    }
                )
            }

            NavigationBarsSafeSpacer()
        }
    }
}

/**
 * 手機條碼大卡：結帳時供店員掃描的實用發票載具條碼卡片。
 */
@Composable
private fun CarrierBarcodeCard(
    session: SessionCredentials,
    onCopyBarcode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
                spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.12f),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.06f)
            )
            .clip(shape)
            .background(if (isDark) AppColors.surfaceDark else Color.White)
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                shape = shape
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 頂部狀態列
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (isDark) Color.White else AppColors.textPrimaryLight)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "手機條碼載具",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.White else AppColors.textPrimaryLight
                )

                Spacer(modifier = Modifier.weight(1f))

                // 灰階膠囊標籤
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.05f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "有效 • 剩餘 ${session.remainingDays} 天",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (isDark) Color.White.copy(alpha = 0.8f) else Color.Black.copy(alpha = 0.7f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // 條碼畫布 (純色高對比 Code39 視覺)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // 條碼條紋寬度陣列 (交替寬窄條)
                    val pattern = intArrayOf(
                        3, 2, 4, 1, 3, 2, 5, 2, 1, 4, 2, 3, 5, 1, 2, 4, 2, 3, 1, 5, 2, 4, 1, 3, 2, 5,
                        1, 4, 2, 3, 5, 1, 2, 4, 3, 2, 1, 5, 2, 4, 1, 3, 5, 2, 1, 4, 2, 3, 1, 5, 2, 4
                    )
                    val totalWeight = pattern.sum()
                    val unit = canvasWidth / (totalWeight + 10)

                    var currentX = unit * 5
                    for (i in pattern.indices) {
                        val barWidth = pattern[i] * unit
                        if (i % 2 == 0) {
                            drawRect(
                                color = Color.Black,
                                topLeft = Offset(currentX, 0f),
                                size = Size(barWidth, canvasHeight)
                            )
                        }
                        currentX += barWidth
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 載具代碼文字 + 複製按鈕
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = session.barcode,
                    fontSize = 18.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = if (isDark) Color.White else AppColors.textPrimaryLight
                )

                Spacer(modifier = Modifier.width(8.dp))

                IconButton(
                    onClick = onCopyBarcode,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "複製條碼",
                        modifier = Modifier.size(16.dp),
                        tint = AppColors.secondary
                    )
                }
            }
        }
    }
}

/**
 * 未授權時之條碼引導卡片。
 */
@Composable
private fun UnauthenticatedBarcodeCard(
    title: String,
    subtitle: String,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(20.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
                spotColor = if (isDark) Color.Black.copy(alpha = 0.45f) else Color.Black.copy(alpha = 0.12f),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.30f) else Color.Black.copy(alpha = 0.06f)
            )
            .clip(shape)
            .background(if (isDark) AppColors.surfaceDark else Color.White)
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                shape = shape
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = if (isDark) Color.White else AppColors.textPrimaryLight,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White else AppColors.textPrimaryLight
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = if (isDark) AppColors.textSecondaryDark else AppColors.textSecondaryLight,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // 深色高對比無彩色按鈕 (黑底白字 / 白底黑字)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color.White else AppColors.textPrimaryLight)
                    .clickable(onClick = onLogin),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "驗證登入 / 綁定載具",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.Black else Color.White
                )
            }
        }
    }
}
