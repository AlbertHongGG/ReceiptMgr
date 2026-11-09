package com.receiptmgr.app.presentation.features.login

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.receiptmgr.app.presentation.components.ImmersiveScaffold
import com.receiptmgr.app.presentation.components.PremiumConfigHeader
import com.receiptmgr.app.presentation.components.SettingsSection
import com.receiptmgr.app.presentation.navigation.contract.LocalCupertinoNavigator
import com.receiptmgr.app.presentation.notification.NotificationController
import com.receiptmgr.app.presentation.theme.AppColors

@Composable
fun LoginScreen(
    viewModel: LoginViewModel
) {
    val navigator = LocalCupertinoNavigator.current
    val clipboardManager = LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()

    var showPassword by remember { mutableStateOf(false) }
    var showAdvancedParams by remember { mutableStateOf(false) }

    val borderColor = if (isDark) Color.White.copy(alpha = 0.12f) else Color.Black.copy(alpha = 0.08f)
    val textSecondary = if (isDark) AppColors.textSecondaryDark else AppColors.textSecondaryLight

    ImmersiveScaffold {
        Box(modifier = Modifier.fillMaxSize()) {
            // 可滾動之表單內容 (無多餘外層水平 padding，與其他頁面 SettingsSection 完全齊平)
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                // 頂部大標題（右滑手勢退棧，無上一頁按鈕）
                PremiumConfigHeader(
                    title = "帳號授權",
                    subtitle = "UIA PROTOCOL AUTHENTICATION",
                    actions = {
                        IconButton(onClick = { viewModel.resetDefaults() }) {
                            Icon(
                                imageVector = Icons.Outlined.RestartAlt,
                                contentDescription = "重設參數",
                                tint = textSecondary
                            )
                        }
                    }
                )

                // 1. 核心憑證輸入卡片
                SettingsSection(title = "登入憑證") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.account,
                            onValueChange = { viewModel.onAccountChange(it) },
                            label = { Text("手機號碼", fontSize = 13.sp) },
                            placeholder = { Text("09xxxxxxxx", fontSize = 13.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Smartphone,
                                    contentDescription = null,
                                    tint = AppColors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (uiState.account.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onAccountChange("") }) {
                                        Icon(
                                            imageVector = Icons.Outlined.RestartAlt,
                                            contentDescription = "清除",
                                            modifier = Modifier.size(16.dp),
                                            tint = textSecondary
                                        )
                                    }
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppColors.primary,
                                unfocusedBorderColor = borderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = { viewModel.onPasswordChange(it) },
                            label = { Text("密碼", fontSize = 13.sp) },
                            placeholder = { Text("請輸入密碼", fontSize = 13.sp) },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = AppColors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showPassword = !showPassword }) {
                                    Icon(
                                        imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                        contentDescription = "切換密碼顯示",
                                        tint = textSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppColors.primary,
                                unfocusedBorderColor = borderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. 終端硬體指紋與協定參數卡片
                SettingsSection(
                    title = "終端協定參數"
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 設備唯一識別碼
                        OutlinedTextField(
                            value = uiState.pdid,
                            onValueChange = { },
                            readOnly = true,
                            label = { Text("設備唯一識別碼", fontSize = 12.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Fingerprint,
                                    contentDescription = null,
                                    tint = AppColors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(uiState.pdid))
                                    NotificationController.showSuccess("已複製識別碼")
                                }) {
                                    Icon(
                                        imageVector = Icons.Outlined.ContentCopy,
                                        contentDescription = "複製",
                                        modifier = Modifier.size(18.dp),
                                        tint = textSecondary
                                    )
                                }
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = AppColors.primary,
                                unfocusedBorderColor = borderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        // 展開/收合進階協定參數切換列
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showAdvancedParams = !showAdvancedParams }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = null,
                                tint = AppColors.secondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (showAdvancedParams) "收合進階協定參數" else "展開進階協定參數",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = AppColors.secondary
                            )
                        }

                        AnimatedVisibility(visible = showAdvancedParams) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = uiState.appver,
                                        onValueChange = { viewModel.onAppverChange(it) },
                                        label = { Text("應用程式版本", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AppColors.primary,
                                            unfocusedBorderColor = borderColor
                                        )
                                    )

                                    OutlinedTextField(
                                        value = uiState.appbn.toString(),
                                        onValueChange = { viewModel.onAppbnChange(it) },
                                        label = { Text("發布版本號", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AppColors.primary,
                                            unfocusedBorderColor = borderColor
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = uiState.deviceModel,
                                        onValueChange = { viewModel.onDeviceModelChange(it) },
                                        label = { Text("設備型號", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AppColors.primary,
                                            unfocusedBorderColor = borderColor
                                        )
                                    )

                                    OutlinedTextField(
                                        value = uiState.osVersion,
                                        onValueChange = { viewModel.onOsVersionChange(it) },
                                        label = { Text("系統版本", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AppColors.primary,
                                            unfocusedBorderColor = borderColor
                                        )
                                    )
                                }

                                OutlinedTextField(
                                    value = uiState.androidId,
                                    onValueChange = { viewModel.onAndroidIdChange(it) },
                                    label = { Text("底層識別碼", fontSize = 12.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppColors.primary,
                                        unfocusedBorderColor = borderColor
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                OutlinedTextField(
                                    value = uiState.ptoken,
                                    onValueChange = { viewModel.onPtokenChange(it) },
                                    label = { Text("推播權杖", fontSize = 12.sp) },
                                    trailingIcon = {
                                        IconButton(onClick = {
                                            clipboardManager.setText(AnnotatedString(uiState.ptoken))
                                            NotificationController.showSuccess("已複製推播權杖")
                                        }) {
                                            Icon(
                                                imageVector = Icons.Outlined.ContentCopy,
                                                contentDescription = "複製",
                                                modifier = Modifier.size(18.dp),
                                                tint = textSecondary
                                            )
                                        }
                                    },
                                    maxLines = 2,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppColors.primary,
                                        unfocusedBorderColor = borderColor
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // 底部留白，保證滑至最底時，「推播權杖」卡片高於懸浮按鈕 24.dp 以上，絕對零遮擋
                Spacer(modifier = Modifier.height(140.dp))
            }

            // 正中下方懸浮膠囊操作鈕 (Centered Floating Action Pill)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .shadow(
                            elevation = 12.dp,
                            shape = RoundedCornerShape(26.dp),
                            spotColor = if (isDark) Color.Black.copy(alpha = 0.55f) else Color.Black.copy(alpha = 0.22f),
                            ambientColor = if (isDark) Color.Black.copy(alpha = 0.35f) else Color.Black.copy(alpha = 0.10f)
                        )
                        .clip(RoundedCornerShape(26.dp))
                        .background(if (isDark) Color.White else AppColors.textPrimaryLight)
                        .clickable(enabled = !uiState.isLoading) {
                            viewModel.executeLogin {
                                navigator.pop()
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = if (isDark) Color.Black else Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Login,
                                contentDescription = "登入",
                                tint = if (isDark) Color.Black else Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "驗證登入",
                                color = if (isDark) Color.Black else Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
