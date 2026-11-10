package com.receiptmgr.app

import com.receiptmgr.app.domain.model.integration.equitytracker.EquityTrackerPayloadMapper
import com.receiptmgr.app.domain.model.invoice.InvoiceDetail
import com.receiptmgr.app.domain.model.invoice.InvoiceLineItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EquityTrackerPayloadMapperTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun testStandardDetailMapping() {
        val detail = InvoiceDetail(
            invNum = "AB12345678",
            invPeriod = "11512",
            sellerName = "全聯實業股份有限公司",
            sellerBan = "22880011",
            sellerAddress = "台北市中山區",
            amount = 215,
            invDate = "2026-11-10",
            invoiceTime = "14:35:20",
            items = listOf(
                InvoiceLineItem(rowNum = 1, description = "鮮乳", quantity = "1", unitPrice = "95", amount = 95),
                InvoiceLineItem(rowNum = 2, description = "抽取式衛生紙", quantity = "2", unitPrice = "60", amount = 120)
            )
        )

        val dto = EquityTrackerPayloadMapper.toDto(detail)

        assertEquals("全聯實業股份有限公司", dto.merchant)
        assertEquals("2026-11-10T14:35:20", dto.date)
        assertEquals(2, dto.items.size)

        assertEquals("鮮乳", dto.items[0].name)
        assertEquals(95.0, dto.items[0].amount, 0.001)
        assertEquals(1, dto.items[0].quantity)

        assertEquals("抽取式衛生紙", dto.items[1].name)
        assertEquals(120.0, dto.items[1].amount, 0.001)
        assertEquals(2, dto.items[1].quantity)

        // 驗證序列化後的 JSON 是否合法
        val jsonString = json.encodeToString(dto)
        assertNotNull(jsonString)
    }

    @Test
    fun testEmptyItemsSyntheticFallback() {
        // 載具發票無明細時，應自動合成一筆項目
        val detail = InvoiceDetail(
            invNum = "CD87654321",
            invPeriod = "11512",
            sellerName = "統一超商",
            sellerBan = "12345678",
            sellerAddress = "",
            amount = 85,
            invDate = "2026-11-09",
            invoiceTime = "",
            items = emptyList()
        )

        val dto = EquityTrackerPayloadMapper.toDto(detail)

        assertEquals("統一超商", dto.merchant)
        assertEquals("2026-11-09T00:00:00", dto.date)
        assertEquals(1, dto.items.size)

        // 合成項目驗證
        assertEquals("統一超商", dto.items[0].name)
        assertEquals(85.0, dto.items[0].amount, 0.001)
        assertEquals(1, dto.items[0].quantity)
    }

    @Test
    fun testDateFormatNormalization() {
        // 8 碼無分隔符
        assertEquals("2026-11-05T12:00:00", EquityTrackerPayloadMapper.formatIsoDate("20261105", "12:00:00"))
        // 斜線分隔符
        assertEquals("2026-11-05T08:30:00", EquityTrackerPayloadMapper.formatIsoDate("2026/11/05", "08:30:00"))
        // 短時間 (HH:mm)
        assertEquals("2026-11-05T16:20:00", EquityTrackerPayloadMapper.formatIsoDate("2026-11-05", "16:20"))
        // 無時間
        assertEquals("2026-11-05T00:00:00", EquityTrackerPayloadMapper.formatIsoDate("2026-11-05", ""))
        // 空白日期
        assertNull(EquityTrackerPayloadMapper.formatIsoDate("  ", ""))
    }

    @Test
    fun testQuantityParsing() {
        assertEquals(1, EquityTrackerPayloadMapper.parseQuantity("1"))
        assertEquals(3, EquityTrackerPayloadMapper.parseQuantity("3.00"))
        assertEquals(1, EquityTrackerPayloadMapper.parseQuantity("0"))
        assertEquals(1, EquityTrackerPayloadMapper.parseQuantity("-5"))
        assertEquals(1, EquityTrackerPayloadMapper.parseQuantity("abc"))
    }

    @Test
    fun testDeepLinkUriContract() {
        val detail = InvoiceDetail(
            invNum = "AB12345678",
            invPeriod = "11512",
            sellerName = "全聯福利中心",
            sellerBan = "22880011",
            sellerAddress = "",
            amount = 95,
            invDate = "2026-11-10",
            invoiceTime = "12:30:00",
            items = listOf(
                InvoiceLineItem(rowNum = 1, description = "鮮乳", quantity = "1", unitPrice = "95", amount = 95)
            )
        )

        val dto = EquityTrackerPayloadMapper.toDto(detail)
        val jsonStr = json.encodeToString(dto)
        val encodedData = java.net.URLEncoder.encode(jsonStr, "UTF-8")
        val uriStr = "equitytracker://receipt/import?data=$encodedData"

        org.junit.Assert.assertTrue(uriStr.startsWith("equitytracker://receipt/import?data="))

        // 模擬 EquityTracker 端解析
        val uri = java.net.URI(uriStr)
        assertEquals("equitytracker", uri.scheme)
        assertEquals("receipt", uri.host)
        assertEquals("/import", uri.path)

        val queryParam = uri.rawQuery.substringAfter("data=")
        val decodedJson = java.net.URLDecoder.decode(queryParam, "UTF-8")
        val parsedDto = json.decodeFromString<com.receiptmgr.app.domain.model.integration.equitytracker.EquityTrackerReceiptPayloadDto>(decodedJson)

        assertEquals("全聯福利中心", parsedDto.merchant)
        assertEquals(95.0, parsedDto.items[0].amount, 0.001)
    }
}
