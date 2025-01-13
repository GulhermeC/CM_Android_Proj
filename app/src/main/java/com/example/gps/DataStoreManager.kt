package com.example.gps

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class DataStoreManager(private val context: Context) {

    companion object {
        private val KEY_REMEMBER_ME = booleanPreferencesKey("remember_me")
        private val KEY_EMAIL = stringPreferencesKey("email")
    }

    val rememberMeFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_REMEMBER_ME] ?: false
    }

    val savedEmailFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[KEY_EMAIL] ?: ""
    }

    suspend fun saveUser(email: String, rememberMe: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_REMEMBER_ME] = rememberMe
            preferences[KEY_EMAIL] = if (rememberMe) email else ""
        }
    }

    suspend fun clearUser() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_REMEMBER_ME)
            preferences.remove(KEY_EMAIL)
        }
    }
}