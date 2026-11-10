package com.receiptmgr.app.domain.service

import com.receiptmgr.app.domain.model.integration.equitytracker.EquityTrackerReceiptPayloadDto

/**
 * 外部應用收據傳輸介面 (Port)。
 * 專責將已轉換之 Payload 傳遞給外部應用。
 */
interface ReceiptTransport {
    fun send(payload: EquityTrackerReceiptPayloadDto): Boolean
}
