package com.receiptmgr.app.presentation.features.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.receiptmgr.app.data.storage.ApiLogManager
import com.receiptmgr.app.domain.model.log.ApiCategoryType
import com.receiptmgr.app.domain.model.log.ApiLogModel
import com.receiptmgr.app.domain.model.log.DefaultApiLogConfigs
import com.receiptmgr.app.presentation.components.LensWisePillTabBar
import com.receiptmgr.app.presentation.components.PremiumEmptyState
import com.receiptmgr.app.presentation.components.SwipeToConfirmDrawer
import com.receiptmgr.app.presentation.components.trash.DragDropTrashContainer
import com.receiptmgr.app.presentation.components.trash.GlassmorphicTrashControl
import com.receiptmgr.app.presentation.components.trash.draggableToTrash
import com.receiptmgr.app.presentation.components.trash.rememberDragDropTrashState
import com.receiptmgr.app.presentation.navigation.contract.LocalCupertinoNavigator
import com.receiptmgr.app.presentation.navigation.model.AppRoute
import com.receiptmgr.app.presentation.theme.AppColors
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import kotlinx.coroutines.launch

/**
 * API 通訊請求日誌介面 (ApiLogViewerScreen)。
 * 1:1 對齊 LensWise 架構與版面規範：
 * - 依系統現行支援功能提供 Tab（全部、帳號授權），具備自適應欄寬演算法。
 * - 空狀態垂直、水平絕對置中，徹底杜絕跑版。
 * - 支援列表卡片點擊查看完整 JSON 樹狀詳情 (按需延遲載入 rawJson)。
 * - 採用鮮明的語意狀態色彩（成功綠、客戶端錯誤紅、伺服器錯誤深紅、Method 藍），拒絕黑白模糊。
 * - 支援長按日誌卡片拖曳至底部垃圾桶即時銷毀單筆。
 * - 底部浮空毛玻璃垃圾桶 (GlassmorphicTrashControl)，點擊開啟滑動抹除抽屜 (SwipeToConfirmDrawer)。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiLogViewerScreen(
    apiLogManager: ApiLogManager,
    modifier: Modifier = Modifier
) {
    val navigator = LocalCupertinoNavigator.current
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val hazeState = remember { HazeState() }
    val dragDropState = rememberDragDropTrashState<ApiLogModel>()
    val logs by apiLogManager.logsFlow.collectAsState()

    val tabs = remember { DefaultApiLogConfigs.allTabs }
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var showClearDrawer by remember { mutableStateOf(false) }

    val currentTab = tabs[selectedTabIndex.coerceIn(0, tabs.size - 1)]

    // 依據目前選取之固定分頁篩選日誌
    val filteredLogs = remember(logs, currentTab) {
        val targetCategory = currentTab.category
        if (targetCategory == null || targetCategory == ApiCategoryType.ALL) {
            logs
        } else {
            logs.filter { it.category == targetCategory }
        }
    }

    if (showClearDrawer) {
        SwipeToConfirmDrawer(
            title = "CLEAR ${currentTab.tabName.uppercase()} LOGS",
            onConfirm = {
                scope.launch {
                    apiLogManager.clearLogsByCategory(currentTab.category)
                    showClearDrawer = false
                }
            },
            onDismiss = { showClearDrawer = false }
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (isDark) AppColors.backgroundDark else AppColors.backgroundLight)
            .statusBarsPadding()
    ) {
        DragDropTrashContainer(
            state = dragDropState,
            onDropOnTrash = { item ->
                scope.launch {
                    apiLogManager.clearLog(item.file)
                }
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .haze(hazeState)
            ) {
                // 1. 頂部自適應滑動膠囊分頁欄 (Pill TabBar，等寬均分，Spring 動態平移)
                LensWisePillTabBar(
                    items = tabs,
                    selectedIndex = selectedTabIndex,
                    onItemSelected = { selectedTabIndex = it },
                    itemTitle = { it.tabName },
                    itemIcon = { it.tabIcon }
                )

                // 2. 分頁內容視窗 (有資料顯示列表，無資料固定呈現對應之 PremiumEmptyState 且絕對置中)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    if (filteredLogs.isEmpty()) {
                        PremiumEmptyState(
                            title = currentTab.emptyStateTitle,
                            subtitle = currentTab.emptyStateDescription,
                            icon = currentTab.emptyStateIcon,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(
                                start = 16.dp,
                                end = 16.dp,
                                top = 4.dp,
                                bottom = 100.dp
                            )
                        ) {
                            items(filteredLogs, key = { it.id }) { log ->
                                val isBeingDragged = dragDropState.isDragging && dragDropState.activeItem?.id == log.id
                                ApiLogCardContent(
                                    log = log,
                                    isDark = isDark,
                                    onClick = {
                                        navigator.push(AppRoute.ApiLogDetail(rawJson = log.resolveRawJson()))
                                    },
                                    modifier = Modifier
                                        .alpha(if (isBeingDragged) 0.3f else 1.0f)
                                        .draggableToTrash(
                                            item = log,
                                            state = dragDropState,
                                            feedback = {
                                                ApiLogCardContent(log = log, isDark = isDark)
                                            }
                                        )
                                )
                            }
                        }
                    }
                }
            }

            // 3. 底部懸浮毛玻璃垃圾桶 (僅當前分頁有日誌時呈現，支援點擊清空與拖曳刪除)
            if (filteredLogs.isNotEmpty()) {
                GlassmorphicTrashControl(
                    onClick = { showClearDrawer = true },
                    hazeState = hazeState,
                    isHovering = dragDropState.isHoveringTrash,
                    bottomSafetyPadding = 24.dp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .onGloballyPositioned { coordinates ->
                            dragDropState.trashBoundsInWindow = coordinates.boundsInWindow()
                        }
                )
            }
        }
    }
}

/**
 * 單筆 API 日誌項目卡片。
 * 1:1 對齊 LensWise 卡片比例，具備明確語意狀態色彩（成功綠、失敗紅、Method 藍）。
 */
