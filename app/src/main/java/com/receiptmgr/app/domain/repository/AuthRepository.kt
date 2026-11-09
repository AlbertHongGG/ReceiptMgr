package com.receiptmgr.app.domain.repository

import com.receiptmgr.app.domain.model.auth.LoginRequestData
import com.receiptmgr.app.domain.model.auth.SessionCredentials
import com.receiptmgr.app.domain.model.auth.SessionState
import kotlinx.coroutines.flow.StateFlow

interface AuthRepository {
    val sessionState: StateFlow<SessionState>
    suspend fun login(requestData: LoginRequestData): Result<SessionCredentials>
    suspend fun clearSession()
}
