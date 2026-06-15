package com.example.musicapplication.data.local.player

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.playerStateDataStore by preferencesDataStore(
    name = "player_state_prefs"
)

class DataStorePlayerStateStore @Inject constructor(
    @ApplicationContext private val context: Context
) : PlayerStateStore {
    private val gson = Gson()

    private object Keys {
        val SONGS = stringPreferencesKey("songs")
        val SONG_IDS = stringPreferencesKey("song_ids")
        val CURRENT_INDEX = intPreferencesKey("current_index")
        val POSITION_MS = longPreferencesKey("position_ms")
        val PLAY_MODE = stringPreferencesKey("play_mode")
    }

    override val snapshotFlow: Flow<PlayerSnapshot?> = context.playerStateDataStore.data
        .catch {
            emit(emptyPreferences())
        }
        .map { prefs ->
            val songs = prefs[Keys.SONGS]
                ?.let { json ->
                    runCatching {
                        gson.fromJson(json, Array<PlayerSnapshotSong>::class.java).toList()
                    }.getOrDefault(emptyList())
                }
                .orEmpty()

            val legacySongIds = prefs[Keys.SONG_IDS]
                ?.split("|")
                ?.mapNotNull { it.toLongOrNull() }
                .orEmpty()

            if (songs.isEmpty() && legacySongIds.isEmpty()) return@map null

            val currentIndex = prefs[Keys.CURRENT_INDEX] ?: 0
            val queueSize = if (songs.isNotEmpty()) songs.size else legacySongIds.size
            PlayerSnapshot(
                songs = songs,
                songIds = legacySongIds,
                currentIndex = currentIndex.coerceIn(0, queueSize - 1),
                positionMs = prefs[Keys.POSITION_MS] ?: 0L,
                playModeName = prefs[Keys.PLAY_MODE].orEmpty()
            )
        }

    override suspend fun saveSnapshot(snapshot: PlayerSnapshot) {
        if (snapshot.songs.isEmpty() && snapshot.songIds.isEmpty()) return

        context.playerStateDataStore.edit { prefs ->
            if (snapshot.songs.isNotEmpty()) {
                prefs[Keys.SONGS] = gson.toJson(snapshot.songs)
            }
            val songIds = snapshot.songIds.ifEmpty {
                snapshot.songs.map { it.songId }
            }
            prefs[Keys.SONG_IDS] = songIds.joinToString("|")
            prefs[Keys.CURRENT_INDEX] = snapshot.currentIndex
            prefs[Keys.POSITION_MS] = snapshot.positionMs.coerceAtLeast(0L)
            prefs[Keys.PLAY_MODE] = snapshot.playModeName
        }
    }

    override suspend fun clearSnapshot() {
        context.playerStateDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
