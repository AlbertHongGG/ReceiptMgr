package com.receiptmgr.app.domain.service

import com.receiptmgr.app.domain.model.invoice.InvoiceDetail
import com.receiptmgr.app.domain.model.invoice.InvoiceHeader

/**
 * 發票外部系統匯出與連動記帳服務介面 (Port)。
 */
interface InvoiceExportService {

    /**
     * 將指定發票明細傳輸至 EquityTracker 進行自動記帳。
     *
     * @param detail 發票完整明細
     * @return 若成功啟動 EquityTracker 返回 true；若未安裝該應用返回 false
     */
    fun exportToEquityTracker(detail: InvoiceDetail): Boolean

    /**
     * 將指定發票表頭傳輸至 EquityTracker 進行自動記帳 (清單頁使用)。
     * 實作者應優先檢索本地快取細項，若無細項則以表頭進行保底合成。
     *
     * @param header 發票表頭摘要
     * @return 若成功啟動 EquityTracker 返回 true；若未安裝該應用返回 false
     */
    suspend fun exportToEquityTracker(header: InvoiceHeader): Boolean
}
