package com.u2k.w4llpaperch4nger

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "wallpaper_settings")

object ImageStorage {
    // 「画像のリスト」ではなく「フォルダの場所」を1つだけ保存する
    private val IMAGE_FOLDER_KEY = stringPreferencesKey("image_folder_uri")

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
}