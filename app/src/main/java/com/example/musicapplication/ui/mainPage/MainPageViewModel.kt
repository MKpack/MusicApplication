package com.example.musicapplication.ui.mainPage

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapplication.data.repository.LoginRepositoryImpi
import com.example.musicapplication.data.repository.MainPageRepositoryImpi
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainPageViewModel @Inject constructor(
    private val loginRepositoryImpi: LoginRepositoryImpi,
    private val mainPageRepositoryImpi: MainPageRepositoryImpi
): ViewModel() {
    private val TAG = "MainPageViewModel"
    private val _accessToken = MutableStateFlow("")
    val accessToken: MutableStateFlow<String> = _accessToken

    private val _refreshToken = MutableStateFlow("")
    val refreshToken: MutableStateFlow<String> = _refreshToken

    private val _testMsg = MutableStateFlow("")
    val testMsg: MutableStateFlow<String> = _testMsg

    public fun showToken(context: Context) {
        val allToken = loginRepositoryImpi.getCachedToken()
        _accessToken.value = allToken["accessToken"].toString()
        _refreshToken.value = allToken["refreshToken"].toString()
    }

    public fun refresh() {
        viewModelScope.launch {
            _accessToken.value = loginRepositoryImpi.refresh(_refreshToken.value).toString()
            Log.d(TAG, _accessToken.value)
        }
    }


    public fun getTextMsg() {
        viewModelScope.launch {
            _testMsg.value = mainPageRepositoryImpi.test().toString()
        }
    }
}