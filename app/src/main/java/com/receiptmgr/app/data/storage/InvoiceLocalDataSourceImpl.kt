package com.receiptmgr.app.data.storage

import android.content.ContentValues
import com.receiptmgr.app.data.storage.db.InvoiceDatabaseHelper
import com.receiptmgr.app.domain.model.invoice.BillingPeriod
import com.receiptmgr.app.domain.model.invoice.InvoiceDetail
import com.receiptmgr.app.domain.model.invoice.InvoiceHeader
import com.receiptmgr.app.domain.model.invoice.InvoiceLineItem
import com.receiptmgr.app.domain.model.invoice.PeriodInvoiceSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 發票本地持久化資料源實作 (InvoiceLocalDataSourceImpl)。
 *
 * 核心保證：
 * 1. 執行緒安全交易事務 (ACID)。
 * 2. 批次寫入冪等性，更新表頭時不破壞既有已下載之 has_detail 標記。
 * 3. 雙月發票一律按開立日期、時間降序排序，確保離線讀取與線上排序一致。
 * 4. 游標 (Cursor) 嚴格使用 use 區塊自動關閉，杜絕記憶體洩漏。
 */
class InvoiceLocalDataSourceImpl(
    private val dbHelper: InvoiceDatabaseHelper
) : InvoiceLocalDataSource {

    override suspend fun getInvoicesByPeriod(period: BillingPeriod): List<InvoiceHeader> =
        withContext(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val periodCode = period.rocPeriodCode
            val cursor = db.query(
                InvoiceDatabaseHelper.TABLE_INVOICES,
                arrayOf(
                    InvoiceDatabaseHelper.COL_INV_NUM,
                    InvoiceDatabaseHelper.COL_INV_PERIOD,
                    InvoiceDatabaseHelper.COL_SELLER_NAME,
                    InvoiceDatabaseHelper.COL_SELLER_BAN,
                    InvoiceDatabaseHelper.COL_SELLER_ADDRESS,
                    InvoiceDatabaseHelper.COL_AMOUNT,
                    InvoiceDatabaseHelper.COL_INV_DATE,
                    InvoiceDatabaseHelper.COL_INVOICE_TIME,
                    InvoiceDatabaseHelper.COL_CARD_NO
                ),
                "${InvoiceDatabaseHelper.COL_INV_PERIOD} = ?",
                arrayOf(periodCode),
                null,
                null,
                "${InvoiceDatabaseHelper.COL_INV_DATE} DESC, ${InvoiceDatabaseHelper.COL_INVOICE_TIME} DESC, ${InvoiceDatabaseHelper.COL_INV_NUM} DESC"
            )

            val list = mutableListOf<InvoiceHeader>()
            cursor.use { c ->
                val idxInvNum = c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_INV_NUM)
                val idxInvPeriod = c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_INV_PERIOD)
                val idxSellerName = c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_SELLER_NAME)
                val idxSellerBan = c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_SELLER_BAN)
                val idxSellerAddress = c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_SELLER_ADDRESS)
                val idxAmount = c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_AMOUNT)
                val idxInvDate = c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_INV_DATE)
                val idxInvoiceTime = c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_INVOICE_TIME)
                val idxCardNo = c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_CARD_NO)

                while (c.moveToNext()) {
                    list.add(
                        InvoiceHeader(
                            invNum = c.getString(idxInvNum),
                            invPeriod = c.getString(idxInvPeriod),
                            sellerName = c.getString(idxSellerName),
                            sellerBan = c.getString(idxSellerBan),
                            sellerAddress = c.getString(idxSellerAddress),
                            amount = c.getInt(idxAmount),
                            invDate = c.getString(idxInvDate),
                            invoiceTime = c.getString(idxInvoiceTime),
                            cardNo = c.getString(idxCardNo)
                        )
                    )
                }
            }
            list
        }

    override suspend fun upsertInvoices(
        period: BillingPeriod,
        invoices: List<InvoiceHeader>
    ): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val now = System.currentTimeMillis()
        val periodCode = period.rocPeriodCode

        db.beginTransaction()
        try {
            for (inv in invoices) {
                val effectivePeriod = if (inv.invPeriod.isNotBlank()) inv.invPeriod else periodCode
                val cv = ContentValues().apply {
                    put(InvoiceDatabaseHelper.COL_INV_PERIOD, effectivePeriod)
                    put(InvoiceDatabaseHelper.COL_SELLER_NAME, inv.sellerName)
                    put(InvoiceDatabaseHelper.COL_SELLER_BAN, inv.sellerBan)
                    if (inv.sellerAddress.isNotBlank()) {
                        put(InvoiceDatabaseHelper.COL_SELLER_ADDRESS, inv.sellerAddress)
                    }
                    put(InvoiceDatabaseHelper.COL_AMOUNT, inv.amount)
                    put(InvoiceDatabaseHelper.COL_INV_DATE, inv.invDate)
                    put(InvoiceDatabaseHelper.COL_INVOICE_TIME, inv.invoiceTime)
                    put(InvoiceDatabaseHelper.COL_CARD_NO, inv.cardNo)
                    put(InvoiceDatabaseHelper.COL_UPDATED_AT, now)
                }

                val updatedRows = db.update(
                    InvoiceDatabaseHelper.TABLE_INVOICES,
                    cv,
                    "${InvoiceDatabaseHelper.COL_INV_NUM} = ?",
                    arrayOf(inv.invNum)
                )

                if (updatedRows == 0) {
                    cv.put(InvoiceDatabaseHelper.COL_INV_NUM, inv.invNum)
                    cv.put(InvoiceDatabaseHelper.COL_HAS_DETAIL, 0)
                    cv.put(InvoiceDatabaseHelper.COL_CREATED_AT, now)
                    db.insert(InvoiceDatabaseHelper.TABLE_INVOICES, null, cv)
                }
            }

            val metaCv = ContentValues().apply {
                put(InvoiceDatabaseHelper.COL_META_PERIOD_CODE, periodCode)
                put(InvoiceDatabaseHelper.COL_META_LAST_SYNC_TIME, now)
                put(InvoiceDatabaseHelper.COL_META_TOTAL_INVOICES, invoices.size)
                put(InvoiceDatabaseHelper.COL_META_TOTAL_AMOUNT, invoices.fold(0L) { acc, item -> acc + item.amount })
            }
            val metaUpdated = db.update(
                InvoiceDatabaseHelper.TABLE_PERIOD_METADATA,
                metaCv,
                "${InvoiceDatabaseHelper.COL_META_PERIOD_CODE} = ?",
                arrayOf(periodCode)
            )
            if (metaUpdated == 0) {
                db.insert(InvoiceDatabaseHelper.TABLE_PERIOD_METADATA, null, metaCv)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun getPendingDetailInvoices(period: BillingPeriod): List<Pair<String, String>> =
        withContext(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val periodCode = period.rocPeriodCode
            val cursor = db.query(
                InvoiceDatabaseHelper.TABLE_INVOICES,
                arrayOf(
                    InvoiceDatabaseHelper.COL_INV_NUM,
                    InvoiceDatabaseHelper.COL_INV_DATE
                ),
                "${InvoiceDatabaseHelper.COL_INV_PERIOD} = ? AND ${InvoiceDatabaseHelper.COL_HAS_DETAIL} = 0",
                arrayOf(periodCode),
                null,
                null,
                "${InvoiceDatabaseHelper.COL_INV_DATE} DESC, ${InvoiceDatabaseHelper.COL_INVOICE_TIME} DESC, ${InvoiceDatabaseHelper.COL_INV_NUM} DESC"
            )

            val list = mutableListOf<Pair<String, String>>()
            cursor.use { c ->
                val idxInvNum = c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_INV_NUM)
                val idxInvDate = c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_INV_DATE)
                while (c.moveToNext()) {
                    list.add(Pair(c.getString(idxInvNum), c.getString(idxInvDate)))
                }
            }
            list
        }

    override suspend fun getInvoiceDetail(invNum: String): InvoiceDetail? =
        withContext(Dispatchers.IO) {
            val db = dbHelper.readableDatabase

            // 先查詢主表 (必須 has_detail == 1 才代表明細已存入)
            val headerCursor = db.query(
                InvoiceDatabaseHelper.TABLE_INVOICES,
                null,
                "${InvoiceDatabaseHelper.COL_INV_NUM} = ? AND ${InvoiceDatabaseHelper.COL_HAS_DETAIL} = 1",
                arrayOf(invNum),
                null,
                null,
                null
            )

            var detailBase: InvoiceDetail? = null
            headerCursor.use { c ->
                if (c.moveToFirst()) {
                    detailBase = InvoiceDetail(
                        invNum = c.getString(c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_INV_NUM)),
                        invPeriod = c.getString(c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_INV_PERIOD)),
                        sellerName = c.getString(c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_SELLER_NAME)),
                        sellerBan = c.getString(c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_SELLER_BAN)),
                        sellerAddress = c.getString(c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_SELLER_ADDRESS)),
                        buyerBan = c.getString(c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_BUYER_BAN)),
                        amount = c.getInt(c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_AMOUNT)),
                        invDate = c.getString(c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_INV_DATE)),
                        invoiceTime = c.getString(c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_INVOICE_TIME)),
                        cardNo = c.getString(c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_CARD_NO)),
                        items = emptyList()
                    )
                }
            }

            if (detailBase == null) return@withContext null

            // 查詢明細品項表
            val itemsCursor = db.query(
                InvoiceDatabaseHelper.TABLE_INVOICE_ITEMS,
                null,
                "${InvoiceDatabaseHelper.COL_ITEM_INV_NUM} = ?",
                arrayOf(invNum),
                null,
                null,
                "${InvoiceDatabaseHelper.COL_ITEM_ROW_NUM} ASC"
            )

            val items = mutableListOf<InvoiceLineItem>()
            itemsCursor.use { c ->
                val idxRow = c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_ITEM_ROW_NUM)
                val idxDesc = c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_ITEM_DESCRIPTION)
                val idxQty = c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_ITEM_QUANTITY)
                val idxPrice = c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_ITEM_UNIT_PRICE)
                val idxAmt = c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_ITEM_AMOUNT)

                while (c.moveToNext()) {
                    items.add(
                        InvoiceLineItem(
                            rowNum = c.getInt(idxRow),
                            description = c.getString(idxDesc),
                            quantity = c.getString(idxQty),
                            unitPrice = c.getString(idxPrice),
                            amount = c.getInt(idxAmt)
                        )
                    )
                }
            }

            detailBase!!.copy(items = items)
        }

    override suspend fun saveInvoiceDetail(detail: InvoiceDetail): Unit =
        withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            val now = System.currentTimeMillis()

            db.beginTransaction()
            try {
                val cv = ContentValues().apply {
                    put(InvoiceDatabaseHelper.COL_INV_PERIOD, detail.invPeriod)
                    put(InvoiceDatabaseHelper.COL_SELLER_NAME, detail.sellerName)
                    put(InvoiceDatabaseHelper.COL_SELLER_BAN, detail.sellerBan)
                    put(InvoiceDatabaseHelper.COL_SELLER_ADDRESS, detail.sellerAddress)
                    put(InvoiceDatabaseHelper.COL_BUYER_BAN, detail.buyerBan)
                    put(InvoiceDatabaseHelper.COL_AMOUNT, detail.amount)
                    put(InvoiceDatabaseHelper.COL_INV_DATE, detail.invDate)
                    put(InvoiceDatabaseHelper.COL_INVOICE_TIME, detail.invoiceTime)
                    put(InvoiceDatabaseHelper.COL_CARD_NO, detail.cardNo)
                    put(InvoiceDatabaseHelper.COL_HAS_DETAIL, 1)
                    put(InvoiceDatabaseHelper.COL_UPDATED_AT, now)
                }

                val updatedRows = db.update(
                    InvoiceDatabaseHelper.TABLE_INVOICES,
                    cv,
                    "${InvoiceDatabaseHelper.COL_INV_NUM} = ?",
                    arrayOf(detail.invNum)
                )

                if (updatedRows == 0) {
                    cv.put(InvoiceDatabaseHelper.COL_INV_NUM, detail.invNum)
                    cv.put(InvoiceDatabaseHelper.COL_CREATED_AT, now)
                    db.insert(InvoiceDatabaseHelper.TABLE_INVOICES, null, cv)
                }

                // 刪除此發票舊品項以確保冪等
                db.delete(
                    InvoiceDatabaseHelper.TABLE_INVOICE_ITEMS,
                    "${InvoiceDatabaseHelper.COL_ITEM_INV_NUM} = ?",
                    arrayOf(detail.invNum)
                )

                // 批次寫入新品項
                for (item in detail.items) {
                    val itemCv = ContentValues().apply {
                        put(InvoiceDatabaseHelper.COL_ITEM_INV_NUM, detail.invNum)
                        put(InvoiceDatabaseHelper.COL_ITEM_ROW_NUM, item.rowNum)
                        put(InvoiceDatabaseHelper.COL_ITEM_DESCRIPTION, item.description)
                        put(InvoiceDatabaseHelper.COL_ITEM_QUANTITY, item.quantity)
                        put(InvoiceDatabaseHelper.COL_ITEM_UNIT_PRICE, item.unitPrice)
                        put(InvoiceDatabaseHelper.COL_ITEM_AMOUNT, item.amount)
                    }
                    db.insert(InvoiceDatabaseHelper.TABLE_INVOICE_ITEMS, null, itemCv)
                }

                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }

    override suspend fun getPeriodSummary(period: BillingPeriod): PeriodInvoiceSummary? =
        withContext(Dispatchers.IO) {
            val invoices = getInvoicesByPeriod(period)
            if (invoices.isEmpty()) null else PeriodInvoiceSummary.fromInvoices(period, invoices)
        }

    override suspend fun hasLocalArchive(period: BillingPeriod): Boolean =
        withContext(Dispatchers.IO) {
            val db = dbHelper.readableDatabase
            val cursor = db.rawQuery(
                "SELECT 1 FROM ${InvoiceDatabaseHelper.TABLE_INVOICES} WHERE ${InvoiceDatabaseHelper.COL_INV_PERIOD} = ? LIMIT 1",
                arrayOf(period.rocPeriodCode)
            )
            cursor.use {
                it.moveToFirst()
            }
        }

    override suspend fun clearAll(): Unit =
        withContext(Dispatchers.IO) {
            val db = dbHelper.writableDatabase
            db.beginTransaction()
            try {
                db.delete(InvoiceDatabaseHelper.TABLE_INVOICE_ITEMS, null, null)
                db.delete(InvoiceDatabaseHelper.TABLE_INVOICE_EXPORT_RECORDS, null, null)
                db.delete(InvoiceDatabaseHelper.TABLE_INVOICES, null, null)
                db.delete(InvoiceDatabaseHelper.TABLE_PERIOD_METADATA, null, null)
                db.setTransactionSuccessful()
            } finally {
                db.endTransaction()
            }
        }
}
