package com.travelhistory.app.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.travelhistory.app.location.TrackingInterval
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "travel_history_settings")

class IntervalPreferences(private val context: Context) {

    private object PreferencesKeys {
        val INTERVAL_MINUTES = intPreferencesKey("interval_minutes")
        val IS_CUSTOM = booleanPreferencesKey("interval_is_custom")
        val IS_TRACKING_ACTIVE = booleanPreferencesKey("is_tracking_active")
        val RESUME_AFTER_REBOOT = booleanPreferencesKey("resume_after_reboot")
    }

    val intervalFlow: Flow<TrackingInterval> = context.dataStore.data.map { preferences ->
        val minutes = preferences[PreferencesKeys.INTERVAL_MINUTES] ?: TrackingInterval.DEFAULT.minutes
        val isCustom = preferences[PreferencesKeys.IS_CUSTOM] ?: TrackingInterval.DEFAULT.isCustom
        TrackingInterval(minutes = minutes, isCustom = isCustom)
    }

    val isTrackingActiveFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.IS_TRACKING_ACTIVE] ?: false
    }

    val resumeAfterRebootFlow: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.RESUME_AFTER_REBOOT] ?: false
    }

    suspend fun saveInterval(interval: TrackingInterval) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.INTERVAL_MINUTES] = interval.minutes
            preferences[PreferencesKeys.IS_CUSTOM] = interval.isCustom
        }
    }

    suspend fun setTrackingActive(active: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_TRACKING_ACTIVE] = active
        }
    }

    suspend fun setResumeAfterReboot(resume: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.RESUME_AFTER_REBOOT] = resume
        }
    }
}
