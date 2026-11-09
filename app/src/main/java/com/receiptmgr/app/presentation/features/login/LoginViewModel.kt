package com.receiptmgr.app.presentation.features.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.receiptmgr.app.data.device.DeviceEnvironmentProvider
import com.receiptmgr.app.domain.model.auth.DeviceFingerprint
import com.receiptmgr.app.domain.model.auth.LoginCommand
import com.receiptmgr.app.domain.model.auth.SessionState
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
    val isSuccess: Boolean = false,
    val error: String? = null
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
            account = (authRepository.sessionState.value as? SessionState.Authenticated)?.credentials?.account.orEmpty(),
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

    init {
        // 若當前已有登入會話且表單尚未輸入帳號，自動預填目前帳號以利再次登入或修改
        viewModelScope.launch {
            authRepository.sessionState.collect { state ->
                if (state is SessionState.Authenticated && _uiState.value.account.isBlank()) {
                    _uiState.update { it.copy(account = state.credentials.account) }
                }
            }
        }
    }

    fun onAccountChange(value: String) {
        _uiState.update { it.copy(account = value.trim(), error = null, isSuccess = false) }
    }

    fun onPhoneChange(value: String) = onAccountChange(value)

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value, error = null, isSuccess = false) }
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

    fun resetDefaults() {
        val currentAccount = (authRepository.sessionState.value as? SessionState.Authenticated)?.credentials?.account.orEmpty()
        val fingerprint = deviceEnvironmentProvider.getDeviceFingerprint()
        _uiState.update {
            it.copy(
                account = if (it.account.isBlank()) currentAccount else it.account,
                appver = TargetAppSpec.DEFAULT_TARGET_APP_VER,
                appbn = TargetAppSpec.DEFAULT_TARGET_APP_BN,
                appId = TargetAppSpec.DEFAULT_APP_ID,
                deviceModel = fingerprint.model,
                osVersion = fingerprint.osVersion,
                androidId = fingerprint.androidId,
                ptoken = fingerprint.ptoken,
                error = null,
                isSuccess = false
            )
        }
        NotificationController.showInfo("已重設為官方標準規格 (版號: ${TargetAppSpec.DEFAULT_TARGET_APP_VER}, 碼號: ${TargetAppSpec.DEFAULT_TARGET_APP_BN})")
    }

    fun executeLogin(onSuccess: (() -> Unit)? = null) {
        val current = _uiState.value
        if (current.account.isBlank() || current.password.isBlank()) {
            val errMsg = "請輸入帳號與密碼"
            _uiState.update { it.copy(error = errMsg) }
            NotificationController.showWarning(errMsg)
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isSuccess = false) }

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
                        isSuccess = true,
                        error = null
                    )
                }
                NotificationController.showSuccess("登入成功，已綁定載具 ${session.barcode}")
                onSuccess?.invoke()
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
                        isSuccess = false,
                        error = errorMsg
                    )
                }
                NotificationController.showError(errorMsg)
            }
        }
    }
}
