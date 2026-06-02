package com.example.musicapplication.ui.mainPage

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.musicapplication.data.repository.AuthRepositoryImpl
import com.example.musicapplication.data.repository.MainPageRepositoryImpl
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class MainPageViewModel @Inject constructor(
    private val loginRepositoryImpi: AuthRepositoryImpl,
    private val mainPageRepositoryImpi: MainPageRepositoryImpl
): ViewModel() {
    private val TAG = "MainPageViewModel"


}