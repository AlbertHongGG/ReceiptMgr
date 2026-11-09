package com.receiptmgr.app.data.network

import android.util.Log
import com.receiptmgr.app.data.crypto.UiaCryptoEngine
import com.receiptmgr.app.data.storage.ApiLogManager
import com.receiptmgr.app.domain.model.auth.LoginRequestData
import com.receiptmgr.app.domain.model.auth.SessionCredentials
import com.receiptmgr.app.domain.model.auth.SessionPayload
import com.receiptmgr.app.domain.model.log.ApiLogRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class UiaAuthApiClient(
    private val apiLogManager: ApiLogManager
) {
    companion object {
        private const val TAG = "UiaAuthApiClient"
        private const val UIA_LOGIN_ENDPOINT = "https://uia.einvoice.nat.gov.tw/mid/v1/login"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    suspend fun executeLogin(requestData: LoginRequestData): Result<SessionCredentials> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var httpStatus = 0
        var rawResponseBody = ""
        var ldataBody = ""
        val headersMap = mutableMapOf<String, String>()

        try {
            // 1. 金鑰派生與明文打包
            val part0 = UiaCryptoEngine.randomSalt(16)
            val part2 = UiaCryptoEngine.randomSalt(16)
            val aesKey = UiaCryptoEngine.deriveLoginAesKey(part0, part2)

            val plaintextJson = json.encodeToString(requestData)
            val part1 = UiaCryptoEngine.encryptLoginPayload(plaintextJson, aesKey)
            val ldata = UiaCryptoEngine.packLdata(part0, part1, part2)

            val requestJson = JSONObject().apply {
                put("ldata", ldata)
            }
            ldataBody = requestJson.toString()

            val requestMap = mapOf(
                "endpoint" to "/mid/v1/login",
                "phone" to requestData.phone,
                "app_id" to requestData.appId,
                "app_version" to requestData.appVersion,
                "device_model" to requestData.deviceModel,
                "os_version" to requestData.osVersion,
                "uuid" to requestData.uuid,
                "ldata" to ldata
            )

            // 2. 建構 OkHttp HTTP/2 請求 (模擬官方行動客戶端)
            val httpRequest = Request.Builder()
                .url(UIA_LOGIN_ENDPOINT)
                .header("Host", "uia.einvoice.nat.gov.tw")
                .header("User-Agent", "okhttp/5.3.0")
                .header("Accept-Encoding", "gzip")
                .header("Content-Type", "application/json; charset=utf-8")
                .post(ldataBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpRequest.headers.forEach { (name, value) ->
                headersMap[name] = value
            }

            // 3. 發送請求
            val response = okHttpClient.newCall(httpRequest).execute()
            httpStatus = response.code
            rawResponseBody = response.body?.string().orEmpty()
            val durationMs = System.currentTimeMillis() - startTime

            if (!response.isSuccessful) {
                val errorMsg = "HTTP $httpStatus: ${rawResponseBody.ifEmpty { response.message }}"
                val responseMap = mapOf(
                    "status" to httpStatus.toString(),
                    "raw_response" to rawResponseBody,
                    "error" to errorMsg
                )
                apiLogManager.logApiInteraction(
                    ApiLogRecord(
                        endpoint = "POST /mid/v1/login",
                        agentName = "帳號授權",
                        method = "POST",
                        status = httpStatus,
                        durationMs = durationMs,
                        request = requestMap,
                        response = responseMap,
                        requestHeaders = headersMap,
                        requestBody = ldataBody,
                        responseBody = rawResponseBody,
                        decryptedPayload = null,
                        error = errorMsg,
                        isSuccess = false
                    )
                )
                return@withContext Result.failure(Exception(errorMsg))
            }

            // 4. 解析回應 JSON
            val respJson = JSONObject(rawResponseBody)
            val result = respJson.optInt("result", -1)
            val message = respJson.optString("message", "")

            if (result != 0) {
                val err = if (message.isNotBlank()) message else "登入失敗 (錯誤碼: $result)"
                val responseMap = mutableMapOf<String, String>(
                    "status" to httpStatus.toString(),
                    "result" to result.toString(),
                    "error" to err
                )
                if (respJson.has("id")) responseMap["id"] = respJson.optString("id")
                if (respJson.has("data")) responseMap["data"] = respJson.optString("data")

                apiLogManager.logApiInteraction(
                    ApiLogRecord(
                        endpoint = "POST /mid/v1/login",
                        agentName = "帳號授權",
                        method = "POST",
                        status = httpStatus,
                        durationMs = durationMs,
                        request = requestMap,
                        response = responseMap,
                        requestHeaders = headersMap,
                        requestBody = ldataBody,
                        responseBody = rawResponseBody,
                        decryptedPayload = null,
                        error = err,
                        isSuccess = false
                    )
                )
                return@withContext Result.failure(Exception(err))
            }

            val encryptedPayload = respJson.optString("payload", "")
            if (encryptedPayload.isBlank()) {
                val err = "伺服器回應未包含 payload 密文"
                val responseMap = mapOf(
                    "status" to httpStatus.toString(),
                    "raw_response" to rawResponseBody,
                    "error" to err
                )
                apiLogManager.logApiInteraction(
                    ApiLogRecord(
                        endpoint = "POST /mid/v1/login",
                        agentName = "帳號授權",
                        method = "POST",
                        status = httpStatus,
                        durationMs = durationMs,
                        request = requestMap,
                        response = responseMap,
                        requestHeaders = headersMap,
                        requestBody = ldataBody,
                        responseBody = rawResponseBody,
                        decryptedPayload = null,
                        error = err,
                        isSuccess = false
                    )
                )
                return@withContext Result.failure(Exception(err))
            }

            // 5. 使用原 AES 金鑰解密伺服器下發之 payload
            val decryptedJsonString = UiaCryptoEngine.decryptLoginResponse(encryptedPayload, aesKey)
            val sessionPayload = json.decodeFromString<SessionPayload>(decryptedJsonString)

            val session = SessionCredentials(
                phone = requestData.phone,
                payload = sessionPayload,
                rawPayloadJson = decryptedJsonString,
                rawServerResponseJson = rawResponseBody
            )

            val responseMap = mutableMapOf<String, String>(
                "status" to httpStatus.toString(),
                "payload" to decryptedJsonString,
                "raw_response" to rawResponseBody
            )

            // 6. 記錄成功稽核日誌
            apiLogManager.logApiInteraction(
                ApiLogRecord(
                    endpoint = "POST /mid/v1/login",
                    agentName = "帳號授權",
                    method = "POST",
                    status = httpStatus,
                    durationMs = durationMs,
                    request = requestMap,
                    response = responseMap,
                    requestHeaders = headersMap,
                    requestBody = ldataBody,
                    responseBody = rawResponseBody,
                    decryptedPayload = decryptedJsonString,
                    error = null,
                    isSuccess = true
                )
            )

            Result.success(session)
        } catch (e: Exception) {
            val durationMs = System.currentTimeMillis() - startTime
            Log.e(TAG, "Login API failed: ${e.message}", e)
            val requestMap = mapOf(
                "endpoint" to "/mid/v1/login",
                "phone" to requestData.phone
            )
            val responseMap = mapOf(
                "error" to (e.message ?: "未知例外異常")
            )
            apiLogManager.logApiInteraction(
                ApiLogRecord(
                    endpoint = "POST /mid/v1/login",
                    agentName = "帳號授權",
                    method = "POST",
                    status = httpStatus,
                    durationMs = durationMs,
                    request = requestMap,
                    response = responseMap,
                    requestHeaders = headersMap,
                    requestBody = ldataBody,
                    responseBody = rawResponseBody,
                    decryptedPayload = null,
                    error = e.message ?: "未知異常",
                    isSuccess = false
                )
            )
            Result.failure(e)
        }
    }
}
