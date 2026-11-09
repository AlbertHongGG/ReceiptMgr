package com.receiptmgr.app.presentation.features.invoice.list

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBackIos
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.receiptmgr.app.domain.model.invoice.BillingPeriod
import com.receiptmgr.app.domain.model.invoice.InvoiceHeader
import com.receiptmgr.app.presentation.components.ImmersiveScaffold
import com.receiptmgr.app.presentation.components.NavigationBarsSafeSpacer
import com.receiptmgr.app.presentation.components.PremiumConfigHeader
import com.receiptmgr.app.presentation.components.PremiumEmptyState
import com.receiptmgr.app.presentation.navigation.contract.LocalCupertinoNavigator
import com.receiptmgr.app.presentation.navigation.model.AppRoute
import com.receiptmgr.app.presentation.theme.AppColors

@Composable
fun InvoiceListScreen(
    viewModel: InvoiceListViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val navigator = LocalCupertinoNavigator.current
    val isDark = isSystemInDarkTheme()

    val textPrimary = if (isDark) AppColors.textPrimaryDark else AppColors.textPrimaryLight
    val textSecondary = if (isDark) AppColors.textSecondaryDark else AppColors.textSecondaryLight

    ImmersiveScaffold {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 頂部導航
            PremiumConfigHeader(
                title = "月分發票明細",
                subtitle = "E-INVOICE & EXPENSE LEDGER",
                showOrbWatermark = false,
                actions = {
                    IconButton(
                        onClick = { viewModel.refresh() },
                        enabled = !uiState.isLoading && !uiState.isRefreshing
                    ) {
                        if (uiState.isRefreshing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = textPrimary
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "重新整理",
                                tint = textPrimary
                            )
                        }
                    }
                }
            )

            // 雙月份期別切換器
            BillingPeriodSelector(
                period = uiState.currentPeriod,
                onPrevious = { viewModel.selectPreviousPeriod() },
                onNext = { viewModel.selectNextPeriod() },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // 本期累積消費總覽卡
            PeriodExpenseSummaryCard(
                totalAmount = uiState.summary?.totalAmount ?: 0L,
                invoiceCount = uiState.summary?.invoiceCount ?: 0,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 下方直接展開發票卡片清單 (無三段式膠囊分頁欄)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    uiState.isLoading -> {
                        InvoiceListSkeleton()
                    }

                    uiState.errorMessage != null && uiState.summary == null -> {
                        PremiumEmptyState(
                            title = "載入失敗",
                            description = uiState.errorMessage ?: "連線逾時或通訊異常",
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong
                        )
                    }

                    uiState.summary?.invoices.isNullOrEmpty() -> {
                        PremiumEmptyState(
                            title = "本期尚無發票紀錄",
                            description = "尚未於此雙月期別開立任何雲端發票",
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong
                        )
                    }

                    else -> {
                        val invoices = uiState.summary?.invoices ?: emptyList()
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(
                                items = invoices,
                                key = { it.invNum }
                            ) { invoice ->
                                InvoiceListItemCard(
                                    invoice = invoice,
                                    onClick = {
                                        navigator.push(
                                            AppRoute.InvoiceDetail(
                                                invNum = invoice.invNum,
                                                invDate = invoice.invDate
                                            )
                                        )
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }

                            item {
                                NavigationBarsSafeSpacer()
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * 雙月份期別切換器元件 (BillingPeriodSelector)
 */
@Composable
private fun BillingPeriodSelector(
    period: BillingPeriod,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(16.dp)
    val textPrimary = if (isDark) AppColors.textPrimaryDark else AppColors.textPrimaryLight
    val textSecondary = if (isDark) AppColors.textSecondaryDark else AppColors.textSecondaryLight
    val isCurrent = period == BillingPeriod.current()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isDark) AppColors.surfaceDark else Color.White)
            .border(
                width = 1.dp,
                color = if (isDark) AppColors.borderDark else AppColors.borderLight,
                shape = shape
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBackIos,
                    contentDescription = "上一期別",
                    modifier = Modifier.size(16.dp),
                    tint = textSecondary
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = period.displayTitle,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )

                    if (isCurrent) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "當期",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimary
                            )
                        }
                    }
                }

                Text(
                    text = "${period.queryStartDate} ~ ${period.queryEndDate}",
                    fontSize = 11.sp,
                    color = textSecondary
                )
            }

            IconButton(
                onClick = onNext,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                    contentDescription = "下一期別",
                    modifier = Modifier.size(16.dp),
                    tint = textSecondary
                )
            }
        }
    }
}

