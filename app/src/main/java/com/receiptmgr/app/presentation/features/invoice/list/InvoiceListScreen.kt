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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.receiptmgr.app.domain.model.invoice.BillingPeriod
import com.receiptmgr.app.domain.model.invoice.InvoiceHeader
import com.receiptmgr.app.presentation.components.ImmersiveScaffold
import com.receiptmgr.app.presentation.components.NavigationBarsSafeSpacer
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

    val currentPeriod = uiState.currentPeriod
    val savedPosition = viewModel.getScrollPosition(currentPeriod)
    val listState = rememberSaveable(
        currentPeriod,
        saver = LazyListState.Saver
    ) {
        LazyListState(
            firstVisibleItemIndex = savedPosition.firstVisibleIndex,
            firstVisibleItemScrollOffset = savedPosition.firstVisibleOffset
        )
    }

    LaunchedEffect(listState.firstVisibleItemIndex, listState.firstVisibleItemScrollOffset) {
        viewModel.saveScrollPosition(
            period = currentPeriod,
            index = listState.firstVisibleItemIndex,
            offset = listState.firstVisibleItemScrollOffset
        )
    }

    ImmersiveScaffold {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // 頂部間距 (無冗餘 Header，純粹沉浸體驗)
            Spacer(modifier = Modifier.height(12.dp))

            // 區域 1: 輕量沉浸期別選取帶 (水平置中對稱，遠離左邊界 36dp 滑動衝突區，禁止看未來)
            PeriodNavigationStrip(
                period = uiState.currentPeriod,
                canGoNext = uiState.canGoNext,
                onPrevious = { viewModel.selectPreviousPeriod() },
                onNext = { viewModel.selectNextPeriod() },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )

            // 區域 2: 大器戰情消費看板 Hero Dashboard (視覺重心，大字體金額)
            ExpenseHeroDashboard(
                totalAmount = uiState.currentSummary?.totalAmount ?: 0L,
                invoiceCount = uiState.currentSummary?.invoiceCount ?: 0,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )

            // 區域 3: 固定分區抬頭 (恆定固定，永不隨清單滾動捲走，載入時亦恆定存在)
            InvoiceListSectionHeader(
                isLoading = uiState.isCurrentLoading,
                invoiceCount = uiState.currentSummary?.invoiceCount,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 8.dp)
            )

            // 區域 4: 語意過渡分區標籤與精緻化帳務清單
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                when {
                    uiState.isCurrentLoading && uiState.currentSummary == null -> {
                        InvoiceListSkeleton()
                    }

                    uiState.isCurrentOutdated -> {
                        PremiumEmptyState(
                            title = "超出查詢期限",
                            description = "官方僅提供近 6 個月發票",
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 32.dp)
                        )
                    }

                    uiState.currentError != null && uiState.currentSummary == null -> {
                        PremiumEmptyState(
                            title = "載入失敗",
                            description = uiState.currentError ?: "請稍後再試",
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 32.dp)
                        )
                    }

                    uiState.currentSummary?.invoices.isNullOrEmpty() -> {
                        PremiumEmptyState(
                            title = "無發票紀錄",
                            description = "本期尚無消費發票",
                            icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(bottom = 32.dp)
                        )
                    }

                    else -> {
                        val invoices = uiState.currentSummary?.invoices ?: emptyList()
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // 發票卡片清單 (無品項圖示、商家抬頭完整多行換行、號碼與日期單行平鋪)
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
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
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
 * 輕量沉浸期別選取帶 (PeriodNavigationStrip)
 * 規格保證：
 * 1. 排版整體水平置中，遠離螢幕左側 36dp 滑動手勢碰撞區。
 * 2. 使用對稱幾何向量 KeyboardArrowLeft / Right，告別視覺歪斜。
 * 3. 達當前最新期別時禁用右側箭頭，鎖定無法檢視未來月份。
 */
@Composable
private fun PeriodNavigationStrip(
    period: BillingPeriod,
    canGoNext: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val textPrimary = if (isDark) AppColors.textPrimaryDark else AppColors.textPrimaryLight
    val textSecondary = if (isDark) AppColors.textSecondaryDark else AppColors.textSecondaryLight

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        IconButton(
            onClick = onPrevious,
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = "上一期別",
                modifier = Modifier.size(24.dp),
                tint = textSecondary
            )
        }

        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = period.displayTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = "${period.apiStartDate} ~ ${period.queryEndDate}",
                fontSize = 12.sp,
                color = textSecondary
            )
        }

        IconButton(
            onClick = onNext,
            enabled = canGoNext,
            modifier = Modifier
                .size(44.dp)
                .alpha(if (canGoNext) 1f else 0.25f)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "下一期別",
                modifier = Modifier.size(24.dp),
                tint = textSecondary
            )
        }
    }
}

