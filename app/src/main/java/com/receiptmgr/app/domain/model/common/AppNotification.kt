package com.receiptmgr.app.domain.model.common

import java.util.UUID

enum class NotificationType {
    SUCCESS,
    ERROR,
    INFO,
    WARNING
}

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val message: String,
    val type: NotificationType,
    val durationMs: Long = 4000L,
    val timestamp: Long = System.currentTimeMillis()
)
