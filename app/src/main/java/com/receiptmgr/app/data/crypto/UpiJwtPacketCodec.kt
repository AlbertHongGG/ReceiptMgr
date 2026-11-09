package com.receiptmgr.app.data.crypto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * UPI 通訊認證上下文模型 (UpiComms)。
 * 財政部規範：exp - iat 必須嚴格等於 40 秒。
 */
@Serializable
data class UpiComms(
    @SerialName("iat") val iat: Long,
    @SerialName("exp") val exp: Long,
    @SerialName("liat") val liat: Long,
    @SerialName("appid") val appId: String,
    @SerialName("barcode") val barcode: String,
    @SerialName("keyType") val keyType: String = "2"
) {
    init {
        val window = exp - iat
        require(window == 40L) { "UPI 協定要求 exp - iat 必須嚴格等於 40 秒，目前為: ${window}s" }
    }
}

/**
 * UPI Payload 根外殼模型
 */
@Serializable
data class UpiPayloadEnvelope<T>(
    @SerialName("comms") val comms: UpiComms,
    @SerialName("reqdata") val reqData: T
)

/**
 * 已簽署之 UPI 封包實體
 */
data class SealedUpiPacket(
    val token: String,
    val headerJson: String,
    val payloadJson: String,
    val signatureB64: String
)

/**
 * 財政部 UPI einvoiceJwt 封包序列化與 HMAC-SHA256 簽章引擎。
 */
class UpiJwtPacketCodec(
    private val clockProvider: ClockProvider = SystemClockProvider(),
    private val json: Json = Json {
        encodeDefaults = true
        ignoreUnknownKeys = true
    }
) {
    companion object {
        private const val HMAC_SHA256 = "HmacSHA256"
        private const val JWT_HEADER_JSON = """{"alg":"HS256","typ":"JWT","ver":"1.0"}"""
        private const val DEFAULT_EXPIRY_WINDOW_SECONDS = 40L
        private val base64UrlEncoder = Base64.getUrlEncoder().withoutPadding()
        private val base64UrlDecoder = Base64.getUrlDecoder()
    }

    /**
     * 建立新鮮之通訊上下文 (exp = iat + 40s)
     */
    fun createFreshComms(
        barcode: String,
        appId: String,
        liat: Long
    ): UpiComms {
        val now = clockProvider.nowEpochSeconds()
        return UpiComms(
            iat = now,
            exp = now + DEFAULT_EXPIRY_WINDOW_SECONDS,
            liat = liat,
            appId = appId,
            barcode = barcode,
            keyType = "2"
        )
    }

    /**
     * 以給定之 Session Key (ss_me) 序列化並簽署請求
     */
    fun <T> seal(
        comms: UpiComms,
        reqData: T,
        serializer: KSerializer<T>,
        sessionKey: String
    ): SealedUpiPacket {
        require(sessionKey.isNotBlank()) { "Session Key 不能為空" }

        val envelope = UpiPayloadEnvelope(comms = comms, reqData = reqData)
        val envelopeSerializer = UpiPayloadEnvelope.serializer(serializer)
        val payloadJson = json.encodeToString(envelopeSerializer, envelope)

        val headerB64 = base64UrlEncode(JWT_HEADER_JSON.toByteArray(Charsets.UTF_8))
        val payloadB64 = base64UrlEncode(payloadJson.toByteArray(Charsets.UTF_8))
        val signingInput = "$headerB64.$payloadB64"

        val keyBytes = sessionKey.toByteArray(Charsets.UTF_8)
        val sigBytes = calculateHmacSha256(signingInput.toByteArray(Charsets.US_ASCII), keyBytes)
        val sigB64 = base64UrlEncode(sigBytes)

        val fullToken = "$signingInput.$sigB64"

        return SealedUpiPacket(
            token = fullToken,
            headerJson = JWT_HEADER_JSON,
            payloadJson = payloadJson,
            signatureB64 = sigB64
        )
    }

    /**
     * 驗證 einvoiceJwt 簽章之合法性 (單元測試與安全校驗用)
     */
    fun verify(token: String, sessionKey: String): Boolean {
        val parts = token.trim().split(".")
        if (parts.size != 3) return false
        val signingInput = "${parts[0]}.${parts[1]}"
        val keyBytes = sessionKey.toByteArray(Charsets.UTF_8)
        val expectedSig = base64UrlEncode(
            calculateHmacSha256(signingInput.toByteArray(Charsets.US_ASCII), keyBytes)
        )
        return parts[2] == expectedSig
    }

    private fun calculateHmacSha256(data: ByteArray, key: ByteArray): ByteArray {
        val mac = Mac.getInstance(HMAC_SHA256)
        mac.init(SecretKeySpec(key, HMAC_SHA256))
        return mac.doFinal(data)
    }

    fun base64UrlEncode(data: ByteArray): String {
        return base64UrlEncoder.encodeToString(data)
    }

    fun base64UrlDecode(str: String): ByteArray {
        return base64UrlDecoder.decode(str)
    }
}
