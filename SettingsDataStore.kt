package com.tuempresa.fugas.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

object SettingsKeys {
    val ENDPOINT = stringPreferencesKey("endpoint")
}

class SettingsDataStore(private val context: Context) {
    val endpoint: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[SettingsKeys.ENDPOINT] ?: "https://tubackend.com/api/"
    }
    suspend fun setEndpoint(url: String) {
        context.dataStore.edit { prefs ->
            prefs[SettingsKeys.ENDPOINT] = url
        }
    }
}
