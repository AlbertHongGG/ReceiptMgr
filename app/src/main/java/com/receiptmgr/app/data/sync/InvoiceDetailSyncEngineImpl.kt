package com.receiptmgr.app.data.sync

import com.receiptmgr.app.data.network.UpiInvoiceApiClient
import com.receiptmgr.app.data.network.dto.UpiInvoiceDetailReqDto
import com.receiptmgr.app.data.storage.InvoiceLocalDataSource
import com.receiptmgr.app.data.storage.SessionDataSource
import com.receiptmgr.app.domain.model.invoice.BillingPeriod
import com.receiptmgr.app.domain.model.invoice.InvoiceDetail
import com.receiptmgr.app.domain.model.invoice.UpiProtocolException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Collections

/**
 * 載具發票消費品項背景同步引擎實作 (InvoiceDetailSyncEngineImpl)。
 *
 * 核心保證：
 * 1. 溫和非同步節流 (Throttled Sequential Queue)：背景預抓間隔 400ms，杜絕 Cloudflare/MOF 429 限流。
 * 2. 高優先權搶佔 (P0 Priority Preemption)：使用者點擊之發票立即插隊抓取，0ms 等待即刻持久化。
 * 3. 斷點續傳與冪等性 (Resume & Idempotence)：只抓取 has_detail == 0 之發票，絕不重複消耗流量。
 * 4. 連續錯誤熔斷：連續 3 筆網路或憑證錯誤自動暫停本期同步，保護使用者電池與流量。
 * 5. 超期防禦：已過期之雙月期別絕不發起無效網路輪詢。
 */
class InvoiceDetailSyncEngineImpl(
    private val upiApiClient: UpiInvoiceApiClient,
    private val sessionDataSource: SessionDataSource,
    private val localDataSource: InvoiceLocalDataSource,
    private val externalScope: CoroutineScope? = null
) : InvoiceDetailSyncEngine {

    private var masterJob = SupervisorJob()
    private var engineScope = externalScope ?: CoroutineScope(masterJob + Dispatchers.IO)

    private val activePeriods = Collections.synchronizedSet(HashSet<BillingPeriod>())
    private val _syncStatus = MutableStateFlow<SyncEngineStatus>(SyncEngineStatus.Idle)
    override val syncStatus: StateFlow<SyncEngineStatus> = _syncStatus.asStateFlow()

    companion object {
        private const val THROTTLE_DELAY_MS = 400L
        private const val MAX_CONSECUTIVE_FAILURES = 3
    }

    override fun scheduleBackgroundSync(period: BillingPeriod) {
        // 若超出財政部官方保存窗口，伺服器已無明細，不發起同步
        if (!period.isWithinMofQueryWindow()) {
            return
        }

        // 防重入：同一期別若已在同步隊列中則略過
        if (!activePeriods.add(period)) {
            return
        }

        engineScope.launch {
            try {
                val pendingList = localDataSource.getPendingDetailInvoices(period)
                if (pendingList.isEmpty()) {
                    return@launch
                }

                val total = pendingList.size
                var processed = 0
                var consecutiveFailures = 0

                _syncStatus.value = SyncEngineStatus.Syncing(
                    period = period,
                    totalPending = total,
                    processedCount = processed
                )

                for ((invNum, invDate) in pendingList) {
                    if (!isActive) break

                    // 若已被使用者在前端以 P0 點擊預先存檔，直接跳過
                    if (localDataSource.getInvoiceDetail(invNum) != null) {
                        processed++
                        _syncStatus.value = SyncEngineStatus.Syncing(period, total, processed)
                        continue
                    }

                    val session = sessionDataSource.getSession()
                    if (session == null || session.ssMe.isBlank()) {
                        // 會話無效或已登出，中止背景作業
                        break
                    }

                    val queryInvDate = invDate.trim().replace("-", "/")
                    val reqDto = UpiInvoiceDetailReqDto(
                        cardNo = session.barcode,
                        invNum = invNum.trim().uppercase(),
                        invDate = queryInvDate
                    )

                    val result = upiApiClient.queryDetails(reqDto, session)
                    result.fold(
                        onSuccess = { detail ->
                            consecutiveFailures = 0
                            localDataSource.saveInvoiceDetail(detail)
                        },
                        onFailure = {
                            consecutiveFailures++
                        }
                    )

                    processed++
                    _syncStatus.value = SyncEngineStatus.Syncing(period, total, processed)

                    // 連續故障熔斷保護
                    if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                        break
                    }

                    // 溫和節流間隔
                    delay(THROTTLE_DELAY_MS)
                }
            } finally {
                activePeriods.remove(period)
                if (activePeriods.isEmpty()) {
                    _syncStatus.value = SyncEngineStatus.Idle
                }
            }
        }
    }

    override suspend fun fetchDetailImmediately(
        invNum: String,
        invDate: String
    ): Result<InvoiceDetail> = withContext(Dispatchers.IO) {
        val cleanInvNum = invNum.trim().uppercase()

        // 1. 本地資料庫優先查詢 (0ms 快取命中)
        val cached = localDataSource.getInvoiceDetail(cleanInvNum)
        if (cached != null) {
            return@withContext Result.success(cached)
        }

        // 2. 本地尚未儲存，以 P0 優先權直接向財政部發起明細請求
        val session = sessionDataSource.getSession()
            ?: return@withContext Result.failure(UpiProtocolException.SessionUnavailable("找不到有效的載具會話"))

        if (session.ssMe.isBlank()) {
            return@withContext Result.failure(UpiProtocolException.SessionUnavailable("載具憑證 (ss_me) 缺失，請重新登入"))
        }

        val queryInvDate = invDate.trim().replace("-", "/")
        val reqDto = UpiInvoiceDetailReqDto(
            cardNo = session.barcode,
            invNum = cleanInvNum,
            invDate = queryInvDate
        )

        upiApiClient.queryDetails(reqDto, session).map { detail ->
            // 3. 原子化寫入本地 SQLite，保證後續永久離線可用
            localDataSource.saveInvoiceDetail(detail)
            detail
        }
    }

    override fun cancelAll() {
        if (externalScope == null) {
            masterJob.cancel()
            masterJob = SupervisorJob()
            engineScope = CoroutineScope(masterJob + Dispatchers.IO)
        }
        activePeriods.clear()
        _syncStatus.value = SyncEngineStatus.Idle
    }
}
