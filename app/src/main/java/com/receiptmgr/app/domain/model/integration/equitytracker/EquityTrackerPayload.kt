package com.receiptmgr.app.domain.model.integration.equitytracker

import com.receiptmgr.app.domain.model.invoice.InvoiceDetail
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 對接 EquityTracker 所需之消費品項明細資料傳輸契約 (DTO)。
 */
@Serializable
data class EquityTrackerReceiptItemDto(
    @SerialName("name") val name: String,
    @SerialName("amount") val amount: Double,
    @SerialName("quantity") val quantity: Int = 1
)

/**
 * 對接 EquityTracker 所需之收據整體資料傳輸契約 (DTO)。
 */
@Serializable
data class EquityTrackerReceiptPayloadDto(
    @SerialName("merchant") val merchant: String?,
    @SerialName("date") val date: String?,
    @SerialName("items") val items: List<EquityTrackerReceiptItemDto>
)

/**
 * 發票領域模型至 EquityTracker 契約模型轉換器 (純領域轉換，無 Android 依賴)。
 */
object EquityTrackerPayloadMapper {

    /**
     * 將 [InvoiceDetail] 轉換為符合 EquityTracker 規格的 [EquityTrackerReceiptPayloadDto]。
     */
    fun toDto(detail: InvoiceDetail): EquityTrackerReceiptPayloadDto {
        val isoDate = formatIsoDate(detail.invDate, detail.invoiceTime)

        // 空品項保底機制：若發票無細項，以總額與商家合成單一項目，防止 EquityTracker 拒收
        val items = if (detail.items.isEmpty()) {
            listOf(
                EquityTrackerReceiptItemDto(
                    name = detail.sellerName.ifBlank { "發票消費" },
                    amount = detail.amount.toDouble(),
                    quantity = 1
                )
            )
        } else {
            detail.items.map { item ->
                EquityTrackerReceiptItemDto(
                    name = item.description.ifBlank { "一般品項" },
                    amount = item.amount.toDouble(),
                    quantity = parseQuantity(item.quantity)
                )
            }
        }

        return EquityTrackerReceiptPayloadDto(
            merchant = detail.sellerName.ifBlank { "收據消費" },
            date = isoDate,
            items = items
        )
    }

    /**
     * 解析數量字串，確保為不小於 1 之正整數。
     */
    fun parseQuantity(raw: String): Int {
        val cleaned = raw.trim()
        val asDouble = cleaned.toDoubleOrNull() ?: 1.0
        val qty = asDouble.toInt()
        return if (qty > 0) qty else 1
    }

    /**
     * 將 invDate 與 invoiceTime 正規化為標準 ISO-8601 (yyyy-MM-ddTHH:mm:ss)。
     */
    fun formatIsoDate(date: String, time: String): String? {
        val d = date.trim()
        if (d.isBlank()) return null

        val normalizedDate = when {
            // YYYYMMDD (例如 "20261105")
            d.length == 8 && d.all { it.isDigit() } -> {
                "${d.substring(0, 4)}-${d.substring(4, 6)}-${d.substring(6, 8)}"
            }
            // YYYY/MM/DD
            d.contains("/") -> d.replace("/", "-")
            else -> d
        }

        val t = time.trim()
        val normalizedTime = when {
            t.isBlank() -> "00:00:00"
            t.length == 5 -> "$t:00" // HH:mm -> HH:mm:00
            t.length == 8 -> t       // HH:mm:ss
            else -> "00:00:00"
        }

        return "${normalizedDate}T$normalizedTime"
    }
}
