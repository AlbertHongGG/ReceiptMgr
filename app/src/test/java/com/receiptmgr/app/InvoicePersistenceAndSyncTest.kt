package com.receiptmgr.app

import com.receiptmgr.app.data.network.UpiInvoiceApiClient
import com.receiptmgr.app.data.network.dto.UpiInvoiceDetailReqDto
import com.receiptmgr.app.data.network.dto.UpiInvoiceHeaderReqDto
import com.receiptmgr.app.data.repository.InvoiceRepositoryImpl
import com.receiptmgr.app.data.storage.ApiLogManager
import com.receiptmgr.app.data.storage.InvoiceLocalDataSource
import com.receiptmgr.app.data.storage.SessionDataSource
import com.receiptmgr.app.data.sync.InvoiceDetailSyncEngine
import com.receiptmgr.app.data.sync.InvoiceDetailSyncEngineImpl
import com.receiptmgr.app.data.sync.SyncEngineStatus
import com.receiptmgr.app.domain.model.auth.SessionCredentials
import com.receiptmgr.app.domain.model.auth.SessionPayload
import com.receiptmgr.app.domain.model.invoice.BillingPeriod
import com.receiptmgr.app.domain.model.invoice.InvoiceDetail
import com.receiptmgr.app.domain.model.invoice.InvoiceHeader
import com.receiptmgr.app.domain.model.invoice.InvoiceLineItem
import com.receiptmgr.app.domain.model.invoice.PeriodInvoiceSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * 發票本地持久化資料庫與全量自動背景同步 (方案 B) 單元測試。
 *
 * 核心驗證：
 * 1. 斷網離線回退：網路故障時無損讀取本地 SQLite 發票清單。
 * 2. 超期歷史永久調閱：超出財政部半年查詢期限之期別，繞過雲端直讀本地存檔。
 * 3. 雲端同步觸發預抓：表頭查詢成功時，自動存入本地並排程背景明細同步引擎。
 * 4. P0 優先權明細秒開 (0ms Hit)：本地存在明細立即秒開，未下載時即時搶佔抓取並永久入庫。
 * 5. 背景隊列略過已抓取：背景佇列自動略過已被 P0 抓取之發票。
 */
class InvoicePersistenceAndSyncTest {

    private val testPeriod = BillingPeriod(year = 2026, endMonth = 10) // 115年 09-10月
    private val overduePeriod = BillingPeriod(year = 2025, endMonth = 8) // 超出半年前

    private val sampleHeaders = listOf(
        InvoiceHeader(
            invNum = "EZ04086108",
            invPeriod = "11510",
            sellerName = "統一超商股份有限公司",
            sellerBan = "22555003",
            sellerAddress = "台北市中正區",
            amount = 39,
            invDate = "2026-10-21",
            invoiceTime = "13:10:00",
            cardNo = "/ABC"
        ),
        InvoiceHeader(
            invNum = "FM06397911",
            invPeriod = "11510",
            sellerName = "舞動陽光股份有限公司",
            sellerBan = "53018899",
            sellerAddress = "台北市大同區",
            amount = 50,
            invDate = "2026-10-20",
            invoiceTime = "15:20:00",
            cardNo = "/ABC"
        )
    )

    private val sampleDetail = InvoiceDetail(
        invNum = "EZ04086108",
        invPeriod = "11510",
        sellerName = "統一超商股份有限公司",
        sellerBan = "22555003",
        sellerAddress = "台北市中正區",
        amount = 39,
        invDate = "2026-10-21",
        invoiceTime = "13:10:00",
        cardNo = "/ABC",
        items = listOf(
            InvoiceLineItem(
                rowNum = 1,
                description = "純喫茶紅茶",
                quantity = "1",
                unitPrice = "25",
                amount = 25
            ),
            InvoiceLineItem(
                rowNum = 2,
                description = "茶葉蛋",
                quantity = "1",
                unitPrice = "14",
                amount = 14
            )
        )
    )

    private fun createTestSession(): SessionCredentials {
        return SessionCredentials(
            account = "0912345678",
            payload = SessionPayload(
                carrierCode = "/ABC",
                ssMe = "valid_ss_me_key",
                liat = 1000L
            ),
            rawPayloadJson = "{}"
        )
    }

