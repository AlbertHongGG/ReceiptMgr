package com.receiptmgr.app.domain.model.invoice

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

/**
 * 台灣統一發票雙月期別強型別值物件 (BillingPeriod)。
 *
 * 核心保證：
 * 1. 嚴格限定每兩月為一期（1-2月, 3-4月, 5-6月, 7-8月, 9-10月, 11-12月），[endMonth] 必須為偶數 (2, 4, 6, 8, 10, 12)。
 * 2. 精準處理大小月與閏年天數（利用 [YearMonth] 計算期末最後一日，例如閏年 2 月為 29 日）。
 * 3. 集中封裝民國年換算、起訖日期字串 (YYYY/MM/DD) 與期別切換純函數，避免外部字串拼接錯誤。
 */
data class BillingPeriod(
    val year: Int,
    val endMonth: Int
) : Comparable<BillingPeriod> {

    init {
        require(year in 1900..2100) { "年份超出有效範圍: $year" }
        require(endMonth in 2..12 && endMonth % 2 == 0) {
            "雙月期別結束月份必須為 2, 4, 6, 8, 10, 12 之一，實得: $endMonth"
        }
    }

    /**
     * 期別起始月份 (1, 3, 5, 7, 9, 11)
     */
    val startMonth: Int get() = endMonth - 1

    /**
     * 民國年份 (西元 - 1911)
     */
    val rocYear: Int get() = year - 1911

    /**
     * 官方期別代碼（例如民國 115 年 5-6 月為 "11506"）
     */
    val rocPeriodCode: String
        get() = String.format(Locale.US, "%d%02d", rocYear, endMonth)

    /**
     * 該期起始日期物件 (起始月 1 號)
     */
    val startDateLocalDate: LocalDate
        get() = LocalDate.of(year, startMonth, 1)

    /**
     * 該期自然結束日期物件 (結束月之最後一日)
     */
    val naturalEndDateLocalDate: LocalDate
        get() = YearMonth.of(year, endMonth).atEndOfMonth()

    /**
     * API 查詢起始日期 (格式: YYYY/MM/DD，固定為起始月份 1 號)
     */
    val apiStartDate: String
        get() = String.format(Locale.US, "%04d/%02d/01", year, startMonth)

    /**
     * API 查詢起始日期 (兼容舊名稱)
     */
    val queryStartDate: String
        get() = apiStartDate

    /**
     * API 自然結束日期 (格式: YYYY/MM/DD，自然月份最後一日)
     */
    val queryEndDate: String
        get() = String.format(Locale.US, "%04d/%02d/%02d", year, endMonth, naturalEndDateLocalDate.dayOfMonth)

    /**
     * 傳送至財政部 API 之精準結束日期 (格式: YYYY/MM/DD)
     * 規格保證：
     * 若為當前正在進行中之期別，結束日期嚴格封頂在今天 (Today)，絕不傳送未來日期至伺服器。
     * 若為過去期別，則為該期最後一日。
     */
    fun getApiEndDate(referenceDate: LocalDate = LocalDate.now(ZoneId.of("Asia/Taipei"))): String {
        val naturalEnd = naturalEndDateLocalDate
        val effectiveEnd = if (naturalEnd.isAfter(referenceDate)) referenceDate else naturalEnd
        return String.format(Locale.US, "%04d/%02d/%02d", effectiveEnd.year, effectiveEnd.monthValue, effectiveEnd.dayOfMonth)
    }

    /**
     * 判定是否落於財政部 UPI 伺服器之發票查詢窗口內。
     * 官方 carrierInvChk 規格限制：僅提供「當期與前 4 期（合計共 5 期 / 最長半年）」之線上查詢。
     */
    fun isWithinMofQueryWindow(currentPeriod: BillingPeriod = BillingPeriod.current()): Boolean {
        val diffInMonths = (currentPeriod.year - this.year) * 12 + (currentPeriod.endMonth - this.endMonth)
        val diffInPeriods = diffInMonths / 2
        return diffInPeriods in 0..4
    }

    /**
     * 人類易讀介面標題 (例如 "115年 05-06月")
     */
    val displayTitle: String
        get() = String.format(Locale.US, "%d年 %02d-%02d月", rocYear, startMonth, endMonth)

    /**
     * 推導上一雙月期別 (跨年自動轉為前一年 11-12 月)
     */
    fun previous(): BillingPeriod {
        return if (endMonth == 2) {
            BillingPeriod(year = year - 1, endMonth = 12)
        } else {
            BillingPeriod(year = year, endMonth = endMonth - 2)
        }
    }

    /**
     * 推導下一雙月期別 (跨年自動轉為次一年 01-02 月)
     */
    fun next(): BillingPeriod {
        return if (endMonth == 12) {
            BillingPeriod(year = year + 1, endMonth = 2)
        } else {
            BillingPeriod(year = year, endMonth = endMonth + 2)
        }
    }

    override fun compareTo(other: BillingPeriod): Int {
        val yearDiff = this.year.compareTo(other.year)
        if (yearDiff != 0) return yearDiff
        return this.endMonth.compareTo(other.endMonth)
    }

    companion object {
        /**
         * 依指定本地日期自動歸納至所屬雙月期別
         */
        fun fromLocalDate(date: LocalDate): BillingPeriod {
            val month = date.monthValue
            val endMonth = if (month % 2 == 0) month else month + 1
            return BillingPeriod(year = date.year, endMonth = endMonth)
        }

        /**
         * 依 Unix 時間戳記與時區歸納期別
         */
        fun fromEpochMillis(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): BillingPeriod {
            val localDate = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
            return fromLocalDate(localDate)
        }

        /**
         * 取得系統當前所在雙月期別
         */
        fun current(zoneId: ZoneId = ZoneId.systemDefault()): BillingPeriod {
            return fromLocalDate(LocalDate.now(zoneId))
        }

        /**
         * 自民國期別代碼（如 "11506" 或 "11512"）解析
         */
        fun fromRocPeriodCode(code: String): BillingPeriod {
            val trimmed = code.trim()
            require(trimmed.length in 5..6) { "無效的民國期別代碼長度: $code" }
            val rocYearPart = trimmed.substring(0, trimmed.length - 2).toInt()
            val endMonthPart = trimmed.substring(trimmed.length - 2).toInt()
            return BillingPeriod(year = rocYearPart + 1911, endMonth = endMonthPart)
        }
    }
}
