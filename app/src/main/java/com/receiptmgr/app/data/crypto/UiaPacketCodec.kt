package com.receiptmgr.app.data.crypto

import com.receiptmgr.app.domain.model.auth.LoginCommand
import com.receiptmgr.app.domain.model.auth.SessionPayload
import com.receiptmgr.app.domain.model.auth.UiaProtocolException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * 官方 UIA 登入設備資訊 Wire DTO
 */
@Serializable
data class UiaDeviceWireDto(
    @SerialName("os")
    val os: String = "a",
    @SerialName("os_version")
    val osVersion: String,
    @SerialName("model")
    val model: String,
    @SerialName("pdid")
    val pdid: String,
    @SerialName("ptoken")
    val ptoken: String,
    @SerialName("appver")
    val appver: String,
    @SerialName("appbn")
    val appbn: Int,
    @SerialName("lang")
    val lang: String = "zh",
    @SerialName("ccs")
    val ccs: String = "tw"
)

/**
 * 官方 UIA 登入請求明文 Wire DTO (428 位元組標準規格字典)
 */
@Serializable
data class UiaLoginWirePayloadDto(
    @SerialName("type")
    val type: Int = 0,
    @SerialName("account")
    val account: String,
    @SerialName("password")
    val password: String,
    @SerialName("ckey")
    val ckey: String,
    @SerialName("device")
    val device: UiaDeviceWireDto,
    @SerialName("ts")
    val ts: Long,
    @SerialName("pdid")
    val pdid: String
)

/**
 * 登入請求封裝完成之封包物件
 */
data class SealedLdataPacket(
    val envelope: LdataEnvelope,
    val ckey: String,
    val aesKey: ByteArray,
    val iv: ByteArray,
    val plaintextJson: String,
    val timestamp: Long
) {
    val ldataString: String get() = envelope.encode()

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as SealedLdataPacket
        if (envelope != other.envelope) return false
        if (ckey != other.ckey) return false
        if (!aesKey.contentEquals(other.aesKey)) return false
        if (!iv.contentEquals(other.iv)) return false
        if (plaintextJson != other.plaintextJson) return false
        if (timestamp != other.timestamp) return false
        return true
    }

    override fun hashCode(): Int {
        var result = envelope.hashCode()
        result = 31 * result + ckey.hashCode()
        result = 31 * result + aesKey.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        result = 31 * result + plaintextJson.hashCode()
        result = 31 * result + timestamp.hashCode()
        return result
    }
}

/**
 * 官方 UIA 封包編解碼協調器
 * 統籌 ckey 生成、即時時間戳注入、SHA-256 金鑰派生、AES-GCM 加密與伺服器回應解密。
 */
class UiaPacketCodec(
    private val cipher: UiaGcmCipher = UiaGcmCipher(),
    private val saltGenerator: SaltGenerator = SecureRandomSaltGenerator(),
    private val clockProvider: ClockProvider = SystemClockProvider(),
    private val json: Json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
) {

    /**
     * 封裝並加密登入指令為官方標準三段式 ldata 封包。
     * 在執行封裝當下即時注入真實系統秒數 (ts) 與客戶端種子 (ckey)，防止 -204 重放攔截。
     */
    fun sealLoginCommand(command: LoginCommand): SealedLdataPacket {
        val currentTimestamp = clockProvider.nowEpochSeconds()
        val ckey = saltGenerator.nextSalt(16)

        val deviceWireDto = UiaDeviceWireDto(
            os = command.os,
            osVersion = command.osVersion,
            model = command.model,
            pdid = command.pdid,
            ptoken = command.ptoken,
            appver = command.appver,
            appbn = command.appbn,
            lang = command.lang,
            ccs = command.ccs
        )

        val wirePayload = UiaLoginWirePayloadDto(
            type = command.type,
            account = command.account,
            password = command.password,
            ckey = ckey,
            device = deviceWireDto,
            ts = currentTimestamp,
            pdid = command.pdid
        )

        val plaintextJson = json.encodeToString(wirePayload)

        // 1. 生成前後各 16 字元隨機英數鹽值
        val part0 = saltGenerator.nextSalt(16)
        val part2 = saltGenerator.nextSalt(16)

        // 2. 官方 ApiCryptoKt 派生 AES Key (32 bytes) 與 IV (12 bytes)
        val aesKey = UiaKeyDerivation.deriveLdataKey(part0, part2)
        val iv = UiaKeyDerivation.deriveLdataIv(part0, part2)

        // 3. 官方標準 AES-GCM (128-bit Tag) 加密並輸出 Base64
        val part1Base64 = cipher.encryptToBase64(plaintextJson, aesKey, iv)

        val envelope = LdataEnvelope(
            part0 = part0,
            part1Base64 = part1Base64,
            part2 = part2
        )

        return SealedLdataPacket(
            envelope = envelope,
            ckey = ckey,
            aesKey = aesKey,
            iv = iv,
            plaintextJson = plaintextJson,
            timestamp = currentTimestamp
        )
    }

    /**
     * 解密伺服器下發之 payload 密文並還原 SessionPayload 物件。
     */
    fun unsealSessionPayload(
        encryptedPayloadBase64: String,
        aesKey: ByteArray,
        iv: ByteArray
    ): Pair<SessionPayload, String> {
        try {
            val decryptedJson = cipher.decryptFromBase64(encryptedPayloadBase64, aesKey, iv)
            val sessionPayload = json.decodeFromString<SessionPayload>(decryptedJson)
            return Pair(sessionPayload, decryptedJson)
        } catch (e: Exception) {
            throw UiaProtocolException.MalformedPayload("伺服器 Session Payload 解密或格式解析失敗: ${e.message}", e)
        }
    }
}
