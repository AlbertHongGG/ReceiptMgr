package com.receiptmgr.app.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * UIA 三段式 ldata 請求主體 DTO
 */
@Serializable
data class UiaLdataRequestDto(
    @SerialName("ldata")
    val ldata: String
)

/**
 * UIA 伺服器最外層回應外殼 DTO
 */
@Serializable
data class UiaEnvelopeResponseDto(
    @SerialName("result")
    val result: Int = -999,
    @SerialName("id")
    val id: String? = null,
    @SerialName("message")
    val message: String? = null,
    @SerialName("payload")
    val payload: String? = null
)
