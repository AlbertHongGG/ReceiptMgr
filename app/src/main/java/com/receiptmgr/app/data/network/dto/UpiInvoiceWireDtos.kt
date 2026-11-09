package com.receiptmgr.app.data.network.dto

import com.receiptmgr.app.domain.model.invoice.InvoiceDetail
import com.receiptmgr.app.domain.model.invoice.InvoiceHeader
import com.receiptmgr.app.domain.model.invoice.InvoiceLineItem
import com.receiptmgr.app.domain.model.invoice.UpiProtocolException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 端口 1 表頭清單查詢請求 DTO (carrierInvChk)
 */
@Serializable
data class UpiInvoiceHeaderReqDto(
    @SerialName("version") val version: String = "1.0",
    @SerialName("cardType") val cardType: String = "3J0002",
    @SerialName("cardNo") val cardNo: String,
    @SerialName("action") val action: String = "carrierInvChk",
    @SerialName("startDate") val startDate: String,
    @SerialName("endDate") val endDate: String,
    @SerialName("onlyWinningInv") val onlyWinningInv: String = "A",
    @SerialName("page") val page: Int = 1
)

/**
 * 端口 2 單筆發票品項明細查詢請求 DTO (carrierInvDetail)
 */
@Serializable
data class UpiInvoiceDetailReqDto(
    @SerialName("version") val version: String = "1.0",
    @SerialName("cardType") val cardType: String = "3J0002",
    @SerialName("cardNo") val cardNo: String,
    @SerialName("action") val action: String = "carrierInvDetail",
    @SerialName("invNum") val invNum: String,
    @SerialName("invDate") val invDate: String
)

/**
 * UPI 伺服器回傳防腐層與資料正規化解析器 (UpiInvoiceWireParser)。
 *
 * 核心保證：
 * 1. 自動降階解開 carrierQueryList 多型外殼。
 * 2. 徹底正規化 invDate (物件 / 8 碼字串 / 斜線分隔 -> YYYY-MM-DD)。
 * 3. 清理 unitPrice 之過長小數位數（如 "260.0000000" -> "260"）。
 * 4. 徹底過濾中獎、狀態與其他無用欄位，產出乾淨之領域實體。
 */
