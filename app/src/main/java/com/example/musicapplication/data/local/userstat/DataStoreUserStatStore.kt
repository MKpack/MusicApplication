package com.example.musicapplication.data.local.userstat

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.musicapplication.domain.model.UserStat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.userStatDataStore by preferencesDataStore(
    name = "user_stat_prefs"
)

class DataStoreUserStatStore @Inject constructor(
    @ApplicationContext private val context: Context
) : UserStatStore {

    private object Keys {
        val USER_ID = longPreferencesKey("user_id")
        val PLAY_COUNT = longPreferencesKey("play_count")
        val FAVORITE_COUNT = longPreferencesKey("favorite_count")
    }

    override val userStatFlow = context.userStatDataStore.data.map { prefs ->
        val userId = prefs[Keys.USER_ID] ?: return@map null

        UserStat(
            userId = userId,
            playCount = prefs[Keys.PLAY_COUNT] ?: 0L,
            favoriteCount = prefs[Keys.FAVORITE_COUNT] ?: 0L
        )
    }

    override suspend fun saveUserStat(userStat: UserStat) {
        context.userStatDataStore.edit { prefs ->
            prefs[Keys.USER_ID] = userStat.userId
            prefs[Keys.PLAY_COUNT] = userStat.playCount
            prefs[Keys.FAVORITE_COUNT] = userStat.favoriteCount
        }
    }

    override suspend fun clearUserStat() {
        context.userStatDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
