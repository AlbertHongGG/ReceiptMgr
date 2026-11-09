package com.receiptmgr.app

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.receiptmgr.app.presentation.features.home.HomeScreen
import com.receiptmgr.app.presentation.features.home.HomeViewModel
import com.receiptmgr.app.presentation.features.login.LoginScreen
import com.receiptmgr.app.presentation.features.login.LoginViewModel
import com.receiptmgr.app.presentation.features.log.ApiLogDetailScreen
import com.receiptmgr.app.presentation.features.log.ApiLogViewerScreen
import com.receiptmgr.app.presentation.features.log.SystemNotificationLogScreen
import com.receiptmgr.app.presentation.features.settings.SettingsScreen
import com.receiptmgr.app.presentation.features.settings.SettingsViewModel
import com.receiptmgr.app.presentation.navigation.components.CupertinoSwipeBackContainer
import com.receiptmgr.app.presentation.navigation.model.AppRoute
import com.receiptmgr.app.presentation.notification.GlobalNotificationOverlay
import com.receiptmgr.app.presentation.theme.ReceiptMgrTheme
import com.receiptmgr.app.presentation.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    private val requestNotificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { _ -> }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        checkAndRequestNotificationPermission()

        val appContainer = (application as ReceiptMgrApplication).appContainer

        setContent {
            val darkModePref by appContainer.settingsRepository.darkModeFlow.collectAsState()
            val isDark = darkModePref ?: isSystemInDarkTheme()

            ReceiptMgrTheme(darkTheme = isDark) {
                GlobalNotificationOverlay {
                    Surface(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val navState by mainViewModel.uiState.collectAsState()

                        val homeViewModel = remember {
                            HomeViewModel(appContainer.authRepository)
                        }
                        val loginViewModel = remember {
                            LoginViewModel(appContainer.authRepository, applicationContext)
                        }
                        val settingsViewModel = remember {
                            SettingsViewModel(
                                appContainer.authRepository,
                                appContainer.settingsRepository,
                                applicationContext
                            )
                        }

                        CupertinoSwipeBackContainer(
                            backStack = navState.backStack,
                            onPushRoute = { route -> mainViewModel.pushRoute(route) },
                            onPopRoute = { mainViewModel.popRoute() }
                        ) { route ->
                            when (route) {
                                is AppRoute.Home -> {
                                    HomeScreen(viewModel = homeViewModel)
                                }
                                is AppRoute.Login -> {
                                    LoginScreen(viewModel = loginViewModel)
                                }
                                is AppRoute.Settings -> {
                                    SettingsScreen(viewModel = settingsViewModel)
                                }
                                is AppRoute.ApiLogs -> {
                                    ApiLogViewerScreen(apiLogManager = appContainer.apiLogManager)
                                }
                                is AppRoute.ApiLogDetail -> {
                                    ApiLogDetailScreen(rawJson = route.rawJson)
                                }
                                is AppRoute.NotificationLogs -> {
                                    SystemNotificationLogScreen(notificationLogManager = appContainer.notificationLogManager)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                requestNotificationPermissionLauncher.launch(permission)
            }
        }
    }
}
