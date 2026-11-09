package com.receiptmgr.app.domain.model.invoice

/**
 * 財政部 UPI 電子發票協定強型別領域例外體系。
 */
sealed class UpiProtocolException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    /**
     * 未登入或會話已過期，無法提取 ss_me 與載具憑證
     */
    class SessionUnavailable(
        message: String = "載具會話不存在或已過期，請重新登入"
    ) : UpiProtocolException(message)

    /**
     * 伺服器業務拒絕 (例如代碼非 200，參數格式錯誤或載具無效)
     */
    class BusinessRejected(
        val code: String,
        val serverMessage: String
    ) : UpiProtocolException("財政部伺服器業務拒絕 (代碼: $code): $serverMessage")

    /**
     * 回傳資料損毀或格式不符合規格
     */
    class MalformedPayload(
        message: String,
        cause: Throwable? = null
    ) : UpiProtocolException(message, cause)

    /**
     * HTTP 網路傳輸層或連線逾時失敗
     */
    class TransportFailure(
        val httpStatus: Int,
        message: String,
        cause: Throwable? = null
    ) : UpiProtocolException(message, cause)
}
