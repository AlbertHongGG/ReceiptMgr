package com.receiptmgr.app.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun SwipeToConfirmButton(
    title: String = "SLIDE TO CONFIRM",
    confirmedTitle: String = "RELEASE",
    activeColor: Color = Color(0xFFEF4444),
    onConfirmed: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isSystemInDarkTheme()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    val trackHeight = 56.dp
    val thumbSize = 48.dp
    val thumbPadding = 4.dp

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(RoundedCornerShape(trackHeight / 2))
            .background(if (isDark) Color(0xFF2C2C2E) else Color(0xFFE5E5EA))
    ) {
        val maxDragPx = with(density) {
            (maxWidth - thumbSize - (thumbPadding * 2)).toPx()
        }

        val dragOffsetX = remember { Animatable(0f) }
        val progress by remember {
            derivedStateOf {
                if (maxDragPx > 0f) (dragOffsetX.value / maxDragPx).coerceIn(0f, 1f) else 0f
            }
        }

        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(with(density) { (thumbSize + (thumbPadding * 2) + dragOffsetX.value.toDp()) })
                .clip(RoundedCornerShape(trackHeight / 2))
                .background(activeColor.copy(alpha = 0.15f + (progress * 0.85f)))
        )

        Text(
            text = if (progress > 0.8f) confirmedTitle else title,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.5.sp,
            color = if (isDark) Color.White.copy(alpha = 0.5f) else Color.Black.copy(alpha = 0.4f),
            modifier = Modifier.align(Alignment.Center)
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(x = (thumbPadding.toPx() + dragOffsetX.value).roundToInt(), y = thumbPadding.toPx().roundToInt()) }
                .size(thumbSize)
                .shadow(elevation = 6.dp, shape = CircleShape)
                .clip(CircleShape)
                .background(if (progress > 0.85f) activeColor else Color.White)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { delta ->
                        scope.launch {
                            val newX = (dragOffsetX.value + delta).coerceIn(0f, maxDragPx)
                            dragOffsetX.snapTo(newX)
                        }
                    },
                    onDragStopped = {
                        scope.launch {
                            if (progress >= 0.85f) {
                                dragOffsetX.animateTo(maxDragPx, spring())
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
            Icon(
                imageVector = if (progress > 0.85f) Icons.Default.DeleteForever else Icons.Default.ChevronRight,
                contentDescription = null,
                tint = if (progress > 0.85f) Color.White else activeColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
