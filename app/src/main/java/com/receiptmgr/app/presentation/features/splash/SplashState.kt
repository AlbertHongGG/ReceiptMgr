package com.receiptmgr.app.presentation.features.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * 啟動動畫單一畫格的純狀態模型 (Pure Animation State Model)。
 * 完全解耦數學內插邏輯與 UI 元件繪製。
 */
@Stable
data class SplashState(
    val progress: Float,
    val logoAlpha: Float,
    val logoScale: Float,
    val titleAlpha: Float,
    val titleOffsetY: Dp,
    val dividerWidth: Dp,
    val dividerAlpha: Float
) {
    companion object {
        fun initial(spec: SplashMotionSpec = SplashMotionSpec.Default): SplashState =
            calculate(progress = 0f, spec = spec)

        fun completed(spec: SplashMotionSpec = SplashMotionSpec.Default): SplashState =
            calculate(progress = 1f, spec = spec)

        /**
         * 純函式計算指定進度 t (0f..1f) 下的動畫狀態。
         */
        fun calculate(progress: Float, spec: SplashMotionSpec = SplashMotionSpec.Default): SplashState {
            val t = progress.coerceIn(0f, 1f)

            // Logo Fade (0.2f ~ 0.6f)
            val logoAlpha = when {
                t < spec.logoFadeRange.start -> 0f
                t > spec.logoFadeRange.endInclusive -> 1f
                else -> {
                    val normalized = (t - spec.logoFadeRange.start) /
                        (spec.logoFadeRange.endInclusive - spec.logoFadeRange.start)
                    spec.easeOut.transform(normalized)
                }
            }

            // Logo Scale (0.2f ~ 0.7f, 0.9f -> 1.0f)
            val logoScale = when {
                t < spec.logoScaleRange.start -> spec.logoMinScale
                t > spec.logoScaleRange.endInclusive -> spec.logoMaxScale
                else -> {
                    val normalized = (t - spec.logoScaleRange.start) /
                        (spec.logoScaleRange.endInclusive - spec.logoScaleRange.start)
                    val delta = spec.logoMaxScale - spec.logoMinScale
                    spec.logoMinScale + (delta * spec.easeOutCubic.transform(normalized))
                }
            }

            // Title Unfold (0.5f ~ 0.9f)
            val titleUnfold = when {
                t < spec.titleUnfoldRange.start -> 0f
                t > spec.titleUnfoldRange.endInclusive -> 1f
                else -> {
                    val normalized = (t - spec.titleUnfoldRange.start) /
                        (spec.titleUnfoldRange.endInclusive - spec.titleUnfoldRange.start)
                    spec.easeOutCubic.transform(normalized)
                }
            }

            // Title Fade (0.6f ~ 1.0f)
            val titleAlpha = when {
                t < spec.titleFadeRange.start -> 0f
                t >= spec.titleFadeRange.endInclusive -> 1f
                else -> {
                    val normalized = (t - spec.titleFadeRange.start) /
                        (spec.titleFadeRange.endInclusive - spec.titleFadeRange.start)
                    spec.easeOut.transform(normalized)
                }
            }

            // Slide offset from -maxSlideDistance to 0dp
            val titleOffsetY = if (titleUnfold >= 1f) 0.dp else (-spec.maxSlideDistance.value * (1f - titleUnfold)).dp
            val dividerWidth = spec.maxDividerWidth * titleAlpha
            val dividerAlpha = spec.dividerBaseAlpha * titleAlpha

            return SplashState(
                progress = t,
                logoAlpha = logoAlpha,
                logoScale = logoScale,
                titleAlpha = titleAlpha,
                titleOffsetY = titleOffsetY,
                dividerWidth = dividerWidth,
                dividerAlpha = dividerAlpha
            )
        }
    }
}

/**
 * 建立並驅動啟動動畫狀態的 Composable 狀態持有者。
 */
@Composable
fun rememberSplashAnimationState(
    spec: SplashMotionSpec = SplashMotionSpec.Default,
    onAnimationFinished: () -> Unit
): SplashState {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(spec) {
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = spec.durationMillis, easing = LinearEasing)
        )
        if (spec.postDelayMillis > 0) {
            delay(spec.postDelayMillis)
        }
        onAnimationFinished()
    }

    return remember(progress.value, spec) {
        SplashState.calculate(progress = progress.value, spec = spec)
    }
}
