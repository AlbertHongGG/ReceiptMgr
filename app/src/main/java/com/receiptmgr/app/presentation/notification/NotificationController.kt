package com.receiptmgr.app.presentation.notification

import com.receiptmgr.app.data.storage.NotificationLogManager
import com.receiptmgr.app.domain.model.common.AppNotification
import com.receiptmgr.app.domain.model.common.NotificationType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * 全域通知控制器，管理浮動通知佇列（最多 3 筆，FIFO，定時自動關閉）並寫入持久化日誌。
 */
class NotificationController(
    private val logManager: NotificationLogManager
) {
    companion object {
        lateinit var instance: NotificationController
            private set

        fun init(controller: NotificationController) {
            instance = controller
        }

        fun showSuccess(message: String, durationMs: Long = 4000L) {
            if (::instance.isInitialized) instance.showSuccess(message, durationMs)
        }

        fun showError(message: String, durationMs: Long = 5000L) {
            if (::instance.isInitialized) instance.showError(message, durationMs)
        }

        fun showWarning(message: String, durationMs: Long = 4000L) {
            if (::instance.isInitialized) instance.showWarning(message, durationMs)
        }

        fun showInfo(message: String, durationMs: Long = 4000L) {
            if (::instance.isInitialized) instance.showInfo(message, durationMs)
        }
    }

    private val scope = CoroutineScope(Dispatchers.Main.immediate)
    private val dismissJobs = ConcurrentHashMap<String, Job>()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private fun addNotification(notification: AppNotification) {
        _notifications.update { current ->
            val list = current.toMutableList()
            list.add(notification)
            while (list.size > 3) {
                val removed = list.removeAt(0)
                dismissJobs.remove(removed.id)?.cancel()
            }
            list
        }

        val job = scope.launch {
            delay(notification.durationMs)
            remove(notification.id)
        }
        dismissJobs[notification.id] = job
    }

    fun remove(id: String) {
        dismissJobs.remove(id)?.cancel()
        _notifications.update { current ->
            current.filter { it.id != id }
        }
    }

    fun showSuccess(message: String, durationMs: Long = 4000L) {
        logManager.logNotification(NotificationType.SUCCESS, message)
        addNotification(
            AppNotification(
                type = NotificationType.SUCCESS,
                message = message,
                durationMs = durationMs
            )
        )
    }

    fun showError(message: String, durationMs: Long = 5000L) {
        logManager.logNotification(NotificationType.ERROR, message)
        addNotification(
            AppNotification(
                type = NotificationType.ERROR,
                message = message,
                durationMs = durationMs
            )
        )
    }

    fun showWarning(message: String, durationMs: Long = 4000L) {
        logManager.logNotification(NotificationType.WARNING, message)
        addNotification(
            AppNotification(
                type = NotificationType.WARNING,
                message = message,
                durationMs = durationMs
            )
        )
    }

    fun showInfo(message: String, durationMs: Long = 4000L) {
        logManager.logNotification(NotificationType.INFO, message)
        addNotification(
            AppNotification(
                type = NotificationType.INFO,
                message = message,
                durationMs = durationMs
            )
        )
    }
}
