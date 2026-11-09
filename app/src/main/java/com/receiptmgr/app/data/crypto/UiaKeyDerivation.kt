package com.receiptmgr.app.data.crypto

import java.security.MessageDigest

/**
 * 官方 ApiCryptoKt 金鑰與 IV 派生演算法核心
 * 嚴格對齊 classes2.dex 官方二進位逆向成果：
 * - swapPairs 字符對調
 * - 64 字元字串拼接與 SHA-256 哈希生成 32-byte (256-bit) AES-GCM 金鑰 (lky)
 * - 前後鹽末尾 6 字元拼接生成 12-byte GCM Nonce (liv)
 */
object UiaKeyDerivation {

    /**
     * ApiCryptoKt.swapPairs(String): String
     * 將字串中相鄰兩字元進行成對對調 (1234 -> 2143, abcdef -> badcfe)。
     * 若字串長度為奇數，最後一位字元保持原位。
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
     * 派生 UIA ldata 之 256-bit AES 金鑰 (lky)
     * K_concat = swapPairs(part2) + swapPairs(part0) + part0.reversed() + part2.reversed() (64 chars)
     * K_aes = SHA-256(K_concat.toByteArray(UTF-8)) (32 bytes)
     */
    fun deriveLdataKey(part0: String, part2: String): ByteArray {
        require(part0.length == 16 && part2.length == 16) {
            "隨機鹽長度錯誤：預期各 16 字元，實際 part0=${part0.length}, part2=${part2.length}"
        }
        val concat = swapPairs(part2) + swapPairs(part0) + part0.reversed() + part2.reversed()
        val md = MessageDigest.getInstance("SHA-256")
        return md.digest(concat.toByteArray(Charsets.UTF_8))
    }

    /**
     * 派生金鑰之 64 碼十六進位字串表示 (利於單元測試與安全稽核記錄)
     */
    fun deriveLdataKeyHex(part0: String, part2: String): String {
        val keyBytes = deriveLdataKey(part0, part2)
        return keyBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * 派生 UIA ldata 之 12 位元組 GCM IV/Nonce (liv)
     * liv = (part0.takeLast(6) + part2.takeLast(6)).toByteArray(UTF-8)
     */
    fun deriveLdataIv(part0: String, part2: String): ByteArray {
        require(part0.length == 16 && part2.length == 16) {
            "隨機鹽長度錯誤：預期各 16 字元，實際 part0=${part0.length}, part2=${part2.length}"
        }
        val ivString = part0.takeLast(6) + part2.takeLast(6)
        return ivString.toByteArray(Charsets.UTF_8)
    }

    /**
     * 預留 UIA tdata 端點金鑰派生 (ckey + skey)
     */
    fun deriveTdataKey(ckey: String, skey: String): ByteArray {
        require(ckey.length == 16 && skey.isNotBlank()) {
            "ckey 必須為 16 字元且 skey 不可為空"
        }
        return (ckey + skey).toByteArray(Charsets.UTF_8)
    }
}
