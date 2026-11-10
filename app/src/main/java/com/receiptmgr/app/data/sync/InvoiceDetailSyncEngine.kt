package com.receiptmgr.app.data.sync

import com.receiptmgr.app.domain.model.invoice.BillingPeriod
import com.receiptmgr.app.domain.model.invoice.InvoiceDetail
import kotlinx.coroutines.flow.StateFlow

/**
 * 背景同步引擎狀態
 */
sealed interface SyncEngineStatus {
    object Idle : SyncEngineStatus
    data class Syncing(
        val period: BillingPeriod,
        val totalPending: Int,
        val processedCount: Int
    ) : SyncEngineStatus
}

/**
 * 載具發票消費品項背景同步引擎介面 (InvoiceDetailSyncEngine)。
 *
 * 核心保證：
 * 1. 溫和非同步節流 (Throttled Sequential Queue)：背景預抓間隔 400ms，杜絕 Cloudflare/MOF 429 限流。
 * 2. 高優先權搶佔 (P0 Priority Preemption)：使用者點擊之發票立即插隊抓取，0ms 等待即刻持久化。
 * 3. 斷點續傳與冪等性 (Resume & Idempotence)：只抓取 has_detail == 0 之發票，絕不重複消耗流量。
 * 4. 超期防禦：已過期之雙月期別絕不發起無效網路輪詢。
 */
interface InvoiceDetailSyncEngine {

    /**
     * 排程執行指定期別之發票品項明細背景自動同步
     */
    fun scheduleBackgroundSync(period: BillingPeriod)

    /**
     * 立即以 P0 最高優先權同步單筆發票 (使用者點入收據明細時調用)
     */
    suspend fun fetchDetailImmediately(invNum: String, invDate: String): Result<InvoiceDetail>

    /**
     * 當前背景同步狀態流
     */
    val syncStatus: StateFlow<SyncEngineStatus>

    /**
     * 取消所有進行中之背景同步排程 (例如登出或切換使用者時)
     */
    fun cancelAll()
}
