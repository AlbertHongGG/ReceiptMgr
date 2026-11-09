package com.receiptmgr.app.data.crypto

import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * UIA Byte 0 對齊對稱加密引擎
 * 採用 AES/CTR/NoPadding 流式模式，密文由 Byte 0 直接對齊明文 JSON，
 * 嚴禁前置 12-byte 隨機 Nonce，且無 GCM Auth Tag。
 */
class UiaSymmetricCipher {

    companion object {
        private const val TRANSFORMATION = "AES/CTR/NoPadding"
        private const val ALGORITHM = "AES"
        // 16 位元組確定性初始計數器 (Zero IV)，保證請求與回應兩端 Keystream 自 Byte 0 對齊
        private val ZERO_IV = ByteArray(16)
    }

    /**
     * 加密二進位資料 (Byte 0 對齊)
     */
    fun encrypt(plaintext: ByteArray, key: ByteArray): ByteArray {
        val keySpec = SecretKeySpec(key, ALGORITHM)
        val ivSpec = IvParameterSpec(ZERO_IV)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(plaintext)
    }

    /**
     * 解密二進位密文 (Byte 0 對齊)
     */
    fun decrypt(ciphertext: ByteArray, key: ByteArray): ByteArray {
        val keySpec = SecretKeySpec(key, ALGORITHM)
        val ivSpec = IvParameterSpec(ZERO_IV)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
        return cipher.doFinal(ciphertext)
    }

    /**
     * 加密明文字串並輸出單行 Base64
     */
    fun encryptToBase64(plaintextUtf8: String, key: ByteArray): String {
        val bytes = plaintextUtf8.toByteArray(Charsets.UTF_8)
        val encrypted = encrypt(bytes, key)
        return Base64.getEncoder().encodeToString(encrypted)
    }

    /**
     * 解密 Base64 密文並還原 UTF-8 明文字串
     */
    fun decryptFromBase64(base64Ciphertext: String, key: ByteArray): String {
        val raw = Base64.getDecoder().decode(base64Ciphertext.trim())
        val decrypted = decrypt(raw, key)
        return String(decrypted, Charsets.UTF_8)
    }
}
