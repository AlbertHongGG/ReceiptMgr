package com.receiptmgr.app.presentation.features.invoice.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptmgr.app.domain.model.invoice.BillingPeriod
import com.receiptmgr.app.domain.model.invoice.PeriodInvoiceSummary
import com.receiptmgr.app.domain.repository.InvoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 發票清單 UI 狀態模型
 */
data class InvoiceListUiState(
    val currentPeriod: BillingPeriod = BillingPeriod.current(),
    val summary: PeriodInvoiceSummary? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null
)

/**
 * 發票清單視圖模型 (InvoiceListViewModel)。
 * 職責：管理當前雙月期別、執行表頭載入與穿透強制刷新、匯總帳務指標。
 */
class InvoiceListViewModel(
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoiceListUiState())
    val uiState: StateFlow<InvoiceListUiState> = _uiState.asStateFlow()

    init {
        loadInvoices(period = _uiState.value.currentPeriod, forceRefresh = false)
    }

    fun loadInvoices(period: BillingPeriod, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update {
                if (forceRefresh) {
                    it.copy(currentPeriod = period, isRefreshing = true, errorMessage = null)
                } else {
                    it.copy(currentPeriod = period, isLoading = true, errorMessage = null)
                }
            }

            invoiceRepository.getInvoiceHeaders(period = period, forceRefresh = forceRefresh)
                .onSuccess { summary ->
                    _uiState.update {
                        it.copy(
                            summary = summary,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = error.message ?: "載入發票清單失敗"
                        )
                    }
                }
        }
    }

    fun selectPreviousPeriod() {
        loadInvoices(period = _uiState.value.currentPeriod.previous(), forceRefresh = false)
    }

    fun selectNextPeriod() {
        loadInvoices(period = _uiState.value.currentPeriod.next(), forceRefresh = false)
    }

    fun refresh() {
        loadInvoices(period = _uiState.value.currentPeriod, forceRefresh = true)
    }
}
