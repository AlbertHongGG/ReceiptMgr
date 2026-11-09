package com.receiptmgr.app.presentation.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptmgr.app.data.device.DeviceEnvironmentProvider
import com.receiptmgr.app.domain.model.auth.SessionState
import com.receiptmgr.app.domain.repository.AuthRepository
import com.receiptmgr.app.domain.repository.SettingsRepository
import com.receiptmgr.app.presentation.notification.NotificationController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val deviceUuid: String = "",
    val deviceModel: String = "",
    val osVersion: String = "",
    val appVersion: String = ""
)

class SettingsViewModel(
    private val authRepository: AuthRepository,
    private val settingsRepository: SettingsRepository,
    private val deviceEnvironmentProvider: DeviceEnvironmentProvider
) : ViewModel() {

    val sessionState: StateFlow<SessionState> = authRepository.sessionState
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SessionState.Unauthenticated)

    val darkModeFlow: StateFlow<Boolean?> = settingsRepository.darkModeFlow
    val apiLoggingFlow: StateFlow<Boolean> = settingsRepository.apiLoggingEnabledFlow
    val notificationLoggingFlow: StateFlow<Boolean> = settingsRepository.notificationLoggingEnabledFlow

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            deviceUuid = deviceEnvironmentProvider.getAndroidUuid(),
            deviceModel = deviceEnvironmentProvider.getDeviceModel(),
            osVersion = deviceEnvironmentProvider.getOsVersion(),
            appVersion = deviceEnvironmentProvider.getHostAppVersion()
        )
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    fun setDarkMode(dark: Boolean?) {
        viewModelScope.launch {
            settingsRepository.setDarkMode(dark)
        }
    }

    fun setApiLogging(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setApiLoggingEnabled(enabled)
            NotificationController.showInfo(if (enabled) "已啟用 API 稽核紀錄" else "已關閉 API 稽核紀錄")
        }
    }

    fun setNotificationLogging(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setNotificationLoggingEnabled(enabled)
            NotificationController.showInfo(if (enabled) "已啟用通知日誌" else "已關閉通知日誌")
        }
    }

    fun clearSession() {
        viewModelScope.launch {
            authRepository.clearSession()
            NotificationController.showSuccess("已清除會話憑證")
        }
    }
}
