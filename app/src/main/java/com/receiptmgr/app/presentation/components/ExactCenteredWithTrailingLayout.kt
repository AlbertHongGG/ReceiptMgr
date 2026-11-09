package com.receiptmgr.app.presentation.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 精準幾何置中且帶有右側附隨元件之佈局 (ExactCenteredWithTrailingLayout)。
 *
 * 核心保證：
 * 1. 第一個子元件 (centerContent) 嚴格且絕對置中於容器水平幾何中線：
 *    centerX = (totalWidth - centerWidth) / 2
 * 2. 第二個子元件 (trailingContent) 緊隨於 centerContent 右側邊界：
 *    trailingX = centerX + centerWidth + spacing
 * 3. trailingContent 的存在或寬度絕不會將 centerContent 向左推擠或干擾其居中性。
 */
@Composable
fun ExactCenteredWithTrailingLayout(
    centerContent: @Composable () -> Unit,
    trailingContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    spacing: Dp = 6.dp
) {
    Layout(
        content = {
            centerContent()
            trailingContent()
        },
        modifier = modifier
    ) { measurables, constraints ->
        require(measurables.size >= 2) { "ExactCenteredWithTrailingLayout 需要正好兩個子元件" }

        val centerPlaceable = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val trailingPlaceable = measurables[1].measure(constraints.copy(minWidth = 0, minHeight = 0))

        val totalWidth = constraints.maxWidth
        val totalHeight = maxOf(centerPlaceable.height, trailingPlaceable.height)

        // 絕對幾何居中 (不受 trailing 元件影響)
        val centerX = (totalWidth - centerPlaceable.width) / 2
        val centerY = (totalHeight - centerPlaceable.height) / 2

        // 附隨元件放置於居中元件右側邊界
        val spacingPx = spacing.roundToPx()
        val trailingX = centerX + centerPlaceable.width + spacingPx
        val trailingY = (totalHeight - trailingPlaceable.height) / 2

        layout(totalWidth, totalHeight) {
            centerPlaceable.placeRelative(centerX, centerY)
            trailingPlaceable.placeRelative(trailingX, trailingY)
        }
    }
}
