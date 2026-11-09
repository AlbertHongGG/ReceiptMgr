package com.receiptmgr.app.data.storage

import android.content.Context
import android.util.Log
import com.receiptmgr.app.domain.model.common.NotificationType
import com.receiptmgr.app.domain.model.log.NotificationLogEntry
import com.receiptmgr.app.domain.model.log.NotificationLogRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

/**
 * 系統通知日誌管理器 (NotificationLogManager)。
 * 負責將 App 發布之各項通知（成功、錯誤、警告、資訊）非同步持久化至本地 JSON。
 * 全面採用 kotlinx.serialization 強型別模型 [NotificationLogRecord]，消除舊式 JSONObject。
 */
class NotificationLogManager(private val context: Context) {

    companion object {
        private const val TAG = "NotificationLogManager"
        private const val PREF_KEY_LOGGING_ENABLED = "pref_notification_logging_enabled"
        private const val PREFS_NAME = "receipt_mgr_settings"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val logDir = File(context.filesDir, "logs/notifications").apply {
        if (!exists()) mkdirs()
    }

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _logsFlow = MutableStateFlow<List<NotificationLogEntry>>(emptyList())
    val logsFlow: StateFlow<List<NotificationLogEntry>> = _logsFlow.asStateFlow()

    init {
        reload()
    }

    fun isLoggingEnabled(): Boolean {
        return prefs.getBoolean(PREF_KEY_LOGGING_ENABLED, true)
    }

    fun setLoggingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_KEY_LOGGING_ENABLED, enabled).apply()
    }

    fun logNotification(type: NotificationType, message: String) {
        if (!isLoggingEnabled()) return

        scope.launch {
            try {
                val now = Date()
                val yyyyMMdd = SimpleDateFormat("yyyyMMdd", Locale.US).format(now)
                val hhmmss = SimpleDateFormat("HHmmss", Locale.US).format(now)
                val random = Random.nextInt(100000, 999999)
                val fileName = "${yyyyMMdd}_${hhmmss}_Notification_${type.name.lowercase(Locale.US)}_${random}.json"
                val file = File(logDir, fileName)

                val isoTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).format(now)
                val record = NotificationLogRecord(
                    timestamp = isoTimestamp,
                    type = type.name.lowercase(Locale.US),
                    message = message
                )

                file.writeText(json.encodeToString(record))
                reload()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write notification log: ${e.message}", e)
            }
        }
    }

    fun reload() {
        scope.launch {
            val entries = loadAllEntries()
            _logsFlow.value = entries
        }
    }

    private fun loadAllEntries(): List<NotificationLogEntry> {
        val files = logDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        return files.mapNotNull { file ->
            try {
                val fileName = file.name
                val parts = fileName.removeSuffix(".json").split("_")

                var dtMillis = file.lastModified()
                var displayTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(dtMillis))
                var notifType = NotificationType.INFO
                var message = ""

                if (parts.size >= 2) {
                    val dateStr = parts[0]
                    val timeStr = parts[1]
                    if (dateStr.length == 8 && timeStr.length >= 6) {
                        val parsed = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).parse("${dateStr}_${timeStr.take(6)}")
                        if (parsed != null) {
                            dtMillis = parsed.time
                            displayTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(parsed)
                        }
                    }
                }

                if (parts.size >= 4) {
                    val typeStr = parts[3].lowercase(Locale.US)
                    notifType = when (typeStr) {
                        "success" -> NotificationType.SUCCESS
                        "error" -> NotificationType.ERROR
                        "warning" -> NotificationType.WARNING
                        else -> NotificationType.INFO
                    }
                }

                // 讀取檔案取得 message
                runCatching {
                    val content = file.readText()
                    val record = json.decodeFromString<NotificationLogRecord>(content)
                    message = record.message
                    if (record.timestamp.isNotBlank()) {
                        displayTime = record.timestamp.replace("T", " ")
                    }
                }

                NotificationLogEntry(
                    id = file.name,
                    file = file,
                    type = notifType,
                    message = message,
                    timestampStr = displayTime,
                    timestampMillis = dtMillis
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse notification log: ${file.name}", e)
                null
            }
        }.sortedByDescending { it.timestampMillis }
    }

    fun clearLog(file: File) {
        scope.launch {
            try {
                if (file.exists()) {
                    file.delete()
                }
                reload()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete notification log: ${file.name}", e)
            }
        }
    }

    fun clearAllLogs() {
        scope.launch {
            try {
                logDir.listFiles()?.forEach { it.delete() }
                reload()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear all notification logs: ${e.message}", e)
            }
        }
    }
}
