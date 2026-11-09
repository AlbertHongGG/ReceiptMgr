package com.receiptmgr.app.data.crypto

/**
 * UIA 協定金鑰派生演算法 (ApiCryptoKt.swapPairs 與衍生規則)
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
     * 依據 UIA ldata 規範派生 256 位元 (32 位元組) AES 對稱金鑰：
     * K_raw = part0 + part2 (32 chars)
     * K_aes = swapPairs(K_raw)
     */
    fun deriveLdataKey(part0: String, part2: String): ByteArray {
        require(part0.length == 16 && part2.length == 16) {
            "隨機鹽長度錯誤：預期各 16 字元，實際 part0=${part0.length}, part2=${part2.length}"
        }
        val rawKey = part0 + part2
        val aesKeyString = swapPairs(rawKey)
        return aesKeyString.toByteArray(Charsets.UTF_8)
    }

    /**
     * 派生金鑰字串表示
     */
    fun deriveLdataKeyString(part0: String, part2: String): String {
        require(part0.length == 16 && part2.length == 16) {
            "隨機鹽長度錯誤：預期各 16 字元，實際 part0=${part0.length}, part2=${part2.length}"
        }
        return swapPairs(part0 + part2)
    }

    /**
     * 預留下游 API tdata 派生金鑰 (利用登入簽發之 session.iv 進行 swapPairs 衍生)
     */
    fun deriveTdataKey(sessionIv: String): ByteArray {
        require(sessionIv.isNotBlank()) { "sessionIv 不可為空" }
        return swapPairs(sessionIv).toByteArray(Charsets.UTF_8)
    }
}
