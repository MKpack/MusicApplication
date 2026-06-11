package com.example.musicapplication.data.local.player

import kotlinx.coroutines.flow.Flow

interface PlayerStateStore {
    val snapshotFlow: Flow<PlayerSnapshot?>

    suspend fun saveSnapshot(snapshot: PlayerSnapshot)

    suspend fun clearSnapshot()
}
