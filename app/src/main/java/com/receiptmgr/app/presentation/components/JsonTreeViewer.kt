package com.receiptmgr.app.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import org.json.JSONObject

@Composable
fun JsonTreeViewer(
    data: Any?,
    rootName: String? = null,
    modifier: Modifier = Modifier,
    isDark: Boolean = isSystemInDarkTheme()
) {
    val parsedData = remember(data) {
        if (data is String) {
            val trimmed = data.trim()
            try {
                if (trimmed.startsWith("{")) JSONObject(trimmed)
                else if (trimmed.startsWith("[")) JSONArray(trimmed)
                else data
            } catch (_: Exception) {
                data
            }
        } else {
            data
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        if (rootName != null) {
            if (isValidDisplayNode(parsedData)) {
                JsonNodeRenderer(
                    keyName = rootName,
                    data = parsedData,
                    isDark = isDark,
                    isRoot = true
                )
            }
        } else if (parsedData is JSONObject) {
            val keys = parsedData.keys().asSequence().filter { isValidDisplayNode(parsedData.opt(it)) }.toList()
            keys.forEach { key ->
                JsonNodeRenderer(
                    keyName = key,
                    data = parsedData.opt(key),
                    isDark = isDark,
                    isRoot = true
                )
            }
        } else if (parsedData is JSONArray) {
            for (i in 0 until parsedData.length()) {
                val item = parsedData.opt(i)
                if (isValidDisplayNode(item)) {
                    JsonNodeRenderer(
                        keyName = "[$i]",
                        data = item,
                        isDark = isDark,
                        isRoot = true
                    )
                }
            }
        } else if (isValidDisplayNode(parsedData)) {
            JsonNodeRenderer(
                keyName = "data",
                data = parsedData,
                isDark = isDark,
                isRoot = true
            )
        }
    }
}

private fun isValidDisplayNode(data: Any?): Boolean {
    return when (data) {
        null, JSONObject.NULL -> false
        is String -> data.isNotBlank()
        is JSONArray -> data.length() > 0
        is JSONObject -> {
            var hasValid = false
            val it = data.keys()
            while (it.hasNext()) {
                val k = it.next()
                if (isValidDisplayNode(data.opt(k))) {
                    hasValid = true
                    break
                }
            }
            hasValid
        }
        else -> true
    }
}

@Composable
private fun JsonNodeRenderer(
    keyName: String,
    data: Any?,
    isDark: Boolean,
    isRoot: Boolean = false
) {
    if (!isValidDisplayNode(data)) return

    var expanded by remember { mutableStateOf(isRoot) }

    when (data) {
        is JSONObject -> {
            val keys = remember(data) {
                data.keys().asSequence().filter { isValidDisplayNode(data.opt(it)) }.toList()
            }
            val count = keys.size
            if (count == 0 && !isRoot) return

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.02f))
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = if (expanded) "折疊" else "展開",
                            tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = keyName,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "{$count}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.54f)
                        )
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, bottom = 8.dp)
                        ) {
                            keys.forEach { childKey ->
                                JsonNodeRenderer(
                                    keyName = childKey,
                                    data = data.opt(childKey),
                                    isDark = isDark,
                                    isRoot = false
                                )
                            }
                        }
                    }
                }
            }
        }

        is JSONArray -> {
            val count = data.length()
            if (count == 0 && !isRoot) return

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.02f))
                    .border(
                        width = 1.dp,
                        color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(8.dp)
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.KeyboardArrowDown else Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = if (expanded) "折疊" else "展開",
                            tint = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = keyName,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = if (isDark) Color(0xFF93C5FD) else Color(0xFF1D4ED8)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "[$count]",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.54f)
                        )
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp, end = 8.dp, bottom = 8.dp)
                        ) {
                            for (i in 0 until count) {
                                JsonNodeRenderer(
                                    keyName = "[$i]",
                                    data = data.opt(i),
                                    isDark = isDark,
                                    isRoot = false
                                )
                            }
                        }
                    }
                }
            }
        }

        else -> {
            RenderPrimitiveRow(
                keyName = keyName,
                data = data,
                isDark = isDark
            )
        }
    }
}

@Composable
private fun RenderPrimitiveRow(
    keyName: String,
    data: Any?,
    isDark: Boolean
) {
    val keyColor = if (isDark) Color(0xFFFDBA74) else Color(0xFFC2410C)
    val isBlockKey = keyName.contains("prompt", ignoreCase = true) ||
                     keyName.contains("text", ignoreCase = true) ||
                     keyName.contains("content", ignoreCase = true) ||
                     keyName.contains("payload", ignoreCase = true)

    val isLongString = data is String && (
        isBlockKey ||
        data.contains("\n") ||
        data.length > 30
    )

    if (isLongString) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            Text(
                text = "$keyName:",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = keyColor
            )
            Spacer(modifier = Modifier.size(4.dp))
            PrimitiveValueText(data = data, isDark = isDark, isLong = true)
        }
    } else {
        Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp)
        ) {
            Text(
                text = "$keyName: ",
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = keyColor
            )
            PrimitiveValueText(data = data, isDark = isDark, isLong = false)
        }
    }
}

@Composable
private fun PrimitiveValueText(
    data: Any?,
    isDark: Boolean,
    isLong: Boolean
) {
    when (data) {
        null, JSONObject.NULL -> {
            Text(
                text = "null",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = if (isDark) Color(0xFF9CA3AF) else Color(0xFF6B7280)
            )
        }

        is Boolean -> {
            Text(
                text = data.toString(),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = if (isDark) Color(0xFFD8B4FE) else Color(0xFF7E22CE)
            )
        }

        is Number -> {
            Text(
                text = data.toString(),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = if (isDark) Color(0xFF86EFAC) else Color(0xFF15803D)
            )
        }

        is String -> {
            val str = data.toString()
            if (isLong) {
                var formatted = str
                try {
                    val trimmed = str.trim()
                    if (trimmed.startsWith("{")) {
                        formatted = JSONObject(trimmed).toString(2)
                    } else if (trimmed.startsWith("[")) {
                        formatted = JSONArray(trimmed).toString(2)
                    }
                } catch (_: Exception) {}

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isDark) Color(0xFF1E1E1E) else Color(0xFFF8FAFC))
                        .border(
                            width = 1.dp,
                            color = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(12.dp)
                ) {
                    SelectionContainer {
                        Text(
                            text = formatted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF1E40AF)
                        )
                    }
                }
            } else {
                SelectionContainer {
                    Text(
                        text = "\"$str\"",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        color = if (isDark) Color(0xFFA7F3D0) else Color(0xFF1E40AF)
                    )
                }
            }
        }

        else -> {
            Text(
                text = data.toString(),
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = if (isDark) Color.White.copy(alpha = 0.9f) else Color.Black.copy(alpha = 0.9f)
            )
        }
    }
}
