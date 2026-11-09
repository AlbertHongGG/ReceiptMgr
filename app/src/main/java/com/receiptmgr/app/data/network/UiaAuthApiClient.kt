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
import okhttp3.Response
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * UIA 登入認證網路客戶端
 * 負責發起 POST https://uia.einvoice.nat.gov.tw/mid/v1/login 請求，
 * 統整 AES-GCM 封包加密、HTTP 傳輸、GZIP 解壓縮、錯誤碼分流與單點自動稽核記錄。
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
            // 1. 即時注入時間戳並以 AES-GCM 封裝加密三段式 ldata
            sealedPacket = packetCodec.sealLoginCommand(command)
            val requestDto = UiaLdataRequestDto(ldata = sealedPacket.ldataString)
            rawRequestBody = json.encodeToString(requestDto)

            // 2. 建構 HTTP/2 請求 (模擬官方行動客戶端 Headers)
            // 注意：嚴禁手動設定 "Accept-Encoding: gzip"，以允許 OkHttp 的 BridgeInterceptor 自動執行透明 GZIP 解壓縮
            val httpRequest = Request.Builder()
                .url(UIA_LOGIN_ENDPOINT)
                .header("Host", "uia.einvoice.nat.gov.tw")
                .header("User-Agent", "okhttp/5.3.0")
                .header("Content-Type", "application/json; charset=utf-8")
                .post(rawRequestBody.toRequestBody(JSON_MEDIA_TYPE))
                .build()

            httpRequest.headers.forEach { (name, value) ->
                headersMap[name] = value
            }

            // 3. 發送請求
            val response = okHttpClient.newCall(httpRequest).execute()
            httpStatus = response.code
            rawResponseBody = extractResponseBodyString(response)

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

            // 5. 使用同一組 AES 金鑰與 IV 解密伺服器下發之 payload
            val (sessionPayload, decryptedJson) = packetCodec.unsealSessionPayload(
                encryptedPayloadBase64 = encryptedPayload,
                aesKey = sealedPacket.aesKey,
                iv = sealedPacket.iv
            )
            decryptedPayloadString = decryptedJson

            val session = SessionCredentials(
                account = command.account,
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
                "account" to command.account,
                "pdid" to command.pdid,
                "device_model" to command.model,
                "os_version" to command.osVersion,
                "appver" to command.appver,
                "appbn" to command.appbn.toString(),
                "lang" to command.lang,
                "ccs" to command.ccs
            )
            sealedPacket?.let {
                requestAuditMap["ckey"] = it.ckey
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

    /**
     * 安全提取回應主體文字，具備雙重 GZIP 防禦機制：
     * 1. OkHttp 未自動解壓時依 Content-Encoding: gzip 進行解壓
     * 2. 檢測前兩個位元組是否為 GZIP 魔數 (0x1F, 0x8B)，若是則自動解壓，杜絕亂碼
     */
    private fun extractResponseBodyString(response: Response): String {
        val responseBody = response.body ?: return ""
        val rawBytes = responseBody.bytes()
        if (rawBytes.isEmpty()) return ""

        val encoding = response.header("Content-Encoding")
        val isGzipHeader = encoding?.equals("gzip", ignoreCase = true) == true
        val isGzipMagic = rawBytes.size >= 2 &&
                rawBytes[0] == 0x1F.toByte() &&
                rawBytes[1] == 0x8B.toByte()

        return if (isGzipHeader || isGzipMagic) {
            try {
                GZIPInputStream(ByteArrayInputStream(rawBytes)).bufferedReader(Charsets.UTF_8).use {
                    it.readText()
                }
            } catch (e: Exception) {
                Log.w(TAG, "GZIP decompression fallback failed, using raw string: ${e.message}")
                String(rawBytes, Charsets.UTF_8)
            }
        } else {
            String(rawBytes, Charsets.UTF_8)
        }
    }
}
