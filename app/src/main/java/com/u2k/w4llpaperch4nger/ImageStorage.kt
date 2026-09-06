package com.u2k.w4llpaperch4nger

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "wallpaper_settings")

object ImageStorage {
    private val IMAGE_FOLDER_KEY = stringPreferencesKey("image_folder_uri")
    private val INTERVAL_MINUTES_KEY = intPreferencesKey("interval_minutes")
    private val LAST_UPDATED_KEY = longPreferencesKey("last_updated_millis")

    suspend fun saveFolder(context: Context, folderUri: String) {
        context.dataStore.edit { preferences ->
            preferences[IMAGE_FOLDER_KEY] = folderUri
        }
    }

    fun getFolder(context: Context): Flow<String?> {
        return context.dataStore.data.map { preferences ->
            preferences[IMAGE_FOLDER_KEY]
        }
    }

    suspend fun saveInterval(context: Context, minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[INTERVAL_MINUTES_KEY] = minutes
        }
    }

    fun getInterval(context: Context): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[INTERVAL_MINUTES_KEY] ?: 60
        }
    }

    // 実際に壁紙が切り替わった日時(ミリ秒)を保存する
    suspend fun saveLastUpdated(context: Context, millis: Long) {
        context.dataStore.edit { preferences ->
            preferences[LAST_UPDATED_KEY] = millis
        }
    }

    fun getLastUpdated(context: Context): Flow<Long?> {
        return context.dataStore.data.map { preferences ->
            preferences[LAST_UPDATED_KEY]
        }
    }
}