package com.receiptmgr.app.domain.model.log

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import java.io.File

/**
 * 系統標準 API 互動紀錄資料傳輸模型 (ApiLogRecord)。
 */
@OptIn(ExperimentalSerializationApi::class)
@Serializable
data class ApiLogRecord(
    val timestamp: String = "",
    val endpoint: String = "",
    val method: String = "POST",
    val status: Int = 200,
    val durationMs: Long = 0L,
    val categoryId: String = "all",
    val requestHeaders: Map<String, String> = emptyMap(),
    val requestBody: String = "",
    val responseBody: String = "",
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val decryptedPayload: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val error: String? = null,
    val isSuccess: Boolean = status in 200..299
)

/**
 * API 日誌列表輕量元資料模型 (ApiLogModel)。
 *
 * 遵循記憶體解耦原則：
 * 列表常駐記憶體僅保存輕量索引（id, file, title, endpoint, method, status, durationMs, displayTime, timestamp, category, isSuccess），
 * 巨量酬載（rawJson 及解密後的大型 JSON）改為按需延遲載入（Lazy Loading），
 * 徹底杜絕常駐 Heap 飆破引發的系統 OOM 或 GC 卡頓。
 */
data class ApiLogModel(
    override val id: String,
    override val file: File,
    val title: String,
    val endpoint: String,
    val method: String = "POST",
    val status: Int = 200,
    val durationMs: Long = 0L,
    val displayTime: String = "",
    val timestamp: Long = 0L,
    val category: ApiCategoryType = ApiCategoryType.fromEndpoint(endpoint),
    val isSuccess: Boolean = status in 200..299,
    val rawJson: String = ""
) : DeletableLog {
    override val timestampMillis: Long get() = timestamp

    /**
     * 按需取得完整 JSON 內容（若 rawJson 為空則自磁碟即時延遲讀取）
     */
    fun resolveRawJson(): String = if (rawJson.isNotEmpty()) rawJson else runCatching { file.readText() }.getOrDefault("{}")
}
