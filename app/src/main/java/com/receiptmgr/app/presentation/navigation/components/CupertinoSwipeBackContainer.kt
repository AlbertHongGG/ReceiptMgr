package com.receiptmgr.app.presentation.navigation.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.unit.dp
import com.receiptmgr.app.presentation.navigation.contract.CupertinoNavigator
import com.receiptmgr.app.presentation.navigation.contract.LocalCupertinoNavigator
import com.receiptmgr.app.presentation.navigation.model.AppRoute
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

fun Modifier.touchShield(enabled: Boolean): Modifier = if (!enabled) this else {
    this.pointerInput(Unit) {
        awaitPointerEventScope {
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Initial)
                event.changes.forEach { it.consume() }
            }
        }
    }
}

@Composable
fun CupertinoSwipeBackContainer(
    backStack: List<AppRoute>,
    onPushRoute: (AppRoute) -> Unit,
    onPopRoute: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (route: AppRoute) -> Unit
) {
    val saveableStateHolder = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    val state = rememberCupertinoSwipeBackState(coroutineScope = scope)

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val screenWidthPx = constraints.maxWidth.toFloat()
        state.screenWidthPx = screenWidthPx

        val navigator = remember(state, backStack.size) {
            object : CupertinoNavigator {
                override fun push(route: AppRoute) {
                    scope.launch {
                        state.push(route) { onPushRoute(it) }
                    }
                }
                override fun pop() {
                    scope.launch {
                        state.pop(backStack.size) {
                            val popped = backStack.lastOrNull()
                            onPopRoute()
                            if (popped != null) {
                                saveableStateHolder.removeState(popped.key)
                            }
                        }
                    }
                }
            }
        }

        BackHandler(enabled = backStack.size > 1 && !state.isTransitioning) {
            scope.launch {
                state.pop(backStack.size) {
                    val popped = backStack.lastOrNull()
                    onPopRoute()
                    if (popped != null) {
                        saveableStateHolder.removeState(popped.key)
                    }
                }
            }
        }

        val underlyingRoute: AppRoute? = when {
            state.pushTarget != null -> backStack.lastOrNull()
            backStack.size > 1 -> backStack[backStack.size - 2]
            else -> null
        }

        val topRoute: AppRoute = when {
            state.pushTarget != null -> state.pushTarget!!
            else -> backStack.lastOrNull() ?: AppRoute.Home
        }

        val topTranslationX = state.currentOffset
        val progress = state.progress
        val underlyingTranslationX = -screenWidthPx * 0.33f * (1f - progress)
        val scrimAlpha = 0.25f * (1f - progress)

        CompositionLocalProvider(LocalCupertinoNavigator provides navigator) {
            if (underlyingRoute != null && (state.isTransitioning || backStack.size > 1)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            translationX = underlyingTranslationX
                        }
                        .touchShield(enabled = backStack.size > 1 || state.isTransitioning)
                ) {
                    saveableStateHolder.SaveableStateProvider(key = underlyingRoute.key) {
                        content(underlyingRoute)
                    }

                    if (scrimAlpha > 0.001f) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = scrimAlpha))
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        translationX = topTranslationX
                    }
            ) {
                if (topTranslationX > 0f) {
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .fillMaxHeight()
                            .align(Alignment.CenterStart)
                            .offset(x = (-16).dp)
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.22f)
                                    )
                                )
                            )
                    )
                }

                saveableStateHolder.SaveableStateProvider(key = topRoute.key) {
                    content(topRoute)
                }
            }

            if (backStack.size > 1 || state.isTransitioning) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(36.dp)
                        .align(Alignment.CenterStart)
                        .pointerInput(state, backStack.size) {
                            val velocityTracker = VelocityTracker()
                            val edgeThresholdPx = 36.dp.toPx()
                            val touchSlop = viewConfiguration.touchSlop

                            coroutineScope {
                                while (isActive) {
                                    awaitPointerEventScope {
                                        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Initial)
                                        if (down.position.x > edgeThresholdPx || state.isTransitioning || backStack.size <= 1) {
                                            return@awaitPointerEventScope
                                        }

                                        val startX = down.position.x
                                        val startY = down.position.y
                                        velocityTracker.resetTracking()
                                        velocityTracker.addPosition(down.uptimeMillis, down.position)

                                        var isGestureClaimed = false

                                        try {
                                            while (true) {
                                                val event = awaitPointerEvent(PointerEventPass.Initial)
                                                val change = event.changes.firstOrNull { it.id == down.id } ?: break

                                                if (!change.pressed) {
                                                    if (isGestureClaimed) {
                                                        change.consume()
                                                        val velocityX = velocityTracker.calculateVelocity().x
                                                        launch {
                                                            state.onDragEnd(velocityX) {
                                                                val popped = backStack.lastOrNull()
                                                                onPopRoute()
                                                                if (popped != null) {
                                                                    saveableStateHolder.removeState(popped.key)
                                                                }
                                                            }
                                                        }
                                                    }
                                                    break
                                                }

                                                velocityTracker.addPosition(change.uptimeMillis, change.position)
                                                val totalDeltaX = change.position.x - startX
                                                val totalDeltaY = change.position.y - startY

                                                if (!isGestureClaimed) {
                                                    if (totalDeltaX > touchSlop && totalDeltaX > kotlin.math.abs(totalDeltaY) * 1.1f) {
                                                        isGestureClaimed = state.onDragStart(backStack.size)
                                                        if (isGestureClaimed) {
                                                            change.consume()
                                                            state.setDragOffset(totalDeltaX - touchSlop)
                                                        }
                                                    } else if (kotlin.math.abs(totalDeltaY) > touchSlop) {
                                                        break
                                                    }
                                                } else {
                                                    change.consume()
                                                    state.setDragOffset(totalDeltaX - touchSlop)
                                                }
                                            }
                                        } finally {
                                            if (isGestureClaimed && state.motionState == CupertinoMotionState.DRAGGING) {
                                                launch {
                                                    state.onDragCancel()
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                )
            }
        }
    }
}