object UpiInvoiceWireParser {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * 解析發票表頭清單 (query-invoices-header)
     */
    fun parseHeaderResponse(rawJson: String): List<InvoiceHeader> {
        val rootElement = runCatching {
            json.parseToJsonElement(rawJson).jsonObject
        }.getOrElse { e ->
            throw UpiProtocolException.MalformedPayload("解析發票清單 JSON 失敗: ${e.message}", e)
        }

        // 兼容 carrierQueryList 包覆多型
        val targetObj = if (rootElement.containsKey("carrierQueryList")) {
            rootElement["carrierQueryList"]?.jsonObject ?: rootElement
        } else {
            rootElement
        }

        val code = targetObj["code"]?.jsonPrimitive?.contentOrNull ?: ""
        val msg = targetObj["msg"]?.jsonPrimitive?.contentOrNull ?: ""

        if (code != "200") {
            throw UpiProtocolException.BusinessRejected(code = code.ifBlank { "UNKNOWN" }, serverMessage = msg)
        }

        val detailsArray = targetObj["details"]?.jsonArray ?: return emptyList()

        return detailsArray.mapNotNull { element ->
            val obj = element.jsonObject
            val invNum = obj["invNum"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val invPeriod = obj["invPeriod"]?.jsonPrimitive?.contentOrNull ?: ""
            val sellerName = obj["sellerName"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
            val sellerBan = obj["sellerBan"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
            val sellerAddress = obj["sellerAddress"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
            val amountStr = obj["amount"]?.jsonPrimitive?.contentOrNull ?: "0"
            val amount = amountStr.trim().toIntOrNull() ?: 0
            val invoiceTime = obj["invoiceTime"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
            val cardNo = obj["cardNo"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
            val invDate = normalizeInvDate(obj["invDate"])

            InvoiceHeader(
                invNum = invNum.trim(),
                invPeriod = invPeriod.trim(),
                sellerName = sellerName,
                sellerBan = sellerBan,
                sellerAddress = sellerAddress,
                amount = amount,
                invDate = invDate,
                invoiceTime = invoiceTime,
                cardNo = cardNo
            )
        }
    }

    /**
     * 解析單筆品項明細 (query-invoices-details)
     */
    fun parseDetailResponse(rawJson: String): InvoiceDetail {
        val rootObj = runCatching {
            json.parseToJsonElement(rawJson).jsonObject
        }.getOrElse { e ->
            throw UpiProtocolException.MalformedPayload("解析發票明細 JSON 失敗: ${e.message}", e)
        }

        val code = rootObj["code"]?.jsonPrimitive?.contentOrNull ?: ""
        val msg = rootObj["msg"]?.jsonPrimitive?.contentOrNull ?: ""

        if (code != "200") {
            throw UpiProtocolException.BusinessRejected(code = code.ifBlank { "UNKNOWN" }, serverMessage = msg)
        }

        val invNum = rootObj["invNum"]?.jsonPrimitive?.contentOrNull ?: ""
        val invPeriod = rootObj["invPeriod"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        val sellerName = rootObj["sellerName"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        val sellerBan = rootObj["sellerBan"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        val sellerAddress = rootObj["sellerAddress"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        val buyerBan = rootObj["buyerBan"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        val amountStr = rootObj["amount"]?.jsonPrimitive?.contentOrNull ?: "0"
        val amount = amountStr.trim().toIntOrNull() ?: 0
        val invoiceTime = rootObj["invoiceTime"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        val cardNo = rootObj["cardNo"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
        val invDate = normalizeInvDate(rootObj["invDate"])

        val itemsArray = rootObj["details"]?.jsonArray ?: emptyList()
        val items = itemsArray.mapIndexed { index, element ->
            val itemObj = element.jsonObject
            val rowNum = itemObj["rowNum"]?.jsonPrimitive?.intOrNull ?: (index + 1)
            val description = itemObj["description"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
            val quantity = itemObj["quantity"]?.jsonPrimitive?.contentOrNull.orEmpty().trim().ifBlank { "1" }
            val unitPriceRaw = itemObj["unitPrice"]?.jsonPrimitive?.contentOrNull.orEmpty().trim()
            val unitPrice = cleanUnitPrice(unitPriceRaw)
            val itemAmountStr = itemObj["amount"]?.jsonPrimitive?.contentOrNull ?: "0"
            val itemAmount = itemAmountStr.trim().toIntOrNull() ?: 0

            InvoiceLineItem(
                rowNum = rowNum,
                description = description,
                quantity = quantity,
                unitPrice = unitPrice,
                amount = itemAmount
            )
        }

        return InvoiceDetail(
            invNum = invNum.trim(),
            invPeriod = invPeriod,
            sellerName = sellerName,
            sellerBan = sellerBan,
            sellerAddress = sellerAddress,
            buyerBan = buyerBan,
            amount = amount,
            invDate = invDate,
            invoiceTime = invoiceTime,
            cardNo = cardNo,
            items = items
        )
    }

    /**
     * 正規化各種型別之 invDate 為 YYYY-MM-DD
     */
    fun normalizeInvDate(element: JsonElement?): String {
        if (element == null) return ""

        return when {
            // 階層物件: {"year":"115","month":"4","date":"27",...}
            element is JsonObject -> {
                val y = element["year"]?.jsonPrimitive?.contentOrNull?.trim() ?: ""
                val m = element["month"]?.jsonPrimitive?.contentOrNull?.trim()?.toIntOrNull() ?: 0
                val d = element["date"]?.jsonPrimitive?.contentOrNull?.trim()?.toIntOrNull() ?: 0
                if (y.isNotBlank() && m > 0 && d > 0) {
                    val yearInt = y.toIntOrNull() ?: 0
                    val adYear = if (yearInt < 1000) yearInt + 1911 else yearInt
                    String.format("%04d-%02d-%02d", adYear, m, d)
                } else {
                    ""
                }
            }
            // 純字串格式
            else -> {
                val str = element.jsonPrimitive.contentOrNull?.trim() ?: return ""
                when {
                    // YYYYMMDD (例如 "20261105")
                    str.length == 8 && str.all { it.isDigit() } -> {
                        "${str.substring(0, 4)}-${str.substring(4, 6)}-${str.substring(6, 8)}"
                    }
                    // YYYY/MM/DD
                    str.contains("/") -> str.replace("/", "-")
                    // YYYY-MM-DD
                    str.contains("-") -> str
                    else -> str
                }
            }
        }
    }

    /**
     * 清理單價多餘之小數位數 (如 "260.0000000" -> "260")
     */
    fun cleanUnitPrice(raw: String): String {
        if (!raw.contains(".")) return raw
        val trimmed = raw.trimEnd('0').trimEnd('.')
        return trimmed.ifBlank { "0" }
    }
}
