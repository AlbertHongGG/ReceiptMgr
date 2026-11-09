package com.receiptmgr.app.data.device

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.receiptmgr.app.domain.model.auth.DeviceFingerprint
import java.util.UUID

/**
 * 宿主機台設備環境提供者 (本機環境特徵收集)
 * 專門負責讀取裝置硬體特徵與宿主 App 資訊，嚴格杜絕與財政部目標協定規格混淆。
 */
class DeviceEnvironmentProvider(
    private val context: Context
) {
    companion object {
        private const val PREFS_DEVICE = "receipt_mgr_device_prefs"
        private const val KEY_FALLBACK_UUID = "key_fallback_device_uuid"
        private const val DEFAULT_FALLBACK_MODEL = "SM-A315G"
        private const val DEFAULT_FALLBACK_OS = "14"
        private const val DEFAULT_HOST_VERSION = "1.0.0"
    }

    /**
     * 採集本機硬體指紋物件 (DeviceFingerprint)
     */
    fun getDeviceFingerprint(): DeviceFingerprint {
        return DeviceFingerprint(
            deviceModel = getDeviceModel(),
            osVersion = getOsVersion(),
            androidUuid = getAndroidUuid()
        )
    }

    /**
     * 讀取設備 Android ID，若異常則生成並持久化 16 碼十六進位識別碼
     */
    @SuppressLint("HardwareIds")
    fun getAndroidUuid(): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!androidId.isNullOrBlank() && androidId.lowercase() != "unknown") {
                androidId
            } else {
                getOrCreateFallbackUuid()
            }
        } catch (_: Exception) {
            getOrCreateFallbackUuid()
        }
    }

    /**
     * 設備硬體型號
     */
    fun getDeviceModel(): String {
        val model = Build.MODEL
        return if (!model.isNullOrBlank()) model else DEFAULT_FALLBACK_MODEL
    }

    /**
     * Android 系統發行版號
     */
    fun getOsVersion(): String {
        val release = Build.VERSION.RELEASE
        return if (!release.isNullOrBlank()) release else DEFAULT_FALLBACK_OS
    }

    /**
     * 宿主本機 App (ReceiptMgr) 的版本號 (例如 "1.0.0")，供診斷與設定畫面展示
     * 注意：不可用於 UIA 登入協定 payload 內，目標 API 版號請使用 TargetClientSpec.DEFAULT_TARGET_APP_VERSION
     */
    fun getHostAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: DEFAULT_HOST_VERSION
        } catch (_: Exception) {
            DEFAULT_HOST_VERSION
        }
    }

    private fun getOrCreateFallbackUuid(): String {
        val prefs = context.getSharedPreferences(PREFS_DEVICE, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_FALLBACK_UUID, null)
        if (!existing.isNullOrBlank()) return existing

        val generated = UUID.randomUUID().toString().replace("-", "").take(16)
        prefs.edit().putString(KEY_FALLBACK_UUID, generated).apply()
        return generated
    }
}
