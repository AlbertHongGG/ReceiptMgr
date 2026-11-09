package com.receiptmgr.app.data.crypto

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * 官方 ApiCryptoKt 標準 AES-GCM 對稱加密引擎
 * 採用 AES/GCM/NoPadding 模式與 128-bit (16 位元組) 認證標籤 (Tag)。
 * 密文陣列無額外前置 Nonce，兩端由明文 part0 與 part2 派生出 12 位元組 IV。
 */
class UiaGcmCipher {

    companion object {
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val ALGORITHM = "AES"
        private const val TAG_LENGTH_BITS = 128
        const val IV_LENGTH_BYTES = 12
        const val TAG_LENGTH_BYTES = 16
    }

    /**
     * 以 AES-GCM 加密明文二進位資料
     * @return 包含密文與 16 位元組認證標籤的位元組陣列 (無前置 Nonce)
     */
    fun encrypt(plaintext: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        require(iv.size == IV_LENGTH_BYTES) {
            "GCM IV 長度錯誤：預期 $IV_LENGTH_BYTES 位元組，實際 ${iv.size} 位元組"
        }
        val keySpec = SecretKeySpec(key, ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        return cipher.doFinal(plaintext)
    }

    /**
     * 以 AES-GCM 解密二進位密文 (末尾包含 16 位元組認證標籤)
     */
    fun decrypt(ciphertextWithTag: ByteArray, key: ByteArray, iv: ByteArray): ByteArray {
        require(iv.size == IV_LENGTH_BYTES) {
            "GCM IV 長度錯誤：預期 $IV_LENGTH_BYTES 位元組，實際 ${iv.size} 位元組"
        }
        val keySpec = SecretKeySpec(key, ALGORITHM)
        val gcmSpec = GCMParameterSpec(TAG_LENGTH_BITS, iv)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        return cipher.doFinal(ciphertextWithTag)
    }

    /**
     * 加密明文字串並輸出單行 Base64 字串
     */
    fun encryptToBase64(plaintextUtf8: String, key: ByteArray, iv: ByteArray): String {
        val bytes = plaintextUtf8.toByteArray(Charsets.UTF_8)
        val encrypted = encrypt(bytes, key, iv)
        return Base64.getEncoder().encodeToString(encrypted)
    }

    /**
     * 解密 Base64 密文並還原 UTF-8 明文字串
     */
    fun decryptFromBase64(base64Ciphertext: String, key: ByteArray, iv: ByteArray): String {
        val raw = Base64.getDecoder().decode(base64Ciphertext.trim())
        val decrypted = decrypt(raw, key, iv)
        return String(decrypted, Charsets.UTF_8)
    }
}
