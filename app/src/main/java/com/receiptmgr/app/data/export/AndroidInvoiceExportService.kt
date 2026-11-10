package com.receiptmgr.app.data.export

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.receiptmgr.app.domain.model.integration.equitytracker.EquityTrackerPayloadMapper
import com.receiptmgr.app.domain.model.invoice.InvoiceDetail
import com.receiptmgr.app.domain.service.InvoiceExportService
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 透過 Android 系統 Intent / Deep Link 實作發票外部匯出服務 (Adapter)。
 */
class AndroidInvoiceExportService(
    private val context: Context,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) : InvoiceExportService {

    companion object {
        const val DEEP_LINK_BASE = "equitytracker://receipt/import"
    }

    override fun exportToEquityTracker(detail: InvoiceDetail): Boolean {
        return try {
            val dto = EquityTrackerPayloadMapper.toDto(detail)
            val jsonString = json.encodeToString(dto)
            val encodedData = Uri.encode(jsonString)
            val deepLinkUri = Uri.parse("$DEEP_LINK_BASE?data=$encodedData")

            val intent = Intent(Intent.ACTION_VIEW, deepLinkUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
