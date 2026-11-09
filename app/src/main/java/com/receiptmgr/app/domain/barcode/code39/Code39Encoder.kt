package com.receiptmgr.app.domain.barcode.code39

import com.receiptmgr.app.domain.barcode.BarcodeEncoder
import com.receiptmgr.app.domain.barcode.model.BarcodeElement
import com.receiptmgr.app.domain.barcode.model.BarcodePattern

/**
 * 符合 ISO/IEC 16388 規範之 Code 39 (3 of 9) 條碼編碼器。
 * 專為台灣電子發票手機條碼載具與通用 Code 39 條碼設計。
 *
 * @param wideUnits 寬元素的相對單位數（預設為 2，代表寬窄比為 2:1）
 * @param quietZoneUnits 兩側空白靜區 (Quiet Zone) 單位數（ISO 標準建議至少 10X）
 */
class Code39Encoder(
    private val wideUnits: Int = 3,
    private val quietZoneUnits: Int = 10
) : BarcodeEncoder {

    companion object {
        /**
         * ISO/IEC 16388 Code 39 44 字元標準對照表。
         * 每個字元由 9 個元素組成（5 條黑線 Bar + 4 條白間隙 Space，交替排列：B S B S B S B S B）。
         * 其中正好有 3 個寬元素 ('W') 與 6 個窄元素 ('N')。
         */
        private val CODE39_TABLE: Map<Char, String> = mapOf(
            // 數字 0-9 (Space 3 為寬)
            '0' to "NNNWWNWNN",
            '1' to "WNNWNNNNW",
            '2' to "NNWWNNNNW",
            '3' to "WNWWNNNNN",
            '4' to "NNNWWNNNW",
            '5' to "WNNWWNNNN",
            '6' to "NNWWWNNNN",
            '7' to "NNNWNNWNW",
            '8' to "WNNWNNWNN",
            '9' to "NNWWNNWNN",

            // 大寫英文字母 A-J (Space 5 為寬)
            'A' to "WNNNNWNNW",
            'B' to "NNWNNWNNW",
            'C' to "WNWNNWNNN",
            'D' to "NNNNWWNNW",
            'E' to "WNNNWWNNN",
            'F' to "NNWNWWNNN",
            'G' to "NNNNNWWNW",
            'H' to "WNNNNWWNN",
            'I' to "NNWNNWWNN",
            'J' to "NNNNWWWNN",

            // 大寫英文字母 K-T (Space 7 為寬)
            'K' to "WNNNNNNWW",
            'L' to "NNWNNNNWW",
            'M' to "WNWNNNNWN",
            'N' to "NNNNWNNWW",
            'O' to "WNNNWNNWN",
            'P' to "NNWNWNNWN",
            'Q' to "NNNNNNWWW",
            'R' to "WNNNNNWWN",
            'S' to "NNWNNNWWN",
            'T' to "NNNNWNWWN",

            // 大寫英文字母 U-Z (Space 1 為寬)
            'U' to "WWNNNNNNW",
            'V' to "NWWNNNNNW",
            'W' to "WWWNNNNNN",
            'X' to "NWNNWNNNW",
            'Y' to "WWNNWNNNN",
            'Z' to "NWWNWNNNN",

            // 符號 (Space 1 為寬)
            '-' to "NWNNNNWNW",
            '.' to "WWNNNNWNN",
            ' ' to "NWWNNNWNN",
            '*' to "NWNNWNWNN", // 起始/結束符號

            // 純寬間隙符號 (無寬線，3 個寬間隙)
            '$' to "NWNWNWNNN",
            '/' to "NWNWNNNWN",
            '+' to "NWNNNWNWN",
            '%' to "NNNWNWNWN"
        )

        val Default = Code39Encoder()
    }

    override fun isValid(content: String): Boolean {
        if (content.isBlank()) return false
        val clean = content.trim().uppercase()
        return clean.all { it in CODE39_TABLE || it == '*' }
    }

    override fun encode(content: String): BarcodePattern {
        require(content.isNotBlank()) { "條碼內容不能為空" }
        val normalized = content.trim().uppercase()

        // 確保前後包含 Code 39 起始與結束碼 '*'
        val fullContent = buildString {
            if (!normalized.startsWith('*')) append('*')
            append(normalized)
            if (!normalized.endsWith('*')) append('*')
        }

        val elements = ArrayList<BarcodeElement>()

        for ((charIndex, ch) in fullContent.withIndex()) {
            val patternStr = CODE39_TABLE[ch]
                ?: throw IllegalArgumentException("字元 '$ch' 不在 Code 39 標準字元集中")

            // 遍歷該字元的 9 個元素 (交替排列：0=Bar, 1=Space, 2=Bar, ...)
            for (elementIndex in 0 until 9) {
                val isBar = (elementIndex % 2 == 0)
                val isWide = (patternStr[elementIndex] == 'W')
                val width = if (isWide) wideUnits else 1
                elements.add(BarcodeElement(isBar = isBar, unitWidth = width))
            }

            // 字元間距 (Inter-character gap)：非最後一個字元時，需插入 1 個窄空白
            if (charIndex < fullContent.length - 1) {
                elements.add(BarcodeElement(isBar = false, unitWidth = 1))
            }
        }

        return BarcodePattern(
            rawContent = normalized.removeSurrounding("*"),
            elements = elements,
            quietZoneUnits = quietZoneUnits
        )
    }
}
