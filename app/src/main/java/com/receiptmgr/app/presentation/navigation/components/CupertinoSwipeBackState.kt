package com.receiptmgr.app.presentation.navigation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.receiptmgr.app.presentation.navigation.model.AppRoute
import kotlinx.coroutines.CoroutineScope
import kotlin.math.roundToInt

enum class CupertinoMotionState {
    IDLE,
    PUSHING,
    DRAGGING,
    SETTLING,
    POPPING
}

@Stable
class CupertinoSwipeBackState(
    val coroutineScope: CoroutineScope
) {
    var motionState by mutableStateOf(CupertinoMotionState.IDLE)
        private set

    var screenWidthPx by mutableFloatStateOf(0f)

    var currentOffset by mutableFloatStateOf(0f)
        private set

    var pushTarget by mutableStateOf<AppRoute?>(null)
        private set

    val cupertinoEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)

    val isTransitioning: Boolean
        get() = motionState != CupertinoMotionState.IDLE

    val progress: Float
        get() = if (screenWidthPx > 0f) (currentOffset / screenWidthPx).coerceIn(0f, 1f) else 0f

    suspend fun push(target: AppRoute, onCommit: (AppRoute) -> Unit) {
        if (isTransitioning || screenWidthPx <= 0f) return

        motionState = CupertinoMotionState.PUSHING
        pushTarget = target
        currentOffset = screenWidthPx

        val anim = Animatable(screenWidthPx)
        anim.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 320, easing = cupertinoEasing)
        ) {
            currentOffset = value
        }

        onCommit(target)
        pushTarget = null
        currentOffset = 0f
        motionState = CupertinoMotionState.IDLE
    }

    suspend fun pop(backStackSize: Int, onCommit: () -> Unit) {
        if (isTransitioning || backStackSize <= 1 || screenWidthPx <= 0f) return

        motionState = CupertinoMotionState.POPPING
        currentOffset = 0f

        val anim = Animatable(0f)
        anim.animateTo(
            targetValue = screenWidthPx,
            animationSpec = tween(durationMillis = 280, easing = cupertinoEasing)
        ) {
            currentOffset = value
        }

        onCommit()
        currentOffset = 0f
        motionState = CupertinoMotionState.IDLE
    }

    fun onDragStart(backStackSize: Int): Boolean {
        if (motionState != CupertinoMotionState.IDLE || backStackSize <= 1 || screenWidthPx <= 0f) {
            return false
        }
        motionState = CupertinoMotionState.DRAGGING
        return true
    }

    fun setDragOffset(dragDistance: Float) {
        if (motionState != CupertinoMotionState.DRAGGING) return
        currentOffset = dragDistance.coerceIn(0f, screenWidthPx)
    }

    suspend fun onDragEnd(velocityX: Float, onCommitPop: () -> Unit) {
        if (motionState != CupertinoMotionState.DRAGGING) return

        motionState = CupertinoMotionState.SETTLING
        val ratio = if (screenWidthPx > 0f) currentOffset / screenWidthPx else 0f
        val shouldPop = ratio > 0.35f || velocityX > 800f

        if (shouldPop) {
            val remainingDistance = screenWidthPx - currentOffset
            val remainingRatio = if (screenWidthPx > 0f) remainingDistance / screenWidthPx else 0f
            val duration = (remainingRatio * 260).roundToInt().coerceIn(100, 260)

            val anim = Animatable(currentOffset)
            anim.animateTo(
                targetValue = screenWidthPx,
                animationSpec = tween(durationMillis = duration, easing = FastOutSlowInEasing)
            ) {
                currentOffset = value
            }

            onCommitPop()
            currentOffset = 0f
            motionState = CupertinoMotionState.IDLE
        } else {
            val anim = Animatable(currentOffset)
            anim.animateTo(
                targetValue = 0f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) {
                currentOffset = value
            }
            currentOffset = 0f
            motionState = CupertinoMotionState.IDLE
        }
    }

    suspend fun onDragCancel() {
        if (motionState != CupertinoMotionState.DRAGGING) return

        motionState = CupertinoMotionState.SETTLING
        val anim = Animatable(currentOffset)
        anim.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        ) {
            currentOffset = value
        }
        currentOffset = 0f
        motionState = CupertinoMotionState.IDLE
    }
}

@Composable
fun rememberCupertinoSwipeBackState(
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): CupertinoSwipeBackState {
    return remember(coroutineScope) {
        CupertinoSwipeBackState(coroutineScope)
    }
}
