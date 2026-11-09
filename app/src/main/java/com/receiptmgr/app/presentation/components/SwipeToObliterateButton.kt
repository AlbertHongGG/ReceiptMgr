package com.receiptmgr.app.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * 滑動抹除確認按鈕 (SwipeToObliterateButton)。
 * 1:1 復刻 LensWise 規範：
 * - 尺寸規格：維持 68dp 軌道高度、56dp 圓形滑塊、6dp 安全內距。
 * - 動態文字反饋：拖曳進度達 80% 時，中央文字即時由「SLIDE TO WIPE」切換為「RELEASE TO CLEAR」。
 * - 刪除武裝門檻：拖曳進度達 85% 刪除閥值時，滑塊底色由純白翻轉為鮮紅、圖標切換為純白色垃圾桶 (DeleteForever)，並激發觸覺反饋。
 */
@Composable
fun SwipeToObliterateButton(
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "SLIDE TO WIPE",
    confirmedTitle: String = "RELEASE TO CLEAR",
    isLoading: Boolean = false,
    activeColor: Color = Color(0xFFEF4444)
) {
    val isDark = isSystemInDarkTheme()
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()

    val trackHeight = 68.dp
    val thumbSize = 56.dp
    val thumbPadding = 6.dp
    val trackShape = RoundedCornerShape(trackHeight / 2)

    val trackColor = if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA)
    val textColor = if (isDark) Color.White.copy(alpha = 0.50f) else Color.Black.copy(alpha = 0.40f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
    ) {
        val maxDragPx = with(density) {
            (maxWidth - thumbSize - (thumbPadding * 2)).toPx().coerceAtLeast(0f)
        }

        val dragOffsetX = remember { Animatable(0f) }

        val progress by remember(maxDragPx) {
            derivedStateOf {
                if (maxDragPx > 0f) (dragOffsetX.value / maxDragPx).coerceIn(0f, 1f) else 0f
            }
        }

        val isReleaseText = progress > 0.80f
        val isArmed = progress > 0.85f

        var wasArmed by remember { mutableStateOf(false) }
        LaunchedEffect(isArmed) {
            if (isArmed && !wasArmed) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
            wasArmed = isArmed
        }

        // 外軌道
        Box(
            modifier = Modifier
                .fillMaxSize()
                .shadow(
                    elevation = if (isArmed) 12.dp else 6.dp,
                    shape = trackShape,
                    ambientColor = if (isArmed) activeColor.copy(alpha = 0.35f) else Color.Black.copy(alpha = if (isDark) 0.30f else 0.08f),
                    spotColor = if (isArmed) activeColor.copy(alpha = 0.55f) else Color.Black.copy(alpha = if (isDark) 0.45f else 0.16f)
                )
                .clip(trackShape)
                .background(trackColor)
                .border(
                    width = 1.dp,
                    color = if (isArmed) activeColor.copy(alpha = 0.35f) else if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f),
                    shape = trackShape
                )
        ) {
            // 鮮紅背景揭示條
            if (dragOffsetX.value > 0f) {
                val revealWidthDp = with(density) {
                    (thumbSize.toPx() + (thumbPadding * 2).toPx() + dragOffsetX.value).toDp()
                }
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(revealWidthDp)
                        .clip(trackShape)
                        .background(activeColor.copy(alpha = 0.35f + (progress * 0.65f)))
                )
            }

            // 置中提示文字
            Text(
                text = if (isReleaseText) confirmedTitle.uppercase() else title.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp,
                color = if (isArmed) Color.White.copy(alpha = 0.90f) else textColor,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        val currentMaxDragPx by rememberUpdatedState(maxDragPx)

        // 滑塊本體 (Thumb)
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (thumbPadding.toPx() + dragOffsetX.value).roundToInt(),
                        y = thumbPadding.toPx().roundToInt()
                    )
                }
                .size(thumbSize)
                .shadow(
                    elevation = if (isArmed) 10.dp else 6.dp,
                    shape = CircleShape,
                    ambientColor = if (isArmed) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = if (isDark) 0.35f else 0.15f),
                    spotColor = if (isArmed) Color.Black.copy(alpha = 0.50f) else Color.Black.copy(alpha = if (isDark) 0.45f else 0.20f)
                )
                .clip(CircleShape)
                .background(if (isArmed) activeColor else Color.White)
                .border(
                    width = if (isArmed) 1.5.dp else 1.dp,
                    color = if (isArmed) Color.White.copy(alpha = 0.45f) else if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.06f),
                    shape = CircleShape
                )
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = !isLoading,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val newX = (dragOffsetX.value + delta).coerceIn(0f, currentMaxDragPx)
                            dragOffsetX.snapTo(newX)
                        }
                    },
                    onDragStarted = {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    },
                    onDragStopped = {
                        scope.launch {
                            val currentProgress = if (currentMaxDragPx > 0f) {
                                (dragOffsetX.value / currentMaxDragPx).coerceIn(0f, 1f)
                            } else 0f
                            if (currentProgress >= 0.85f) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                dragOffsetX.animateTo(currentMaxDragPx, spring())
                                onConfirmed()
                                dragOffsetX.animateTo(0f, spring())
                            } else {
                                dragOffsetX.animateTo(0f, spring())
                            }
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    strokeWidth = 2.5.dp,
                    color = if (isArmed) Color.White else activeColor,
                    modifier = Modifier.size(22.dp)
                )
            } else if (isArmed) {
                Icon(
                    imageVector = Icons.Filled.DeleteForever,
                    contentDescription = "釋放以確認刪除",
                    tint = Color.White,
                    modifier = Modifier.size(26.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                    contentDescription = "滑動以確認",
                    tint = activeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
