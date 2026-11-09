package com.receiptmgr.app.domain.repository

import com.receiptmgr.app.domain.model.invoice.BillingPeriod
import com.receiptmgr.app.domain.model.invoice.InvoiceDetail
import com.receiptmgr.app.domain.model.invoice.PeriodInvoiceSummary

/**
 * 載具發票帳務倉儲介面 (InvoiceRepository)。
 */
interface InvoiceRepository {

    /**
     * 依指定雙月期別取得發票表頭清單與消費帳務總覽
     *
     * @param period 雙月期別
     * @param forceRefresh 是否強制穿透快取向財政部雲端重新抓取
     */
    suspend fun getInvoiceHeaders(
        period: BillingPeriod,
        forceRefresh: Boolean = false
    ): Result<PeriodInvoiceSummary>

    /**
     * 查詢單筆發票品項明細
     *
     * @param invNum 發票字軌號碼 (如 "GW38470545")
     * @param invDate 開立日期 (格式 YYYY-MM-DD 或 YYYY/MM/DD)
     * @param forceRefresh 是否強制重新抓取
     */
    suspend fun getInvoiceDetail(
        invNum: String,
        invDate: String,
        forceRefresh: Boolean = false
    ): Result<InvoiceDetail>

    /**
     * 清除記憶體快取 (通常於登出或切換帳號時調用)
     */
    fun clearCache()
}
