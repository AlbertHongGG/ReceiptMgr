package com.receiptmgr.app.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.receiptmgr.app.presentation.theme.AppColors

@Composable
fun PremiumEmptyState(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    description: String? = subtitle,
    icon: ImageVector = Icons.Outlined.Inbox,
    actionText: String? = null,
    onAction: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()

    Column(
        modifier = modifier
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isDark) Color.White.copy(alpha = 0.2f) else Color.Black.copy(alpha = 0.15f),
            modifier = Modifier.size(56.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDark) Color.White.copy(alpha = 0.85f) else Color(0xFF1E293B)
        )

        val desc = description ?: subtitle
        if (!desc.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = desc,
                fontSize = 13.sp,
                color = if (isDark) Color.White.copy(alpha = 0.45f) else Color(0xFF64748B)
            )
        }

        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isDark) Color.White else AppColors.textPrimaryLight)
                    .clickable(onClick = onAction)
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(
                    text = actionText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) Color.Black else Color.White
                )
            }
        }
    }
}
