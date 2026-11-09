package com.receiptmgr.app.domain.model.log

import kotlinx.serialization.Serializable

@Serializable
data class NotificationLogRecord(
    val timestamp: String,
    val type: String,
    val message: String
)
