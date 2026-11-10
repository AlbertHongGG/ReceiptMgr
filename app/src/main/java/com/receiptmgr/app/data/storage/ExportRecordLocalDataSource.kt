package com.receiptmgr.app.data.storage

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.receiptmgr.app.data.storage.db.InvoiceDatabaseHelper
import com.receiptmgr.app.domain.model.export.ExportTarget
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

interface ExportRecordLocalDataSource {
    suspend fun getAllExportRecords(target: ExportTarget = ExportTarget.EQUITY_TRACKER): Map<String, Long>
    suspend fun upsertExportRecord(invNum: String, target: ExportTarget, timestamp: Long)
    suspend fun clearAll()
}

class ExportRecordLocalDataSourceImpl(
    private val dbHelper: InvoiceDatabaseHelper
) : ExportRecordLocalDataSource {

    override suspend fun getAllExportRecords(target: ExportTarget): Map<String, Long> = withContext(Dispatchers.IO) {
        val db = dbHelper.readableDatabase
        val map = mutableMapOf<String, Long>()
        val cursor = db.query(
            InvoiceDatabaseHelper.TABLE_INVOICE_EXPORT_RECORDS,
            arrayOf(
                InvoiceDatabaseHelper.COL_EXPORT_INV_NUM,
                InvoiceDatabaseHelper.COL_EXPORT_EXPORTED_AT
            ),
            "${InvoiceDatabaseHelper.COL_EXPORT_TARGET} = ?",
            arrayOf(target.name),
            null,
            null,
            null
        )
        cursor.use { c ->
            val idxInvNum = c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_EXPORT_INV_NUM)
            val idxExportedAt = c.getColumnIndexOrThrow(InvoiceDatabaseHelper.COL_EXPORT_EXPORTED_AT)
            while (c.moveToNext()) {
                map[c.getString(idxInvNum)] = c.getLong(idxExportedAt)
            }
        }
        map
    }

    override suspend fun upsertExportRecord(invNum: String, target: ExportTarget, timestamp: Long): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        val cv = ContentValues().apply {
            put(InvoiceDatabaseHelper.COL_EXPORT_INV_NUM, invNum)
            put(InvoiceDatabaseHelper.COL_EXPORT_TARGET, target.name)
            put(InvoiceDatabaseHelper.COL_EXPORT_EXPORTED_AT, timestamp)
        }
        db.insertWithOnConflict(
            InvoiceDatabaseHelper.TABLE_INVOICE_EXPORT_RECORDS,
            null,
            cv,
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    override suspend fun clearAll(): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.writableDatabase
        db.delete(InvoiceDatabaseHelper.TABLE_INVOICE_EXPORT_RECORDS, null, null)
    }
}
