package com.receiptmgr.app.data.device

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import com.receiptmgr.app.domain.model.auth.DeviceFingerprint
import com.receiptmgr.app.domain.model.auth.TargetAppSpec
import java.util.UUID

/**
 * 宿主機台設備環境提供者 (本機環境特徵收集)
 * 專門負責讀取裝置硬體特徵 (Model, OS, Android ID) 與推播識別碼 (ptoken)，
 * 嚴格區分本機硬體特徵與財政部目標協定規格。
 */
class DeviceEnvironmentProvider(
    private val context: Context
) {
    companion object {
        private const val PREFS_DEVICE = "receipt_mgr_device_prefs"
        private const val KEY_FALLBACK_UUID = "key_fallback_device_uuid"
        private const val KEY_CUSTOM_PTOKEN = "key_custom_device_ptoken"
        private const val DEFAULT_FALLBACK_MODEL = "SM-A315G"
        private const val DEFAULT_FALLBACK_OS = "14"
        private const val DEFAULT_HOST_VERSION = "1.0.0"
    }

    /**
     * 採集本機硬體指紋物件 (DeviceFingerprint)
     */
    fun getDeviceFingerprint(customPtoken: String? = null): DeviceFingerprint {
        val ptokenToUse = customPtoken?.takeIf { it.isNotBlank() }
            ?: getPtoken()

        return DeviceFingerprint(
            os = DeviceFingerprint.DEFAULT_OS_ANDROID,
            osVersion = getOsVersion(),
            model = getDeviceModel(),
            androidId = getAndroidId(),
            ptoken = ptokenToUse
        )
    }

    /**
     * 讀取設備 16 碼十六進位 Android ID，若異常則生成並持久化 16 碼退回識別碼
     */
    @SuppressLint("HardwareIds")
    fun getAndroidId(): String {
        return try {
            val rawId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!rawId.isNullOrBlank() && rawId.lowercase() != "unknown") {
                rawId
            } else {
                getOrCreateFallbackAndroidId()
            }
        } catch (_: Exception) {
            getOrCreateFallbackAndroidId()
        }
    }

    /**
     * 相容別名：獲取 Android UUID / Android ID
     */
    fun getAndroidUuid(): String = getAndroidId()

    /**
     * 獲取設備硬體型號字串 (例如 "samsung SM-A315G")
     */
    fun getDeviceModel(): String {
        val manufacturer = Build.MANUFACTURER.orEmpty().trim()
        val model = Build.MODEL.orEmpty().trim().ifEmpty { DEFAULT_FALLBACK_MODEL }

        return if (manufacturer.isNotBlank() && !model.startsWith(manufacturer, ignoreCase = true)) {
            "$manufacturer $model"
        } else {
            model
        }
    }

    /**
     * 獲取 Android 系統發行版號 (例如 "14")
     */
    fun getOsVersion(): String {
        val release = Build.VERSION.RELEASE
        return if (!release.isNullOrBlank()) release else DEFAULT_FALLBACK_OS
    }

    /**
     * 獲取推播識別碼 (ptoken)，優先使用使用者自訂或持久化值，預設為官方標準 152 碼 Token
     */
    fun getPtoken(): String {
        val prefs = context.getSharedPreferences(PREFS_DEVICE, Context.MODE_PRIVATE)
        val saved = prefs.getString(KEY_CUSTOM_PTOKEN, null)
        return if (!saved.isNullOrBlank()) saved else TargetAppSpec.DEFAULT_PTOKEN
    }

    /**
     * 儲存自訂推播識別碼
     */
    fun saveCustomPtoken(ptoken: String) {
        val prefs = context.getSharedPreferences(PREFS_DEVICE, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_CUSTOM_PTOKEN, ptoken.trim()).apply()
    }

    /**
     * 宿主本機 App (ReceiptMgr) 的版本號 (例如 "1.0.0")，僅供診斷與設定畫面展示
     * 注意：不可用於 UIA 登入協定 payload 內，目標 API 版號請使用 TargetAppSpec.DEFAULT_TARGET_APP_VER
     */
    fun getHostAppVersion(): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: DEFAULT_HOST_VERSION
        } catch (_: Exception) {
            DEFAULT_HOST_VERSION
        }
    }

    private fun getOrCreateFallbackAndroidId(): String {
        val prefs = context.getSharedPreferences(PREFS_DEVICE, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_FALLBACK_UUID, null)
        if (!existing.isNullOrBlank()) return existing

        val generated = UUID.randomUUID().toString().replace("-", "").take(16)
        prefs.edit().putString(KEY_FALLBACK_UUID, generated).apply()
        return generated
    }
}
