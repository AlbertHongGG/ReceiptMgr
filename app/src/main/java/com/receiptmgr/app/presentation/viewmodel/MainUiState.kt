package com.receiptmgr.app.presentation.viewmodel

import com.receiptmgr.app.presentation.navigation.model.AppRoute

data class MainUiState(
    val backStack: List<AppRoute> = listOf(AppRoute.Home)
)
