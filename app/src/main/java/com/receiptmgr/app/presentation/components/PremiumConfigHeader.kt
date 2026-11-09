package com.receiptmgr.app.presentation.components

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.layout.layout
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.receiptmgr.app.presentation.theme.AppColors

@Composable
fun PremiumConfigHeader(
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    showOrbWatermark: Boolean = true,
    actions: (@Composable () -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 24.dp, bottom = 12.dp)
    ) {
        // Orb 水印 (僅在非主畫面時選染，亦可依參數關閉)
        if (showOrbWatermark) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(
                            constraints.copy(minWidth = 0, minHeight = 0)
                        )
                        layout(0, 0) {
                            placeable.placeRelative(
                                -placeable.width + 16.dp.roundToPx(),
                                -placeable.height / 2
                            )
                        }
                    }
            ) {
                ThinkingOrb(
                    modifier = Modifier
                        .width(160.dp)
                        .height(160.dp)
                        .alpha(if (isDark) 0.16f else 0.08f),
                    size = 160.dp,
                    isDark = isDark
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = title,
                    style = TextStyle(
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) AppColors.textPrimaryDark else AppColors.textPrimaryLight,
                        platformStyle = PlatformTextStyle(
                            includeFontPadding = false
                        ),
                        lineHeightStyle = LineHeightStyle(
                            alignment = LineHeightStyle.Alignment.Proportional,
                            trim = LineHeightStyle.Trim.Both
                        )
                    ),
                    letterSpacing = (-0.5).sp
                )

                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = subtitle.uppercase(),
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (isDark) AppColors.textSecondaryDark else AppColors.textSecondaryLight,
                            platformStyle = PlatformTextStyle(
                                includeFontPadding = false
                            ),
                            lineHeightStyle = LineHeightStyle(
                                alignment = LineHeightStyle.Alignment.Proportional,
                                trim = LineHeightStyle.Trim.Both
                            )
                        ),
                        letterSpacing = 1.0.sp
                    )
                }
            }

            if (actions != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    actions()
                }
            }
        }
    }
}
