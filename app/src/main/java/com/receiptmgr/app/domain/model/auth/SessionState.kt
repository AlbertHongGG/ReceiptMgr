package com.receiptmgr.app.domain.model.auth

sealed interface SessionState {
    data object Unauthenticated : SessionState
    data class Authenticated(val credentials: SessionCredentials) : SessionState
    data class Expired(val credentials: SessionCredentials) : SessionState
}
