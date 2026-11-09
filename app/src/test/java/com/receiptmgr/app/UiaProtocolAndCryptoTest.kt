package com.receiptmgr.app

import com.receiptmgr.app.data.crypto.ClockProvider
import com.receiptmgr.app.data.crypto.LdataEnvelope
import com.receiptmgr.app.data.crypto.SaltGenerator
import com.receiptmgr.app.data.crypto.UiaKeyDerivation
import com.receiptmgr.app.data.crypto.UiaPacketCodec
import com.receiptmgr.app.data.crypto.UiaSymmetricCipher
import com.receiptmgr.app.domain.model.auth.DeviceFingerprint
import com.receiptmgr.app.domain.model.auth.LoginCommand
import com.receiptmgr.app.domain.model.auth.SessionPayload
import com.receiptmgr.app.domain.model.auth.TargetClientSpec
import com.receiptmgr.app.domain.model.auth.UiaProtocolException
import com.receiptmgr.app.domain.model.auth.UiaResultCode
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class UiaProtocolAndCryptoTest {

    // 1. swapPairs 演算法單元測試
    @Test
    fun testSwapPairs_evenLength() {
        assertEquals("21436587", UiaKeyDerivation.swapPairs("12345678"))
        assertEquals("badcfe", UiaKeyDerivation.swapPairs("abcdef"))
    }

    @Test
    fun testSwapPairs_oddLength() {
        assertEquals("21435", UiaKeyDerivation.swapPairs("12345"))
    }

    // 2. 金鑰派生與 HAR 向量比對
    @Test
    fun testDeriveLdataKey_matchesOfficialVector() {
        val part0 = "NDsZe3cT5ldP42UJ"
        val part2 = "rMBLz2Ps36Ea5ATQ"
        val keyString = UiaKeyDerivation.deriveLdataKeyString(part0, part2)

        assertEquals(32, keyString.length)
        assertTrue(keyString.startsWith("DNZs"))
        assertEquals("DNZs3eTcl5Pd24JUMrLB2zsP63aEA5QT", keyString)

        val keyBytes = UiaKeyDerivation.deriveLdataKey(part0, part2)
        assertEquals(32, keyBytes.size)
        assertEquals(keyString, String(keyBytes, Charsets.UTF_8))
    }

    @Test
    fun testDeriveKey_invalidLengthDefense() {
        assertThrows(IllegalArgumentException::class.java) {
            UiaKeyDerivation.deriveLdataKey("short", "rMBLz2Ps36Ea5ATQ")
        }
    }

    // 3. Byte 0 對齊與嚴禁 12-byte Nonce 測試
    @Test
    fun testSymmetricCipher_strictlyByteZeroAligned_andNoNoncePrefix() {
        val cipher = UiaSymmetricCipher()
        val key = UiaKeyDerivation.deriveLdataKey("NDsZe3cT5ldP42UJ", "rMBLz2Ps36Ea5ATQ")

        val plaintextA = """{"phone":"0912345678","password":"SecretPassword123"}"""
        val plaintextB = """{"sid":"kU45-45157039-mTFI","token":"xyzSampleToken999"}"""

        val plainBytesA = plaintextA.toByteArray(Charsets.UTF_8)
        val plainBytesB = plaintextB.toByteArray(Charsets.UTF_8)

        val cipherBytesA = cipher.encrypt(plainBytesA, key)
        val cipherBytesB = cipher.encrypt(plainBytesB, key)

        // 鐵證 1: 密文長度嚴格等於明文 UTF-8 長度 (無 12 位元組 Nonce 前綴，亦無 16 位元組 GCM Auth Tag)
        assertEquals(
            "密文長度必須與明文長度 1:1 精確相等，禁止前置 12-byte Nonce",
            plainBytesA.size,
            cipherBytesA.size
        )

        // 鐵證 2: 兩組明文皆以 '{"' (0x7B 0x22) 開頭，在相同金鑰下從 Byte 0 對齊，故密文前兩位元組完全相等 (XOR 為 0x00 0x00)
        assertEquals("Byte 0 密文必須完全一致", cipherBytesA[0], cipherBytesB[0])
        assertEquals("Byte 1 密文必須完全一致", cipherBytesA[1], cipherBytesB[1])
        val xorByte0 = cipherBytesA[0].toInt() xor cipherBytesB[0].toInt()
        val xorByte1 = cipherBytesA[1].toInt() xor cipherBytesB[1].toInt()
        assertEquals(0, xorByte0)
        assertEquals(0, xorByte1)

        // 鐵證 3: 往返解密完全保真
        val decryptedA = cipher.decrypt(cipherBytesA, key)
        val decryptedB = cipher.decrypt(cipherBytesB, key)
        assertEquals(plaintextA, String(decryptedA, Charsets.UTF_8))
        assertEquals(plaintextB, String(decryptedB, Charsets.UTF_8))
    }

    // 4. LdataEnvelope 三段式格式測試
    @Test
    fun testLdataEnvelope_encodeAndParse() {
        val part0 = "NDsZe3cT5ldP42UJ"
        val part1 = "SampleBase64=="
        val part2 = "rMBLz2Ps36Ea5ATQ"

        val envelope = LdataEnvelope(part0, part1, part2)
        val encoded = envelope.encode()
        assertEquals("$part0|$part1|$part2", encoded)

        val parsed = LdataEnvelope.parse(encoded)
        assertEquals(part0, parsed.part0)
        assertEquals(part1, parsed.part1Base64)
        assertEquals(part2, parsed.part2)
    }

    @Test
    fun testLdataEnvelope_malformedDefense() {
        assertThrows(IllegalArgumentException::class.java) {
            LdataEnvelope.parse("invalid|two_parts_only")
        }
        assertThrows(IllegalArgumentException::class.java) {
            LdataEnvelope("short", "base64", "1234567890123456")
        }
    }

    // 5. UiaPacketCodec 協同測試 (即時時間戳與 6.800.11 目標版號)
    @Test
    fun testPacketCodec_sealsWithInjectedRealTimeTimestamp_andOfficialAppVersion() {
        val fixedTimestamp = 1794134994L
        val mockClock = ClockProvider { fixedTimestamp }
        var saltCounter = 0
        val mockSalt = SaltGenerator { length ->
            saltCounter++
            if (saltCounter % 2 == 1) "NDsZe3cT5ldP42UJ" else "rMBLz2Ps36Ea5ATQ"
        }

        val codec = UiaPacketCodec(
            cipher = UiaSymmetricCipher(),
            saltGenerator = mockSalt,
            clockProvider = mockClock
        )

        val command = LoginCommand(
            phone = "0912345678",
            password = "TestPassword",
            clientSpec = TargetClientSpec(), // 預設 appId = "EINV6202512184519", appVersion = "6.800.11"
            deviceFingerprint = DeviceFingerprint(
                deviceModel = "SM-A315G",
                osVersion = "14",
                androidUuid = "c5f4acd16fc113f8"
            )
        )

        val sealed = codec.sealLoginCommand(command)

        // 驗證時間戳即時注入
        assertEquals(fixedTimestamp, sealed.timestamp)
        assertTrue(sealed.plaintextJson.contains("\"timestamp\":1794134994"))

        // 驗證 App 版本號為官方目標規格 "6.800.11"，而非 "1.0.0"
        assertTrue(sealed.plaintextJson.contains("\"app_version\":\"6.800.11\""))
        assertFalse(sealed.plaintextJson.contains("\"app_version\":\"1.0.0\""))

        // 驗證 ldata 三段拓撲
        assertEquals("NDsZe3cT5ldP42UJ", sealed.envelope.part0)
        assertEquals("rMBLz2Ps36Ea5ATQ", sealed.envelope.part2)

        // 驗證 Base64 密文可被對稱解密還原
        val cipher = UiaSymmetricCipher()
        val decryptedJson = cipher.decryptFromBase64(sealed.envelope.part1Base64, sealed.aesKey)
        assertEquals(sealed.plaintextJson, decryptedJson)
    }

    // 6. 伺服器 Response Payload 解密與會話憑證模型對接
    @Test
    fun testPacketCodec_unsealResponsePayload() {
        val cipher = UiaSymmetricCipher()
        val key = UiaKeyDerivation.deriveLdataKey("NDsZe3cT5ldP42UJ", "rMBLz2Ps36Ea5ATQ")

        val serverResponseJson = """
            {
              "sid": "kU45-45157039-mTFI",
              "token": "w+GN4RUXZo1X63xufP/xj/Bhfpx9foGX213lzXbYgPg=",
              "svr_code": "0000",
              "iv": "31323334353637383930313233343536",
              "liat": 1794134995,
              "app_id": "EINV6202512184519",
              "ss_me": "VsE82SBRFbSpcn7-2BTdP1RCwwH2h9h4WbIpq1pTq9g"
            }
        """.trimIndent()

        val encryptedPayloadB64 = cipher.encryptToBase64(serverResponseJson, key)

        val codec = UiaPacketCodec(cipher = cipher)
        val (sessionPayload, rawJson) = codec.unsealSessionPayload(encryptedPayloadB64, key)

        assertEquals("kU45-45157039-mTFI", sessionPayload.sid)
        assertEquals("VsE82SBRFbSpcn7-2BTdP1RCwwH2h9h4WbIpq1pTq9g", sessionPayload.ssMe)
        assertEquals(1794134995L, sessionPayload.liat)
        assertEquals("0000", sessionPayload.svrCode)
        assertNotNull(rawJson)
    }

    // 7. 伺服器狀態碼列舉與例外映射測試
    @Test
    fun testUiaResultCode_mapping() {
        assertEquals(UiaResultCode.SUCCESS, UiaResultCode.fromCode(0))
        assertEquals(UiaResultCode.TIMESTAMP_EXPIRED, UiaResultCode.fromCode(-204))
        assertEquals(UiaResultCode.DECRYPTION_FAILED, UiaResultCode.fromCode(-205))
        assertEquals(UiaResultCode.UNKNOWN, UiaResultCode.fromCode(-999))
    }

    @Test
    fun testUiaProtocolException_types() {
        val ex204 = UiaProtocolException.TimestampExpired("id123", "過期")
        assertTrue(ex204.message?.contains("-204") == true)

        val ex205 = UiaProtocolException.DecryptionFailed("id123", "無法解密")
        assertTrue(ex205.message?.contains("-205") == true)
    }
}
