package com.example.qrbarcodescanner.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    private object PreferencesKeys {
        val COPY_TO_CLIPBOARD = booleanPreferencesKey("copy_to_clipboard")
        val RETRIEVE_URL_INFO = booleanPreferencesKey("retrieve_url_info")
        val AUTO_FOCUS = booleanPreferencesKey("auto_focus")
        val TOUCH_FOCUS = booleanPreferencesKey("touch_focus")
        val KEEP_DUPLICATES = booleanPreferencesKey("keep_duplicates")
        val USE_IN_APP_BROWSER = booleanPreferencesKey("use_in_app_browser")
        val ADD_SCANS_TO_HISTORY = booleanPreferencesKey("add_scans_to_history")
    }

    val copyToClipboard: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.COPY_TO_CLIPBOARD] ?: false
        }

    val retrieveUrlInfo: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.RETRIEVE_URL_INFO] ?: false
        }

    val autoFocus: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.AUTO_FOCUS] ?: true
        }

    val touchFocus: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.TOUCH_FOCUS] ?: false
        }

    val keepDuplicates: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.KEEP_DUPLICATES] ?: false
        }

    val useInAppBrowser: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.USE_IN_APP_BROWSER] ?: true
        }

    val addScansToHistory: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[PreferencesKeys.ADD_SCANS_TO_HISTORY] ?: true
        }

    suspend fun updateCopyToClipboard(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.COPY_TO_CLIPBOARD] = value
        }
    }

    suspend fun updateRetrieveUrlInfo(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RETRIEVE_URL_INFO] = value
        }
    }

    suspend fun updateAutoFocus(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.AUTO_FOCUS] = value
        }
    }

    suspend fun updateTouchFocus(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.TOUCH_FOCUS] = value
        }
    }

    suspend fun updateKeepDuplicates(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.KEEP_DUPLICATES] = value
        }
    }

    suspend fun updateUseInAppBrowser(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.USE_IN_APP_BROWSER] = value
        }
    }

    suspend fun updateAddScansToHistory(value: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.ADD_SCANS_TO_HISTORY] = value
        }
    }
}