@Composable
private fun ApiLogCardContent(
    log: ApiLogModel,
    isDark: Boolean,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    // 狀態語意顏色計算 (僅作用於狀態徽章，圖示保持中性黑白)
    val (statusBgColor, statusTextColor) = when {
        log.status in 200..299 -> {
            val greenLight = Color(0xFF059669)
            val greenDark = Color(0xFF34D399)
            Pair(
                Color(0xFF10B981).copy(alpha = if (isDark) 0.20f else 0.12f),
                if (isDark) greenDark else greenLight
            )
        }
        log.status in 400..499 -> {
            val red = Color(0xFFEF4444)
            Pair(
                red.copy(alpha = if (isDark) 0.22f else 0.12f),
                red
            )
        }
        log.status >= 500 -> {
            val darkRed = Color(0xFFDC2626)
            Pair(
                darkRed.copy(alpha = if (isDark) 0.25f else 0.15f),
                darkRed
            )
        }
        else -> {
            val neutral = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            Pair(
                neutral.copy(alpha = 0.12f),
                neutral
            )
        }
    }

    // 左側圖示：使用者指定維持中性黑白 Slate 配色，移除綠色
    val iconBgColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.05f)
    val iconTintColor = if (isDark) Color.White else Color(0xFF1E293B)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .shadow(
                elevation = 6.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.06f),
                spotColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.14f)
            )
            .clip(RoundedCornerShape(16.dp))
            .background(if (isDark) Color(0xFF1E1E1E) else Color.White)
            .border(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.06f),
                shape = RoundedCornerShape(16.dp)
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = ripple(bounded = true),
                        onClick = onClick
                    )
                } else Modifier
            )
            .padding(horizontal = 20.dp, vertical = 14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 左側中性黑白圖示圓圈
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(iconBgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = log.category.icon,
                    contentDescription = null,
                    tint = iconTintColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                // 第一行：項目主標題 + Method 徽章 + 狀態碼徽章
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = log.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) Color.White else Color(0xFF1E293B)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Method 標籤 (POST / GET 語意色彩)
                    val methodBg = if (log.method.equals("POST", ignoreCase = true)) {
                        Color(0xFF3B82F6).copy(alpha = if (isDark) 0.22f else 0.12f)
                    } else {
                        Color(0xFF10B981).copy(alpha = if (isDark) 0.22f else 0.12f)
                    }
                    val methodFg = if (log.method.equals("POST", ignoreCase = true)) {
                        if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)
                    } else {
                        if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(methodBg)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = log.method,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = methodFg
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // HTTP 狀態碼標籤 (綠 / 紅)
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusBgColor)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${log.status}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = statusTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // 第二行：格式化時間與耗時毫秒
                Text(
                    text = if (log.durationMs > 0) "${log.displayTime} · ${log.durationMs} ms" else log.displayTime,
                    fontSize = 12.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.54f) else Color(0xFF64748B)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "查看詳情",
                tint = if (isDark) Color.White.copy(alpha = 0.38f) else Color.Black.copy(alpha = 0.38f),
                modifier = Modifier.size(22.dp)
            )
        }
    }
}
