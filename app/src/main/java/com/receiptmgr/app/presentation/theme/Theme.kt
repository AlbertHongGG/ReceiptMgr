package com.receiptmgr.app.presentation.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = AppColors.accentBlue,
    secondary = AppColors.secondary,
    background = AppColors.backgroundDark,
    surface = AppColors.surfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = AppColors.textPrimaryDark,
    onSurface = AppColors.textPrimaryDark,
    error = AppColors.danger
)

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
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
