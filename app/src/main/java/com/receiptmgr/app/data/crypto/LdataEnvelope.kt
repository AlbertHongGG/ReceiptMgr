package com.receiptmgr.app.data.crypto

/**
 * UIA 三段式 ldata 拓撲封裝值物件
 * 結構: part0 (16 chars) | part1 (Base64) | part2 (16 chars)
 */
data class LdataEnvelope(
    val part0: String,
    val part1Base64: String,
    val part2: String
) {
    init {
        require(part0.length == 16) {
            "part0 隨機鹽長度不合法: 預期 16 字元，實際 ${part0.length} 字元"
        }
        require(part2.length == 16) {
            "part2 隨機鹽長度不合法: 預期 16 字元，實際 ${part2.length} 字元"
        }
        require(part1Base64.isNotBlank()) {
            "part1 密文不可為空"
        }
        require(!part1Base64.contains("|")) {
            "part1 密文不可包含分隔符號 '|'"
        }
    }

    /**
     * 序列化為傳輸線路格式: part0|part1|part2
     */
    fun encode(): String = "$part0|$part1Base64|$part2"

    companion object {
        /**
         * 解析傳輸線路字串為 LdataEnvelope
         */
        fun parse(raw: String): LdataEnvelope {
            val parts = raw.trim().split("|")
            require(parts.size == 3) {
                "無效的 ldata 封裝拓撲，預期 3 段以 '|' 分隔，實際取得 ${parts.size} 段"
            }
            return LdataEnvelope(
                part0 = parts[0],
                part1Base64 = parts[1],
                part2 = parts[2]
            )
        }
    }
}
