package com.receiptmgr.app.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.receiptmgr.app.presentation.theme.AppColors

@Composable
fun AppSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    activeColor: Color = AppColors.primary,
    inactiveColor: Color? = null,
    width: Dp = 50.dp,
    height: Dp = 28.dp,
    padding: Dp = 2.dp
) {
    val isDark = isSystemInDarkTheme()
    val haptic = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val defaultInactiveColor = if (isDark) Color(0x3DFFFFFF) else Color(0x1F000000)
    val targetTrackColor = if (checked) activeColor else (inactiveColor ?: defaultInactiveColor)

    val trackColor by animateColorAsState(
        targetValue = targetTrackColor,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "trackColor"
    )

    val thumbSize = height - (padding * 2)
    val targetThumbWidth = if (isPressed && enabled) thumbSize * 1.25f else thumbSize

    val animatedThumbWidth by animateDpAsState(
        targetValue = targetThumbWidth,
        animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        label = "thumbWidth"
    )

    val maxOffset = width - (padding * 2) - animatedThumbWidth
    val targetOffset = if (checked) maxOffset else 0.dp

    val thumbOffset by animateDpAsState(
        targetValue = targetOffset,
        animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        label = "thumbOffset"
    )

    Box(
        modifier = modifier
            .size(width = width, height = height)
            .clip(RoundedCornerShape(height / 2))
            .background(trackColor)
            .semantics { role = Role.Switch }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled && onCheckedChange != null
            ) {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange?.invoke(!checked)
            }
            .padding(padding),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .width(animatedThumbWidth)
                .height(thumbSize)
                .shadow(
                    elevation = 3.dp,
                    shape = RoundedCornerShape(thumbSize / 2),
                    clip = false
                )
                .background(
                    color = Color.White,
                    shape = RoundedCornerShape(thumbSize / 2)
                )
        )
    }
}
