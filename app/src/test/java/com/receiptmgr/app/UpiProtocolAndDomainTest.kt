package com.receiptmgr.app

import com.receiptmgr.app.data.crypto.ClockProvider
import com.receiptmgr.app.data.crypto.UpiComms
import com.receiptmgr.app.data.crypto.UpiJwtPacketCodec
import com.receiptmgr.app.domain.model.invoice.BillingPeriod
import com.receiptmgr.app.domain.model.invoice.InvoiceHeader
import com.receiptmgr.app.domain.model.invoice.InvoiceLineItem
import com.receiptmgr.app.domain.model.invoice.PeriodInvoiceSummary
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class UpiProtocolAndDomainTest {

    @Serializable
    private data class TestDetailReqData(
        @SerialName("version") val version: String = "1.0",
        @SerialName("cardType") val cardType: String = "3J0002",
        @SerialName("cardNo") val cardNo: String,
        @SerialName("action") val action: String = "carrierInvDetail",
        @SerialName("invNum") val invNum: String,
        @SerialName("invDate") val invDate: String
    )

    @Test
    fun `BillingPeriod calculates ROC year, dates, leap years, and periods correctly`() {
        // 2026/05/15 應歸納至 115年 05-06月
        val periodMay = BillingPeriod.fromLocalDate(LocalDate.of(2026, 5, 15))
        assertEquals(2026, periodMay.year)
        assertEquals(6, periodMay.endMonth)
        assertEquals(5, periodMay.startMonth)
        assertEquals(115, periodMay.rocYear)
        assertEquals("11506", periodMay.rocPeriodCode)
        assertEquals("2026/05/01", periodMay.queryStartDate)
        assertEquals("2026/06/30", periodMay.queryEndDate)
        assertEquals("115年 05-06月", periodMay.displayTitle)

        // 閏年 2 月 (2024 年) 結束日期必須為 29 日
        val leapPeriod = BillingPeriod(year = 2024, endMonth = 2)
        assertEquals("2024/01/01", leapPeriod.queryStartDate)
        assertEquals("2024/02/29", leapPeriod.queryEndDate)

        // 非閏年 2 月 (2023 年) 結束日期必須為 28 日
        val nonLeapPeriod = BillingPeriod(year = 2023, endMonth = 2)
        assertEquals("2023/02/28", nonLeapPeriod.queryEndDate)

        // 跨年推進與倒退
        val earlyPeriod = BillingPeriod(year = 2026, endMonth = 2)
        val prevPeriod = earlyPeriod.previous()
        assertEquals(2025, prevPeriod.year)
        assertEquals(12, prevPeriod.endMonth)

        val latePeriod = BillingPeriod(year = 2026, endMonth = 12)
        val nextPeriod = latePeriod.next()
        assertEquals(2027, nextPeriod.year)
        assertEquals(2, nextPeriod.endMonth)

        // 排序驗證
        assertTrue(prevPeriod < earlyPeriod)
        assertTrue(earlyPeriod < latePeriod)
        assertTrue(latePeriod < nextPeriod)
    }

    @Test
    fun `UpiComms enforces strict 40-second validity window`() {
        // 40 秒有效
        val validComms = UpiComms(
            iat = 1000L,
            exp = 1040L,
            liat = 900L,
            appId = "APP",
            barcode = "/ABC"
        )
        assertEquals(40L, validComms.exp - validComms.iat)

        // 非 40 秒拋出例外
        var threw = false
        try {
            UpiComms(
                iat = 1000L,
                exp = 1050L,
                liat = 900L,
                appId = "APP",
                barcode = "/ABC"
            )
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertTrue("非 40 秒視窗必須拋出 IllegalArgumentException", threw)
    }

    @Test
    fun `UpiJwtPacketCodec matches reference Python HMAC-SHA256 signature byte-for-byte`() {
        // 來自 .runtime/20261109_132909_query-invoices-details.json 的真實生產向量
        val sessionKey = "VsE82SBRFbSpcn7-2BTdP1RCwwH2h9h4WbIpq1pTq9g"
        val fixedClock = ClockProvider { 1794202149L }
        val codec = UpiJwtPacketCodec(clockProvider = fixedClock)

        val comms = codec.createFreshComms(
            barcode = "/8J722KT",
            appId = "EINV6202512184519",
            liat = 1794134995L
        )
        assertEquals(1794202149L, comms.iat)
        assertEquals(1794202189L, comms.exp)

        val reqData = TestDetailReqData(
            cardNo = "/8J722KT",
            invNum = "GW38470545",
            invDate = "2026/11/05"
        )

        val sealed = codec.seal(
            comms = comms,
            reqData = reqData,
            serializer = TestDetailReqData.serializer(),
            sessionKey = sessionKey
        )

        val expectedToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsInZlciI6IjEuMCJ9.eyJjb21tcyI6eyJpYXQiOjE3OTQyMDIxNDksImV4cCI6MTc5NDIwMjE4OSwibGlhdCI6MTc5NDEzNDk5NSwiYXBwaWQiOiJFSU5WNjIwMjUxMjE4NDUxOSIsImJhcmNvZGUiOiIvOEo3MjJLVCIsImtleVR5cGUiOiIyIn0sInJlcWRhdGEiOnsidmVyc2lvbiI6IjEuMCIsImNhcmRUeXBlIjoiM0owMDAyIiwiY2FyZE5vIjoiLzhKNzIyS1QiLCJhY3Rpb24iOiJjYXJyaWVySW52RGV0YWlsIiwiaW52TnVtIjoiR1czODQ3MDU0NSIsImludkRhdGUiOiIyMDI2LzExLzA1In19.AqDJ47RFVlPnH4j-9r5POfPft1SlzKboYzu1-2UbIJg"
        val expectedSig = "AqDJ47RFVlPnH4j-9r5POfPft1SlzKboYzu1-2UbIJg"

        assertEquals(expectedSig, sealed.signatureB64)
        assertEquals(expectedToken, sealed.token)

        // 驗證簽名校驗
        assertTrue(codec.verify(sealed.token, sessionKey))
        assertFalse(codec.verify(sealed.token, "tampered_key_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"))
    }

    @Test
    fun `Domain model formatting and period summary aggregation`() {
        val invoice1 = InvoiceHeader(
            invNum = "GW38470545",
            invPeriod = "11512",
            sellerName = "全聯實業",
            sellerBan = "83111853",
            amount = 23,
            invDate = "2026-11-05",
            invoiceTime = "16:56:28"
        )
        val invoice2 = InvoiceHeader(
            invNum = "BK22807399",
            invPeriod = "11512",
            sellerName = "摩托市集",
            sellerBan = "90293978",
            amount = 2090,
            invDate = "2026-11-06",
            invoiceTime = "18:27:20"
        )

        assertEquals("GW-38470545", invoice1.formattedInvNum)
        assertEquals("BK-22807399", invoice2.formattedInvNum)
        assertEquals("2026-11-05 16:56:28", invoice1.fullDateTime)

        val period = BillingPeriod(year = 2026, endMonth = 12)
        val summary = PeriodInvoiceSummary.fromInvoices(period, listOf(invoice1, invoice2))

        assertEquals(2, summary.invoiceCount)
        assertEquals(2113L, summary.totalAmount)
    }

    @Test
    fun `UpiInvoiceWireParser parses polymorphic header responses and normalizes dates`() {
        // 包含 carrierQueryList 與物件型態之 invDate
        val rawWrappedJson = """
            {
              "carrierQueryList": {
                "version": "1.0",
                "code": "200",
                "msg": "執行成功",
                "details": [
                  {
                    "rowNum": "1",
                    "invPeriod": "11504",
                    "invNum": "YV12348140",
                    "invoiceTime": "15:32:39",
                    "sellerBan": "54912969",
                    "sellerName": "統一超商",
                    "amount": "39",
                    "invDate": {
                      "year": "115",
                      "month": "4",
                      "date": "27"
                    }
                  }
                ]
              }
            }
        """.trimIndent()

        val headers = com.receiptmgr.app.data.network.dto.UpiInvoiceWireParser.parseHeaderResponse(rawWrappedJson)
        assertEquals(1, headers.size)
        val h = headers[0]
        assertEquals("YV12348140", h.invNum)
        assertEquals("YV-12348140", h.formattedInvNum)
        assertEquals(39, h.amount)
        assertEquals("2026-04-27", h.invDate)
        assertEquals("15:32:39", h.invoiceTime)
        assertEquals("2026-04-27 15:32:39", h.fullDateTime)
    }

    @Test
    fun `UpiInvoiceWireParser parses detail response and cleans unit price trailing zeros`() {
        val rawDetailJson = """
            {
              "version": "1.0",
              "code": "200",
              "msg": "執行成功",
              "invPeriod": "11508",
              "invNum": "DR97950438",
              "invDate": "20260822",
              "invoiceTime": "14:48:02",
              "sellerBan": "00528680",
              "sellerName": "餐廳企業",
              "sellerAddress": "臺北市萬華區",
              "amount": "658",
              "details": [
                {
                  "rowNum": "1",
                  "description": "經典漢堡套餐",
                  "quantity": "1",
                  "unitPrice": "260.0000000",
                  "amount": "260"
                },
                {
                  "rowNum": "2",
                  "description": "濃湯飲品",
                  "quantity": "1",
                  "unitPrice": "89.50",
                  "amount": "89"
                }
              ]
            }
        """.trimIndent()

        val detail = com.receiptmgr.app.data.network.dto.UpiInvoiceWireParser.parseDetailResponse(rawDetailJson)
        assertEquals("DR97950438", detail.invNum)
        assertEquals("DR-97950438", detail.formattedInvNum)
        assertEquals(658, detail.amount)
        assertEquals("2026-08-22", detail.invDate)
        assertEquals("14:48:02", detail.invoiceTime)
        assertEquals(2, detail.items.size)

        // 驗證 unitPrice 尾數清理
        assertEquals("260", detail.items[0].unitPrice)
        assertEquals("89.5", detail.items[1].unitPrice)
    }
}
