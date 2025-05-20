package com.tuempresa.fugas.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.onboardingDataStore by preferencesDataStore(name = "onboarding")

object OnboardingKeys {
    val DEVICE_LINKED = booleanPreferencesKey("device_linked")
}

class OnboardingDataStore(private val context: Context) {
    val isDeviceLinked: Flow<Boolean> = context.onboardingDataStore.data.map { prefs ->
        prefs[OnboardingKeys.DEVICE_LINKED] ?: false
    }
    suspend fun setDeviceLinked(linked: Boolean) {
        context.onboardingDataStore.edit { prefs ->
            prefs[OnboardingKeys.DEVICE_LINKED] = linked
        }
    }
}
