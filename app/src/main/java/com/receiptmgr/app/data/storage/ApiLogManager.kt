package com.receiptmgr.app.data.storage

import android.content.Context
import android.util.Log
import com.receiptmgr.app.domain.model.log.ApiCategoryType
import com.receiptmgr.app.domain.model.log.ApiLogModel
import com.receiptmgr.app.domain.model.log.ApiLogRecord
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
 * API 通訊稽核日誌管理器 (ApiLogManager)。
 * 1:1 對標 LensWise 核心日誌引擎：
 * 1. 記憶體解耦快讀（Memory Decoupling）：列表載入僅自檔名與屬性讀取輕量元資料，拒絕在 Heap 常駐巨量 JSON。
 * 2. 確定性分類過濾與分頁支援：支援 6 大發票 API 類別篩選與專屬空狀態。
 * 3. 顆粒化刪除管理：支援長按拖曳單筆銷毀、分類批次抹除與全域清空。
 */
class ApiLogManager(private val context: Context) {

    companion object {
        private const val TAG = "ApiLogManager"
        private const val PREF_KEY_LOGGING_ENABLED = "pref_api_logging_enabled"
        private const val PREFS_NAME = "receipt_mgr_settings"
    }

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val logDir = File(context.filesDir, "logs/api").apply {
        if (!exists()) mkdirs()
    }

    private val logJson = Json {
        prettyPrint = true
        encodeDefaults = false
        explicitNulls = false
        ignoreUnknownKeys = true
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val _logsFlow = MutableStateFlow<List<ApiLogModel>>(emptyList())
    val logsFlow: StateFlow<List<ApiLogModel>> = _logsFlow.asStateFlow()

    init {
        reload()
    }

    fun isLoggingEnabled(): Boolean {
        return prefs.getBoolean(PREF_KEY_LOGGING_ENABLED, true)
    }

    fun setLoggingEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(PREF_KEY_LOGGING_ENABLED, enabled).apply()
    }

    fun logApiInteraction(record: ApiLogRecord) {
        if (!isLoggingEnabled()) return

        scope.launch {
            try {
                val now = Date()
                val yyyyMMdd = SimpleDateFormat("yyyyMMdd", Locale.US).format(now)
                val hhmmss = SimpleDateFormat("HHmmss", Locale.US).format(now)
                val random = Random.nextInt(100000, 999999)

                val category = ApiCategoryType.fromEndpoint(record.endpoint)
                val safeEndpoint = record.endpoint.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                val fileName = "${yyyyMMdd}_${hhmmss}_${category.id}_${safeEndpoint}_${record.status}_${record.durationMs}_${random}.json"
                val file = File(logDir, fileName)

                val recordToWrite = if (record.timestamp.isEmpty()) {
                    val isoTimestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS", Locale.US).format(now)
                    record.copy(timestamp = isoTimestamp, categoryId = category.id)
                } else {
                    record.copy(categoryId = category.id)
                }

                val jsonString = logJson.encodeToString(recordToWrite)
                file.writeText(jsonString)
                reload()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to write API log: ${e.message}", e)
            }
        }
    }

    fun reload() {
        scope.launch {
            val entries = loadAllEntries()
            _logsFlow.value = entries
        }
    }

    /**
     * 輕量載入所有日誌元資料，不解構磁碟大型 JSON 內容
     */
    private fun loadAllEntries(): List<ApiLogModel> {
        val files = logDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
        return files.mapNotNull { file ->
            try {
                val fileName = file.name
                val parts = fileName.removeSuffix(".json").split("_")

                var dtMillis = file.lastModified()
                var displayTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date(dtMillis))
                var category = ApiCategoryType.ALL
                var endpoint = "API Request"
                var status = 200
                var durationMs = 0L

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

                if (parts.size >= 3) {
                    category = ApiCategoryType.fromId(parts[2]) ?: ApiCategoryType.ALL
                }

                if (parts.size >= 4) {
                    endpoint = parts[3].replace("_", " ")
                }

                if (parts.size >= 5) {
                    status = parts[4].toIntOrNull() ?: 200
                }

                if (parts.size >= 6) {
                    durationMs = parts[5].toLongOrNull() ?: 0L
                }

                val title = if (endpoint.isNotBlank() && endpoint != "API Request") {
                    endpoint
                } else {
                    category.displayName
                }

                ApiLogModel(
                    id = file.name,
                    file = file,
                    title = title,
                    endpoint = endpoint,
                    method = if (endpoint.startsWith("GET")) "GET" else "POST",
                    status = status,
                    durationMs = durationMs,
                    displayTime = displayTime,
                    timestamp = dtMillis,
                    category = category,
                    isSuccess = status in 200..299
                )
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse API log metadata: ${file.name}", e)
                null
            }
        }.sortedByDescending { it.timestampMillis }
    }

    /**
     * 清空指定分類之日誌（若傳入 null 或 ALL 則清空全部）
     */
    fun clearLogsByCategory(category: ApiCategoryType?) {
        scope.launch {
            try {
                val files = logDir.listFiles { f -> f.extension == "json" } ?: emptyArray()
                for (file in files) {
                    if (category == null || category == ApiCategoryType.ALL) {
                        file.delete()
                    } else {
                        val parts = file.name.split("_")
                        if (parts.size >= 3 && parts[2].equals(category.id, ignoreCase = true)) {
                            file.delete()
                        }
                    }
                }
                reload()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear logs for category $category: ${e.message}", e)
            }
        }
    }

    /**
     * 刪除單一檔案（用於拖曳至垃圾桶）
     */
    fun clearLog(file: File) {
        scope.launch {
            try {
                if (file.exists()) {
                    file.delete()
                }
                reload()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to delete log file: ${file.name}", e)
            }
        }
    }

    /**
     * 清空全部日誌
     */
    fun clearAllLogs() {
        scope.launch {
            try {
                logDir.listFiles()?.forEach { it.delete() }
                reload()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to clear all API logs: ${e.message}", e)
            }
        }
    }
}
