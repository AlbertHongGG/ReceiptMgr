package com.receiptmgr.app.data.storage

import com.receiptmgr.app.domain.model.auth.SessionCredentials

/**
 * 載具登入會話儲存介面契約 (SessionDataSource)。
 */
interface SessionDataSource {
    suspend fun saveSession(session: SessionCredentials)
    suspend fun getSession(): SessionCredentials?
    suspend fun clearSession()
}
