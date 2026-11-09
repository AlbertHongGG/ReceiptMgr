package com.receiptmgr.app.domain.model.log

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.outlined.Key
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * 台灣電子發票系統 API 分類列舉。
 * 支援分頁識別、專屬名稱、圖示及空狀態資訊。
 */
enum class ApiCategoryType(
    val id: String,
    val displayName: String,
    val icon: ImageVector,
    val emptyTitle: String,
    val emptyDescription: String
) {
    ALL(
        id = "all",
        displayName = "全部",
        icon = Icons.Default.SwapHoriz,
        emptyTitle = "尚無任何 API 紀錄",
        emptyDescription = "尚未發送任何雲端發票或授權請求"
    ),
    AUTH(
        id = "auth",
        displayName = "帳號授權",
        icon = Icons.Outlined.Key,
        emptyTitle = "尚無帳號授權日誌",
        emptyDescription = "尚未發送 UIA 憑證簽發或登入驗證請求"
    ),
    INVOICE_LIST(
        id = "inv_list",
        displayName = "發票清單",
        icon = Icons.AutoMirrored.Filled.ReceiptLong,
        emptyTitle = "尚無發票清單日誌",
        emptyDescription = "尚未向財政部查詢載具發票期別清單"
    ),
    INVOICE_DETAIL(
        id = "inv_detail",
        displayName = "發票明細",
        icon = Icons.Default.Description,
        emptyTitle = "尚無發票明細日誌",
        emptyDescription = "尚未查詢單張發票消費品項明細"
    ),
    CARRIER(
        id = "carrier",
        displayName = "載具歸戶",
        icon = Icons.Default.CreditCard,
        emptyTitle = "尚無載具歸戶日誌",
        emptyDescription = "尚未發送手機條碼歸戶查詢請求"
    ),
    AWARD(
        id = "award",
        displayName = "中獎查詢",
        icon = Icons.Default.EmojiEvents,
        emptyTitle = "尚無中獎查詢日誌",
        emptyDescription = "尚未發送統一發票對獎與獎金查詢請求"
    );

    companion object {
        fun fromId(id: String?): ApiCategoryType? {
            if (id == null) return null
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) }
        }

        fun fromEndpoint(endpoint: String): ApiCategoryType {
            return when {
                endpoint.contains("login", ignoreCase = true) || endpoint.contains("auth", ignoreCase = true) || endpoint.contains("uia", ignoreCase = true) -> AUTH
                endpoint.contains("carrierInvChk", ignoreCase = true) || endpoint.contains("invList", ignoreCase = true) -> INVOICE_LIST
                endpoint.contains("carrierInvDetail", ignoreCase = true) || endpoint.contains("invDetail", ignoreCase = true) -> INVOICE_DETAIL
                endpoint.contains("carrierList", ignoreCase = true) || endpoint.contains("carrier", ignoreCase = true) -> CARRIER
                endpoint.contains("award", ignoreCase = true) || endpoint.contains("lottery", ignoreCase = true) -> AWARD
                else -> ALL
            }
        }
    }
}
