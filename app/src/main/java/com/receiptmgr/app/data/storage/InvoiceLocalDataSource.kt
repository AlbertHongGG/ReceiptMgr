package com.receiptmgr.app.data.storage

import com.receiptmgr.app.domain.model.invoice.BillingPeriod
import com.receiptmgr.app.domain.model.invoice.InvoiceDetail
import com.receiptmgr.app.domain.model.invoice.InvoiceHeader
import com.receiptmgr.app.domain.model.invoice.PeriodInvoiceSummary

/**
 * 發票本地持久化資料源介面契約 (InvoiceLocalDataSource)。
 *
 * 核心保證：
 * 1. 統一管理 SQLite 本地發票表頭、消費品項明細與期別元資料。
 * 2. 支援斷網離線回退與逾半年過期發票歷史永久調閱。
 * 3. 執行緒安全交易事務 (ACID)，保證資料寫入之完整性與一致性。
 */
interface InvoiceLocalDataSource {

    /**
     * 依雙月期別取得本地儲存之所有發票表頭 (依日期時間降序排列)
     */
    suspend fun getInvoicesByPeriod(period: BillingPeriod): List<InvoiceHeader>

    /**
     * 批次寫入或更新發票表頭。
     * 更新時保留既有 has_detail 標記與地址，杜絕重複覆蓋造成明細狀態遺失。
     */
    suspend fun upsertInvoices(period: BillingPeriod, invoices: List<InvoiceHeader>)

    /**
     * 提取指定期別中「尚未下載品項明細」之發票清單 (回傳 Pair(invNum, invDate))
     */
    suspend fun getPendingDetailInvoices(period: BillingPeriod): List<Pair<String, String>>

    /**
     * 取得單張發票完整消費品項明細。
     * 若本機無紀錄或尚未下載明細 (has_detail == 0)，回傳 null。
     */
    suspend fun getInvoiceDetail(invNum: String): InvoiceDetail?

    /**
     * 原子化保存發票明細 (寫入 invoice_items 並更新 invoices.has_detail = 1)
     */
    suspend fun saveInvoiceDetail(detail: InvoiceDetail)

    /**
     * 取得該期別本地匯總統計
     */
    suspend fun getPeriodSummary(period: BillingPeriod): PeriodInvoiceSummary?

    /**
     * 檢查該期別是否在本地有任何存檔紀錄
     */
    suspend fun hasLocalArchive(period: BillingPeriod): Boolean

    /**
     * 清空本地所有發票與明細紀錄 (登出或重設時使用)
     */
    suspend fun clearAll()
}
