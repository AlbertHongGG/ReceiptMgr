package com.receiptmgr.app.presentation.features.log

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.rounded.CalendarToday
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.receiptmgr.app.domain.model.log.ApiLogRecord
import com.receiptmgr.app.presentation.components.ImmersiveScaffold
import com.receiptmgr.app.presentation.components.JsonTreeViewer
import com.receiptmgr.app.presentation.components.NavigationBarsSafeSpacer
import com.receiptmgr.app.presentation.notification.NotificationController
import com.receiptmgr.app.presentation.theme.AppColors
import kotlinx.serialization.json.Json
import org.json.JSONObject

private val detailJson = Json { ignoreUnknownKeys = true }

/**
 * API 日誌詳細資料畫面 (ApiLogDetailScreen)。
 * 1:1 復刻 LensWise 圖三之雙核心樹狀卡片：
 * - 頂部看板維持：端點名稱、狀態碼徽章、耗時、時間戳與複製按鈕。
 * - 內容區呈現 `> request {N}` 與 `> response {N}` 互動式摺疊樹狀結構。
 * - 徹底移除「請求標頭 (Headers)」與散裝 SettingsSection。
 */
@Composable
fun ApiLogDetailScreen(
    rawJson: String,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    val isDark = isSystemInDarkTheme()

    val record = remember(rawJson) {
        try {
            detailJson.decodeFromString<ApiLogRecord>(rawJson)
        } catch (_: Exception) {
            ApiLogRecord(endpoint = "API 詳情", status = 0)
        }
    }

    val parsed = remember(rawJson) {
        runCatching { JSONObject(rawJson) }.getOrDefault(JSONObject())
    }

    val requestObj = remember(parsed, record) {
        when {
            parsed.has("request") && parsed.opt("request") != JSONObject.NULL -> {
                parsed.opt("request")
            }
            record.request != null -> {
                JSONObject(record.request as Map<*, *>)
            }
            record.requestBody.isNotBlank() -> {
                val body = record.requestBody.trim()
                if (body.startsWith("{")) {
                    runCatching { JSONObject(body) }.getOrDefault(body)
                } else {
                    val map = JSONObject()
                    body.split("&").forEach { pair ->
                        val parts = pair.split("=", limit = 2)
                        if (parts.size == 2) {
                            map.put(parts[0], java.net.URLDecoder.decode(parts[1], "UTF-8"))
                        } else if (parts.isNotEmpty() && parts[0].isNotBlank()) {
                            map.put("data", parts[0])
                        }
                    }
                    if (map.length() > 0) map else body
                }
            }
            else -> null
        }
    }

    val responseObj = remember(parsed, record) {
        when {
            parsed.has("response") && parsed.opt("response") != JSONObject.NULL -> {
                val resp = parsed.opt("response")
                if (resp is JSONObject && resp.has("payload")) {
                    val p = resp.opt("payload")
                    if (p is String && p.trim().startsWith("{")) {
                        runCatching {
                            val innerJson = JSONObject(p.trim())
                            val combined = JSONObject()
                            val it = resp.keys()
                            while (it.hasNext()) {
                                val k = it.next()
                                if (k == "payload") combined.put("payload", innerJson)
                                else combined.put(k, resp.get(k))
                            }
                            combined
                        }.getOrDefault(resp)
                    } else resp
                } else resp
            }
            record.response != null -> {
                JSONObject(record.response as Map<*, *>)
            }
            !record.decryptedPayload.isNullOrBlank() -> {
                val dec = record.decryptedPayload.trim()
                if (dec.startsWith("{")) {
                    runCatching { JSONObject(dec) }.getOrDefault(dec)
                } else dec
            }
            record.responseBody.isNotBlank() -> {
                val rb = record.responseBody.trim()
                if (rb.startsWith("{")) {
                    runCatching { JSONObject(rb) }.getOrDefault(rb)
                } else rb
            }
            else -> null
        }
    }

    val metaIconColor = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.54f)
    val metaTextColor = if (isDark) Color.White.copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.87f)

    ImmersiveScaffold(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // 頂部看板 (右滑返回，無上一頁按鈕)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = record.endpoint,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) Color.White else Color(0xFF1E293B)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        val (statusBg, statusFg) = if (record.isSuccess) {
                            Pair(
                                Color(0xFF10B981).copy(alpha = if (isDark) 0.20f else 0.14f),
                                if (isDark) Color(0xFF34D399) else Color(0xFF059669)
                            )
                        } else {
                            Pair(
                                Color(0xFFEF4444).copy(alpha = if (isDark) 0.22f else 0.14f),
                                Color(0xFFEF4444)
                            )
                        }

                        // HTTP 狀態碼徽章
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(statusBg)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${record.status}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = statusFg
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Timer,
                            contentDescription = null,
                            tint = metaIconColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${record.durationMs} ms",
                            fontSize = 12.sp,
                            color = metaTextColor
                        )

                        Spacer(modifier = Modifier.width(16.dp))

                        Icon(
                            imageVector = Icons.Rounded.CalendarToday,
                            contentDescription = null,
                            tint = metaIconColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = record.timestamp.replace("T", " "),
                            fontSize = 12.sp,
                            color = metaTextColor
                        )
                    }
                }

                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(rawJson))
                        NotificationController.showSuccess("已複製日誌 JSON")
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "複製完整日誌",
                        tint = if (isDark) Color.White.copy(alpha = 0.7f) else AppColors.primary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. 錯誤訊息橫幅 (若有)
                if (!record.error.isNullOrBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEF4444).copy(alpha = 0.12f))
                            .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "錯誤: ${record.error}",
                            fontSize = 13.sp,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // 2. Request 摺疊樹狀卡片 (1:1 對標 LensWise 圖三)
                if (requestObj != null) {
                    JsonTreeViewer(
                        data = requestObj,
                        rootName = "request",
                        initialExpanded = false,
                        isDark = isDark
                    )
                }

                // 3. Response 摺疊樹狀卡片 (1:1 對標 LensWise 圖三)
                if (responseObj != null) {
                    JsonTreeViewer(
                        data = responseObj,
                        rootName = "response",
                        initialExpanded = false,
                        isDark = isDark
                    )
                }
            }

            NavigationBarsSafeSpacer()
        }
    }
}
