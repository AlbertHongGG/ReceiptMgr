package com.receiptmgr.app.presentation.features.splash

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 啟動動畫時間線與物理緩動規格 (Value Object)。
 * 封裝動畫長度、階梯式時間區間與幾何尺寸，杜絕程式碼中的 Magic Numbers。
 */
data class SplashMotionSpec(
    val durationMillis: Int = 2400,
    val postDelayMillis: Long = 150L,
    val logoSize: Dp = 140.dp,
    val maxSlideDistance: Dp = 24.dp,
    val maxDividerWidth: Dp = 120.dp,
    val dividerBaseAlpha: Float = 0.30f,
    val logoFadeRange: ClosedFloatingPointRange<Float> = 0.2f..0.6f,
    val logoScaleRange: ClosedFloatingPointRange<Float> = 0.2f..0.7f,
    val titleUnfoldRange: ClosedFloatingPointRange<Float> = 0.5f..0.9f,
    val titleFadeRange: ClosedFloatingPointRange<Float> = 0.6f..1.0f,
    val logoMinScale: Float = 0.9f,
    val logoMaxScale: Float = 1.0f,
    val easeOutCubic: Easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1.0f),
    val easeOut: Easing = FastOutSlowInEasing
) {
    companion object {
        val Default = SplashMotionSpec()
    }
}
