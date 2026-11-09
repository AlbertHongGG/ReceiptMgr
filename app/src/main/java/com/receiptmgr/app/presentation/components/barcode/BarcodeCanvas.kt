package com.receiptmgr.app.presentation.components.barcode

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.receiptmgr.app.domain.barcode.code39.Code39Encoder
import com.receiptmgr.app.domain.barcode.model.BarcodePattern

/**
 * 原生向量條碼繪製畫布。
 * 零 Bitmap 記憶體配置，純向量幾何計算，在任何螢幕解析度下皆維持極致銳利，確保條碼掃描槍 100% 辨識。
 */
@Composable
fun BarcodeCanvas(
    content: String,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Black,
    backgroundColor: Color = Color.White,
    encoder: Code39Encoder = Code39Encoder.Default
) {
    val pattern = remember(content, encoder) {
        if (encoder.isValid(content)) {
            encoder.encode(content)
        } else {
            null
        }
    }

    if (pattern != null) {
        BarcodePatternCanvas(
            pattern = pattern,
            barColor = barColor,
            backgroundColor = backgroundColor,
            modifier = modifier
        )
    }
}

/**
 * 依據 BarcodePattern 模型繪製條碼條紋的無狀態組件。
 */
@Composable
fun BarcodePatternCanvas(
    pattern: BarcodePattern,
    modifier: Modifier = Modifier,
    barColor: Color = Color.Black,
    backgroundColor: Color = Color.White
) {
    Box(
        modifier = modifier.background(backgroundColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            val totalUnits = pattern.totalUnits
            if (totalUnits <= 0) return@Canvas

            val unitWidthPx = canvasWidth / totalUnits
            var currentXPx = pattern.quietZoneUnits * unitWidthPx

            for (element in pattern.elements) {
                val elementWidthPx = element.unitWidth * unitWidthPx
                if (element.isBar) {
                    drawRect(
                        color = barColor,
                        topLeft = Offset(currentXPx, 0f),
                        size = Size(elementWidthPx, canvasHeight)
                    )
                }
                currentXPx += elementWidthPx
            }
        }
    }
}

@Preview(name = "Carrier Barcode (/8J722KT)", showBackground = true)
@Composable
private fun BarcodeCanvasPreview() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        BarcodeCanvas(
            content = "/8J722KT",
            modifier = Modifier
                .fillMaxWidth()
                .height(72.dp)
        )
    }
}
