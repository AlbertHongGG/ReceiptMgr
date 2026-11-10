package com.receiptmgr.app.domain.service

import com.receiptmgr.app.domain.model.invoice.InvoiceDetail

/**
 * 發票外部系統匯出與連動記帳服務介面 (Port)。
 */
interface InvoiceExportService {

    /**
     * 將指定發票明細透過 Deep Link 傳輸至 EquityTracker 進行自動記帳。
     *
     * @param detail 發票明細
     * @return 若成功啟動 EquityTracker 返回 true；若未安裝該應用返回 false
     */
    fun exportToEquityTracker(detail: InvoiceDetail): Boolean
}
