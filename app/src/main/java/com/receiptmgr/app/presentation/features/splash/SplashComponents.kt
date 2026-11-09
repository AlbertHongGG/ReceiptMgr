package com.receiptmgr.app.presentation.features.splash

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.receiptmgr.app.R

/**
 * 啟動畫面中心標誌元件。
 */
@Composable
fun SplashLogo(
    scale: Float,
    alpha: Float,
    modifier: Modifier = Modifier,
    size: Dp = 140.dp,
    @DrawableRes logoRes: Int = R.drawable.app_logo
) {
    Image(
        painter = painterResource(id = logoRes),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(size)
            .scale(scale)
            .alpha(alpha)
    )
}

/**
 * 啟動畫面品牌文字排版與分割線展開元件。
 */
@Composable
fun SplashBrandText(
    title: String,
    subtitle: String,
    titleAlpha: Float,
    titleOffsetY: Dp,
    dividerWidth: Dp,
    dividerAlpha: Float,
    titleColor: Color,
    subtitleColor: Color,
    dividerColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clipToBounds()
            .alpha(titleAlpha)
            .offset { IntOffset(0, titleOffsetY.roundToPx()) }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .width(dividerWidth)
                    .height(1.dp)
                    .background(dividerColor.copy(alpha = dividerAlpha))
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Medium,
                color = titleColor,
                letterSpacing = 6.0.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = subtitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Normal,
                color = subtitleColor,
                letterSpacing = 3.0.sp
            )
        }
    }
}
