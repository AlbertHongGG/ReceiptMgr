package com.receiptmgr.app.presentation.navigation.contract

import androidx.compose.runtime.staticCompositionLocalOf

val LocalCupertinoNavigator = staticCompositionLocalOf<CupertinoNavigator> {
    error("No CupertinoNavigator provided in composition tree")
}
