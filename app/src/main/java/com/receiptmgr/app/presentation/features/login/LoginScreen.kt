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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.receiptmgr.app.presentation.components.ImmersiveScaffold
import com.receiptmgr.app.presentation.components.JsonTreeViewer
import com.receiptmgr.app.presentation.components.NavigationBarsSafeSpacer
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
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()

    var showPassword by remember { mutableStateOf(false) }
    var showAdvancedParams by remember { mutableStateOf(false) }

    val bgCard = if (isDark) Color(0xFF161922) else Color.White
    val borderColor = if (isDark) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.06f)
    val textPrimary = if (isDark) Color(0xFFE2E8F0) else Color(0xFF0F172A)
    val textSecondary = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    ImmersiveScaffold {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            PremiumConfigHeader(
                title = "帳號授權",
                subtitle = "UIA 協定登入與憑證簽發",
                navigationIcon = {
                    IconButton(onClick = { navigator.pop() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回",
                            tint = textPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetDefaults(context) }) {
                        Icon(
                            imageVector = Icons.Outlined.RestartAlt,
                            contentDescription = "重設",
                            tint = textSecondary
                        )
                    }
                }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. 核心憑證輸入卡片
                SettingsSection(title = "登入憑證") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.phone,
                            onValueChange = { viewModel.onPhoneChange(it) },
                            label = { Text("手機號碼 / 載具", fontSize = 13.sp) },
                            placeholder = { Text("09xxxxxxxx", fontSize = 13.sp) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Smartphone,
                                    contentDescription = null,
                                    tint = AppColors.AccentTeal,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (uiState.phone.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.onPhoneChange("") }) {
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
                                focusedBorderColor = AppColors.AccentTeal,
                                unfocusedBorderColor = borderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = { viewModel.onPasswordChange(it) },
                            label = { Text("發票驗證碼 (密碼)", fontSize = 13.sp) },
                            singleLine = true,
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Lock,
                                    contentDescription = null,
                                    tint = AppColors.AccentIndigo,
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
                                focusedBorderColor = AppColors.AccentIndigo,
                                unfocusedBorderColor = borderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 2. 終端硬體指紋參數卡片 (全部 9 個項目)
                SettingsSection(
                    title = "終端參數",
                    action = {
                        IconButton(onClick = { showAdvancedParams = !showAdvancedParams }) {
                            Icon(
                                imageVector = Icons.Outlined.Tune,
                                contentDescription = "展開參數",
                                tint = if (showAdvancedParams) AppColors.AccentTeal else textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 設備 UUID (自動抓取 Android ID)
                        OutlinedTextField(
                            value = uiState.uuid,
                            onValueChange = { viewModel.onUuidChange(it) },
                            label = { Text("設備唯一識別碼 (UUID)", fontSize = 12.sp) },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Outlined.Fingerprint,
                                    contentDescription = null,
                                    tint = AppColors.AccentAmber,
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(uiState.uuid))
                                    NotificationController.showSuccess("已複製 UUID")
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
                                focusedBorderColor = AppColors.AccentAmber,
                                unfocusedBorderColor = borderColor
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        AnimatedVisibility(visible = showAdvancedParams) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                OutlinedTextField(
                                    value = uiState.appId,
                                    onValueChange = { viewModel.onAppIdChange(it) },
                                    label = { Text("客戶端代碼 (app_id)", fontSize = 12.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = AppColors.AccentTeal,
                                        unfocusedBorderColor = borderColor
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

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
                                            focusedBorderColor = AppColors.AccentTeal,
                                            unfocusedBorderColor = borderColor
                                        )
                                    )

                                    OutlinedTextField(
                                        value = uiState.osVersion,
                                        onValueChange = { viewModel.onOsVersionChange(it) },
                                        label = { Text("系統版號", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AppColors.AccentTeal,
                                            unfocusedBorderColor = borderColor
                                        )
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = uiState.appVersion,
                                        onValueChange = { viewModel.onAppVersionChange(it) },
                                        label = { Text("App 版本", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AppColors.AccentTeal,
                                            unfocusedBorderColor = borderColor
                                        )
                                    )

                                    OutlinedTextField(
                                        value = uiState.loginMod,
                                        onValueChange = { viewModel.onLoginModChange(it) },
                                        label = { Text("模式 (login_mod)", fontSize = 12.sp) },
                                        singleLine = true,
                                        modifier = Modifier.weight(1f),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = AppColors.AccentTeal,
                                            unfocusedBorderColor = borderColor
                                        )
                                    )
                                }
                            }
                        }
                    }
                }

                // 3. 登入操作按鈕 (極簡 Icon + Focus)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(AppColors.AccentTeal)
                        .clickable(enabled = !uiState.isLoading) {
                            viewModel.executeLogin()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.Black,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Outlined.Login,
                                contentDescription = "登入",
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "驗證登入",
                                color = Color.Black,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // 4. 回應資訊檢視區 (完整 Response Payload 檢視器)
                if (uiState.responseSession != null) {
                    val session = uiState.responseSession!!
                    SettingsSection(
                        title = "會話憑證 (Response Payload)",
                        action = {
                            IconButton(onClick = {
                                val toCopy = when (uiState.responseTab) {
                                    0 -> "ss_me: ${session.ssMe}\ntoken: ${session.token}\nsid: ${session.sid}\nliat: ${session.liat}"
                                    1 -> session.rawPayloadJson
                                    else -> session.rawServerResponseJson
                                }
                                clipboardManager.setText(AnnotatedString(toCopy))
                                NotificationController.showSuccess("已複製到剪貼簿")
                            }) {
                                Icon(
                                    imageVector = Icons.Outlined.ContentCopy,
                                    contentDescription = "複製",
                                    tint = AppColors.AccentTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp)
                        ) {
                            // 分頁選擇器
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("關鍵權杖", "解密 JSON", "原始回應").forEachIndexed { idx, tabTitle ->
                                    val isSelected = uiState.responseTab == idx
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (isSelected) AppColors.AccentTeal.copy(alpha = 0.16f)
                                                else Color.Transparent
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) AppColors.AccentTeal else borderColor,
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .clickable { viewModel.onSelectResponseTab(idx) }
                                            .padding(vertical = 8.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = tabTitle,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) AppColors.AccentTeal else textSecondary
                                        )
                                    }
                                }
                            }

                            when (uiState.responseTab) {
                                0 -> {
                                    // 關鍵權杖條目
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        CredentialItem(
                                            label = "ss_me (發票查詢簽名金鑰)",
                                            value = session.ssMe,
                                            highlight = true,
                                            onCopy = {
                                                clipboardManager.setText(AnnotatedString(session.ssMe))
                                                NotificationController.showSuccess("已複製 ss_me")
                                            }
                                        )
                                        CredentialItem(
                                            label = "token (UIA 權杖)",
                                            value = session.token,
                                            onCopy = {
                                                clipboardManager.setText(AnnotatedString(session.token))
                                                NotificationController.showSuccess("已複製 token")
                                            }
                                        )
                                        CredentialItem(
                                            label = "sid (會話識別碼)",
                                            value = session.sid,
                                            onCopy = {
                                                clipboardManager.setText(AnnotatedString(session.sid))
                                                NotificationController.showSuccess("已複製 sid")
                                            }
                                        )
                                        CredentialItem(
                                            label = "liat (登入時間戳)",
                                            value = "${session.liat} (有效剩餘: ${session.remainingDays} 天)",
                                            onCopy = {
                                                clipboardManager.setText(AnnotatedString(session.liat.toString()))
                                                NotificationController.showSuccess("已複製 liat")
                                            }
                                        )
                                        CredentialItem(
                                            label = "svr_code",
                                            value = session.svrCode
                                        )
                                        CredentialItem(
                                            label = "iv",
                                            value = session.iv
                                        )
                                        CredentialItem(
                                            label = "app_id",
                                            value = session.appId
                                        )
                                    }
                                }
                                1 -> {
                                    JsonTreeViewer(
                                        data = session.rawPayloadJson,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                2 -> {
                                    JsonTreeViewer(
                                        data = session.rawServerResponseJson,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                NavigationBarsSafeSpacer()
            }
        }
    }
}

@Composable
private fun CredentialItem(
    label: String,
    value: String,
    highlight: Boolean = false,
    onCopy: (() -> Unit)? = null
) {
    val isDark = isSystemInDarkTheme()
    val shape = RoundedCornerShape(8.dp)
    val cardBg = if (highlight) {
        if (isDark) AppColors.AccentTeal.copy(alpha = 0.12f) else AppColors.AccentTeal.copy(alpha = 0.08f)
    } else {
        if (isDark) Color.White.copy(alpha = 0.03f) else Color.Black.copy(alpha = 0.02f)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(cardBg)
            .border(
                width = 1.dp,
                color = if (highlight) AppColors.AccentTeal.copy(alpha = 0.35f) else Color.Transparent,
                shape = shape
            )
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (highlight) AppColors.AccentTeal else if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value.ifEmpty { "—" },
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = if (highlight) FontWeight.Bold else FontWeight.Normal,
                    color = if (isDark) Color.White else Color.Black
                )
            }

            if (onCopy != null) {
                IconButton(
                    onClick = onCopy,
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "複製",
                        modifier = Modifier.size(14.dp),
                        tint = if (highlight) AppColors.AccentTeal else Color.Gray
                    )
                }
            }
        }
    }
}
