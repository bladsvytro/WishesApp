package com.wishesapp.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.wishesapp.data.model.WidgetMode
import com.wishesapp.data.model.WidgetState
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.widgetDataStore: DataStore<Preferences> by preferencesDataStore("widget_prefs")

@Singleton
class WidgetPrefsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val MODE = stringPreferencesKey("widget_mode")
    private val WISH_ID = stringPreferencesKey("wish_id")
    private val WISH_TEXT = stringPreferencesKey("wish_text")
    private val AUTHOR_NAME = stringPreferencesKey("author_name")
    private val IMAGE_PATH = stringPreferencesKey("image_path")

    val widgetState: Flow<WidgetState> = context.widgetDataStore.data.map { prefs ->
        WidgetState(
            mode = WidgetMode.valueOf(prefs[MODE] ?: WidgetMode.LATEST.name),
            wishId = prefs[WISH_ID] ?: "",
            wishText = prefs[WISH_TEXT] ?: "",
            authorName = prefs[AUTHOR_NAME] ?: "",
            imagePath = prefs[IMAGE_PATH],
        )
    }

    suspend fun saveWidgetState(state: WidgetState) {
        context.widgetDataStore.edit { prefs ->
            prefs[MODE] = state.mode.name
            prefs[WISH_ID] = state.wishId
            prefs[WISH_TEXT] = state.wishText
            prefs[AUTHOR_NAME] = state.authorName
            state.imagePath?.let { prefs[IMAGE_PATH] = it } ?: prefs.remove(IMAGE_PATH)
        }
    }

    suspend fun setWidgetMode(mode: WidgetMode) {
        context.widgetDataStore.edit { prefs ->
            prefs[MODE] = mode.name
        }
    }
}
