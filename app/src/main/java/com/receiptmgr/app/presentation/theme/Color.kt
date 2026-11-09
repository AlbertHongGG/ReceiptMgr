package com.receiptmgr.app.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * 依據 LensWise 原生標準色彩定義之冷灰無色系調色盤 (Muted Slate & Deep Neutral)。
 */
object AppColors {
    // 背景層
    val backgroundLight = Color(0xFFF5F7FA) // 柔和灰白
    val backgroundDark = Color(0xFF0F111A)  // 深邃夜藍黑

    // 卡片層
    val surfaceLight = Color(0xFFFFFFFF)    // 純白
    val surfaceDark = Color(0xFF1E1E1E)     // 曜石深灰

    // 沉穩冷石板強調色
    val primary = Color(0xFF334155)         // Slate 700 (高質感冷深石板灰)
    val secondary = Color(0xFF64748B)       // Slate 500 (中性灰)
    val slateMuted = Color(0xFF475569)      // Slate 600

    // 文字
    val textPrimaryLight = Color(0xFF0F172A)
    val textSecondaryLight = Color(0xFF64748B)
    val textPrimaryDark = Color(0xFFFFFFFF)
    val textSecondaryDark = Color(0xFF94A3B8)

    // 邊界與分割線
    val borderLight = Color(0x0F000000)     // 黑 6% 極細邊界
    val borderDark = Color(0x1FFFFFFF)      // 白 12% 極細邊界
    val divider = Color(0x0D000000)         // 淺色分割線
    val dividerDark = Color(0x14FFFFFF)     // 深色分割線

    // 警示與危險
    val danger = Color(0xFFEF4444)
    val success = Color(0xFF10B981)
}
