package com.example.musicapplication.data.local.token

import android.content.Context
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class SharedPrefsTokenStore @Inject constructor(
    @ApplicationContext private val context: Context
) : TokenStore {

    private val prefs by lazy {
        context.getSharedPreferences("token_prefs", Context.MODE_PRIVATE)
    }

    override fun getAccessToken(): String? {
        return prefs.getString("access_token", null)
    }

    override fun getRefreshToken(): String? {
        return prefs.getString("refresh_token", null)
    }

    override fun saveTokens(accessToken: String?, refreshToken: String?) {
        prefs.edit {
            if (!accessToken.isNullOrBlank()) {
                putString("access_token", accessToken)
            }
            if (!refreshToken.isNullOrBlank()) {
                putString("refresh_token", refreshToken)
            }
        }
    }

    override fun updateAccessToken(accessToken: String?) {
        prefs.edit {
            if (!accessToken.isNullOrBlank()) {
                putString("access_token", accessToken)
            }
        }
    }

    override fun clear() {
        prefs.edit {
            remove("access_token")
            remove("refresh_token")
        }
    }

    override fun hasLoginState(): Boolean {
        return !getAccessToken().isNullOrBlank() || !getRefreshToken().isNullOrBlank()
    }
}