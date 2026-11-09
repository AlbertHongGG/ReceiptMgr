package com.receiptmgr.app.domain.model.log

import androidx.compose.ui.graphics.vector.ImageVector

/**
 * API 日誌分頁配置模型 (ApiLogTabConfig)。
 * 封裝分頁識別元、名稱、圖示、對應的 API 類別以及專屬空狀態中繼資料。
 */
data class ApiLogTabConfig(
    val id: String,
    val tabName: String,
    val tabIcon: ImageVector,
    val category: ApiCategoryType?, // null 代表「全部」
    val emptyStateTitle: String,
    val emptyStateDescription: String,
    val emptyStateIcon: ImageVector
)

/**
 * 系統預設 API 日誌分頁型錄 (DefaultApiLogConfigs)。
 * 嚴格遵循「系統有什麼功能，才提供什麼 Tab」原則。
 * 現階段支援：全部、帳號授權、發票清單、發票明細。
 */
object DefaultApiLogConfigs {
    val allTabs: List<ApiLogTabConfig> = listOf(
        ApiLogTabConfig(
            id = ApiCategoryType.ALL.id,
            tabName = ApiCategoryType.ALL.displayName,
            tabIcon = ApiCategoryType.ALL.icon,
            category = null,
            emptyStateTitle = ApiCategoryType.ALL.emptyTitle,
            emptyStateDescription = ApiCategoryType.ALL.emptyDescription,
            emptyStateIcon = ApiCategoryType.ALL.icon
        ),
        ApiLogTabConfig(
            id = ApiCategoryType.AUTH.id,
            tabName = ApiCategoryType.AUTH.displayName,
            tabIcon = ApiCategoryType.AUTH.icon,
            category = ApiCategoryType.AUTH,
            emptyStateTitle = ApiCategoryType.AUTH.emptyTitle,
            emptyStateDescription = ApiCategoryType.AUTH.emptyDescription,
            emptyStateIcon = ApiCategoryType.AUTH.icon
        ),
        ApiLogTabConfig(
            id = ApiCategoryType.INVOICE_LIST.id,
            tabName = ApiCategoryType.INVOICE_LIST.displayName,
            tabIcon = ApiCategoryType.INVOICE_LIST.icon,
            category = ApiCategoryType.INVOICE_LIST,
            emptyStateTitle = ApiCategoryType.INVOICE_LIST.emptyTitle,
            emptyStateDescription = ApiCategoryType.INVOICE_LIST.emptyDescription,
            emptyStateIcon = ApiCategoryType.INVOICE_LIST.icon
        ),
        ApiLogTabConfig(
            id = ApiCategoryType.INVOICE_DETAIL.id,
            tabName = ApiCategoryType.INVOICE_DETAIL.displayName,
            tabIcon = ApiCategoryType.INVOICE_DETAIL.icon,
            category = ApiCategoryType.INVOICE_DETAIL,
            emptyStateTitle = ApiCategoryType.INVOICE_DETAIL.emptyTitle,
            emptyStateDescription = ApiCategoryType.INVOICE_DETAIL.emptyDescription,
            emptyStateIcon = ApiCategoryType.INVOICE_DETAIL.icon
        )
    )
}
