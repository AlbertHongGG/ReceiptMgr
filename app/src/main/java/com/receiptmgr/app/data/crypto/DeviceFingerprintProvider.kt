package com.receiptmgr.app.data.crypto

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.UUID

object DeviceFingerprintProvider {

    const val DEFAULT_APP_ID = "EINV6202512184519"
    const val DEFAULT_APP_VERSION = "6.800.11"
    private const val PREFS_DEVICE = "receipt_mgr_device_prefs"
    private const val KEY_FALLBACK_UUID = "key_fallback_device_uuid"

    @SuppressLint("HardwareIds")
    fun getAndroidUuid(context: Context): String {
        return try {
            val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            if (!androidId.isNullOrBlank() && androidId.lowercase() != "unknown") {
                androidId
            } else {
                getOrCreateFallbackUuid(context)
            }
        } catch (_: Exception) {
            getOrCreateFallbackUuid(context)
        }
    }

    private fun getOrCreateFallbackUuid(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_DEVICE, Context.MODE_PRIVATE)
        val existing = prefs.getString(KEY_FALLBACK_UUID, null)
        if (!existing.isNullOrBlank()) return existing

        val generated = UUID.randomUUID().toString().replace("-", "").take(16)
        prefs.edit().putString(KEY_FALLBACK_UUID, generated).apply()
        return generated
    }

    fun getDeviceModel(): String {
        val model = Build.MODEL
        return if (!model.isNullOrBlank()) model else "SM-A315G"
    }

    fun getOsVersion(): String {
        val release = Build.VERSION.RELEASE
        return if (!release.isNullOrBlank()) release else "14"
    }

    fun getAppVersion(context: Context): String {
        return try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: DEFAULT_APP_VERSION
        } catch (_: Exception) {
            DEFAULT_APP_VERSION
        }
    }
}
