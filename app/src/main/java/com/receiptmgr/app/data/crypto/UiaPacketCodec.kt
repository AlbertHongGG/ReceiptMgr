package com.receiptmgr.app.data.crypto

import com.receiptmgr.app.domain.model.auth.LoginCommand
import com.receiptmgr.app.domain.model.auth.SessionPayload
import com.receiptmgr.app.domain.model.auth.UiaProtocolException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 登入請求封裝完成之封包物件
 */
data class SealedLdataPacket(
    val envelope: LdataEnvelope,
    val aesKey: ByteArray,
    val aesKeyString: String,
    val plaintextJson: String,
    val timestamp: Long
) {
    val ldataString: String get() = envelope.encode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SealedLdataPacket
        if (envelope != other.envelope) return false
        if (!aesKey.contentEquals(other.aesKey)) return false
        if (aesKeyString != other.aesKeyString) return false
        if (plaintextJson != other.plaintextJson) return false
        if (timestamp != other.timestamp) return false
        return true
    }

    override fun hashCode(): Int {
        var result = envelope.hashCode()
        result = 31 * result + aesKey.contentHashCode()
        result = 31 * result + aesKeyString.hashCode()
        result = 31 * result + plaintextJson.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * 內部通訊協定傳輸 Wire DTO (精確對齊官方 API 參數規格)
 */
@Serializable
internal data class UiaLoginWirePayloadDto(
    @SerialName("phone")
    val phone: String,
    @SerialName("password")
    val password: String,
    @SerialName("app_id")
    val appId: String,
    @SerialName("app_version")
    val appVersion: String,
    @SerialName("device_model")
    val deviceModel: String,
    @SerialName("os_version")
    val osVersion: String,
    @SerialName("uuid")
    val uuid: String,
    @SerialName("timestamp")
    val timestamp: Long,
    @SerialName("login_mod")
    val loginMod: String = "0"
)

/**
 * UIA 封包編解碼協調器
 * 統籌隨機鹽產生、即時時間戳注入、Byte 0 對齊加密與伺服器回應解密封裝。
 */
class UiaPacketCodec(
    private val cipher: UiaSymmetricCipher = UiaSymmetricCipher(),
    private val saltGenerator: SaltGenerator = SecureRandomSaltGenerator(),
    private val clockProvider: ClockProvider = SystemClockProvider(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {

    /**
     * 封裝並加密登入指令為三段式 ldata 封包。
     * 在執行封裝當下即時注入真實時間戳，防範 -204 重放攔截。
     */
    fun sealLoginCommand(command: LoginCommand): SealedLdataPacket {
        val currentTimestamp = clockProvider.nowEpochSeconds()

        val wirePayload = UiaLoginWirePayloadDto(
            phone = command.phone,
            password = command.password,
            appId = command.appId,
            appVersion = command.appVersion,
            deviceModel = command.deviceModel,
            osVersion = command.osVersion,
            uuid = command.uuid,
            timestamp = currentTimestamp,
            loginMod = command.loginMod
        )

        val plaintextJson = json.encodeToString(wirePayload)

        // 1. 生成前後各 16 字元隨機英數鹽值
        val part0 = saltGenerator.nextSalt(16)
        val part2 = saltGenerator.nextSalt(16)

        // 2. 派生 256 位元 AES 金鑰
        val aesKey = UiaKeyDerivation.deriveLdataKey(part0, part2)
        val aesKeyString = UiaKeyDerivation.deriveLdataKeyString(part0, part2)

        // 3. Byte 0 對齊加密 (直接以密文二進位 Base64 編碼，無 Nonce 前置)
        val part1Base64 = cipher.encryptToBase64(plaintextJson, aesKey)

        val envelope = LdataEnvelope(
            part0 = part0,
            part1Base64 = part1Base64,
            part2 = part2
        )

        return SealedLdataPacket(
            envelope = envelope,
            aesKey = aesKey,
            aesKeyString = aesKeyString,
            plaintextJson = plaintextJson,
            timestamp = currentTimestamp
        )
    }

    /**
     * 解密伺服器下發之 payload 密文並還原 SessionPayload 物件。
     */
    fun unsealSessionPayload(encryptedPayloadBase64: String, aesKey: ByteArray): Pair<SessionPayload, String> {
        try {
            val decryptedJson = cipher.decryptFromBase64(encryptedPayloadBase64, aesKey)
            val sessionPayload = json.decodeFromString<SessionPayload>(decryptedJson)
            return Pair(sessionPayload, decryptedJson)
        } catch (e: Exception) {
            throw UiaProtocolException.MalformedPayload("伺服器 Session Payload 解密或格式解析失敗: ${e.message}", e)
        }
    }
}
