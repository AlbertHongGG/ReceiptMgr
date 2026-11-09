package com.receiptmgr.app.di

import android.content.Context
import com.receiptmgr.app.data.network.UiaAuthApiClient
import com.receiptmgr.app.data.repository.AuthRepositoryImpl
import com.receiptmgr.app.data.repository.SettingsRepositoryImpl
import com.receiptmgr.app.data.storage.ApiLogManager
import com.receiptmgr.app.data.storage.EncryptedSessionDataSource
import com.receiptmgr.app.data.storage.NotificationLogManager
import com.receiptmgr.app.data.storage.SettingsDataStore
import com.receiptmgr.app.domain.repository.AuthRepository
import com.receiptmgr.app.domain.repository.SettingsRepository
import com.receiptmgr.app.presentation.notification.NotificationController

interface AppContainer {
    val notificationLogManager: NotificationLogManager
    val apiLogManager: ApiLogManager
    val notificationController: NotificationController
    val settingsDataStore: SettingsDataStore
    val encryptedSessionDataSource: EncryptedSessionDataSource
    val uiaAuthApiClient: UiaAuthApiClient
    val authRepository: AuthRepository
    val settingsRepository: SettingsRepository
}

class DefaultAppContainer(private val context: Context) : AppContainer {

    override val notificationLogManager: NotificationLogManager by lazy {
        NotificationLogManager(context)
    }

    override val apiLogManager: ApiLogManager by lazy {
        ApiLogManager(context)
    }

    override val notificationController: NotificationController by lazy {
        NotificationController(notificationLogManager).also {
            NotificationController.init(it)
        }
    }

    override val settingsDataStore: SettingsDataStore by lazy {
        SettingsDataStore(context)
    }

    override val encryptedSessionDataSource: EncryptedSessionDataSource by lazy {
        EncryptedSessionDataSource(context)
    }

    override val uiaAuthApiClient: UiaAuthApiClient by lazy {
        UiaAuthApiClient(apiLogManager)
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(uiaAuthApiClient, encryptedSessionDataSource)
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(settingsDataStore, apiLogManager, notificationLogManager)
    }
}
