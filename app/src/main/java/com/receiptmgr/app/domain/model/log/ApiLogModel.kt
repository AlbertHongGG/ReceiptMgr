package com.receiptmgr.app.domain.model.log

import kotlinx.serialization.Serializable
import java.io.File

@Serializable
data class ApiLogRecord(
    val timestamp: String = "",
    val endpoint: String,
    val method: String = "POST",
    val status: Int = 0,
    val durationMs: Long = 0,
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String = "",
    val responseBody: String = "",
    val decryptedPayload: String? = null,
    val error: String? = null,
    val isSuccess: Boolean = false
)

data class ApiLogModel(
    override val id: String,
    override val file: File,
    val record: ApiLogRecord,
    val timestampStr: String,
    override val timestampMillis: Long
) : DeletableLog
