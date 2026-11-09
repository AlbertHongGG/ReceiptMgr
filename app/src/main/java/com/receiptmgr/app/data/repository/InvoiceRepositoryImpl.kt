package com.receiptmgr.app.data.repository

import com.receiptmgr.app.data.network.UpiInvoiceApiClient
import com.receiptmgr.app.data.network.dto.UpiInvoiceDetailReqDto
import com.receiptmgr.app.data.network.dto.UpiInvoiceHeaderReqDto
import com.receiptmgr.app.data.storage.EncryptedSessionDataSource
import com.receiptmgr.app.domain.model.invoice.BillingPeriod
import com.receiptmgr.app.domain.model.invoice.InvoiceDetail
import com.receiptmgr.app.domain.model.invoice.InvoiceHeader
import com.receiptmgr.app.domain.model.invoice.PeriodInvoiceSummary
import com.receiptmgr.app.domain.model.invoice.UpiProtocolException
import com.receiptmgr.app.domain.repository.InvoiceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * 載具發票帳務倉儲實作 (InvoiceRepositoryImpl)。
 *
 * 核心保證：
 * 1. 記憶體執行緒安全快取 (ConcurrentHashMap)，切換期別秒開。
 * 2. 自動提取登入會話之 ss_me、barcode 與 liat，若無效立即回傳 SessionUnavailable。
 * 3. 發票自動依開立時間降序排列。
 * 4. 支援 forceRefresh 強制穿透雲端同步。
 */
class InvoiceRepositoryImpl(
    private val upiApiClient: UpiInvoiceApiClient,
    private val sessionDataSource: EncryptedSessionDataSource
) : InvoiceRepository {

    private val headerCache = ConcurrentHashMap<BillingPeriod, PeriodInvoiceSummary>()
    private val detailCache = ConcurrentHashMap<String, InvoiceDetail>()

    override suspend fun getInvoiceHeaders(
        period: BillingPeriod,
        forceRefresh: Boolean
    ): Result<PeriodInvoiceSummary> = withContext(Dispatchers.IO) {
        if (!forceRefresh) {
            headerCache[period]?.let {
                return@withContext Result.success(it)
            }
        }

        val session = sessionDataSource.getSession()
            ?: return@withContext Result.failure(UpiProtocolException.SessionUnavailable("找不到有效的載具會話"))

        if (session.ssMe.isBlank()) {
            return@withContext Result.failure(UpiProtocolException.SessionUnavailable("載具憑證 (ss_me) 缺失，請重新登入"))
        }

        val reqDto = UpiInvoiceHeaderReqDto(
            cardNo = session.barcode,
            startDate = period.queryStartDate,
            endDate = period.queryEndDate,
            onlyWinningInv = "A",
            page = 1
        )

        upiApiClient.queryHeaders(reqDto, session).map { rawInvoices ->
            // 依開立日期與時間降序排列 (最新發票置頂)
            val sortedInvoices = rawInvoices.sortedWith(
                compareByDescending<InvoiceHeader> { it.invDate }
                    .thenByDescending { it.invoiceTime }
                    .thenByDescending { it.invNum }
            )
            val summary = PeriodInvoiceSummary.fromInvoices(period, sortedInvoices)
            headerCache[period] = summary
            summary
        }
    }

    override suspend fun getInvoiceDetail(
        invNum: String,
        invDate: String,
        forceRefresh: Boolean
    ): Result<InvoiceDetail> = withContext(Dispatchers.IO) {
        val cleanInvNum = invNum.trim().uppercase()
        // API 要求 YYYY/MM/DD
        val queryInvDate = invDate.trim().replace("-", "/")
        val cacheKey = "$cleanInvNum|$queryInvDate"

        if (!forceRefresh) {
            detailCache[cacheKey]?.let {
                return@withContext Result.success(it)
            }
        }

        val session = sessionDataSource.getSession()
            ?: return@withContext Result.failure(UpiProtocolException.SessionUnavailable("找不到有效的載具會話"))

        if (session.ssMe.isBlank()) {
            return@withContext Result.failure(UpiProtocolException.SessionUnavailable("載具憑證 (ss_me) 缺失，請重新登入"))
        }

        val reqDto = UpiInvoiceDetailReqDto(
            cardNo = session.barcode,
            invNum = cleanInvNum,
            invDate = queryInvDate
        )

        upiApiClient.queryDetails(reqDto, session).map { detail ->
            detailCache[cacheKey] = detail
            detail
        }
    }

    override fun clearCache() {
        headerCache.clear()
        detailCache.clear()
    }
}
