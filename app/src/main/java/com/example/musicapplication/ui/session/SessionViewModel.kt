package com.example.musicapplication.ui.session

import androidx.lifecycle.ViewModel
import com.example.musicapplication.data.session.SessionManager
import com.example.musicapplication.domain.player.PlayerController
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionManager: SessionManager,
    private val playerController: PlayerController
) : ViewModel() {
    val authState = sessionManager.authState

    fun consumeSessionExpired() {
        sessionManager.markUnauthenticated()
        // 登录过期后可能换别的账号登录，不能接着放上一个账号的队列
        playerController.clear()
    }

    fun logout() {
        sessionManager.logout()
        playerController.clear()
    }
}
