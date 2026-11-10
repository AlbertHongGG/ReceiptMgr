package com.receiptmgr.app.data.network

import android.util.Log
import com.receiptmgr.app.data.crypto.SealedUpiPacket
import com.receiptmgr.app.data.crypto.UpiJwtPacketCodec
import com.receiptmgr.app.data.network.dto.UpiInvoiceDetailReqDto
import com.receiptmgr.app.data.network.dto.UpiInvoiceHeaderReqDto
import com.receiptmgr.app.data.network.dto.UpiInvoiceWireParser
import com.receiptmgr.app.data.storage.ApiLogManager
import com.receiptmgr.app.domain.model.auth.SessionCredentials
import com.receiptmgr.app.domain.model.invoice.InvoiceDetail
import com.receiptmgr.app.domain.model.invoice.InvoiceHeader
import com.receiptmgr.app.domain.model.invoice.UpiProtocolException
import com.receiptmgr.app.domain.model.log.ApiLogRecord
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.ByteArrayInputStream
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream

/**
 * 財政部 UPI 電子發票客戶端 (UpiInvoiceApiClient)。
 *
 * 核心保證：
 * 1. 雙核心端口: query-invoices-header 與 query-invoices-details。
 * 2. 嚴格依據 einvoiceJwt 規範進行 Form-URL-Encoded POST 傳輸。
 * 3. 雙重 GZIP 魔數解碼保護，杜絕亂碼。
 * 4. 單點全鏈路審計持久化，發送前後自動寫入 ApiLogManager (發票清單 / 發票明細)。
 */
