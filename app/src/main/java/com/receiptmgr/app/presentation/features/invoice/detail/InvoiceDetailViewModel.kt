package com.receiptmgr.app.presentation.features.invoice.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptmgr.app.domain.model.invoice.InvoiceDetail
import com.receiptmgr.app.domain.repository.InvoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InvoiceDetailUiState(
    val detail: InvoiceDetail? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class InvoiceDetailViewModel(
    private val invNum: String,
    private val invDate: String,
    private val invoiceRepository: InvoiceRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvoiceDetailUiState(isLoading = true))
    val uiState: StateFlow<InvoiceDetailUiState> = _uiState.asStateFlow()

    init {
        loadDetail(forceRefresh = false)
    }

    fun loadDetail(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            invoiceRepository.getInvoiceDetail(invNum = invNum, invDate = invDate, forceRefresh = forceRefresh)
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            detail = detail,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
                .onFailure { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message ?: "載入品項明細失敗"
                        )
                    }
                }
        }
    }
}
