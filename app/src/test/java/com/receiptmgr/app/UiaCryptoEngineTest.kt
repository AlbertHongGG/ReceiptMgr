package com.receiptmgr.app

import com.receiptmgr.app.data.crypto.UiaCryptoEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UiaCryptoEngineTest {

    @Test
    fun testSwapPairs_evenLength() {
        val input = "12345678"
        val expected = "21436587"
        assertEquals(expected, UiaCryptoEngine.swapPairs(input))

        val letters = "abcdef"
        val expectedLetters = "badcfe"
        assertEquals(expectedLetters, UiaCryptoEngine.swapPairs(letters))
    }

    @Test
    fun testSwapPairs_oddLength() {
        val input = "12345"
        val expected = "21435"
        assertEquals(expected, UiaCryptoEngine.swapPairs(input))
    }

    @Test
    fun testDeriveLoginAesKey_matchesReferenceVector() {
        val part0 = "NDsZe3cT5ldP42UJ"
        val part2 = "rMBLz2Ps36Ea5ATQ"
        val derivedKey = UiaCryptoEngine.deriveLoginAesKey(part0, part2)

        assertEquals(32, derivedKey.length)
        assertTrue(derivedKey.startsWith("DNZs"))
        assertEquals("DNZs3eTcl5Pd24JUMrLB2zsP63aEA5QT", derivedKey)
    }

    @Test
    fun testPackAndUnpackLdata() {
        val part0 = "1234567890123456"
        val part1 = "Base64Ciphertext=="
        val part2 = "6543210987654321"

        val packed = UiaCryptoEngine.packLdata(part0, part1, part2)
        assertEquals("$part0|$part1|$part2", packed)

        val (p0, p1, p2) = UiaCryptoEngine.unpackLdata(packed)
        assertEquals(part0, p0)
        assertEquals(part1, p1)
        assertEquals(part2, p2)
    }

    @Test
    fun testAesGcm_encryptAndDecryptRoundTrip() {
        val part0 = UiaCryptoEngine.randomSalt(16)
        val part2 = UiaCryptoEngine.randomSalt(16)
        val aesKey = UiaCryptoEngine.deriveLoginAesKey(part0, part2)

        val plaintextJson = """{"phone":"0912345678","password":"TestPassword","app_id":"EINV6202512184519","uuid":"9774d56d682e549c"}"""

        val ciphertextBase64 = UiaCryptoEngine.encryptLoginPayload(plaintextJson, aesKey)
        assertNotEquals(plaintextJson, ciphertextBase64)

        val decrypted = UiaCryptoEngine.decryptLoginResponse(ciphertextBase64, aesKey)
        assertEquals(plaintextJson, decrypted)
    }
}
