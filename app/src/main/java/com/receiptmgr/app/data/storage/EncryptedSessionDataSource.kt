package com.receiptmgr.app.data.storage

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.receiptmgr.app.domain.model.auth.SessionCredentials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EncryptedSessionDataSource(private val context: Context) {

    companion object {
        private const val TAG = "EncryptedSessionData"
        private const val PREFS_FILE_NAME = "receiptmgr_secure_session"
        private const val KEY_SESSION_DATA = "key_encrypted_session_json"
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val securePrefs: SharedPreferences by lazy {
        try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w(TAG, "MasterKey/EncryptedSharedPreferences initialization failed, falling back: ${e.message}")
            context.getSharedPreferences("${PREFS_FILE_NAME}_compat", Context.MODE_PRIVATE)
        }
    }

    suspend fun saveSession(session: SessionCredentials) = withContext(Dispatchers.IO) {
        try {
            val serialized = json.encodeToString(session)
            securePrefs.edit().putString(KEY_SESSION_DATA, serialized).commit()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist session: ${e.message}", e)
        }
    }

    suspend fun getSession(): SessionCredentials? = withContext(Dispatchers.IO) {
        try {
            val serialized = securePrefs.getString(KEY_SESSION_DATA, null) ?: return@withContext null
            json.decodeFromString<SessionCredentials>(serialized)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read session: ${e.message}", e)
            null
        }
    }

    suspend fun clearSession() = withContext(Dispatchers.IO) {
        try {
            securePrefs.edit().remove(KEY_SESSION_DATA).commit()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear session: ${e.message}", e)
        }
    }
}
