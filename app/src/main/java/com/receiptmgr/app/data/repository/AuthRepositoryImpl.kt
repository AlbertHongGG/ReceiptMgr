package com.receiptmgr.app.data.repository

import com.receiptmgr.app.data.network.UiaAuthApiClient
import com.receiptmgr.app.data.storage.EncryptedSessionDataSource
import com.receiptmgr.app.domain.model.auth.LoginCommand
import com.receiptmgr.app.domain.model.auth.SessionCredentials
import com.receiptmgr.app.domain.model.auth.SessionState
import com.receiptmgr.app.domain.repository.AuthRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthRepositoryImpl(
    private val apiClient: UiaAuthApiClient,
    private val sessionDataSource: EncryptedSessionDataSource
) : AuthRepository {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Unauthenticated)
    override val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    init {
        scope.launch {
            val savedSession = sessionDataSource.getSession()
            if (savedSession != null) {
                if (savedSession.isExpired) {
                    _sessionState.value = SessionState.Expired(savedSession)
                } else {
                    _sessionState.value = SessionState.Authenticated(savedSession)
                }
            } else {
                _sessionState.value = SessionState.Unauthenticated
            }
        }
    }

    override suspend fun login(command: LoginCommand): Result<SessionCredentials> {
        val result = apiClient.executeLogin(command)
        result.onSuccess { session ->
            sessionDataSource.saveSession(session)
            _sessionState.value = SessionState.Authenticated(session)
        }
        return result
    }

    override suspend fun clearSession() {
        sessionDataSource.clearSession()
        _sessionState.value = SessionState.Unauthenticated
    }
}