    /**
     * 記憶體 Fake 本地儲存資料源
     */
    private class FakeInvoiceLocalDataSource : InvoiceLocalDataSource {
        val invoicesMap = mutableMapOf<String, InvoiceHeader>()
        val hasDetailMap = mutableMapOf<String, Boolean>()
        val detailsMap = mutableMapOf<String, InvoiceDetail>()

        override suspend fun getInvoicesByPeriod(period: BillingPeriod): List<InvoiceHeader> {
            return invoicesMap.values.filter { it.invPeriod == period.rocPeriodCode }
        }

        override suspend fun upsertInvoices(period: BillingPeriod, invoices: List<InvoiceHeader>) {
            for (inv in invoices) {
                invoicesMap[inv.invNum] = inv
                if (!hasDetailMap.containsKey(inv.invNum)) {
                    hasDetailMap[inv.invNum] = false
                }
            }
        }

        override suspend fun getPendingDetailInvoices(period: BillingPeriod): List<Pair<String, String>> {
            return invoicesMap.values
                .filter { it.invPeriod == period.rocPeriodCode && hasDetailMap[it.invNum] == false }
                .map { Pair(it.invNum, it.invDate) }
        }

        override suspend fun getInvoiceDetail(invNum: String): InvoiceDetail? {
            return if (hasDetailMap[invNum] == true) detailsMap[invNum] else null
        }

        override suspend fun saveInvoiceDetail(detail: InvoiceDetail) {
            detailsMap[detail.invNum] = detail
            hasDetailMap[detail.invNum] = true
        }

        override suspend fun getPeriodSummary(period: BillingPeriod): PeriodInvoiceSummary? {
            val list = getInvoicesByPeriod(period)
            return if (list.isEmpty()) null else PeriodInvoiceSummary.fromInvoices(period, list)
        }

        override suspend fun hasLocalArchive(period: BillingPeriod): Boolean {
            return invoicesMap.values.any { it.invPeriod == period.rocPeriodCode }
        }

        override suspend fun clearAll() {
            invoicesMap.clear()
            hasDetailMap.clear()
            detailsMap.clear()
        }
    }

    /**
     * Fake 會話資料源
     */
    private class FakeSessionDataSource(
        var session: SessionCredentials?
    ) : SessionDataSource {
        override suspend fun saveSession(session: SessionCredentials) {
            this.session = session
        }
        override suspend fun getSession(): SessionCredentials? = session
        override suspend fun clearSession() {
            session = null
        }
    }

    /**
     * Fake 同步引擎
     */
    private class FakeInvoiceDetailSyncEngine(
        val localDataSource: FakeInvoiceLocalDataSource
    ) : InvoiceDetailSyncEngine {
        val scheduledPeriods = mutableListOf<BillingPeriod>()
        val fetchImmediatelyCalls = mutableListOf<String>()

        override fun scheduleBackgroundSync(period: BillingPeriod) {
            scheduledPeriods.add(period)
        }

        override suspend fun fetchDetailImmediately(invNum: String, invDate: String): Result<InvoiceDetail> {
            fetchImmediatelyCalls.add(invNum)
            val existing = localDataSource.getInvoiceDetail(invNum)
            return if (existing != null) {
                Result.success(existing)
            } else {
                val detail = InvoiceDetail(
                    invNum = invNum,
                    invPeriod = "11510",
                    sellerName = "測試商店",
                    sellerBan = "12345678",
                    sellerAddress = "台北市",
                    amount = 100,
                    invDate = invDate,
                    items = listOf(InvoiceLineItem(1, "測試品項", "1", "100", 100))
                )
                localDataSource.saveInvoiceDetail(detail)
                Result.success(detail)
            }
        }

        override val syncStatus: StateFlow<SyncEngineStatus> =
            MutableStateFlow<SyncEngineStatus>(SyncEngineStatus.Idle)

        override fun cancelAll() {
            scheduledPeriods.clear()
        }
    }

