package com.receiptmgr.app.data.export

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import com.receiptmgr.app.data.storage.InvoiceLocalDataSource
import com.receiptmgr.app.domain.model.integration.equitytracker.EquityTrackerPayloadMapper
import com.receiptmgr.app.domain.model.invoice.InvoiceDetail
import com.receiptmgr.app.domain.model.invoice.InvoiceHeader
import com.receiptmgr.app.domain.service.InvoiceExportService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 透過 Android 系統 Intent / 定向傳輸實作發票外部匯出服務 (Adapter)。
 */
class AndroidInvoiceExportService(
    private val context: Context,
    private val invoiceLocalDataSource: InvoiceLocalDataSource,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : InvoiceExportService {

    companion object {
        const val TARGET_PACKAGE = "com.jasonhong.equitytracker"
    }

    override fun exportToEquityTracker(detail: InvoiceDetail): Boolean {
        return try {
            val dto = EquityTrackerPayloadMapper.toDto(detail)
            val jsonString = json.encodeToString(dto)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_TEXT, jsonString)
                setPackage(TARGET_PACKAGE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }

    override suspend fun exportToEquityTracker(header: InvoiceHeader): Boolean {
        // 1. 優先檢索本地快取細項
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
