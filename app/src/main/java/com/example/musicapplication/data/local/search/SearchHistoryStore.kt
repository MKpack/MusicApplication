package com.example.musicapplication.data.local.search

import kotlinx.coroutines.flow.Flow

interface SearchHistoryStore {
    val searchHistoryFlow: Flow<List<Long>>

    suspend fun saveSong(songId: Long)

    suspend fun clearSearchHistory()
}
