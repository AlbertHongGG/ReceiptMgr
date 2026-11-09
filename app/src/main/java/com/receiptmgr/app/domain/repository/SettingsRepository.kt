package com.receiptmgr.app.domain.repository

import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val darkModeFlow: StateFlow<Boolean?>
    suspend fun setDarkMode(dark: Boolean?)

    val apiLoggingEnabledFlow: StateFlow<Boolean>
    suspend fun setApiLoggingEnabled(enabled: Boolean)

    val notificationLoggingEnabledFlow: StateFlow<Boolean>
    suspend fun setNotificationLoggingEnabled(enabled: Boolean)
}