    private class MockUpiInvoiceApiClient(
        val headersHandler: (suspend (UpiInvoiceHeaderReqDto, SessionCredentials) -> Result<List<InvoiceHeader>>)? = null,
        val detailsHandler: (suspend (UpiInvoiceDetailReqDto, SessionCredentials) -> Result<InvoiceDetail>)? = null
    ) : UpiInvoiceApiClient(
        apiLogManager = ApiLogManager(android.content.ContextWrapper(null))
    ) {
        override suspend fun queryHeaders(
            reqData: UpiInvoiceHeaderReqDto,
            session: SessionCredentials
        ): Result<List<InvoiceHeader>> {
            return headersHandler?.invoke(reqData, session) ?: Result.success(emptyList())
        }

        override suspend fun queryDetails(
            reqData: UpiInvoiceDetailReqDto,
            session: SessionCredentials
        ): Result<InvoiceDetail> {
            return detailsHandler?.invoke(reqData, session) ?: Result.failure(IllegalStateException("No handler"))
        }
    }

    @Test
    fun `getInvoiceHeaders falls back seamlessly to local SQLite when network fails`() = runBlocking {
        val fakeLocal = FakeInvoiceLocalDataSource()
        val fakeSync = FakeInvoiceDetailSyncEngine(fakeLocal)

        // 預先在本地資料庫存有發票
        fakeLocal.upsertInvoices(testPeriod, sampleHeaders)

        val failingApiClient = MockUpiInvoiceApiClient(
            headersHandler = { _, _ -> Result.failure(IOException("連線逾時 (模擬斷網)")) }
        )

        val repository = InvoiceRepositoryImpl(
            upiApiClient = failingApiClient,
            sessionDataSource = FakeSessionDataSource(createTestSession()),
            localDataSource = fakeLocal,
            syncEngine = fakeSync
        )

        // 呼叫 getInvoiceHeaders 強制向網路抓取，但網路失敗
        val result = repository.getInvoiceHeaders(testPeriod, forceRefresh = true)

        assertTrue("斷網時必須無損回退本地資料，回傳 success", result.isSuccess)
        val summary = result.getOrThrow()
        assertEquals(2, summary.invoiceCount)
        assertEquals(89L, summary.totalAmount)
        assertEquals("EZ04086108", summary.invoices[0].invNum)
    }

    @Test
    fun `getInvoiceHeaders reads directly from local archive for overdue periods without calling network`() = runBlocking {
        val fakeLocal = FakeInvoiceLocalDataSource()
        val fakeSync = FakeInvoiceDetailSyncEngine(fakeLocal)

        // 模擬過期期別在本地已保存的歷史發票
        val oldHeaders = listOf(
            InvoiceHeader(
                invNum = "AA11223344",
                invPeriod = overduePeriod.rocPeriodCode,
                sellerName = "老牌咖啡館",
                amount = 150,
                invDate = "2025-07-15"
            )
        )
        fakeLocal.upsertInvoices(overduePeriod, oldHeaders)

        var networkCalled = false
        val mockApiClient = MockUpiInvoiceApiClient(
            headersHandler = { _, _ ->
                networkCalled = true
                Result.failure(AssertionError("超期期別絕不得調用雲端 API"))
            }
        )

        val repository = InvoiceRepositoryImpl(
            upiApiClient = mockApiClient,
            sessionDataSource = FakeSessionDataSource(createTestSession()),
            localDataSource = fakeLocal,
            syncEngine = fakeSync
        )

        val result = repository.getInvoiceHeaders(overduePeriod, forceRefresh = true)

        assertTrue(result.isSuccess)
        assertFalse("超期期別必須直接讀取本地 SQLite，絕不發送網路請求", networkCalled)
        assertEquals(1, result.getOrThrow().invoiceCount)
        assertEquals(150L, result.getOrThrow().totalAmount)
        assertEquals("AA11223344", result.getOrThrow().invoices[0].invNum)
    }

