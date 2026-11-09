package com.receiptmgr.app.domain.model.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LoginRequestData(
    @SerialName("phone")
    val phone: String,
    @SerialName("password")
    val password: String,
    @SerialName("app_id")
    val appId: String = "EINV6202512184519",
    @SerialName("app_version")
    val appVersion: String = "6.800.11",
    @SerialName("device_model")
    val deviceModel: String,
    @SerialName("os_version")
    val osVersion: String,
    @SerialName("uuid")
    val uuid: String,
    @SerialName("timestamp")
    val timestamp: Long = System.currentTimeMillis() / 1000,
    @SerialName("login_mod")
    val loginMod: String = "0"
)
