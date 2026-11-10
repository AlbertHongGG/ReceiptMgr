package com.receiptmgr.app.data.repository

import com.receiptmgr.app.data.network.UpiInvoiceApiClient
import com.receiptmgr.app.data.network.dto.UpiInvoiceHeaderReqDto
import com.receiptmgr.app.data.storage.InvoiceLocalDataSource
import com.receiptmgr.app.data.storage.SessionDataSource
import com.receiptmgr.app.data.sync.InvoiceDetailSyncEngine
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
 * 2. 雙向同步機制 (方案 B)：雲端抓取表頭成功後，立即寫入本地 SQLite 並啟動背景明細自動同步引擎。
 * 3. 離線無損回退 (Offline Resilience)：斷網或伺服器異常時，自動回退讀取本地已保存之發票與品項明細。
 * 4. 超期歷史永久調閱 (Overdue Archive)：超過官方半年查詢期限者，直接讀取本地 SQLite 永久存檔。
 * 5. P0 優先權明細快取 (0ms Hit)：使用者點擊時優先讀取本地已快取品項，無快取則即時向雲端抓取並存檔。
 */
class InvoiceRepositoryImpl(
    private val upiApiClient: UpiInvoiceApiClient,
    private val sessionDataSource: SessionDataSource,
    private val localDataSource: InvoiceLocalDataSource,
    private val syncEngine: InvoiceDetailSyncEngine
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

        // 1. 若該期別已超出財政部 5 期 (半年) 保存期限，官方伺服器已淘汰資料
        if (!period.isWithinMofQueryWindow()) {
            val localInvoices = localDataSource.getInvoicesByPeriod(period)
            val summary = PeriodInvoiceSummary.fromInvoices(period, localInvoices)
            headerCache[period] = summary
            return@withContext Result.success(summary)
        }

        // 2. 查詢當前會話憑證
        val session = sessionDataSource.getSession()
        if (session == null || session.ssMe.isBlank()) {
            // 無登入憑證時嘗試回退本地資料庫
            val localInvoices = localDataSource.getInvoicesByPeriod(period)
            if (localInvoices.isNotEmpty()) {
                val summary = PeriodInvoiceSummary.fromInvoices(period, localInvoices)
                headerCache[period] = summary
                return@withContext Result.success(summary)
            }
            return@withContext Result.failure(UpiProtocolException.SessionUnavailable("找不到有效的載具會話，請重新登入"))
        }

        val reqDto = UpiInvoiceHeaderReqDto(
            cardNo = session.barcode,
            startDate = period.apiStartDate,
            endDate = period.getApiEndDate(),
            onlyWinningInv = "A",
            page = 1
        )

        upiApiClient.queryHeaders(reqDto, session).fold(
            onSuccess = { rawInvoices ->
                // 依開立日期與時間降序排列 (最新發票置頂)
                val sortedInvoices = rawInvoices.sortedWith(
                    compareByDescending<InvoiceHeader> { it.invDate }
                        .thenByDescending { it.invoiceTime }
                        .thenByDescending { it.invNum }
                )

                // 寫入本地 SQLite (以 INSERT OR REPLACE 方式，同時保留既有 has_detail 明細狀態)
                localDataSource.upsertInvoices(period, sortedInvoices)

                // 啟動背景明細自動抓取 (方案 B: 400ms 節流佇列)
                syncEngine.scheduleBackgroundSync(period)

                val summary = PeriodInvoiceSummary.fromInvoices(period, sortedInvoices)
                headerCache[period] = summary
                Result.success(summary)
            },
            onFailure = { error ->
                // 斷網或 API 異常時，自動回退本地 SQLite 資料庫
                val localInvoices = localDataSource.getInvoicesByPeriod(period)
                if (localInvoices.isNotEmpty()) {
                    val summary = PeriodInvoiceSummary.fromInvoices(period, localInvoices)
                    headerCache[period] = summary
                    Result.success(summary)
                } else if (error is UpiProtocolException.BusinessRejected && error.code == "903") {
                    // 若伺服器回傳 903 參數錯誤，領域防禦轉譯為合法空資料
                    val emptySummary = PeriodInvoiceSummary(
                        period = period,
                        totalAmount = 0L,
                        invoiceCount = 0,
                        invoices = emptyList()
                    )
                    headerCache[period] = emptySummary
                    Result.success(emptySummary)
                } else {
                    Result.failure(error)
                }
            }
        )
    }

    override suspend fun getInvoiceDetail(
        invNum: String,
        invDate: String,
        forceRefresh: Boolean
    ): Result<InvoiceDetail> = withContext(Dispatchers.IO) {
        val cleanInvNum = invNum.trim().uppercase()
        val queryInvDate = invDate.trim().replace("-", "/")
        val cacheKey = "$cleanInvNum|$queryInvDate"

        if (!forceRefresh) {
            detailCache[cacheKey]?.let {
                return@withContext Result.success(it)
            }
        }

        // 1. 本地 SQLite 查詢 (0ms 命中即回傳)
        if (!forceRefresh) {
            val localDetail = localDataSource.getInvoiceDetail(cleanInvNum)
            if (localDetail != null) {
                detailCache[cacheKey] = localDetail
                return@withContext Result.success(localDetail)
            }
        }

        // 2. 本地尚無資料或強制刷新：調用 syncEngine 以 P0 優先權即時向財政部同步
        syncEngine.fetchDetailImmediately(cleanInvNum, invDate).fold(
            onSuccess = { detail ->
                detailCache[cacheKey] = detail
                Result.success(detail)
            },
            onFailure = { error ->
                // 若網路請求失敗，再次檢查本地是否存有過期備份
                val fallbackDetail = localDataSource.getInvoiceDetail(cleanInvNum)
                if (fallbackDetail != null) {
                    detailCache[cacheKey] = fallbackDetail
                    Result.success(fallbackDetail)
                } else {
                    Result.failure(error)
                }
            }
        )
    }

    override fun clearCache() {
        headerCache.clear()
        detailCache.clear()
        syncEngine.cancelAll()
    }
}
