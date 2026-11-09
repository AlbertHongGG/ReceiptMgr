package com.receiptmgr.app.presentation.navigation.contract

import com.receiptmgr.app.presentation.navigation.model.AppRoute

interface CupertinoNavigator {
    fun push(route: AppRoute)
    fun pop()
}
