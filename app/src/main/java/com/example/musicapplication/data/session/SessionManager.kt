package com.example.musicapplication.data.session

import com.example.musicapplication.data.local.token.TokenStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SessionManager @Inject constructor(
    private val tokenStore: TokenStore
) {
    private val _authState = MutableStateFlow(
        if (tokenStore.hasLoginState()) {
            AuthState.Authenticated
        } else {
            AuthState.Unauthenticated
        }
    )

    val authState = _authState.asStateFlow()

    fun onLoginSuccess(accessToken: String, refreshToken: String) {
        tokenStore.saveTokens(accessToken, refreshToken)
        _authState.value = AuthState.Authenticated
    }

    fun onSessionExpired() {
        tokenStore.clear()
        _authState.value = AuthState.SessionExpired
    }

    fun logout() {
        tokenStore.clear()
        _authState.value = AuthState.Unauthenticated
    }

    fun markUnauthenticated() {
        _authState.value = AuthState.Unauthenticated
    }
}