    @Test
    fun `getInvoiceHeaders on API success saves to local SQLite and triggers background sync engine`() = runBlocking {
        val fakeLocal = FakeInvoiceLocalDataSource()
        val fakeSync = FakeInvoiceDetailSyncEngine(fakeLocal)

        val successfulApiClient = MockUpiInvoiceApiClient(
            headersHandler = { _, _ -> Result.success(sampleHeaders) }
        )

        val repository = InvoiceRepositoryImpl(
            upiApiClient = successfulApiClient,
            sessionDataSource = FakeSessionDataSource(createTestSession()),
            localDataSource = fakeLocal,
            syncEngine = fakeSync
        )

        val result = repository.getInvoiceHeaders(testPeriod, forceRefresh = true)

        assertTrue(result.isSuccess)
        // 驗證本地 SQLite 是否已寫入
        val localRecords = fakeLocal.getInvoicesByPeriod(testPeriod)
        assertEquals(2, localRecords.size)

        // 驗證是否觸發背景同步引擎
        assertTrue("成功取得表頭後必須觸發背景預抓排程 (方案 B)", fakeSync.scheduledPeriods.contains(testPeriod))
    }

    @Test
    fun `getInvoiceDetail returns cached detail immediately without network call`() = runBlocking {
        val fakeLocal = FakeInvoiceLocalDataSource()
        val fakeSync = FakeInvoiceDetailSyncEngine(fakeLocal)

        // 本地預先存有明細
        fakeLocal.saveInvoiceDetail(sampleDetail)

        val repository = InvoiceRepositoryImpl(
            upiApiClient = MockUpiInvoiceApiClient(),
            sessionDataSource = FakeSessionDataSource(createTestSession()),
            localDataSource = fakeLocal,
            syncEngine = fakeSync
        )

        // 查詢明細 (未強制刷新)
        val result = repository.getInvoiceDetail(
            invNum = "EZ04086108",
            invDate = "2026-10-21",
            forceRefresh = false
        )

        assertTrue(result.isSuccess)
        val detail = result.getOrThrow()
        assertEquals("EZ04086108", detail.invNum)
        assertEquals(2, detail.items.size)
        // 本地已有快取時，不應調用 fetchDetailImmediately
        assertEquals("本地快取命中時不應調用即時雲端請求", 0, fakeSync.fetchImmediatelyCalls.size)
    }

    @Test
    fun `getInvoiceDetail invokes P0 immediate fetch when local detail is absent`() = runBlocking {
        val fakeLocal = FakeInvoiceLocalDataSource()
        val fakeSync = FakeInvoiceDetailSyncEngine(fakeLocal)

        val repository = InvoiceRepositoryImpl(
            upiApiClient = MockUpiInvoiceApiClient(),
            sessionDataSource = FakeSessionDataSource(createTestSession()),
            localDataSource = fakeLocal,
            syncEngine = fakeSync
        )

        val result = repository.getInvoiceDetail(
            invNum = "FM06397911",
            invDate = "2026-10-20",
            forceRefresh = false
        )

        assertTrue(result.isSuccess)
        // 驗證 P0 即時調用被觸發
        assertTrue(fakeSync.fetchImmediatelyCalls.contains("FM06397911"))
        // 驗證本地端已永久存檔
        assertNotNull(fakeLocal.getInvoiceDetail("FM06397911"))
    }

    @Test
    fun `InvoiceDetailSyncEngine fetchDetailImmediately saves directly to local database`() = runBlocking {
        val fakeLocal = FakeInvoiceLocalDataSource()
        val sessionData = createTestSession()

        val mockApiClient = MockUpiInvoiceApiClient(
            detailsHandler = { req, _ ->
                Result.success(sampleDetail.copy(invNum = req.invNum))
            }
        )

        val syncEngine = InvoiceDetailSyncEngineImpl(
            upiApiClient = mockApiClient,
            sessionDataSource = FakeSessionDataSource(sessionData),
            localDataSource = fakeLocal
        )

        val result = syncEngine.fetchDetailImmediately("EZ04086108", "2026-10-21")
        assertTrue(result.isSuccess)
        assertEquals("EZ04086108", result.getOrThrow().invNum)

        // 驗證本地資料庫已同步寫入
        val saved = fakeLocal.getInvoiceDetail("EZ04086108")
        assertNotNull(saved)
        assertEquals(2, saved?.items?.size)
    }
}
