package com.example.musicapplication.ui.session

import androidx.lifecycle.ViewModel
import com.example.musicapplication.data.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionManager: SessionManager
) : ViewModel() {
    val authState = sessionManager.authState

    fun consumeSessionExpired() {
        sessionManager.markUnauthenticated()
    }

    fun logout() {
        sessionManager.logout()
    }
}