open class UpiInvoiceApiClient(
    private val apiLogManager: ApiLogManager,
    private val packetCodec: UpiJwtPacketCodec = UpiJwtPacketCodec(),
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {
    companion object {
        private const val TAG = "UpiInvoiceApiClient"
        private const val UPI_HOST = "https://upi.einvoice.nat.gov.tw"
        const val ENDPOINT_HEADER = "/einvoice/carriers/query-invoices-header"
        const val ENDPOINT_DETAIL = "/einvoice/carriers/query-invoices-details"
        private const val DEFAULT_APP_ID = "EINV6202512184519"
    }

    /**
     * 端口 1: 查詢載具發票表頭清單
     */
    open suspend fun queryHeaders(
        reqData: UpiInvoiceHeaderReqDto,
        session: SessionCredentials
    ): Result<List<InvoiceHeader>> = withContext(Dispatchers.IO) {
        val endpointUrl = "$UPI_HOST$ENDPOINT_HEADER"
        val appId = session.appId.ifBlank { DEFAULT_APP_ID }
        val comms = packetCodec.createFreshComms(
            barcode = session.barcode,
            appId = appId,
            liat = session.liat
        )

        val sealedPacket = packetCodec.seal(
            comms = comms,
            reqData = reqData,
            serializer = UpiInvoiceHeaderReqDto.serializer(),
            sessionKey = session.ssMe
        )

        executePost(
            endpointPath = ENDPOINT_HEADER,
            endpointUrl = endpointUrl,
            agentName = "發票清單",
            sealedPacket = sealedPacket,
            requestSummary = mapOf(
                "action" to reqData.action,
                "startDate" to reqData.startDate,
                "endDate" to reqData.endDate,
                "cardNo" to reqData.cardNo,
                "page" to reqData.page.toString()
            )
        ) { rawJson ->
            UpiInvoiceWireParser.parseHeaderResponse(rawJson)
        }
    }

    /**
     * 端口 2: 查詢單筆發票品項明細
     */
    open suspend fun queryDetails(
        reqData: UpiInvoiceDetailReqDto,
        session: SessionCredentials
    ): Result<InvoiceDetail> = withContext(Dispatchers.IO) {
        val endpointUrl = "$UPI_HOST$ENDPOINT_DETAIL"
        val appId = session.appId.ifBlank { DEFAULT_APP_ID }
        val comms = packetCodec.createFreshComms(
            barcode = session.barcode,
            appId = appId,
            liat = session.liat
        )

        val sealedPacket = packetCodec.seal(
            comms = comms,
            reqData = reqData,
            serializer = UpiInvoiceDetailReqDto.serializer(),
            sessionKey = session.ssMe
        )

        executePost(
            endpointPath = ENDPOINT_DETAIL,
            endpointUrl = endpointUrl,
            agentName = "發票明細",
            sealedPacket = sealedPacket,
            requestSummary = mapOf(
                "action" to reqData.action,
                "invNum" to reqData.invNum,
                "invDate" to reqData.invDate,
                "cardNo" to reqData.cardNo
            )
        ) { rawJson ->
            UpiInvoiceWireParser.parseDetailResponse(rawJson)
        }
    }

    private inline fun <T> executePost(
        endpointPath: String,
        endpointUrl: String,
        agentName: String,
        sealedPacket: SealedUpiPacket,
        requestSummary: Map<String, String>,
        parser: (String) -> T
    ): Result<T> {
        val startTime = System.currentTimeMillis()
        var httpStatus = 0
        var rawResponseBody = ""
        var auditError: String? = null
        val headersMap = mutableMapOf<String, String>()

        return try {
            val formBody = FormBody.Builder()
                .add("einvoiceJwt", sealedPacket.token)
                .build()

            val httpRequest = Request.Builder()
                .url(endpointUrl)
                .header("Host", "upi.einvoice.nat.gov.tw")
                .header("User-Agent", "okhttp/5.3.0")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                .post(formBody)
                .build()

            httpRequest.headers.forEach { (name, value) ->
                headersMap[name] = value
            }

            val response = okHttpClient.newCall(httpRequest).execute()
            httpStatus = response.code
            rawResponseBody = extractResponseBodyString(response)

            if (!response.isSuccessful) {
                val ex = UpiProtocolException.TransportFailure(
                    httpStatus = httpStatus,
                    message = "HTTP 請求失敗 ($httpStatus): ${rawResponseBody.ifEmpty { response.message }}"
                )
                auditError = ex.message
                throw ex
            }

            val parsedResult = parser(rawResponseBody)
            Result.success(parsedResult)
        } catch (e: Exception) {
            Log.e(TAG, "UPI API execution failed ($endpointPath): ${e.message}", e)
            if (auditError == null) {
                auditError = e.message ?: "未知例外異常"
            }
            Result.failure(e)
        } finally {
            val durationMs = System.currentTimeMillis() - startTime
            val requestAuditMap = mutableMapOf(
                "endpoint" to endpointPath,
                "jwt_payload" to sealedPacket.payloadJson,
                "signature" to sealedPacket.signatureB64
            ).apply {
                putAll(requestSummary)
            }

            val responseAuditMap = mutableMapOf(
                "status" to httpStatus.toString(),
                "raw_response" to rawResponseBody
            )
            if (auditError != null) {
                responseAuditMap["error"] = auditError
            }

            apiLogManager.logApiInteraction(
                ApiLogRecord(
                    endpoint = "POST $endpointPath",
                    agentName = agentName,
                    method = "POST",
                    status = httpStatus,
                    durationMs = durationMs,
                    request = requestAuditMap,
                    response = responseAuditMap,
                    requestHeaders = headersMap,
                    requestBody = "einvoiceJwt=${sealedPacket.token}",
                    responseBody = rawResponseBody,
                    decryptedPayload = sealedPacket.payloadJson,
                    error = auditError,
                    isSuccess = (auditError == null)
                )
            )
        }
    }

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
                Log.w(TAG, "GZIP decompression fallback failed: ${e.message}")
                String(rawBytes, Charsets.UTF_8)
            }
        } else {
            String(rawBytes, Charsets.UTF_8)
        }
    }
}
