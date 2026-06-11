package com.example.musicapplication.data.local.search

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.searchHistoryDataStore by preferencesDataStore(
    name = "search_history_prefs"
)

class DataStoreSearchHistoryStore @Inject constructor(
    @ApplicationContext private val context: Context
) : SearchHistoryStore {

    private val searchHistoryKey = stringPreferencesKey("search_history")
    private val maxHistorySize = 10

    override val searchHistoryFlow: Flow<List<Long>> = context.searchHistoryDataStore.data
        .catch {
            emit(emptyPreferences())
        }
        .map { prefs ->
            prefs[searchHistoryKey]
                ?.split("|")
                ?.map { it.trim() }
                ?.mapNotNull { it.toLongOrNull() }
                .orEmpty()
        }

    override suspend fun saveSong(songId: Long) {
        val oldHistory = searchHistoryFlow.first()
        val newHistory = listOf(songId) + oldHistory.filterNot { it == songId }

        context.searchHistoryDataStore.edit { prefs ->
            prefs[searchHistoryKey] = newHistory
                .take(maxHistorySize)
                .joinToString("|")
        }
    }

    override suspend fun clearSearchHistory() {
        context.searchHistoryDataStore.edit { prefs ->
            prefs.remove(searchHistoryKey)
        }
    }
}
