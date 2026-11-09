package com.receiptmgr.app.presentation.features.login

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptmgr.app.data.crypto.DeviceFingerprintProvider
import com.receiptmgr.app.domain.model.auth.LoginRequestData
import com.receiptmgr.app.domain.model.auth.SessionCredentials
import com.receiptmgr.app.domain.repository.AuthRepository
import com.receiptmgr.app.presentation.notification.NotificationController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val phone: String = "",
    val password: String = "",
    val appId: String = DeviceFingerprintProvider.DEFAULT_APP_ID,
    val appVersion: String = DeviceFingerprintProvider.DEFAULT_APP_VERSION,
    val deviceModel: String = "",
    val osVersion: String = "",
    val uuid: String = "",
    val loginMod: String = "0",
    val isLoading: Boolean = false,
    val error: String? = null,
    val responseSession: SessionCredentials? = null,
    val responseTab: Int = 0 // 0: 核心憑證, 1: 解密 JSON, 2: 伺服器原始 JSON
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(
            deviceModel = DeviceFingerprintProvider.getDeviceModel(),
            osVersion = DeviceFingerprintProvider.getOsVersion(),
            uuid = DeviceFingerprintProvider.getAndroidUuid(context),
            appVersion = DeviceFingerprintProvider.getAppVersion(context)
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onPhoneChange(value: String) {
        _uiState.update { it.copy(phone = value.trim(), error = null) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun onAppIdChange(value: String) {
        _uiState.update { it.copy(appId = value.trim()) }
    }

    fun onAppVersionChange(value: String) {
        _uiState.update { it.copy(appVersion = value.trim()) }
    }

    fun onDeviceModelChange(value: String) {
        _uiState.update { it.copy(deviceModel = value.trim()) }
    }

    fun onOsVersionChange(value: String) {
        _uiState.update { it.copy(osVersion = value.trim()) }
    }

    fun onUuidChange(value: String) {
        _uiState.update { it.copy(uuid = value.trim()) }
    }

    fun onLoginModChange(value: String) {
        _uiState.update { it.copy(loginMod = value.trim()) }
    }

    fun onSelectResponseTab(tab: Int) {
        _uiState.update { it.copy(responseTab = tab) }
    }

    fun resetDefaults(context: Context) {
        _uiState.update {
            it.copy(
                appId = DeviceFingerprintProvider.DEFAULT_APP_ID,
                appVersion = DeviceFingerprintProvider.getAppVersion(context),
                deviceModel = DeviceFingerprintProvider.getDeviceModel(),
                osVersion = DeviceFingerprintProvider.getOsVersion(),
                uuid = DeviceFingerprintProvider.getAndroidUuid(context),
                loginMod = "0",
                error = null
            )
        }
        NotificationController.showInfo("已重設預設參數")
    }

    fun executeLogin() {
        val current = _uiState.value
        if (current.phone.isBlank() || current.password.isBlank()) {
            _uiState.update { it.copy(error = "請輸入帳號與密碼") }
            NotificationController.showWarning("請輸入帳號與密碼")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val requestData = LoginRequestData(
                phone = current.phone,
                password = current.password,
                appId = current.appId,
                appVersion = current.appVersion,
                deviceModel = current.deviceModel,
                osVersion = current.osVersion,
                uuid = current.uuid,
                timestamp = System.currentTimeMillis() / 1000L,
                loginMod = current.loginMod
            )

            val result = authRepository.login(requestData)
            result.onSuccess { session ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        responseSession = session,
                        error = null
                    )
                }
                NotificationController.showSuccess("登入成功，已取得會話")
            }.onFailure { ex ->
                val errorMsg = ex.message ?: "登入失敗"
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = errorMsg
                    )
                }
                NotificationController.showError(errorMsg)
            }
        }
    }
}
