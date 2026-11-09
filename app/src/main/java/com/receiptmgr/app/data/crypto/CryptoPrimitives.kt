package com.receiptmgr.app.data.crypto

import java.security.SecureRandom

/**
 * 隨機鹽值生成器抽象介面 (利於單元測試注入確定性測試雙胞胎)
 */
fun interface SaltGenerator {
    fun nextSalt(length: Int): String
}

/**
 * 系統即時時鐘提供者 (利於單元測試注入固定秒數，避免 -204 時間戳漂移)
 */
fun interface ClockProvider {
    fun nowEpochSeconds(): Long
}

/**
 * 生產環境標準隨機鹽生成器 (字母與數字池，共 62 字元)
 */
class SecureRandomSaltGenerator(
    private val random: SecureRandom = SecureRandom()
) : SaltGenerator {
    companion object {
        private const val ALPHANUMERIC = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
    }

    override fun nextSalt(length: Int): String {
        val sb = StringBuilder(length)
        for (i in 0 until length) {
            val idx = random.nextInt(ALPHANUMERIC.length)
            sb.append(ALPHANUMERIC[idx])
        }
        return sb.toString()
    }
}

/**
 * 生產環境真實系統時鐘
 */
class SystemClockProvider : ClockProvider {
    override fun nowEpochSeconds(): Long {
        return System.currentTimeMillis() / 1000L
    }
}
