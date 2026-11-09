package com.receiptmgr.app.presentation.features.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptmgr.app.data.device.DeviceEnvironmentProvider
import com.receiptmgr.app.domain.model.auth.DeviceFingerprint
import com.receiptmgr.app.domain.model.auth.LoginCommand
import com.receiptmgr.app.domain.model.auth.SessionCredentials
import com.receiptmgr.app.domain.model.auth.TargetClientSpec
import com.receiptmgr.app.domain.model.auth.UiaProtocolException
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
    val appId: String = TargetClientSpec.DEFAULT_APP_ID,
    val appVersion: String = TargetClientSpec.DEFAULT_TARGET_APP_VERSION,
    val deviceModel: String = "",
    val osVersion: String = "",
    val uuid: String = "",
    val loginMod: String = TargetClientSpec.DEFAULT_LOGIN_MOD,
    val isLoading: Boolean = false,
    val error: String? = null,
    val responseSession: SessionCredentials? = null,
    val responseTab: Int = 0 // 0: 核心憑證, 1: 解密 JSON, 2: 伺服器原始 JSON
)

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val deviceEnvironmentProvider: DeviceEnvironmentProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(
            appId = TargetClientSpec.DEFAULT_APP_ID,
            appVersion = TargetClientSpec.DEFAULT_TARGET_APP_VERSION,
            deviceModel = deviceEnvironmentProvider.getDeviceModel(),
            osVersion = deviceEnvironmentProvider.getOsVersion(),
            uuid = deviceEnvironmentProvider.getAndroidUuid(),
            loginMod = TargetClientSpec.DEFAULT_LOGIN_MOD
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

    fun resetDefaults() {
        val fingerprint = deviceEnvironmentProvider.getDeviceFingerprint()
        _uiState.update {
            it.copy(
                appId = TargetClientSpec.DEFAULT_APP_ID,
                appVersion = TargetClientSpec.DEFAULT_TARGET_APP_VERSION,
                deviceModel = fingerprint.deviceModel,
                osVersion = fingerprint.osVersion,
                uuid = fingerprint.androidUuid,
                loginMod = TargetClientSpec.DEFAULT_LOGIN_MOD,
                error = null
            )
        }
        NotificationController.showInfo("已重設為官方標準參數 (版號: ${TargetClientSpec.DEFAULT_TARGET_APP_VERSION})")
    }

    fun executeLogin() {
        val current = _uiState.value
        if (current.phone.isBlank() || current.password.isBlank()) {
            val errMsg = "請輸入帳號與密碼"
            _uiState.update { it.copy(error = errMsg) }
            NotificationController.showWarning(errMsg)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val command = LoginCommand(
                phone = current.phone,
                password = current.password,
                clientSpec = TargetClientSpec(
                    appId = current.appId,
                    appVersion = current.appVersion,
                    loginMod = current.loginMod
                ),
                deviceFingerprint = DeviceFingerprint(
                    deviceModel = current.deviceModel,
                    osVersion = current.osVersion,
                    androidUuid = current.uuid
                )
            )

            val result = authRepository.login(command)
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
                val errorMsg = when (ex) {
                    is UiaProtocolException.DecryptionFailed ->
                        "伺服器密文解密失敗 (-205): 請確認 Byte 0 對齊與金鑰"
                    is UiaProtocolException.TimestampExpired ->
                        "伺服器時間戳過期或重放攔截 (-204): 請確認系統時鐘即時性"
                    is UiaProtocolException.ServerRejected ->
                        "登入遭伺服器拒絕 (代碼: ${ex.resultCode})"
                    is UiaProtocolException.TransportFailure ->
                        "伺服器網路傳輸失敗: HTTP ${ex.httpStatus}"
                    else ->
                        ex.message ?: "登入失敗"
                }

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
