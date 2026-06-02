package com.example.musicapplication.data.session

interface AuthState {
    data object Authenticated : AuthState
    data object Unauthenticated : AuthState
    data object SessionExpired : AuthState
}