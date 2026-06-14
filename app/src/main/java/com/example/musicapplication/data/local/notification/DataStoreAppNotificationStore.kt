package com.example.musicapplication.data.local.notification

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private val Context.appNotificationDataStore by preferencesDataStore(
    name = "app_notification_prefs"
)

class DataStoreAppNotificationStore @Inject constructor(
    @ApplicationContext private val context: Context
) : AppNotificationStore {

    private val lastAutoShownNotificationIdKey = longPreferencesKey("last_auto_shown_notification_id")

    override suspend fun getLastAutoShownNotificationId(): Long? {
        return context.appNotificationDataStore.data
            .catch {
                emit(androidx.datastore.preferences.core.emptyPreferences())
            }
            .map { prefs ->
                prefs[lastAutoShownNotificationIdKey]
            }
            .first()
    }

    override suspend fun saveLastAutoShownNotificationId(notificationId: Long) {
        context.appNotificationDataStore.edit { prefs ->
            prefs[lastAutoShownNotificationIdKey] = notificationId
        }
    }
}
