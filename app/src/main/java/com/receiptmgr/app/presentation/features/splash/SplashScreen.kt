package com.receiptmgr.app.presentation.features.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import com.receiptmgr.app.presentation.theme.AppColors
import com.receiptmgr.app.presentation.theme.ReceiptMgrTheme

/**
 * 啟動畫面專用調色盤，支援深淺色模式無縫適配。
 */
data class SplashColors(
    val background: Color,
    val title: Color,
    val subtitle: Color,
    val divider: Color
)

@Composable
fun defaultSplashColors(): SplashColors {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        SplashColors(
            background = AppColors.backgroundDark,
            title = AppColors.textPrimaryDark,
            subtitle = AppColors.textSecondaryDark,
            divider = AppColors.textPrimaryDark
        )
    } else {
        SplashColors(
            background = Color.White,
            title = Color(0xFF1D1D1F),
            subtitle = Color(0xFF86868B),
            divider = Color(0xFF1D1D1F)
        )
    }
}

/**
 * 原生啟動動畫頂層介面 (Stateful)。
 * 承接動畫時鐘驅動，並將狀態分發至純展示組件。
 */
@Composable
fun SplashScreen(
    onAnimationFinished: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "RECEIPT MGR",
    subtitle: String = "EXPENSE & INVOICE TRACKING",
    spec: SplashMotionSpec = SplashMotionSpec.Default,
    colors: SplashColors = defaultSplashColors()
) {
    val state = rememberSplashAnimationState(
        spec = spec,
        onAnimationFinished = onAnimationFinished
    )

    SplashContent(
        state = state,
        title = title,
        subtitle = subtitle,
        spec = spec,
        colors = colors,
        modifier = modifier
    )
}

/**
 * 純展示啟動畫面內容 (Stateless)。
 * 方便 Compose Preview 預覽特定關鍵影格與進行視覺測試。
 */
@Composable
fun SplashContent(
    state: SplashState,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    spec: SplashMotionSpec = SplashMotionSpec.Default,
    colors: SplashColors = defaultSplashColors()
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            SplashLogo(
                scale = state.logoScale,
                alpha = state.logoAlpha,
                size = spec.logoSize
            )

            SplashBrandText(
                title = title,
                subtitle = subtitle,
                titleAlpha = state.titleAlpha,
                titleOffsetY = state.titleOffsetY,
                dividerWidth = state.dividerWidth,
                dividerAlpha = state.dividerAlpha,
                titleColor = colors.title,
                subtitleColor = colors.subtitle,
                dividerColor = colors.divider
            )
        }
    }
}

@Preview(name = "Splash - Start (0%)", showBackground = true)
@Composable
private fun SplashScreenPreviewStart() {
    ReceiptMgrTheme(darkTheme = false) {
        SplashContent(
            state = SplashState.calculate(0.0f),
            title = "RECEIPT MGR",
            subtitle = "EXPENSE & INVOICE TRACKING"
        )
    }
}

@Preview(name = "Splash - Mid (50%)", showBackground = true)
@Composable
private fun SplashScreenPreviewMid() {
    ReceiptMgrTheme(darkTheme = false) {
        SplashContent(
            state = SplashState.calculate(0.5f),
            title = "RECEIPT MGR",
            subtitle = "EXPENSE & INVOICE TRACKING"
        )
    }
}

@Preview(name = "Splash - Complete (100%)", showBackground = true)
@Composable
private fun SplashScreenPreviewComplete() {
    ReceiptMgrTheme(darkTheme = false) {
        SplashContent(
            state = SplashState.calculate(1.0f),
            title = "RECEIPT MGR",
            subtitle = "EXPENSE & INVOICE TRACKING"
        )
    }
}

@Preview(name = "Splash - Dark Mode (100%)", showBackground = true)
@Composable
private fun SplashScreenPreviewDark() {
    ReceiptMgrTheme(darkTheme = true) {
        SplashContent(
            state = SplashState.calculate(1.0f),
            title = "RECEIPT MGR",
            subtitle = "EXPENSE & INVOICE TRACKING"
        )
    }
}
