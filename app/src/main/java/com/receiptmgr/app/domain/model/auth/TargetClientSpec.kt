package com.receiptmgr.app.domain.model.auth

/**
 * 目標 API 客戶端規格 (Target Client Protocol Specification)
 * 代表財政部官方 App 的協定識別參數，非宿主 App 之版本與代碼。
 */
data class TargetClientSpec(
    val appId: String = DEFAULT_APP_ID,
    val appVersion: String = DEFAULT_TARGET_APP_VERSION,
    val loginMod: String = DEFAULT_LOGIN_MOD
) {
    companion object {
        const val DEFAULT_APP_ID = "EINV6202512184519"
        const val DEFAULT_TARGET_APP_VERSION = "6.800.11"
        const val DEFAULT_LOGIN_MOD = "0"
    }
}
