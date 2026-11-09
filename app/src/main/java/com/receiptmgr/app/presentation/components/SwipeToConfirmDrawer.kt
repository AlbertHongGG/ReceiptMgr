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
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToConfirmDrawer(
    title: String,
    description: String? = null,
    confirmText: String? = null,
    sliderTitle: String = confirmText ?: "SLIDE TO CONFIRM",
    confirmedTitle: String = "RELEASE",
    activeColor: Color = Color(0xFFEF4444),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    SwipeToConfirmDrawerInternal(
        title = title,
        description = description,
        sliderTitle = sliderTitle,
        confirmedTitle = confirmedTitle,
        activeColor = activeColor,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        sheetState = sheetState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeToConfirmDrawerInternal(
    title: String,
    description: String? = null,
    sliderTitle: String = "SLIDE TO CONFIRM",
    confirmedTitle: String = "RELEASE",
    activeColor: Color = Color(0xFFEF4444),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    sheetState: SheetState
) {
    val isDark = isSystemInDarkTheme()
    val sheetBackgroundColor = if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
    val handleColor = if (isDark) Color.White.copy(alpha = 0.24f) else Color.Black.copy(alpha = 0.12f)
    val titleColor = if (isDark) Color.White.copy(alpha = 0.54f) else Color.Black.copy(alpha = 0.54f)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        containerColor = sheetBackgroundColor,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp)
                .padding(top = 16.dp, bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(handleColor)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = title.uppercase(),
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
                color = titleColor
            )

            if (!description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = if (isDark) Color.White.copy(alpha = 0.7f) else Color.Black.copy(alpha = 0.65f),
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            SwipeToConfirmButton(
                onConfirmed = onConfirm,
                title = sliderTitle,
                confirmedTitle = confirmedTitle,
                activeColor = activeColor
            )
        }
    }
}
