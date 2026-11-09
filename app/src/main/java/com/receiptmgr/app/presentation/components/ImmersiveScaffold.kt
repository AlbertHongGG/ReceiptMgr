package com.receiptmgr.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.receiptmgr.app.presentation.theme.AppColors

@Composable
fun ImmersiveScaffold(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val bgColor = if (isDark) AppColors.backgroundDark else AppColors.backgroundLight

    // 外層滿屏填色，100% 穿透狀態列與導航列，杜絕黑邊
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(bgColor)
    ) {
        // 內層視口安全內縮，保證內容安全避開狀態列
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            content = content
        )
    }
}

@Composable
fun NavigationBarsSafeSpacer(
    extraHeight: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    Spacer(
        modifier = modifier
            .navigationBarsPadding()
            .height(extraHeight)
    )
}
