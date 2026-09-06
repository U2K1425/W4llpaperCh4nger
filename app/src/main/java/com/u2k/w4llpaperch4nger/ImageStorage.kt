package com.u2k.w4llpaperch4nger

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "wallpaper_settings")

object ImageStorage {
    private val IMAGE_FOLDER_KEY = stringPreferencesKey("image_folder_uri")
    private val INTERVAL_MINUTES_KEY = intPreferencesKey("interval_minutes")

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

    // 切り替え間隔(分)を保存する
    suspend fun saveInterval(context: Context, minutes: Int) {
        context.dataStore.edit { preferences ->
            preferences[INTERVAL_MINUTES_KEY] = minutes
        }
    }

    // 保存されている間隔を取得する(未設定の場合は60分をデフォルトにする)
    fun getInterval(context: Context): Flow<Int> {
        return context.dataStore.data.map { preferences ->
            preferences[INTERVAL_MINUTES_KEY] ?: 60
        }
    }
}