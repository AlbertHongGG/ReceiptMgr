package com.receiptmgr.app.presentation.viewmodel

import androidx.lifecycle.ViewModel
import com.receiptmgr.app.presentation.navigation.model.AppRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun pushRoute(route: AppRoute) {
        _uiState.update { state ->
            if (state.backStack.lastOrNull() == route) {
                state
            } else {
                state.copy(backStack = state.backStack + route)
            }
        }
    }

    fun popRoute(): Boolean {
        var didPop = false
        _uiState.update { state ->
            if (state.backStack.size > 1) {
                didPop = true
                state.copy(backStack = state.backStack.dropLast(1))
            } else {
                state
            }
        }
        return didPop
    }
}
