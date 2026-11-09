package com.receiptmgr.app.domain.model.auth

/**
 * 登入執行指令 (Domain Command)
 * 封裝使用者輸入之憑證、目標 API 客戶端規格與設備指紋。
 * 乾淨的領域指令，不含底層網路時間戳 (timestamp 由 Protocol 層在封裝加密當下即時注入)。
 */
data class LoginCommand(
    val phone: String,
    val password: String,
    val clientSpec: TargetClientSpec = TargetClientSpec(),
    val deviceFingerprint: DeviceFingerprint
) {
    val appId: String get() = clientSpec.appId
    val appVersion: String get() = clientSpec.appVersion
    val loginMod: String get() = clientSpec.loginMod
    val deviceModel: String get() = deviceFingerprint.deviceModel
    val osVersion: String get() = deviceFingerprint.osVersion
    val uuid: String get() = deviceFingerprint.androidUuid
}
