package com.receiptmgr.app.presentation.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.receiptmgr.app.presentation.theme.AppColors

@Composable
fun SettingsSection(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        if (!title.isNullOrBlank()) {
            Text(
                text = title,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) Color.White.copy(alpha = 0.54f) else Color(0xFF64748B),
                modifier = Modifier.padding(start = 16.dp, bottom = 8.dp, top = 22.dp)
            )
        }

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    spotColor = if (isDark) Color.Black.copy(alpha = 0.40f) else Color.Black.copy(alpha = 0.14f),
                    ambientColor = if (isDark) Color.Black.copy(alpha = 0.25f) else Color.Black.copy(alpha = 0.06f)
                ),
            shape = RoundedCornerShape(16.dp),
            color = if (isDark) AppColors.surfaceDark else AppColors.surfaceLight,
            border = BorderStroke(
                width = 1.dp,
                color = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.04f)
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                content()
            }
        }
    }
}

@Composable
fun SettingsDivider() {
    val isDark = isSystemInDarkTheme()
    HorizontalDivider(
        modifier = Modifier.padding(start = 56.dp, end = 16.dp),
        thickness = 1.dp,
        color = if (isDark) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.06f)
    )
}
