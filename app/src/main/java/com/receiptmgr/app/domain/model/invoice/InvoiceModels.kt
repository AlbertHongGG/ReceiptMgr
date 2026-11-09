package com.receiptmgr.app.domain.model.invoice

import kotlinx.serialization.Serializable

/**
 * 發票表頭摘要實體 (InvoiceHeader)。
 * 純淨領域物件：徹底剔除中獎資訊與伺服器冗餘狀態文字。
 */
@Serializable
data class InvoiceHeader(
    val invNum: String,
    val invPeriod: String,
    val sellerName: String,
    val sellerBan: String,
    val sellerAddress: String = "",
    val amount: Int,
    val invDate: String,
    val invoiceTime: String = "",
    val cardNo: String = ""
) {
    /**
     * 格式化發票字軌號碼 (例如: "GW38470545" -> "GW-38470545")
     */
    val formattedInvNum: String
        get() = if (invNum.length == 10) {
            "${invNum.substring(0, 2)}-${invNum.substring(2)}"
        } else {
            invNum
        }

    /**
     * 完整開立日期時間 (例如: "2026-11-05 16:56:28")
     */
    val fullDateTime: String
        get() = if (invoiceTime.isNotBlank()) "$invDate $invoiceTime" else invDate
}

/**
 * 單筆發票消費品項明細單行實體 (InvoiceLineItem)。
 */
@Serializable
data class InvoiceLineItem(
    val rowNum: Int,
    val description: String,
    val quantity: String,
    val unitPrice: String,
    val amount: Int
)

/**
 * 單筆發票完整消費明細實體 (InvoiceDetail)。
 * 專為「極簡數位熱感收據」呈現設計。
 */
@Serializable
data class InvoiceDetail(
    val invNum: String,
    val invPeriod: String,
    val sellerName: String,
    val sellerBan: String,
    val sellerAddress: String,
    val buyerBan: String = "",
    val amount: Int,
    val invDate: String,
    val invoiceTime: String = "",
    val cardNo: String = "",
    val items: List<InvoiceLineItem> = emptyList()
) {
    /**
     * 格式化發票字軌號碼 (例如: "GW-38470545")
     */
    val formattedInvNum: String
        get() = if (invNum.length == 10) {
            "${invNum.substring(0, 2)}-${invNum.substring(2)}"
        } else {
            invNum
        }

    /**
     * 完整開立日期時間
     */
    val fullDateTime: String
        get() = if (invoiceTime.isNotBlank()) "$invDate $invoiceTime" else invDate

    /**
     * 民國期別易讀抬頭 (例如 "115 年 11-12 月")
     */
    val formattedPeriodTitle: String
        get() = runCatching {
            BillingPeriod.fromRocPeriodCode(invPeriod).displayTitle
        }.getOrDefault(invPeriod)
}

/**
 * 本期發票總覽帳務統計實體 (PeriodInvoiceSummary)。
 */
data class PeriodInvoiceSummary(
    val period: BillingPeriod,
    val totalAmount: Long,
    val invoiceCount: Int,
    val invoices: List<InvoiceHeader>
) {
    companion object {
        fun fromInvoices(period: BillingPeriod, invoices: List<InvoiceHeader>): PeriodInvoiceSummary {
            val total = invoices.fold(0L) { acc, item -> acc + item.amount }
            return PeriodInvoiceSummary(
                period = period,
                totalAmount = total,
                invoiceCount = invoices.size,
                invoices = invoices
            )
        }
    }
}
