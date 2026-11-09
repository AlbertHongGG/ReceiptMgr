package com.receiptmgr.app.presentation.components.trash

import android.util.Log
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import kotlin.math.hypot
import kotlin.math.roundToInt

private const val TAG = "DragDropTrash"

class DragDropTrashState<T> {
    var isDragging by mutableStateOf(false)
    var isDropping by mutableStateOf(false)
    var dropScale by mutableFloatStateOf(1f)
    var dropAlpha by mutableFloatStateOf(0.85f)
    var activeItem by mutableStateOf<T?>(null)
    var dragPositionInWindow by mutableStateOf(Offset.Zero)
    var touchOffsetInItem by mutableStateOf(Offset.Zero)
    var itemSize by mutableStateOf(Offset.Zero)
    var isHoveringTrash by mutableStateOf(false)
    var trashBoundsInWindow by mutableStateOf(Rect.Zero)
    var containerBoundsInWindow by mutableStateOf(Rect.Zero)
    var feedbackContent by mutableStateOf<(@Composable () -> Unit)?>(null)
    var onDropAction: ((T) -> Unit)? = null

    suspend fun animateDropToTrash() {
        isDropping = true
        val anim = androidx.compose.animation.core.Animatable(1f)
        anim.animateTo(
            targetValue = 0f,
            animationSpec = androidx.compose.animation.core.tween(
                durationMillis = 140,
                easing = androidx.compose.animation.core.FastOutSlowInEasing
            )
        ) {
            dropScale = value
            dropAlpha = value * 0.85f
        }
        clearDrag()
    }

    fun clearDrag() {
        isDragging = false
        isDropping = false
        activeItem = null
        isHoveringTrash = false
        feedbackContent = null
        dropScale = 1f
        dropAlpha = 0.85f
    }
}

@Composable
fun <T> rememberDragDropTrashState(): DragDropTrashState<T> {
    return remember { DragDropTrashState() }
}

@Composable
fun <T> DragDropTrashContainer(
    state: DragDropTrashState<T>,
    onDropOnTrash: (T) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    state.onDropAction = onDropOnTrash
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { coordinates ->
                state.containerBoundsInWindow = coordinates.boundsInWindow()
            }
    ) {
        content()

        if (state.isDragging && state.feedbackContent != null) {
            val relativeX = state.dragPositionInWindow.x - state.touchOffsetInItem.x - state.containerBoundsInWindow.left
            val relativeY = state.dragPositionInWindow.y - state.touchOffsetInItem.y - state.containerBoundsInWindow.top

            val widthDp = with(density) { state.itemSize.x.toDp() }
            val heightDp = with(density) { state.itemSize.y.toDp() }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .zIndex(999f)
            ) {
                val sizeModifier = if (widthDp > 10.dp && heightDp > 10.dp) {
                    Modifier.requiredSize(width = widthDp, height = heightDp)
                } else {
                    Modifier.fillMaxWidth()
                }

                Box(
                    modifier = Modifier
                        .offset { IntOffset(relativeX.roundToInt(), relativeY.roundToInt()) }
                        .then(sizeModifier)
                        .graphicsLayer {
                            alpha = state.dropAlpha
                            shadowElevation = 24.dp.toPx() * state.dropScale
                            scaleX = 1.02f * state.dropScale
                            scaleY = 1.02f * state.dropScale
                            rotationZ = -1.5f * state.dropScale
                        }
                ) {
                    state.feedbackContent?.invoke()
                }
            }
        }
    }
}

@Composable
fun <T> Modifier.draggableToTrash(
    item: T,
    state: DragDropTrashState<T>,
    feedback: @Composable () -> Unit
): Modifier {
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    var itemBoundsInWindow by remember(item) { mutableStateOf(Rect.Zero) }

    return this
        .onGloballyPositioned { coordinates ->
            itemBoundsInWindow = coordinates.boundsInWindow()
        }
        .pointerInput(item) {
            detectDragGesturesAfterLongPress(
                onDragStart = { localOffset ->
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    state.dropScale = 1f
                    state.dropAlpha = 0.85f
                    state.isDropping = false
                    state.isDragging = true
                    state.activeItem = item
                    state.touchOffsetInItem = localOffset
                    state.itemSize = Offset(itemBoundsInWindow.width, itemBoundsInWindow.height)
                    state.dragPositionInWindow = itemBoundsInWindow.topLeft + localOffset
                    state.feedbackContent = feedback
                },
                onDrag = { change, dragAmount ->
                    change.consume()
                    state.dragPositionInWindow += dragAmount

                    val trashBounds = state.trashBoundsInWindow
                    val isHovering = if (trashBounds != Rect.Zero) {
                        val expandedTrash = Rect(
                            left = trashBounds.left - 160f,
                            top = trashBounds.top - 160f,
                            right = trashBounds.right + 160f,
                            bottom = trashBounds.bottom + 160f
                        )
                        val cardTopLeft = state.dragPositionInWindow - state.touchOffsetInItem
                        val cardRight = cardTopLeft.x + state.itemSize.x
                        val cardBottom = cardTopLeft.y + state.itemSize.y

                        val fingerInTrash = expandedTrash.contains(state.dragPositionInWindow)
                        val cardOverlapsTrash = !(cardRight < expandedTrash.left ||
                                cardTopLeft.x > expandedTrash.right ||
                                cardBottom < expandedTrash.top ||
                                cardTopLeft.y > expandedTrash.bottom)
                        val trashCenter = trashBounds.center
                        val dist = hypot(
                            (state.dragPositionInWindow.x - trashCenter.x).toDouble(),
                            (state.dragPositionInWindow.y - trashCenter.y).toDouble()
                        ).toFloat()

                        fingerInTrash || cardOverlapsTrash || (dist < 360f)
                    } else false

                    if (isHovering != state.isHoveringTrash) {
                        state.isHoveringTrash = isHovering
                        if (isHovering) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    }
                },
                onDragEnd = {
                    val wasHovering = state.isHoveringTrash
                    val droppedItem = state.activeItem ?: item
                    if (wasHovering && droppedItem != null) {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        state.onDropAction?.invoke(droppedItem)
                        coroutineScope.launch {
                            state.animateDropToTrash()
                        }
                    } else {
                        state.clearDrag()
                    }
                },
                onDragCancel = {
                    state.clearDrag()
                }
            )
        }
}
