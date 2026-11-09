package com.receiptmgr.app

import android.app.Application
import com.receiptmgr.app.di.AppContainer
import com.receiptmgr.app.di.DefaultAppContainer
import com.receiptmgr.app.presentation.notification.NotificationController

class ReceiptMgrApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = DefaultAppContainer(this)
        // 強制初始化全域通知控制器
        NotificationController.init(appContainer.notificationController)
    }
}
