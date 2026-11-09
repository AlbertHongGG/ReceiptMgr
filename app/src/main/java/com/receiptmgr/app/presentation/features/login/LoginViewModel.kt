package com.receiptmgr.app.presentation.features.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptmgr.app.data.device.DeviceEnvironmentProvider
import com.receiptmgr.app.domain.model.auth.DeviceFingerprint
import com.receiptmgr.app.domain.model.auth.LoginCommand
import com.receiptmgr.app.domain.model.auth.SessionCredentials
import com.receiptmgr.app.domain.model.auth.TargetAppSpec
import com.receiptmgr.app.domain.model.auth.UiaProtocolException
import com.receiptmgr.app.domain.repository.AuthRepository
import com.receiptmgr.app.presentation.notification.NotificationController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginUiState(
    val account: String = "",
    val password: String = "",
    val appver: String = TargetAppSpec.DEFAULT_TARGET_APP_VER,
    val appbn: Int = TargetAppSpec.DEFAULT_TARGET_APP_BN,
    val appId: String = TargetAppSpec.DEFAULT_APP_ID,
    val deviceModel: String = "",
    val osVersion: String = "",
    val androidId: String = "",
    val ptoken: String = TargetAppSpec.DEFAULT_PTOKEN,
    val isLoading: Boolean = false,
    val error: String? = null,
    val responseSession: SessionCredentials? = null,
    val responseTab: Int = 0 // 0: 核心憑證, 1: 解密 JSON, 2: 伺服器原始 JSON
) {
    val phone: String get() = account
    val pdid: String get() = "${DeviceFingerprint.PREFIX_ANDROID}$androidId"
}

class LoginViewModel(
    private val authRepository: AuthRepository,
    private val deviceEnvironmentProvider: DeviceEnvironmentProvider
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        LoginUiState(
            appver = TargetAppSpec.DEFAULT_TARGET_APP_VER,
            appbn = TargetAppSpec.DEFAULT_TARGET_APP_BN,
            appId = TargetAppSpec.DEFAULT_APP_ID,
            deviceModel = deviceEnvironmentProvider.getDeviceModel(),
            osVersion = deviceEnvironmentProvider.getOsVersion(),
            androidId = deviceEnvironmentProvider.getAndroidId(),
            ptoken = deviceEnvironmentProvider.getPtoken()
        )
    )
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onAccountChange(value: String) {
        _uiState.update { it.copy(account = value.trim(), error = null) }
    }

    fun onPhoneChange(value: String) = onAccountChange(value)

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null) }
    }

    fun onDeviceModelChange(value: String) {
        _uiState.update { it.copy(deviceModel = value.trim()) }
    }

    fun onOsVersionChange(value: String) {
        _uiState.update { it.copy(osVersion = value.trim()) }
    }

    fun onAndroidIdChange(value: String) {
        _uiState.update { it.copy(androidId = value.trim()) }
    }

    fun onPtokenChange(value: String) {
        _uiState.update { it.copy(ptoken = value.trim()) }
    }

    fun onAppverChange(value: String) {
        _uiState.update { it.copy(appver = value.trim()) }
    }

    fun onAppbnChange(value: String) {
        val bn = value.trim().toIntOrNull() ?: TargetAppSpec.DEFAULT_TARGET_APP_BN
        _uiState.update { it.copy(appbn = bn) }
    }

    fun onSelectResponseTab(tab: Int) {
        _uiState.update { it.copy(responseTab = tab) }
    }

    fun resetDefaults() {
        val fingerprint = deviceEnvironmentProvider.getDeviceFingerprint()
        _uiState.update {
            it.copy(
                appver = TargetAppSpec.DEFAULT_TARGET_APP_VER,
                appbn = TargetAppSpec.DEFAULT_TARGET_APP_BN,
                appId = TargetAppSpec.DEFAULT_APP_ID,
                deviceModel = fingerprint.model,
                osVersion = fingerprint.osVersion,
                androidId = fingerprint.androidId,
                ptoken = fingerprint.ptoken,
                error = null
            )
        }
        NotificationController.showInfo("已重設為官方標準規格 (版號: ${TargetAppSpec.DEFAULT_TARGET_APP_VER}, 碼號: ${TargetAppSpec.DEFAULT_TARGET_APP_BN})")
    }

    fun executeLogin() {
        val current = _uiState.value
        if (current.account.isBlank() || current.password.isBlank()) {
            val errMsg = "請輸入帳號與密碼"
            _uiState.update { it.copy(error = errMsg) }
            NotificationController.showWarning(errMsg)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val command = LoginCommand(
                account = current.account,
                password = current.password,
                targetSpec = TargetAppSpec(
                    appId = current.appId,
                    appver = current.appver,
                    appbn = current.appbn
                ),
                deviceFingerprint = DeviceFingerprint(
                    model = current.deviceModel,
                    osVersion = current.osVersion,
                    androidId = current.androidId,
                    ptoken = current.ptoken
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
                NotificationController.showSuccess("登入成功，已取得會話憑證")
            }.onFailure { ex ->
                val errorMsg = when (ex) {
                    is UiaProtocolException.DecryptionFailed ->
                        "伺服器密文解密失敗 (-205): 請確認 AES-GCM 密碼學與 Tag"
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
