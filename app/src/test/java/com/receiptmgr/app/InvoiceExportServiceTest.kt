package com.receiptmgr.app

import com.receiptmgr.app.data.export.DefaultInvoiceExportService
import com.receiptmgr.app.data.repository.ExportRecordRepositoryImpl
import com.receiptmgr.app.data.storage.ExportRecordLocalDataSource
import com.receiptmgr.app.data.storage.InvoiceLocalDataSource
import com.receiptmgr.app.domain.model.export.ExportTarget
import com.receiptmgr.app.domain.model.integration.equitytracker.EquityTrackerReceiptPayloadDto
import com.receiptmgr.app.domain.model.invoice.BillingPeriod
import com.receiptmgr.app.domain.model.invoice.InvoiceDetail
import com.receiptmgr.app.domain.model.invoice.InvoiceHeader
import com.receiptmgr.app.domain.model.invoice.InvoiceLineItem
import com.receiptmgr.app.domain.model.invoice.PeriodInvoiceSummary
import com.receiptmgr.app.domain.service.ReceiptTransport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InvoiceExportServiceTest {

    private class FakeReceiptTransport(var shouldSucceed: Boolean = true) : ReceiptTransport {
        val sentPayloads = mutableListOf<EquityTrackerReceiptPayloadDto>()

        override fun send(payload: EquityTrackerReceiptPayloadDto): Boolean {
            return if (shouldSucceed) {
                sentPayloads.add(payload)
                true
            } else {
                false
            }
        }
    }

    private class FakeExportRecordLocalDataSource : ExportRecordLocalDataSource {
        val records = mutableMapOf<String, Long>()

        override suspend fun getAllExportRecords(target: ExportTarget): Map<String, Long> {
            return records.toMap()
        }

        override suspend fun upsertExportRecord(invNum: String, target: ExportTarget, timestamp: Long) {
            records[invNum] = timestamp
        }

        override suspend fun clearAll() {
            records.clear()
        }
    }

    private class FakeInvoiceLocalDataSource : InvoiceLocalDataSource {
        val details = mutableMapOf<String, InvoiceDetail>()

        override suspend fun getInvoicesByPeriod(period: BillingPeriod): List<InvoiceHeader> = emptyList()
        override suspend fun upsertInvoices(period: BillingPeriod, invoices: List<InvoiceHeader>) {}
        override suspend fun getPendingDetailInvoices(period: BillingPeriod): List<Pair<String, String>> = emptyList()

        override suspend fun getInvoiceDetail(invNum: String): InvoiceDetail? = details[invNum]

        override suspend fun saveInvoiceDetail(detail: InvoiceDetail) {
            details[detail.invNum] = detail
        }

        override suspend fun getPeriodSummary(period: BillingPeriod): PeriodInvoiceSummary? = null
        override suspend fun hasLocalArchive(period: BillingPeriod): Boolean = false
        override suspend fun clearAll() {
            details.clear()
        }
    }

    @Test
    fun testExportDetailSuccessMarksExported() = runBlocking {
        val transport = FakeReceiptTransport(shouldSucceed = true)
        val exportLocal = FakeExportRecordLocalDataSource()
        val exportRepo = ExportRecordRepositoryImpl(exportLocal, CoroutineScope(Dispatchers.Unconfined))
        val invoiceLocal = FakeInvoiceLocalDataSource()

        val service = DefaultInvoiceExportService(transport, invoiceLocal, exportRepo)

        val detail = InvoiceDetail(
            invNum = "AB12345678",
            invPeriod = "11512",
            sellerName = "全聯福利中心",
            sellerBan = "22880011",
            sellerAddress = "",
            amount = 95,
            invDate = "2026-11-10",
            invoiceTime = "12:30:00",
            items = listOf(InvoiceLineItem(rowNum = 1, description = "鮮乳", quantity = "1", unitPrice = "95", amount = 95))
        )

        assertFalse(exportRepo.isExported("AB12345678"))

        val result = service.exportToEquityTracker(detail)

        assertTrue(result)
        assertEquals(1, transport.sentPayloads.size)
        assertEquals("全聯福利中心", transport.sentPayloads.first().merchant)
        assertTrue(exportRepo.isExported("AB12345678"))
    }

    @Test
    fun testExportDetailFailureDoesNotMarkExported() = runBlocking {
        val transport = FakeReceiptTransport(shouldSucceed = false)
        val exportLocal = FakeExportRecordLocalDataSource()
        val exportRepo = ExportRecordRepositoryImpl(exportLocal, CoroutineScope(Dispatchers.Unconfined))
        val invoiceLocal = FakeInvoiceLocalDataSource()

        val service = DefaultInvoiceExportService(transport, invoiceLocal, exportRepo)

        val detail = InvoiceDetail(
            invNum = "AB12345678",
            invPeriod = "11512",
            sellerName = "全聯福利中心",
            sellerBan = "22880011",
            sellerAddress = "",
            amount = 95,
            invDate = "2026-11-10",
            invoiceTime = "12:30:00",
            items = listOf(InvoiceLineItem(rowNum = 1, description = "鮮乳", quantity = "1", unitPrice = "95", amount = 95))
        )

        val result = service.exportToEquityTracker(detail)

        assertFalse(result)
        assertEquals(0, transport.sentPayloads.size)
        assertFalse(exportRepo.isExported("AB12345678"))
    }

    @Test
    fun testExportHeaderUsesCachedDetailWhenAvailable() = runBlocking {
        val transport = FakeReceiptTransport(shouldSucceed = true)
        val exportLocal = FakeExportRecordLocalDataSource()
        val exportRepo = ExportRecordRepositoryImpl(exportLocal, CoroutineScope(Dispatchers.Unconfined))
        val invoiceLocal = FakeInvoiceLocalDataSource()

        val cachedDetail = InvoiceDetail(
            invNum = "CD99887766",
            invPeriod = "11512",
            sellerName = "家樂福",
            sellerBan = "12345678",
            sellerAddress = "",
            amount = 300,
            invDate = "2026-11-09",
            invoiceTime = "18:00:00",
            items = listOf(
                InvoiceLineItem(rowNum = 1, description = "蘋果", quantity = "2", unitPrice = "50", amount = 100),
                InvoiceLineItem(rowNum = 2, description = "牛排", quantity = "1", unitPrice = "200", amount = 200)
            )
        )
        invoiceLocal.saveInvoiceDetail(cachedDetail)

        val service = DefaultInvoiceExportService(transport, invoiceLocal, exportRepo)

        val header = InvoiceHeader(
            invNum = "CD99887766",
            invPeriod = "11512",
            sellerName = "家樂福",
            amount = 300,
            invDate = "2026-11-09",
            invoiceTime = "18:00:00"
        )

        val result = service.exportToEquityTracker(header)

        assertTrue(result)
        assertEquals(1, transport.sentPayloads.size)
        val payload = transport.sentPayloads.first()
        assertEquals(2, payload.items.size)
        assertEquals("蘋果", payload.items[0].name)
        assertEquals("牛排", payload.items[1].name)
        assertTrue(exportRepo.isExported("CD99887766"))
    }

    @Test
    fun testExportHeaderUsesFallbackWhenCacheMiss() = runBlocking {
        val transport = FakeReceiptTransport(shouldSucceed = true)
        val exportLocal = FakeExportRecordLocalDataSource()
        val exportRepo = ExportRecordRepositoryImpl(exportLocal, CoroutineScope(Dispatchers.Unconfined))
        val invoiceLocal = FakeInvoiceLocalDataSource() // No cached detail

        val service = DefaultInvoiceExportService(transport, invoiceLocal, exportRepo)

        val header = InvoiceHeader(
            invNum = "EF11223344",
            invPeriod = "11512",
            sellerName = "路易莎咖啡",
            amount = 150,
            invDate = "2026-11-08",
            invoiceTime = "09:15:00"
        )

        val result = service.exportToEquityTracker(header)

        assertTrue(result)
        assertEquals(1, transport.sentPayloads.size)
        val payload = transport.sentPayloads.first()
        assertEquals(1, payload.items.size)
        assertEquals("路易莎咖啡", payload.items[0].name)
        assertEquals(150.0, payload.items[0].amount, 0.001)
        assertTrue(exportRepo.isExported("EF11223344"))
    }
}
