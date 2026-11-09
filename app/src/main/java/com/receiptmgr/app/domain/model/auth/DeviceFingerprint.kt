package com.receiptmgr.app.domain.model.auth

/**
 * 設備硬體指紋資訊 (本機環境特徵)
 */
data class DeviceFingerprint(
    val deviceModel: String,
    val osVersion: String,
    val androidUuid: String
)
