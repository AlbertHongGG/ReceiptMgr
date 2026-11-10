package com.receiptmgr.app.presentation.features.invoice.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptmgr.app.domain.model.invoice.BillingPeriod
import com.receiptmgr.app.domain.model.invoice.InvoiceHeader
import com.receiptmgr.app.domain.model.invoice.PeriodInvoiceSummary
import com.receiptmgr.app.domain.repository.InvoiceRepository
import com.receiptmgr.app.domain.service.InvoiceExportService
import com.receiptmgr.app.presentation.notification.NotificationController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 發票清單捲動位置記錄實體
 */
data class ListScrollPosition(
    val firstVisibleIndex: Int = 0,
    val firstVisibleOffset: Int = 0
)

/**
 * 發票清單 UI 狀態模型 (強型別期別隔離架構)
 */
data class InvoiceListUiState(
    val currentPeriod: BillingPeriod = BillingPeriod.current(),
    val periodSummaries: Map<BillingPeriod, PeriodInvoiceSummary> = emptyMap(),
    val periodErrors: Map<BillingPeriod, String> = emptyMap(),
    val loadingPeriods: Set<BillingPeriod> = emptySet(),
    val isOutdatedPeriods: Set<BillingPeriod> = emptySet(),
    val scrollPositions: Map<BillingPeriod, ListScrollPosition> = emptyMap()
) {
    /** 當前畫面唯一合法的發票摘要，嚴格綁定 currentPeriod */
    val currentSummary: PeriodInvoiceSummary? get() = periodSummaries[currentPeriod]

    /** 當前期別之錯誤訊息 */
    val currentError: String? get() = periodErrors[currentPeriod]

    /** 當前期別是否正在載入中 */
    val isCurrentLoading: Boolean get() = loadingPeriods.contains(currentPeriod)

    /** 當前期別是否超出財政部 5 期 (半年) 保存期限 */
    val isCurrentOutdated: Boolean get() = isOutdatedPeriods.contains(currentPeriod)

    /** 是否允許往後切換至未來月份 (上限鎖定至當前雙月) */
    val canGoNext: Boolean get() = currentPeriod < BillingPeriod.current()

    // 兼容屬性
    val summary: PeriodInvoiceSummary? get() = currentSummary
    val isLoading: Boolean get() = isCurrentLoading
    val errorMessage: String? get() = currentError
}

/**
 * 發票清單視圖模型 (InvoiceListViewModel)。
 * 職責：
 * 1. 管理雙月期別狀態機，每次切換一律重新呼叫 API 取得最新資料。
 * 2. 嚴格期別槽位隔離，杜絕跨期別資料幽靈殘留。
 * 3. 封裝未來月份阻斷與歷史 5 期超期自動偵測。
 * 4. 支援發票表頭直接外部匯出記帳 (優先本地細項快取與保底機制)。
 */
class InvoiceListViewModel(
    private val invoiceRepository: InvoiceRepository,
    private val invoiceExportService: InvoiceExportService
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoiceListUiState())
    val uiState: StateFlow<InvoiceListUiState> = _uiState.asStateFlow()

    init {
        loadInvoices(period = _uiState.value.currentPeriod)
    }

    /**
     * 載入指定期別發票。
     * 每次切換期別均穿透發送 Fresh API 請求 (forceRefresh = true)。
     */
    fun loadInvoices(period: BillingPeriod) {
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(
                    currentPeriod = period,
                    loadingPeriods = state.loadingPeriods + period,
                    periodErrors = state.periodErrors - period
                )
            }

            // 1. 若該期別已超出財政部 5 期 (半年) 保存窗口，直接標記過期並賦予合規空資料
            if (!period.isWithinMofQueryWindow()) {
                val emptySummary = PeriodInvoiceSummary(
                    period = period,
                    totalAmount = 0L,
                    invoiceCount = 0,
                    invoices = emptyList()
                )
                _uiState.update { state ->
                    state.copy(
                        loadingPeriods = state.loadingPeriods - period,
                        periodSummaries = state.periodSummaries + (period to emptySummary),
                        isOutdatedPeriods = state.isOutdatedPeriods + period
                    )
                }
                return@launch
            }

            // 2. 每次切換期別一律強制穿透向財政部發送即時請求 (forceRefresh = true)
            invoiceRepository.getInvoiceHeaders(period = period, forceRefresh = true)
                .onSuccess { summary ->
                    _uiState.update { state ->
                        state.copy(
                            loadingPeriods = state.loadingPeriods - period,
                            periodSummaries = state.periodSummaries + (period to summary),
                            isOutdatedPeriods = state.isOutdatedPeriods - period
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update { state ->
                        state.copy(
                            loadingPeriods = state.loadingPeriods - period,
                            periodErrors = state.periodErrors + (period to (error.message ?: "載入發票失敗")),
                            // 失敗時確保清除該期別資料，避免殘留
                            periodSummaries = state.periodSummaries - period
                        )
                    }
                }
        }
    }

    fun selectPreviousPeriod() {
        loadInvoices(period = _uiState.value.currentPeriod.previous())
    }

    fun selectNextPeriod() {
        if (_uiState.value.canGoNext) {
            loadInvoices(period = _uiState.value.currentPeriod.next())
        }
    }

    /**
     * 儲存指定期別之清單捲動位置
     */
    fun saveScrollPosition(period: BillingPeriod, index: Int, offset: Int) {
        _uiState.update { state ->
            state.copy(
                scrollPositions = state.scrollPositions + (period to ListScrollPosition(index, offset))
            )
        }
    }

    /**
     * 取得指定期別之清單捲動位置
     */
    fun getScrollPosition(period: BillingPeriod): ListScrollPosition {
        return _uiState.value.scrollPositions[period] ?: ListScrollPosition(0, 0)
    }

    /**
     * 將指定發票表頭直接匯出至 EquityTracker 自動記帳。
     */
    fun exportInvoice(invoice: InvoiceHeader) {
        viewModelScope.launch {
            val success = invoiceExportService.exportToEquityTracker(invoice)
            if (!success) {
                NotificationController.showError("尚未安裝 EquityTracker 記帳軟體")
            }
        }
    }
}
