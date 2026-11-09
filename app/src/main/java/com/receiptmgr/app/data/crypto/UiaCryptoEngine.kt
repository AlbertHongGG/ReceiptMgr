package com.receiptmgr.app.data.crypto

import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

object UiaCryptoEngine {

    private const val ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    private const val GCM_TAG_LENGTH_BITS = 128
    private const val GCM_NONCE_LENGTH_BYTES = 12
    private val secureRandom = SecureRandom()

    /**
     * ApiCryptoKt.swapPairs(String): String
     * 將字串中的相鄰兩字元進行成對對調 (1234 -> 2143, abcdef -> badcfe)
     * 若字串長度為奇數，最後一個字元保持原位。
     */
    fun swapPairs(s: String): String {
        val chars = s.toCharArray()
        for (i in 0 until chars.size - 1 step 2) {
            val tmp = chars[i]
            chars[i] = chars[i + 1]
            chars[i + 1] = tmp
        }
        return String(chars)
    }

    /**
     * 生成長度為 length 的隨機英數字元鹽值 (預設 16 字元)
     */
    fun randomSalt(length: Int = 16): String {
        val sb = java.lang.StringBuilder(length)
        for (i in 0 until length) {
            val index = secureRandom.nextInt(ALPHANUMERIC.length)
            sb.append(ALPHANUMERIC[index])
        }
        return sb.toString()
    }

    /**
     * 派生 256 位元 (32 字元) AES 金鑰：
     * K_raw = part0 + part2
     * K_aes = swapPairs(K_raw)
     */
    fun deriveLoginAesKey(part0: String, part2: String): String {
        require(part0.length == 16 && part2.length == 16) {
            "隨機鹽長度錯誤：預期各 16 字元，實際 part0=${part0.length}, part2=${part2.length}"
        }
        val rawKey = part0 + part2
        return swapPairs(rawKey)
    }

    /**
     * 打包三段式 ldata：part0|part1|part2
     */
    fun packLdata(part0: String, part1CiphertextB64: String, part2: String): String {
        return "$part0|$part1CiphertextB64|$part2"
    }

    /**
     * 解包三段式 ldata
     */
    fun unpackLdata(ldata: String): Triple<String, String, String> {
        val parts = ldata.trim().split("|")
        require(parts.size == 3) {
            "無效的 ldata 封裝，預期 3 段以 '|' 分隔，實際取得 ${parts.size} 段"
        }
        return Triple(parts[0], parts[1], parts[2])
    }

    /**
     * 使用 AES-256-GCM (12-byte Nonce + 128-bit Tag) 加密登入請求明文 JSON
     * 輸出為 Base64(nonce + ciphertextWithTag)
     */
    fun encryptLoginPayload(plaintextJson: String, aesKey: String): String {
        val nonce = ByteArray(GCM_NONCE_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val keySpec = SecretKeySpec(aesKey.toByteArray(Charsets.UTF_8), "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)

        val ciphertextWithTag = cipher.doFinal(plaintextJson.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(nonce.size + ciphertextWithTag.size)
        System.arraycopy(nonce, 0, combined, 0, nonce.size)
        System.arraycopy(ciphertextWithTag, 0, combined, nonce.size, ciphertextWithTag.size)

        return Base64.getEncoder().encodeToString(combined)
    }

    /**
     * 使用 AES-256-GCM 解密伺服器回傳之 Base64 payload
     * 前 12 位元組為 Nonce，其餘為 ciphertextWithTag
     */
    fun decryptLoginResponse(encryptedBase64: String, aesKey: String): String {
        val raw = Base64.getDecoder().decode(encryptedBase64.trim())
        require(raw.size > GCM_NONCE_LENGTH_BYTES) {
            "密文長度不足：預期大於 $GCM_NONCE_LENGTH_BYTES 位元組，實際取得 ${raw.size}"
        }

        val nonce = raw.copyOfRange(0, GCM_NONCE_LENGTH_BYTES)
        val ciphertextWithTag = raw.copyOfRange(GCM_NONCE_LENGTH_BYTES, raw.size)

        val keySpec = SecretKeySpec(aesKey.toByteArray(Charsets.UTF_8), "AES")
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce)

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)

        val decryptedBytes = cipher.doFinal(ciphertextWithTag)
        return String(decryptedBytes, Charsets.UTF_8)
    }
}
