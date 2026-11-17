package com.example.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "hey_bk_prefs")

data class HeyBkSettings(
    val darkTheme: Boolean = true,
    val reminderEnabled: Boolean = true,
    val reminderHour: Int = 18, // 6:00 PM default
    val reminderMinute: Int = 0,
    val defaultBookId: String = "book_diary",
    val displayName: String = "Bk"
)

class AppPreferencesDataStore(private val context: Context) {

    private object Keys {
        val DARK_THEME = booleanPreferencesKey("dark_theme")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val DEFAULT_BOOK_ID = stringPreferencesKey("default_book_id")
        val DISPLAY_NAME = stringPreferencesKey("display_name")
    }

    val settingsFlow: Flow<HeyBkSettings> = context.dataStore.data.map { prefs ->
        HeyBkSettings(
            darkTheme = prefs[Keys.DARK_THEME] ?: true,
            reminderEnabled = prefs[Keys.REMINDER_ENABLED] ?: true,
            reminderHour = prefs[Keys.REMINDER_HOUR] ?: 18,
            reminderMinute = prefs[Keys.REMINDER_MINUTE] ?: 0,
            defaultBookId = prefs[Keys.DEFAULT_BOOK_ID] ?: "book_diary",
            displayName = prefs[Keys.DISPLAY_NAME] ?: "Bk"
        )
    }

    suspend fun setDarkTheme(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DARK_THEME] = enabled }
    }

    suspend fun setReminder(enabled: Boolean, hour: Int, minute: Int) {
        context.dataStore.edit {
            it[Keys.REMINDER_ENABLED] = enabled
            it[Keys.REMINDER_HOUR] = hour
            it[Keys.REMINDER_MINUTE] = minute
        }
    }

    suspend fun setDefaultBookId(bookId: String) {
        context.dataStore.edit { it[Keys.DEFAULT_BOOK_ID] = bookId }
    }

    suspend fun setDisplayName(name: String) {
        context.dataStore.edit { it[Keys.DISPLAY_NAME] = name.ifBlank { "Bk" } }
    }
}
