package com.receiptmgr.app.domain.model.auth

/**
 * 設備硬體指紋資訊 (Device Fingerprint)
 * 封裝本機硬體環境特徵與推播識別資訊，嚴格對齊官方通訊協定字典。
 */
data class DeviceFingerprint(
    val os: String = DEFAULT_OS_ANDROID,
    val osVersion: String,
    val model: String,
    val androidId: String,
    val ptoken: String = TargetAppSpec.DEFAULT_PTOKEN
) {
    /**
     * 官方協定規範之設備專屬識別碼：前綴 "a:" 加上 16 碼十六進位 Android ID
     */
    val pdid: String get() = "$PREFIX_ANDROID$androidId"

    companion object {
        const val DEFAULT_OS_ANDROID = "a"
        const val PREFIX_ANDROID = "a:"
    }
}
