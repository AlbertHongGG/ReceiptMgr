package com.receiptmgr.app.domain.model.auth

/**
 * 登入執行指令 (Domain Command)
 * 封裝使用者憑證、目標 API 客戶端規格與設備指紋。
 * 乾淨的領域指令，不含底層網路時間戳 (ts) 與客戶端隨機種子 (ckey)，
 * 由 Protocol 編解碼層在封裝加密當下即時注入。
 */
data class LoginCommand(
    val account: String,
    val password: String,
    val targetSpec: TargetAppSpec = TargetAppSpec(),
    val deviceFingerprint: DeviceFingerprint
) {
    val phone: String get() = account
    val type: Int get() = targetSpec.type
    val appId: String get() = targetSpec.appId
    val appver: String get() = targetSpec.appver
    val appbn: Int get() = targetSpec.appbn
    val lang: String get() = targetSpec.lang
    val ccs: String get() = targetSpec.ccs
    val pdid: String get() = deviceFingerprint.pdid
    val model: String get() = deviceFingerprint.model
    val osVersion: String get() = deviceFingerprint.osVersion
    val os: String get() = deviceFingerprint.os
    val ptoken: String get() = deviceFingerprint.ptoken
}