/**
 * 大器戰情消費看板 (ExpenseHeroDashboard)
 * 規格保證：
 * 建立全頁強烈的視覺焦點 (Focal Point)，大字體 Typography 凸顯金額。
 */
@Composable
private fun ExpenseHeroDashboard(
    totalAmount: Long,
    invoiceCount: Int,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(22.dp)
    val textPrimary = if (isDark) AppColors.textPrimaryDark else AppColors.textPrimaryLight
    val textSecondary = if (isDark) AppColors.textSecondaryDark else AppColors.textSecondaryLight

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = shape,
                spotColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.08f),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.04f)
            )
            .clip(shape)
            .background(if (isDark) AppColors.surfaceDark else Color.White)
            .border(
                width = 1.dp,
                color = if (isDark) AppColors.borderDark else AppColors.borderLight,
                shape = shape
            )
            .padding(horizontal = 22.dp, vertical = 20.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "本期累積消費",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = textSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "NT$ $totalAmount",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = textPrimary
                )

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Text(
                        text = "共 $invoiceCount 筆發票",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary
                    )
                }
            }
        }
    }
}

/**
 * 發票清單固定分區抬頭 (InvoiceListSectionHeader)
 * 規格保證：
 * 1. 恆定固定於看板下方，絕不隨列表滾動捲走。
 * 2. 載入中 (isLoading) 恆定展示「消費紀錄」，右側筆數展示微骨架脈衝塊。
 * 3. 載入完成平滑展示「共 X 筆」。
 */
@Composable
private fun InvoiceListSectionHeader(
    isLoading: Boolean,
    invoiceCount: Int?,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val textSecondary = if (isDark) AppColors.textSecondaryDark else AppColors.textSecondaryLight
    val placeholderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)

    val transition = rememberInfiniteTransition(label = "headerSkeletonPulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(800),
            repeatMode = RepeatMode.Reverse
        ),
        label = "headerSkeletonAlpha"
    )

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "消費紀錄",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = textSecondary
        )

        if (isLoading && invoiceCount == null) {
            Box(
                modifier = Modifier
                    .width(48.dp)
                    .height(13.dp)
                    .alpha(alpha)
                    .clip(RoundedCornerShape(4.dp))
                    .background(placeholderColor)
            )
        } else if (invoiceCount != null) {
            Text(
                text = "共 $invoiceCount 筆",
                fontSize = 12.sp,
                color = textSecondary
            )
        }
    }
}

/**
 * 精緻化發票卡片 (InvoiceListItemCard)
 * 規格保證：
 * 1. 無品項圖示，釋放完整橫向空間。
 * 2. 商家抬頭完整顯示，支援自然多行換行，不截斷。
 * 3. 發票號碼與開立日期單行平鋪，絕不發生斷字折行。
 * 4. 僅顯示開立日期 (invoice.invDate)，不顯示秒數時間。
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
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // 商家抬頭：完整顯示，支援自然折行，無 ellipsis
                Text(
                    text = invoice.sellerName.ifBlank { "電子發票" },
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimary,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(6.dp))

                // 發票號碼與日期：單行平鋪
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = invoice.formattedInvNum,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )

                    Text(
                        text = "·",
                        fontSize = 12.sp,
                        color = textSecondary
                    )

                    Text(
                        text = invoice.invDate,
                        fontSize = 12.sp,
                        color = textSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            // 金額與進入明細箭頭
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
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
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
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
                    .padding(horizontal = 18.dp, vertical = 14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .width(140.dp)
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
                            .width(52.dp)
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
