package com.receiptmgr.app.domain.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SessionPayload(
    @SerialName("sid")
    val sid: String = "",
    @SerialName("token")
    val token: String = "",
    @SerialName("svr_code")
    val svrCode: String = "",
    @SerialName("iv")
    val iv: String = "",
    @SerialName("liat")
    val liat: Long = 0L,
    @SerialName("app_id")
    val appId: String = "",
    @SerialName("ss_me")
    val ssMe: String = ""
)

@Serializable
data class SessionCredentials(
    val phone: String,
    val payload: SessionPayload,
    val rawPayloadJson: String,
    val rawServerResponseJson: String = "",
    val savedAtMillis: Long = System.currentTimeMillis()
) {
    val sid: String get() = payload.sid
    val token: String get() = payload.token
    val svrCode: String get() = payload.svrCode
    val iv: String get() = payload.iv
    val liat: Long get() = payload.liat
    val appId: String get() = payload.appId
    val ssMe: String get() = payload.ssMe

    /**
     * UIA 登入會話有效期限為 180 天 (15,552,000 秒)
     */
    val expiryTimestampSeconds: Long get() = liat + 180L * 86400L

    val isExpired: Boolean get() {
        val nowSec = System.currentTimeMillis() / 1000L
        return nowSec >= expiryTimestampSeconds
    }

    val remainingDays: Int get() {
        val nowSec = System.currentTimeMillis() / 1000L
        val diff = expiryTimestampSeconds - nowSec
        return if (diff > 0) (diff / 86400L).toInt() else 0
    }
}
