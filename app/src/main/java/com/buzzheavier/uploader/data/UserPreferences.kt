package com.buzzheavier.uploader.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "buzzheavier_settings")

class UserPreferences(private val context: Context) {

    companion object {
        val KEY_ACCOUNT_ID = stringPreferencesKey("account_id")
        val KEY_PARENT_DIR = stringPreferencesKey("parent_directory")
        val KEY_LOCATION_ID = stringPreferencesKey("location_id")
        val KEY_IS_ANONYMOUS = booleanPreferencesKey("is_anonymous")
        val KEY_LAST_NOTE = stringPreferencesKey("last_note")
    }

    val accountId: Flow<String> = context.dataStore.data.map { it[KEY_ACCOUNT_ID] ?: "" }
    val parentDirectory: Flow<String> = context.dataStore.data.map { it[KEY_PARENT_DIR] ?: "" }
    val locationId: Flow<String> = context.dataStore.data.map { it[KEY_LOCATION_ID] ?: "" }
    val isAnonymous: Flow<Boolean> = context.dataStore.data.map { it[KEY_IS_ANONYMOUS] ?: true }
    val lastNote: Flow<String> = context.dataStore.data.map { it[KEY_LAST_NOTE] ?: "" }

    suspend fun saveAccountId(id: String) {
        context.dataStore.edit { it[KEY_ACCOUNT_ID] = id }
    }

    suspend fun saveParentDirectory(dir: String) {
        context.dataStore.edit { it[KEY_PARENT_DIR] = dir }
    }

    suspend fun saveLocationId(id: String) {
        context.dataStore.edit { it[KEY_LOCATION_ID] = id }
    }

    suspend fun saveIsAnonymous(anonymous: Boolean) {
        context.dataStore.edit { it[KEY_IS_ANONYMOUS] = anonymous }
    }

    suspend fun saveLastNote(note: String) {
        context.dataStore.edit { it[KEY_LAST_NOTE] = note }
    }
}
