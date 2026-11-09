package com.receiptmgr.app.data.repository

import com.receiptmgr.app.data.storage.ApiLogManager
import com.receiptmgr.app.data.storage.NotificationLogManager
import com.receiptmgr.app.data.storage.SettingsDataStore
import com.receiptmgr.app.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsRepositoryImpl(
    private val settingsDataStore: SettingsDataStore,
    private val apiLogManager: ApiLogManager,
    private val notificationLogManager: NotificationLogManager
) : SettingsRepository {

    private val scope = CoroutineScope(Dispatchers.IO)

    override val apiLoggingEnabledFlow: StateFlow<Boolean> = settingsDataStore.apiLoggingEnabledFlow
        .stateIn(scope, SharingStarted.Eagerly, apiLogManager.isLoggingEnabled())

    override val notificationLoggingEnabledFlow: StateFlow<Boolean> = settingsDataStore.notificationLoggingEnabledFlow
        .stateIn(scope, SharingStarted.Eagerly, notificationLogManager.isLoggingEnabled())

    override suspend fun setApiLoggingEnabled(enabled: Boolean) {
        settingsDataStore.setApiLoggingEnabled(enabled)
        apiLogManager.setLoggingEnabled(enabled)
    }

    override suspend fun setNotificationLoggingEnabled(enabled: Boolean) {
        settingsDataStore.setNotificationLoggingEnabled(enabled)
        notificationLogManager.setLoggingEnabled(enabled)
    }
}
