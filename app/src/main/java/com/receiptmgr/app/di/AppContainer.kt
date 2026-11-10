package com.receiptmgr.app.di

import android.content.Context
import com.receiptmgr.app.data.crypto.UiaGcmCipher
import com.receiptmgr.app.data.crypto.UiaPacketCodec
import com.receiptmgr.app.data.crypto.UpiJwtPacketCodec
import com.receiptmgr.app.data.device.DeviceEnvironmentProvider
import com.receiptmgr.app.data.export.AndroidEquityTrackerTransport
import com.receiptmgr.app.data.export.DefaultInvoiceExportService
import com.receiptmgr.app.data.network.UiaAuthApiClient
import com.receiptmgr.app.data.network.UpiInvoiceApiClient
import com.receiptmgr.app.data.repository.AuthRepositoryImpl
import com.receiptmgr.app.data.repository.ExportRecordRepositoryImpl
import com.receiptmgr.app.data.repository.InvoiceRepositoryImpl
import com.receiptmgr.app.data.repository.SettingsRepositoryImpl
import com.receiptmgr.app.data.storage.ApiLogManager
import com.receiptmgr.app.data.storage.EncryptedSessionDataSource
import com.receiptmgr.app.data.storage.ExportRecordLocalDataSource
import com.receiptmgr.app.data.storage.ExportRecordLocalDataSourceImpl
import com.receiptmgr.app.data.storage.InvoiceLocalDataSource
import com.receiptmgr.app.data.storage.InvoiceLocalDataSourceImpl
import com.receiptmgr.app.data.storage.NotificationLogManager
import com.receiptmgr.app.data.storage.SettingsDataStore
import com.receiptmgr.app.data.storage.db.InvoiceDatabaseHelper
import com.receiptmgr.app.data.sync.InvoiceDetailSyncEngine
import com.receiptmgr.app.data.sync.InvoiceDetailSyncEngineImpl
import com.receiptmgr.app.domain.repository.AuthRepository
import com.receiptmgr.app.domain.repository.ExportRecordRepository
import com.receiptmgr.app.domain.repository.InvoiceRepository
import com.receiptmgr.app.domain.repository.SettingsRepository
import com.receiptmgr.app.domain.service.InvoiceExportService
import com.receiptmgr.app.domain.service.ReceiptTransport
import com.receiptmgr.app.presentation.notification.NotificationController

interface AppContainer {
    val notificationLogManager: NotificationLogManager
    val apiLogManager: ApiLogManager
    val notificationController: NotificationController
    val settingsDataStore: SettingsDataStore
    val encryptedSessionDataSource: EncryptedSessionDataSource
    val invoiceDatabaseHelper: InvoiceDatabaseHelper
    val invoiceLocalDataSource: InvoiceLocalDataSource
    val invoiceDetailSyncEngine: InvoiceDetailSyncEngine
    val deviceEnvironmentProvider: DeviceEnvironmentProvider
    val uiaPacketCodec: UiaPacketCodec
    val uiaAuthApiClient: UiaAuthApiClient
    val authRepository: AuthRepository
    val settingsRepository: SettingsRepository
    val upiJwtPacketCodec: UpiJwtPacketCodec
    val upiInvoiceApiClient: UpiInvoiceApiClient
    val invoiceRepository: InvoiceRepository
    val exportRecordRepository: ExportRecordRepository
    val invoiceExportService: InvoiceExportService
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

    override val invoiceDatabaseHelper: InvoiceDatabaseHelper by lazy {
        InvoiceDatabaseHelper(context)
    }

    override val invoiceLocalDataSource: InvoiceLocalDataSource by lazy {
        InvoiceLocalDataSourceImpl(invoiceDatabaseHelper)
    }

    override val invoiceDetailSyncEngine: InvoiceDetailSyncEngine by lazy {
        InvoiceDetailSyncEngineImpl(
            upiApiClient = upiInvoiceApiClient,
            sessionDataSource = encryptedSessionDataSource,
            localDataSource = invoiceLocalDataSource
        )
    }

    override val deviceEnvironmentProvider: DeviceEnvironmentProvider by lazy {
        DeviceEnvironmentProvider(context)
    }

    override val uiaPacketCodec: UiaPacketCodec by lazy {
        UiaPacketCodec(cipher = UiaGcmCipher())
    }

    override val uiaAuthApiClient: UiaAuthApiClient by lazy {
        UiaAuthApiClient(
            apiLogManager = apiLogManager,
            packetCodec = uiaPacketCodec
        )
    }

    override val authRepository: AuthRepository by lazy {
        AuthRepositoryImpl(uiaAuthApiClient, encryptedSessionDataSource)
    }

    override val settingsRepository: SettingsRepository by lazy {
        SettingsRepositoryImpl(settingsDataStore, apiLogManager, notificationLogManager)
    }

    override val upiJwtPacketCodec: UpiJwtPacketCodec by lazy {
        UpiJwtPacketCodec()
    }

    override val upiInvoiceApiClient: UpiInvoiceApiClient by lazy {
        UpiInvoiceApiClient(
            apiLogManager = apiLogManager,
            packetCodec = upiJwtPacketCodec
        )
    }

    override val invoiceRepository: InvoiceRepository by lazy {
        InvoiceRepositoryImpl(
            upiApiClient = upiInvoiceApiClient,
            sessionDataSource = encryptedSessionDataSource,
            localDataSource = invoiceLocalDataSource,
            syncEngine = invoiceDetailSyncEngine
        )
    }

    val exportRecordLocalDataSource: ExportRecordLocalDataSource by lazy {
        ExportRecordLocalDataSourceImpl(invoiceDatabaseHelper)
    }

    override val exportRecordRepository: ExportRecordRepository by lazy {
        ExportRecordRepositoryImpl(exportRecordLocalDataSource)
    }

    val receiptTransport: ReceiptTransport by lazy {
        AndroidEquityTrackerTransport(context)
    }

    override val invoiceExportService: InvoiceExportService by lazy {
        DefaultInvoiceExportService(
            transport = receiptTransport,
            invoiceLocalDataSource = invoiceLocalDataSource,
            exportRecordRepository = exportRecordRepository
        )
    }
}
