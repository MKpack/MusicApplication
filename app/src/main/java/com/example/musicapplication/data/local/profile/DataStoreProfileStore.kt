package com.example.musicapplication.data.local.profile

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.musicapplication.domain.model.UserProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import javax.inject.Inject


private val Context.profileDataStore by preferencesDataStore(
    name = "profile_prefs"
)

class DataStoreProfileStore @Inject constructor(
    @ApplicationContext private val context: Context
) : ProfileStore {

    private object Keys {
        // 新KEY：Long类型
        val USER_ID = longPreferencesKey("user_id_long")
        // 旧KEY：以前保存过 Int 类型
        val LEGACY_USER_ID = intPreferencesKey("user_id")
        val EMAIL = stringPreferencesKey("email")
        val NICK_NAME = stringPreferencesKey("nick_name")
        val AVATAR_URL = stringPreferencesKey("avatar_url")
    }

    override val profileFlow = context.profileDataStore.data.map { prefs ->
        val userId: Long = prefs[Keys.USER_ID]
            ?: prefs[Keys.LEGACY_USER_ID]?.toLong()
            ?: return@map null

        UserProfile(
            userId = userId,
            email = prefs[Keys.EMAIL].orEmpty(),
            nickName = prefs[Keys.NICK_NAME],
            avatarUrl = prefs[Keys.AVATAR_URL]
        )
    }

    override suspend fun saveProfile(profile: UserProfile) {
        context.profileDataStore.edit { prefs ->
            // 删除旧类型Int
            prefs.remove(Keys.LEGACY_USER_ID)
            prefs[Keys.USER_ID] = profile.userId
            prefs[Keys.EMAIL] = profile.email
            prefs[Keys.NICK_NAME] = profile.nickName.orEmpty()
            prefs[Keys.AVATAR_URL] = profile.avatarUrl.orEmpty()
        }
    }

    override suspend fun clearProfile() {
        context.profileDataStore.edit { prefs ->
            prefs.clear()
        }
    }
}
