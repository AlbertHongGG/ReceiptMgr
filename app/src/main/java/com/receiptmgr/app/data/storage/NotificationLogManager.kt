package com.receiptmgr.app.data.storage

import android.content.Context
import android.util.Log
import com.receiptmgr.app.domain.model.common.NotificationType
import com.receiptmgr.app.domain.model.log.NotificationLogEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random

class NotificationLogManager(private val context: Context) {

    companion object {
        private const val TAG = "NotificationLogManager"
        private const val PREF_KEY_LOGGING_ENABLED = "pref_notification_logging_enabled"
        private const val PREFS_NAME = "receipt_mgr_settings"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val logDir = File(context.filesDir, "logs/notifications").apply {
        if (!exists()) mkdirs()
    }

    private val scope = CoroutineScope(Dispatchers.IO)
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
                val fileName = "${yyyyMMdd}_${hhmmss}_Notification_${random}.json"
                val file = File(logDir, fileName)

                val isoTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).format(now)
                val json = JSONObject().apply {
                    put("timestamp", isoTimestamp)
                    put("type", type.name.lowercase(Locale.US))
                    put("message", message)
                }

                file.writeText(json.toString(2))
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
                val content = file.readText()
                val json = JSONObject(content)
                val typeStr = json.optString("type", "info")
                val type = when (typeStr.lowercase(Locale.US)) {
                    "success" -> NotificationType.SUCCESS
                    "error" -> NotificationType.ERROR
                    "warning" -> NotificationType.WARNING
                    else -> NotificationType.INFO
                }
                val message = json.optString("message", "")
                val timestampStr = json.optString("timestamp", "")
                val fileModified = file.lastModified()

                var displayTime = timestampStr
                if (timestampStr.isNotEmpty()) {
                    try {
                        val parsedDate = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).parse(timestampStr)
                        if (parsedDate != null) {
                            displayTime = SimpleDateFormat("yyyy/MM/dd HH:mm:ss", Locale.US).format(parsedDate)
                        }
                    } catch (_: Exception) {}
                }

                NotificationLogEntry(
                    id = file.name,
                    file = file,
                    type = type,
                    message = message,
                    timestampStr = displayTime,
                    timestampMillis = fileModified
                )
            } catch (e: Exception) {
                null
            }
        }.sortedByDescending { it.file.name }
    }

    suspend fun clearLog(file: File) = withContext(Dispatchers.IO) {
        try {
            if (file.exists()) {
                file.delete()
            }
            _logsFlow.value = _logsFlow.value.filter {
                it.file.absolutePath != file.absolutePath && it.id != file.name
            }
            val entries = loadAllEntries()
            _logsFlow.value = entries
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete log file: ${e.message}", e)
        }
    }

    suspend fun clearAllLogs() = withContext(Dispatchers.IO) {
        try {
            if (logDir.exists()) {
                logDir.listFiles()?.forEach { f ->
                    try {
                        if (f.isDirectory) f.deleteRecursively() else f.delete()
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to delete ${f.name}: ${e.message}")
                    }
                }
            }
            _logsFlow.value = emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all logs: ${e.message}", e)
            _logsFlow.value = emptyList()
        }
    }
}
