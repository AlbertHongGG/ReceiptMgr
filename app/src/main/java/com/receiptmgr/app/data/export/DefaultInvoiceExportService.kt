package com.receiptmgr.app.data.export

import com.receiptmgr.app.data.storage.InvoiceLocalDataSource
import com.receiptmgr.app.domain.model.export.ExportTarget
import com.receiptmgr.app.domain.model.integration.equitytracker.EquityTrackerPayloadMapper
import com.receiptmgr.app.domain.model.invoice.InvoiceDetail
import com.receiptmgr.app.domain.model.invoice.InvoiceHeader
import com.receiptmgr.app.domain.repository.ExportRecordRepository
import com.receiptmgr.app.domain.service.InvoiceExportService
import com.receiptmgr.app.domain.service.ReceiptTransport

/**
 * 預設發票外部匯出服務實作 (純 JVM 邏輯，不依賴 Android Context)。
 *
 * 職責：
 * 1. 協調快取檢索與保底合成。
 * 2. 呼叫 ReceiptTransport 傳輸。
 * 3. 傳輸成功時原子化記錄已傳送狀態至 ExportRecordRepository。
 */
class DefaultInvoiceExportService(
    private val transport: ReceiptTransport,
    private val invoiceLocalDataSource: InvoiceLocalDataSource,
    private val exportRecordRepository: ExportRecordRepository
) : InvoiceExportService {

    override suspend fun exportToEquityTracker(detail: InvoiceDetail): Boolean {
        val payload = EquityTrackerPayloadMapper.toDto(detail)
        val success = transport.send(payload)
        if (success) {
            exportRecordRepository.markExported(detail.invNum, ExportTarget.EQUITY_TRACKER)
        }
        return success
    }

    override suspend fun exportToEquityTracker(header: InvoiceHeader): Boolean {
        val cachedDetail = invoiceLocalDataSource.getInvoiceDetail(header.invNum)
        val detailToExport = cachedDetail ?: InvoiceDetail(
            invNum = header.invNum,
            invPeriod = header.invPeriod,
            sellerName = header.sellerName,
            sellerBan = header.sellerBan,
            sellerAddress = header.sellerAddress,
            amount = header.amount,
            invDate = header.invDate,
            invoiceTime = header.invoiceTime,
            cardNo = header.cardNo,
            items = emptyList()
        )
        return exportToEquityTracker(detailToExport)
    }
}
