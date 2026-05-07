package com.gallery.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val gridSize: GridSize = GridSize.NORMAL,
    val slideshowInterval: Int = 4
)

enum class ThemeMode { LIGHT, DARK, SYSTEM }
enum class GridSize(val columns: Int) { COMPACT(4), NORMAL(3), LARGE(2) }

@Singleton
class UserPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    private object Keys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val GRID_SIZE = stringPreferencesKey("grid_size")
        val SLIDESHOW_INTERVAL = intPreferencesKey("slideshow_interval")
    }

    val preferences: Flow<UserPreferences> = dataStore.data
        .catch { cause ->
            if (cause is IOException) emit(emptyPreferences())
            else throw cause
        }
        .map { prefs ->
            UserPreferences(
                themeMode = prefs[Keys.THEME_MODE]
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM,
                dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
                gridSize = prefs[Keys.GRID_SIZE]
                    ?.let { runCatching { GridSize.valueOf(it) }.getOrNull() }
                    ?: GridSize.NORMAL,
                slideshowInterval = prefs[Keys.SLIDESHOW_INTERVAL] ?: 4
            )
        }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    suspend fun setGridSize(size: GridSize) {
        dataStore.edit { it[Keys.GRID_SIZE] = size.name }
    }

    suspend fun setSlideshowInterval(seconds: Int) {
        dataStore.edit { it[Keys.SLIDESHOW_INTERVAL] = seconds }
    }
}
