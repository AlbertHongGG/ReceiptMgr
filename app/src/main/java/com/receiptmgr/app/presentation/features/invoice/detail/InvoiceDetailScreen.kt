package com.receiptmgr.app.presentation.features.invoice.detail

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
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.receiptmgr.app.domain.model.invoice.InvoiceDetail
import com.receiptmgr.app.presentation.components.ExactCenteredWithTrailingLayout
import com.receiptmgr.app.presentation.components.ImmersiveScaffold
import com.receiptmgr.app.presentation.components.NavigationBarsSafeSpacer
import com.receiptmgr.app.presentation.components.PremiumEmptyState
import com.receiptmgr.app.presentation.notification.NotificationController
import com.receiptmgr.app.presentation.theme.AppColors

@Composable
fun InvoiceDetailScreen(
    viewModel: InvoiceDetailViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val clipboardManager = LocalClipboardManager.current

    val textPrimary = if (isDark) AppColors.textPrimaryDark else AppColors.textPrimaryLight
    val textSecondary = if (isDark) AppColors.textSecondaryDark else AppColors.textSecondaryLight

    ImmersiveScaffold {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 頂部間距 (統一右滑返回，無上一頁按鈕)
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                color = textPrimary
                            )
                        }
                    }

                    uiState.errorMessage != null && uiState.detail == null -> {
                        PremiumEmptyState(
                            title = "載入失敗",
                            description = uiState.errorMessage ?: "無法取得品項明細",
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 32.dp)
                        )
                    }

                    uiState.detail != null -> {
                        val detail = uiState.detail!!
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                        ) {
                            // 數位熱感實體收據卡片
                            DigitalReceiptCard(
                                detail = detail,
                                onCopyInvoiceNumber = {
                                    clipboardManager.setText(AnnotatedString(detail.invNum))
                                    NotificationController.showSuccess("已複製發票號碼")
                                },
                                onExportToEquityTracker = {
                                    viewModel.exportToEquityTracker()
                                }
                            )

                            Spacer(modifier = Modifier.height(24.dp))
                            NavigationBarsSafeSpacer()
                        }
                    }
                }
            }
        }
    }
}

/**
 * 極簡數位收據卡片元件 (DigitalReceiptCard)
 * 規格保證：
 * 1. 發票號碼本體絕對幾何置中 (ExactCenteredWithTrailingLayout)，複製圖示置於其右側不干擾居中。
 * 2. 徹底移除上方 Header 標題與副標題。
 * 3. 徹底移除「開立已確認」狀態與中獎資訊。
 * 4. 底部無冗餘按鈕。
 */
@Composable
private fun DigitalReceiptCard(
    detail: InvoiceDetail,
    onCopyInvoiceNumber: () -> Unit,
    onExportToEquityTracker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(20.dp)
    val textPrimary = if (isDark) AppColors.textPrimaryDark else AppColors.textPrimaryLight
    val textSecondary = if (isDark) AppColors.textSecondaryDark else AppColors.textSecondaryLight
    val dividerColor = if (isDark) AppColors.dividerDark else AppColors.divider

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
                spotColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.10f),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.05f)
            )
            .clip(shape)
            .background(if (isDark) AppColors.surfaceDark else Color.White)
            .border(
                width = 1.dp,
                color = if (isDark) AppColors.borderDark else AppColors.borderLight,
                shape = shape
            )
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            // 收據頂部
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "電子發票證明聯",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = detail.formattedPeriodTitle,
                    fontSize = 13.sp,
                    color = textSecondary
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 發票號碼字串先幾何置中，操作圖示錨定於右側 (不擠壓置中性)
                ExactCenteredWithTrailingLayout(
                    centerContent = {
                        Text(
                            text = detail.formattedInvNum,
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = textPrimary
                        )
                    },
                    trailingContent = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = onCopyInvoiceNumber,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "複製發票號碼",
                                    modifier = Modifier.size(17.dp),
                                    tint = textSecondary
                                )
                            }
                            IconButton(
                                onClick = onExportToEquityTracker,
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Share,
                                    contentDescription = "分享至 EquityTracker 記帳",
                                    modifier = Modifier.size(17.dp),
                                    tint = textSecondary
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = detail.fullDateTime,
                    fontSize = 12.sp,
                    color = textSecondary
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = dividerColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // 營業人資訊
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReceiptInfoRow(label = "營業人", value = detail.sellerName.ifBlank { "-" })
                ReceiptInfoRow(label = "統一編號", value = detail.sellerBan.ifBlank { "-" })
                if (detail.buyerBan.isNotBlank()) {
                    ReceiptInfoRow(label = "買方統編", value = detail.buyerBan)
                } else {
                    ReceiptInfoRow(label = "買方", value = "個人消費")
                }
                if (detail.sellerAddress.isNotBlank()) {
                    ReceiptInfoRow(label = "地址", value = detail.sellerAddress)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = dividerColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // 品項清單表格
            Text(
                text = "消費明細",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 表頭
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "品名", fontSize = 12.sp, color = textSecondary, modifier = Modifier.weight(2.2f))
                Text(text = "數量", fontSize = 12.sp, color = textSecondary, textAlign = TextAlign.Center, modifier = Modifier.weight(0.8f))
                Text(text = "單價", fontSize = 12.sp, color = textSecondary, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
                Text(text = "小計", fontSize = 12.sp, color = textSecondary, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
            }

            HorizontalDivider(color = dividerColor, thickness = 0.5.dp)

            // 逐行項目
            if (detail.items.isEmpty()) {
                Text(
                    text = "此筆發票未回傳品項明細資訊",
                    fontSize = 12.sp,
                    color = textSecondary,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                detail.items.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.description.ifBlank { "一般品項" },
                            fontSize = 13.sp,
                            color = textPrimary,
                            modifier = Modifier.weight(2.2f)
                        )
                        Text(
                            text = item.quantity,
                            fontSize = 13.sp,
                            color = textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(0.8f)
                        )
                        Text(
                            text = "$${item.unitPrice}",
                            fontSize = 13.sp,
                            color = textSecondary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "$${item.amount}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = textPrimary,
                            textAlign = TextAlign.End,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = dividerColor, thickness = 1.dp)
            Spacer(modifier = Modifier.height(16.dp))

            // 總計金額與載具
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "總計金額",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )

                Text(
                    text = "NT$ ${detail.amount}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            }

            if (detail.cardNo.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "載具號碼",
                        fontSize = 12.sp,
                        color = textSecondary
                    )

                    Text(
                        text = detail.cardNo,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = textSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun ReceiptInfoRow(label: String, value: String) {
    val isDark = isSystemInDarkTheme()
    val textPrimary = if (isDark) AppColors.textPrimaryDark else AppColors.textPrimaryLight
    val textSecondary = if (isDark) AppColors.textSecondaryDark else AppColors.textSecondaryLight

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = textSecondary,
            modifier = Modifier.width(68.dp)
        )
        Text(
            text = value,
            fontSize = 12.sp,
            color = textPrimary,
            modifier = Modifier.weight(1f)
        )
    }
}
