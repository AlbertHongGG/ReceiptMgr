package com.receiptmgr.app.presentation.components.trash

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild

/**
 * 沉浸式毛玻璃懸浮垃圾桶按鈕 (GlassmorphicTrashControl)。
 * 1:1 復刻 LensWise 規範：
 * - 平時狀態：低調沉浸毛玻璃磨砂（深色 85% 灰黑 / 淺色 85% 白），細緻中性灰邊框，灰白細線刪除圖標。
 * - 懸停狀態：彈簧流暢擴大至 64dp，亮起鮮紅漸層與紅色光暈，圖標切換為純白 DeleteForever。
 */
@Composable
fun GlassmorphicTrashControl(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    isHovering: Boolean = false,
    bottomSafetyPadding: Dp = 28.dp
) {
    val isDark = isSystemInDarkTheme()

    val size by animateDpAsState(
        targetValue = if (isHovering) 64.dp else 56.dp,
        animationSpec = spring(dampingRatio = 0.75f, stiffness = 400f),
        label = "trashSize"
    )

    val borderBrush = if (isHovering) {
        Brush.verticalGradient(
            listOf(Color(0xFFFCA5A5), Color(0xFFB91C1C))
        )
    } else {
        SolidColor(
            if (isDark) Color.White.copy(alpha = 0.15f)
            else Color.Black.copy(alpha = 0.08f)
        )
    }

    Box(
        modifier = modifier
            .navigationBarsPadding()
            .padding(bottom = bottomSafetyPadding)
            .size(size)
            .shadow(
                elevation = if (isHovering) 20.dp else 8.dp,
                shape = CircleShape,
                ambientColor = if (isHovering) Color(0xFFEF4444).copy(alpha = 0.50f) else Color.Black.copy(alpha = 0.25f),
                spotColor = if (isHovering) Color(0xFFEF4444).copy(alpha = 0.70f) else Color.Black.copy(alpha = 0.25f)
            )
            .clip(CircleShape)
            .then(
                if (isHovering) {
                    Modifier.background(
                        Brush.verticalGradient(
                            listOf(Color(0xFFEF4444), Color(0xFFDC2626))
                        )
                    )
                } else if (hazeState != null) {
                    Modifier.hazeChild(state = hazeState) {
                        blurRadius = 24.dp
                        noiseFactor = 0f
                        backgroundColor = if (isDark) Color(0xFF1E1E1E).copy(alpha = 0.85f) else Color.White.copy(alpha = 0.85f)
                    }
                } else {
                    Modifier.background(
                        if (isDark) Color(0xFF1E1E1E) else Color.White
                    )
                }
            )
            .border(
                width = if (isHovering) 1.5.dp else 1.dp,
                brush = borderBrush,
                shape = CircleShape
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true, radius = 28.dp),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isHovering) Icons.Filled.DeleteForever else Icons.Outlined.DeleteOutline,
            contentDescription = "清空紀錄",
            tint = if (isHovering) Color.White else (if (isDark) Color.White.copy(alpha = 0.75f) else Color.Black.copy(alpha = 0.70f)),
            modifier = Modifier.size(24.dp)
        )
    }
}
