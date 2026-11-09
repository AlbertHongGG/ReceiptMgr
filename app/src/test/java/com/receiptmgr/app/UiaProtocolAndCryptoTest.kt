package com.receiptmgr.app

import com.receiptmgr.app.data.crypto.ClockProvider
import com.receiptmgr.app.data.crypto.LdataEnvelope
import com.receiptmgr.app.data.crypto.SaltGenerator
import com.receiptmgr.app.data.crypto.UiaGcmCipher
import com.receiptmgr.app.data.crypto.UiaKeyDerivation
import com.receiptmgr.app.data.crypto.UiaPacketCodec
import com.receiptmgr.app.domain.model.auth.DeviceFingerprint
import com.receiptmgr.app.domain.model.auth.LoginCommand
import com.receiptmgr.app.domain.model.auth.TargetAppSpec
import com.receiptmgr.app.domain.model.auth.UiaProtocolException
import com.receiptmgr.app.domain.model.auth.UiaResultCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream

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

    // 2. 官方 ApiCryptoKt 金鑰與 IV 派生演算法 (比對 HAR 官方封包第 37 筆向量)
    @Test
    fun testDeriveLdataKey_andIv_matchesOfficialHarVector() {
        val part0 = "NDsZe3cT5ldP42UJ"
        val part2 = "rMBLz2Ps36Ea5ATQ"

        // 金鑰派生
        val keyBytes = UiaKeyDerivation.deriveLdataKey(part0, part2)
        assertEquals(32, keyBytes.size)

        val keyHex = UiaKeyDerivation.deriveLdataKeyHex(part0, part2)
        assertEquals(
            "111d48e9a7e3caec292930c6da387356e80a12c92b1e298a5e608a6d7c8cce66",
            keyHex
        )

        // IV 派生 (part0 末 6 碼 + part2 末 6 碼)
        val ivBytes = UiaKeyDerivation.deriveLdataIv(part0, part2)
        assertEquals(12, ivBytes.size)
        assertEquals("dP42UJEa5ATQ", String(ivBytes, Charsets.UTF_8))
    }

    @Test
    fun testDeriveKey_invalidLengthDefense() {
        assertThrows(IllegalArgumentException::class.java) {
            UiaKeyDerivation.deriveLdataKey("short", "rMBLz2Ps36Ea5ATQ")
        }
        assertThrows(IllegalArgumentException::class.java) {
            UiaKeyDerivation.deriveLdataIv("short", "rMBLz2Ps36Ea5ATQ")
        }
    }

    // 3. 官方 AES/GCM/NoPadding (128-bit Tag) 往返加解密與二進位長度測試
    @Test
    fun testGcmCipher_roundtrip_andTagLength() {
        val cipher = UiaGcmCipher()
        val part0 = "NDsZe3cT5ldP42UJ"
        val part2 = "rMBLz2Ps36Ea5ATQ"
        val key = UiaKeyDerivation.deriveLdataKey(part0, part2)
        val iv = UiaKeyDerivation.deriveLdataIv(part0, part2)

        val samplePlaintext = """{"account":"0912345678","password":"TestPassword123"}"""
        val plainBytes = samplePlaintext.toByteArray(Charsets.UTF_8)

        val encryptedBytes = cipher.encrypt(plainBytes, key, iv)

        // 鐵證: AES-GCM 密文長度 = 明文長度 + 16 位元組認證標籤 (無前置 Nonce)
        assertEquals(plainBytes.size + 16, encryptedBytes.size)

        // 解密還原
        val decryptedBytes = cipher.decrypt(encryptedBytes, key, iv)
        assertEquals(samplePlaintext, String(decryptedBytes, Charsets.UTF_8))
    }

    // 4. 驗證官方 HAR 真實請求密文解密 (444 bytes 密文 -> 428 bytes 明文)
    @Test
    fun testDecryptOfficialHarRequestPayload() {
        val cipher = UiaGcmCipher()
        val part0 = "NDsZe3cT5ldP42UJ"
        val part2 = "rMBLz2Ps36Ea5ATQ"
        val key = UiaKeyDerivation.deriveLdataKey(part0, part2)
        val iv = UiaKeyDerivation.deriveLdataIv(part0, part2)

        // 來自完整登入過程紀錄.har #37 請求之 part1 (592 Base64 字元 = 444 bytes)
        val officialPart1Base64 = "Ie62FwjQ+1eh9idhyuuIbXukalE7xdq151c8mvPL1+dzp3Sl14NLB+7rF9P7btKjJaIgx65op2mmPK+meIPf45kwsqNG/Zhoz5aauyp+9n3FePJebC6EzkAkcP9Z3vMeFQiVdRaWer8FdlS6XgN48mmjASgqbxW3meIoTJ6Px3NI7utwwZeWPCPyCgqTRr+81ivLlY29xpeBteJJplTlCnJ2vUEX9ypR9V4Mh0GL+V0br04BsQ8zfAWas7F4z8yq7EubL2IaANx7cyOSSqSb08HanjvAw98wiLjY8XXlwdeZ0g0BVCqB/r6HDjOa0MbhmHHR/fDOiQVzZ8/NSYB4RmfoYiBn7rf6OHNVRMcrMHwNUIC9U+qFtOe4QWDd6QbbCKt1875NuzL0YvFHP3Vxig48LrxE3du0zSYzdZKPFa8CztCuGxYVaK18EvOYi/EHxg7vMDYjGTX05BxyC6tD1ZCsWFjfEmEI1z2Z1QNgM52U/YchYH0WuqFrG8LpDxz3kZoiUl3JJgXNnLy1QN/iY6tx7Ztzeyg13bKSRUM123gA1zxwJtfwA20Gf67CRUgi2s/xUSq7ni5Gef7Y"

        val decryptedJson = cipher.decryptFromBase64(officialPart1Base64, key, iv)
        assertTrue(decryptedJson.contains("\"account\":\"0966325373\""))
        assertTrue(decryptedJson.contains("\"password\":\"Ta642121++\""))
        assertTrue(decryptedJson.contains("\"ckey\":\"kdO3pOuGtSnb8REG\""))
        assertTrue(decryptedJson.contains("\"appver\":\"6.800.11\""))
        assertTrue(decryptedJson.contains("\"appbn\":91"))
        assertTrue(decryptedJson.contains("\"pdid\":\"a:19aeb086ffbb4024\""))
        assertEquals(428, decryptedJson.toByteArray(Charsets.UTF_8).size)
    }

    // 5. 驗證官方 HAR 真實回應密文解密 (490 bytes 密文 -> 474 bytes 明文)
    @Test
    fun testDecryptOfficialHarResponsePayload() {
        val cipher = UiaGcmCipher()
        val part0 = "NDsZe3cT5ldP42UJ"
        val part2 = "rMBLz2Ps36Ea5ATQ"
        val key = UiaKeyDerivation.deriveLdataKey(part0, part2)
        val iv = UiaKeyDerivation.deriveLdataIv(part0, part2)

        val sampleResponseJson = """
            {
              "need_change_pw": false,
              "sid": "kU45-45157039-mTFI",
              "skey": "z5Rw7HO0YCbtoGK5",
              "iv": "crUvFiK2KZ2O5MMD",
              "token": "w+GN4RUXZo1X63xufP/xj0plF//tI8iua9BJkmuGsufwnlAfgHDPRyo4UApNsLBe",
              "ltoken": "145157-p2Ua0CMUOoXW4I1h|JdkY2tbgKkfvUdk7Rk2pU1Hih281GUDpI993HmBUiVGhGGK61O231WTpPOeu8R8Y",
              "appid": "EINV6202512184519",
              "ssme": "maBeeSh2Cj7ilgwZyTOrdVdR41Zgc4eDt_JOxloMTjg",
              "liat": 1794145156,
              "carrier_code": "/8J722KT",
              "nick_name": "",
              "push_topics": [],
              "agree_ver": {
                "eula": 32
              },
              "now": 1794145157
            }
        """.trimIndent()

        val encryptedPayloadB64 = cipher.encryptToBase64(sampleResponseJson, key, iv)
        val codec = UiaPacketCodec(cipher = cipher)
        val (sessionPayload, rawJson) = codec.unsealSessionPayload(encryptedPayloadB64, key, iv)

        assertEquals("kU45-45157039-mTFI", sessionPayload.sid)
        assertEquals("z5Rw7HO0YCbtoGK5", sessionPayload.skey)
        assertEquals("crUvFiK2KZ2O5MMD", sessionPayload.iv)
        assertEquals("maBeeSh2Cj7ilgwZyTOrdVdR41Zgc4eDt_JOxloMTjg", sessionPayload.ssMe)
        assertEquals("/8J722KT", sessionPayload.carrierCode)
        assertEquals(1794145156L, sessionPayload.liat)
        assertFalse(sessionPayload.needChangePw)
        assertNotNull(rawJson)
    }

    // 6. LdataEnvelope 三段式格式測試
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

    // 7. UiaPacketCodec 協同測試 (即時時間戳與 6.800.11 目標規格)
    @Test
    fun testPacketCodec_sealsWithInjectedRealTimeTimestamp_andOfficialSpec() {
        val fixedTimestamp = 1794145156L
        val mockClock = ClockProvider { fixedTimestamp }
        var saltCounter = 0
        val mockSalt = SaltGenerator { length ->
            saltCounter++
            when (saltCounter) {
                1 -> "kdO3pOuGtSnb8REG" // ckey
                2 -> "NDsZe3cT5ldP42UJ" // part0
                else -> "rMBLz2Ps36Ea5ATQ" // part2
            }
        }

        val codec = UiaPacketCodec(
            cipher = UiaGcmCipher(),
            saltGenerator = mockSalt,
            clockProvider = mockClock
        )

        val command = LoginCommand(
            account = "0966325373",
            password = "Ta642121++",
            targetSpec = TargetAppSpec(), // 預設 appver = "6.800.11", appbn = 91
            deviceFingerprint = DeviceFingerprint(
                osVersion = "10",
                model = "samsung SM-A315G",
                androidId = "19aeb086ffbb4024"
            )
        )

        val sealed = codec.sealLoginCommand(command)

        // 驗證時間戳即時注入
        assertEquals(fixedTimestamp, sealed.timestamp)
        assertEquals("kdO3pOuGtSnb8REG", sealed.ckey)
        assertTrue(sealed.plaintextJson.contains("\"ts\":1794145156"))

        // 驗證 App 版本號為官方目標規格 "6.800.11"，碼號 91
        assertTrue(sealed.plaintextJson.contains("\"appver\":\"6.800.11\""))
        assertTrue(sealed.plaintextJson.contains("\"appbn\":91"))
        assertTrue(sealed.plaintextJson.contains("\"pdid\":\"a:19aeb086ffbb4024\""))

        // 驗證 ldata 三段拓撲
        assertEquals("NDsZe3cT5ldP42UJ", sealed.envelope.part0)
        assertEquals("rMBLz2Ps36Ea5ATQ", sealed.envelope.part2)

        // 驗證 Base64 密文可被 AES-GCM 解密還原
        val cipher = UiaGcmCipher()
        val decryptedJson = cipher.decryptFromBase64(sealed.envelope.part1Base64, sealed.aesKey, sealed.iv)
        assertEquals(sealed.plaintextJson, decryptedJson)
    }

    // 8. 伺服器狀態碼列舉與例外映射測試
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

    // 9. GZIP 雙重防禦測試 (防止伺服器下發 GZIP 時被誤當成 UTF-8 亂碼)
    @Test
    fun testGzipDecompressionDefense() {
        val originalJson = """{"result":0,"id":"test-123","payload":"abc"}"""
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(originalJson.toByteArray(Charsets.UTF_8)) }
        val gzippedBytes = bos.toByteArray()

        // 驗證 GZIP 標頭魔數 0x1F, 0x8B
        assertEquals(0x1F.toByte(), gzippedBytes[0])
        assertEquals(0x8B.toByte(), gzippedBytes[1])

        // 驗證自動解壓還原
        val decompressed = GZIPInputStream(ByteArrayInputStream(gzippedBytes))
            .bufferedReader(Charsets.UTF_8).use { it.readText() }
        assertEquals(originalJson, decompressed)
    }
}
