package com.receiptmgr.app.domain.repository

import com.receiptmgr.app.domain.model.export.ExportTarget
import kotlinx.coroutines.flow.StateFlow

/**
 * 發票匯出/記帳紀錄倉儲介面 (Domain Port)。
 * 作為發票「已傳送」狀態的單一事實來源 (Single Source of Truth)。
 */
interface ExportRecordRepository {

    /**
     * 觀察指定目標之所有已匯出發票紀錄 (Map<invNum, exportedAt>)
     */
    val exportedAt: StateFlow<Map<String, Long>>

    /**
     * 標記發票已傳送至指定目標，並持久化至本地資料庫。
     *
     * @param invNum 發票號碼
     * @param target 匯出目標
     * @param timestamp 傳送時間戳 (毫秒)
     */
    suspend fun markExported(
        invNum: String,
        target: ExportTarget = ExportTarget.EQUITY_TRACKER,
        timestamp: Long = System.currentTimeMillis()
    )

    /**
     * 檢查特定發票是否已傳送至指定目標。
     */
    fun isExported(invNum: String, target: ExportTarget = ExportTarget.EQUITY_TRACKER): Boolean

    /**
     * 清空所有匯出紀錄 (通常於登出或清空所有發票時使用)。
     */
    suspend fun clearAll()
}
