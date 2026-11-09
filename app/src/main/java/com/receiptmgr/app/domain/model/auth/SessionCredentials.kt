package com.receiptmgr.app.domain.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 伺服器解密後會話資料結構 (對齊官方 LoginData 欄位規範)
 */
@Serializable
data class SessionPayload(
    @SerialName("need_change_pw")
    val needChangePw: Boolean = false,
    @SerialName("sid")
    val sid: String = "",
    @SerialName("skey")
    val skey: String = "",
    @SerialName("iv")
    val iv: String = "",
    @SerialName("token")
    val token: String = "",
    @SerialName("ltoken")
    val ltoken: String = "",
    @SerialName("appid")
    val appId: String = "",
    @SerialName("ssme")
    val ssMe: String = "",
    @SerialName("liat")
    val liat: Long = 0L,
    @SerialName("carrier_code")
    val carrierCode: String = "",
    @SerialName("nick_name")
    val nickName: String = "",
    @SerialName("now")
    val now: Long = 0L
)

/**
 * 系統核心會話憑證實體 (Domain Session Entity)
 */
@Serializable
data class SessionCredentials(
    val account: String,
    val payload: SessionPayload,
    val rawPayloadJson: String,
    val rawServerResponseJson: String = "",
    val savedAtMillis: Long = System.currentTimeMillis()
) {
    val phone: String get() = account
    val sid: String get() = payload.sid
    val skey: String get() = payload.skey
    val iv: String get() = payload.iv
    val token: String get() = payload.token
    val ltoken: String get() = payload.ltoken
    val appId: String get() = payload.appId
    val ssMe: String get() = payload.ssMe
    val ssme: String get() = payload.ssMe
    val liat: Long get() = payload.liat
    val carrierCode: String get() = payload.carrierCode
    val nickName: String get() = payload.nickName
    val needChangePw: Boolean get() = payload.needChangePw
    val now: Long get() = payload.now

    /**
     * 載具條碼字串：優先使用財政部下發之手機條碼載具 (如 /8J722KT)，未綁定時回退為使用者帳號
     */
    val barcode: String get() = if (payload.carrierCode.isNotBlank()) payload.carrierCode else account

    /**
     * UIA 內部調用常用組合權杖: "sid|token"
     */
    val accessToken: String get() = "$sid|$token"

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
