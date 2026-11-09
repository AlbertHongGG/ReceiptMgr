package com.receiptmgr.app.data.network

import android.util.Log
import com.receiptmgr.app.data.crypto.SealedLdataPacket
import com.receiptmgr.app.data.crypto.UiaPacketCodec
import com.receiptmgr.app.data.network.dto.UiaEnvelopeResponseDto
import com.receiptmgr.app.data.network.dto.UiaLdataRequestDto
import com.receiptmgr.app.data.storage.ApiLogManager
import com.receiptmgr.app.domain.model.auth.LoginCommand
import com.receiptmgr.app.domain.model.auth.SessionCredentials
import com.receiptmgr.app.domain.model.auth.UiaProtocolException
import com.receiptmgr.app.domain.model.log.ApiLogRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

/**
 * UIA 登入認證網路客戶端
 * 負責發起 POST https://uia.einvoice.nat.gov.tw/mid/v1/login 請求，
 * 統整封包加密、HTTP 傳輸、錯誤碼分流與單點自動稽核記錄。
 */
class UiaAuthApiClient(
    private val apiLogManager: ApiLogManager,
    private val packetCodec: UiaPacketCodec = UiaPacketCodec(),
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {
    companion object {
        private const val TAG = "UiaAuthApiClient"
        private const val UIA_LOGIN_ENDPOINT = "https://uia.einvoice.nat.gov.tw/mid/v1/login"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }

    suspend fun executeLogin(command: LoginCommand): Result<SessionCredentials> = withContext(Dispatchers.IO) {
        val startTime = System.currentTimeMillis()
        var httpStatus = 0
        var rawResponseBody = ""
        var rawRequestBody = ""
        var decryptedPayloadString: String? = null
        var auditError: String? = null
        val headersMap = mutableMapOf<String, String>()
        var sealedPacket: SealedLdataPacket? = null

        try {
            // 1. 即時注入時間戳並以 Byte 0 對齊方式封裝加密 ldata
            sealedPacket = packetCodec.sealLoginCommand(command)
            val requestDto = UiaLdataRequestDto(ldata = sealedPacket.ldataString)
            rawRequestBody = json.encodeToString(requestDto)

            // 2. 建構 HTTP/2 請求 (模擬官方行動客戶端 Headers)
            val httpRequest = Request.Builder()
                .url(UIA_LOGIN_ENDPOINT)
                .header("Host", "uia.einvoice.nat.gov.tw")
                .header("User-Agent", "okhttp/5.3.0")
                .header("Accept-Encoding", "gzip")
                .header("Content-Type", "application/json; charset=utf-8")
                .post(rawRequestBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpRequest.headers.forEach { (name, value) ->
                headersMap[name] = value
            }

            // 3. 發送請求
            val response = okHttpClient.newCall(httpRequest).execute()
            httpStatus = response.code
            rawResponseBody = response.body?.string().orEmpty()

            if (!response.isSuccessful) {
                val ex = UiaProtocolException.TransportFailure(
                    httpStatus = httpStatus,
                    rawBody = rawResponseBody,
                    message = "HTTP 請求失敗 ($httpStatus): ${rawResponseBody.ifEmpty { response.message }}"
                )
                auditError = ex.message
                throw ex
            }

            // 4. 解析伺服器回應外殼 (純 Kotlinx 序列化)
            val respDto = json.decodeFromString<UiaEnvelopeResponseDto>(rawResponseBody)
            val resultCode = respDto.result

            if (resultCode != 0) {
                val ex = when (resultCode) {
                    -204 -> UiaProtocolException.TimestampExpired(respDto.id, respDto.message)
                    -205 -> UiaProtocolException.DecryptionFailed(respDto.id, respDto.message)
                    else -> UiaProtocolException.ServerRejected(resultCode, respDto.id, respDto.message)
                }
                auditError = ex.message
                throw ex
            }

            val encryptedPayload = respDto.payload
            if (encryptedPayload.isNullOrBlank()) {
                val ex = UiaProtocolException.MalformedPayload("伺服器回應狀態碼為 0 但未包含 payload 密文")
                auditError = ex.message
                throw ex
            }

            // 5. 使用原 AES 金鑰以 Byte 0 對齊方式解密 payload
            val (sessionPayload, decryptedJson) = packetCodec.unsealSessionPayload(
                encryptedPayloadBase64 = encryptedPayload,
                aesKey = sealedPacket.aesKey
            )
            decryptedPayloadString = decryptedJson

            val session = SessionCredentials(
                phone = command.phone,
                payload = sessionPayload,
                rawPayloadJson = decryptedJson,
                rawServerResponseJson = rawResponseBody
            )

            Result.success(session)
        } catch (e: Exception) {
            Log.e(TAG, "Login API execution failed: ${e.message}", e)
            if (auditError == null) {
                auditError = e.message ?: "未知例外異常"
            }
            Result.failure(e)
        } finally {
            val durationMs = System.currentTimeMillis() - startTime
            val requestAuditMap = mutableMapOf(
                "endpoint" to "/mid/v1/login",
                "phone" to command.phone,
                "app_id" to command.appId,
                "app_version" to command.appVersion,
                "device_model" to command.deviceModel,
                "os_version" to command.osVersion,
                "uuid" to command.uuid,
                "login_mod" to command.loginMod
            )
            sealedPacket?.let {
                requestAuditMap["ldata"] = it.ldataString
                requestAuditMap["plaintext_preview"] = it.plaintextJson
                requestAuditMap["timestamp"] = it.timestamp.toString()
            }

            val responseAuditMap = mutableMapOf(
                "status" to httpStatus.toString(),
                "raw_response" to rawResponseBody
            )
            if (decryptedPayloadString != null) {
                responseAuditMap["decrypted_payload"] = decryptedPayloadString
            }
            if (auditError != null) {
                responseAuditMap["error"] = auditError
            }

            apiLogManager.logApiInteraction(
                ApiLogRecord(
                    endpoint = "POST /mid/v1/login",
                    agentName = "帳號授權",
                    method = "POST",
                    status = httpStatus,
                    durationMs = durationMs,
                    request = requestAuditMap,
                    response = responseAuditMap,
                    requestHeaders = headersMap,
                    requestBody = rawRequestBody,
                    responseBody = rawResponseBody,
                    decryptedPayload = decryptedPayloadString,
                    error = auditError,
                    isSuccess = (auditError == null)
                )
            )
        }
    }
}
