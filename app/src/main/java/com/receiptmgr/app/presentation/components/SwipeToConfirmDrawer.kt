package com.receiptmgr.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 沉浸式下拉刪除抽屜容器 (SwipeToConfirmDrawer)。
 * 1:1 復刻 LensWise 規範：
 * 採用 24dp 頂部大圓角，頂部 40x4dp 指示條，置中 13sp Bold 標題，
 * 內嵌具備「雙段文字反饋」與「垃圾桶圖標轉場」的 [SwipeToObliterateButton]。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToConfirmDrawer(
    title: String,
    modifier: Modifier = Modifier,
    description: String? = null,
    sliderTitle: String = "SLIDE TO WIPE",
    confirmedTitle: String = "RELEASE TO CLEAR",
    activeColor: Color = Color(0xFFEF4444),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isSystemInDarkTheme()
    val sheetBackgroundColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val handleColor = if (isDark) Color.White.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.12f)
    val titleColor = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.54f)
    val descColor = if (isDark) Color.White.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.45f)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = sheetBackgroundColor,
        dragHandle = null,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 12.dp, bottom = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(handleColor)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = title.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp,
                color = titleColor
            )

            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = descColor,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            SwipeToObliterateButton(
                title = sliderTitle,
                confirmedTitle = confirmedTitle,
                activeColor = activeColor,
                onConfirmed = onConfirm
            )
        }
    }
}
