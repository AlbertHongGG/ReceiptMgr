package com.receiptmgr.app.presentation.theme

import android.app.Activity
import android.content.res.Configuration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = AppColors.primary,
    secondary = AppColors.secondary,
    background = AppColors.backgroundLight,
    surface = AppColors.surfaceLight,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = AppColors.textPrimaryLight,
    onSurface = AppColors.textPrimaryLight,
    error = AppColors.danger
)

@Composable
fun ReceiptMgrTheme(
    content: @Composable () -> Unit
) {
    val currentConfig = LocalConfiguration.current
    val lightConfig = Configuration(currentConfig).apply {
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or Configuration.UI_MODE_NIGHT_NO
    }

    CompositionLocalProvider(LocalConfiguration provides lightConfig) {
        val view = LocalView.current
        if (!view.isInEditMode) {
            SideEffect {
                val window = (view.context as? Activity)?.window ?: return@SideEffect
                WindowCompat.getInsetsController(window, view).apply {
                    isAppearanceLightStatusBars = true
                    isAppearanceLightNavigationBars = true
                }
            }
        }

        MaterialTheme(
            colorScheme = LightColorScheme,
            typography = Typography,
            content = content
        )
    }
}
