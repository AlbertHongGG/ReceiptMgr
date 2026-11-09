package com.receiptmgr.app.presentation.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun GlobalNotificationOverlay(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val notifications by NotificationController.instance.notifications.collectAsState()
    val easeOutQuart = remember { CubicBezierEasing(0.25f, 1.0f, 0.5f, 1.0f) }

    Box(modifier = modifier.fillMaxSize()) {
        content()

        if (notifications.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(top = 8.dp)
                    .align(Alignment.TopCenter)
            ) {
                notifications.forEach { notification ->
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(
                            initialOffsetY = { -it },
                            animationSpec = tween(durationMillis = 300, easing = easeOutQuart)
                        ) + fadeIn(animationSpec = tween(300, easing = easeOutQuart)),
                        exit = slideOutVertically(
                            targetOffsetY = { -it },
                            animationSpec = tween(durationMillis = 250, easing = easeOutQuart)
                        ) + fadeOut(animationSpec = tween(250))
                    ) {
                        NotificationToast(
                            notification = notification,
                            onDismiss = {
                                NotificationController.instance.remove(notification.id)
                            }
                        )
                    }
                }
            }
        }
    }
}
