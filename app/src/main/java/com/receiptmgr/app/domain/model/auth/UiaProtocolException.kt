package com.receiptmgr.app.domain.model.auth

/**
 * UIA 伺服器狀態碼列舉
 */
enum class UiaResultCode(val code: Int, val description: String) {
    SUCCESS(0, "成功"),
    TIMESTAMP_EXPIRED(-204, "伺服器時間戳過期或觸發重放攔截"),
    DECRYPTION_FAILED(-205, "伺服器無法解密請求密文 (格式或金鑰對齊錯誤)"),
    UNKNOWN(-999, "未知伺服器狀態碼");

    companion object {
        fun fromCode(code: Int): UiaResultCode {
            return entries.firstOrNull { it.code == code } ?: UNKNOWN
        }
    }
}

/**
 * UIA 協定層強型別例外體系
 */
sealed class UiaProtocolException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * 代碼 -204: 伺服器解密成功，但 timestamp 過期或觸發 anti-replay 機制
     */
    class TimestampExpired(
        val serverId: String?,
        val serverMessage: String?
    ) : UiaProtocolException(
        "伺服器時間戳過期或重放攔截 (代碼: -204, id: ${serverId ?: "無"}, 訊息: ${serverMessage ?: "無"})"
    )

    /**
     * 代碼 -205: 伺服器自 Byte 0 解密失敗 (非 Byte 0 對齊、含 Nonce 前置或金鑰錯誤)
     */
    class DecryptionFailed(
        val serverId: String?,
        val serverMessage: String?
    ) : UiaProtocolException(
        "伺服器密文解密失敗 (代碼: -205, id: ${serverId ?: "無"}, 訊息: ${serverMessage ?: "無"})"
    )

    /**
     * 其他伺服器業務拒絕代碼 (如密碼錯誤、帳號鎖定等)
     */
    class ServerRejected(
        val resultCode: Int,
        val serverId: String?,
        val serverMessage: String?
    ) : UiaProtocolException(
        "伺服器拒絕登入 (代碼: $resultCode, id: ${serverId ?: "無"}, 訊息: ${serverMessage ?: "無"})"
    )

    /**
     * 密文解密異常或 Payload 格式損毀
     */
    class MalformedPayload(
        message: String,
        cause: Throwable? = null
    ) : UiaProtocolException(message, cause)

    /**
     * HTTP 傳輸層失敗 (如 404, 500, 502 等)
     */
    class TransportFailure(
        val httpStatus: Int,
        val rawBody: String,
        message: String = "HTTP 請求失敗 (狀態碼: $httpStatus)"
    ) : UiaProtocolException(message)
}
