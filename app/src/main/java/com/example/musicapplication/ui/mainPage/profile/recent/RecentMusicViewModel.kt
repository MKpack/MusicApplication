package com.example.musicapplication.ui.mainPage.profile.recent

import androidx.lifecycle.ViewModel
import com.example.musicapplication.data.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject



@HiltViewModel
class RecentMusicViewModel @Inject constructor(
    private val songRepository: SongRepository
) : ViewModel() {


}