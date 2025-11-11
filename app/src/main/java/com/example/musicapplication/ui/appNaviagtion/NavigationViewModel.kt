package com.example.musicapplication.ui.appNaviagtion

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import com.example.musicapplication.data.remote.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import jakarta.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class NavigationViewModel @Inject constructor(

) : ViewModel() {
    private val TAG = "NavigationViewModel"
    private val _openExternalSong = MutableStateFlow<Pair<Song?, Uri?>?>(null)
    val openExternalSong = _openExternalSong.asStateFlow()

    fun changeOpenExternalSong(externalSong: Pair<Song?, Uri?>?) {
        _openExternalSong.value = externalSong
        Log.d(TAG, externalSong?.first.toString() + " " + externalSong?.second)
    }
}