/**
 * 本期消費總覽統計卡 (PeriodExpenseSummaryCard)
 */
@Composable
private fun PeriodExpenseSummaryCard(
    totalAmount: Long,
    invoiceCount: Int,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(20.dp)
    val textPrimary = if (isDark) AppColors.textPrimaryDark else AppColors.textPrimaryLight
    val textSecondary = if (isDark) AppColors.textSecondaryDark else AppColors.textSecondaryLight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 6.dp,
                shape = shape,
                spotColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.08f),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.20f) else Color.Black.copy(alpha = 0.04f)
            )
            .clip(shape)
            .background(if (isDark) AppColors.surfaceDark else Color.White)
            .border(
                width = 1.dp,
                color = if (isDark) AppColors.borderDark else AppColors.borderLight,
                shape = shape
            )
            .padding(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "本期累積消費",
                    fontSize = 12.sp,
                    color = textSecondary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "NT$ $totalAmount",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary
                )
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "共 $invoiceCount 筆發票",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = textPrimary
                )
            }
        }
    }
}

/**
 * 單筆發票卡片元件 (InvoiceListItemCard)
 * 純淨呈現：商家、時間、發票號碼與金額，無狀態文字與中獎標籤。
 */
@Composable
private fun InvoiceListItemCard(
    invoice: InvoiceHeader,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(16.dp)
    val textPrimary = if (isDark) AppColors.textPrimaryDark else AppColors.textPrimaryLight
    val textSecondary = if (isDark) AppColors.textSecondaryDark else AppColors.textSecondaryLight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(if (isDark) AppColors.surfaceDark else Color.White)
            .border(
                width = 1.dp,
                color = if (isDark) AppColors.borderDark else AppColors.borderLight,
                shape = shape
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = invoice.sellerName.ifBlank { "電子發票" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = invoice.fullDateTime,
                        fontSize = 12.sp,
                        color = textSecondary
                    )

                    Text(
                        text = invoice.formattedInvNum,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Text(
                text = "$${invoice.amount}",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(modifier = Modifier.width(4.dp))

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "查看明細",
                modifier = Modifier.size(18.dp),
                tint = textSecondary
            )
        }
    }
}

/**
 * 載入中骨架屏 (InvoiceListSkeleton)
 */
@Composable
private fun InvoiceListSkeleton() {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(16.dp)
    val placeholderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    val transition = rememberInfiniteTransition(label = "skeletonPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeletonAlpha"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(5) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .clip(shape)
                    .background(if (isDark) AppColors.surfaceDark else Color.White)
                    .border(
                        width = 1.dp,
                        color = if (isDark) AppColors.borderDark else AppColors.borderLight,
                        shape = shape
                    )
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .width(120.dp)
                                .height(14.dp)
                                .alpha(alpha)
                                .clip(RoundedCornerShape(4.dp))
                                .background(placeholderColor)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .width(180.dp)
                                .height(11.dp)
                                .alpha(alpha)
                                .clip(RoundedCornerShape(4.dp))
                                .background(placeholderColor)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(50.dp)
                            .height(18.dp)
                            .alpha(alpha)
                            .clip(RoundedCornerShape(4.dp))
                            .background(placeholderColor)
                    )
                }
            }
        }
    }
}
