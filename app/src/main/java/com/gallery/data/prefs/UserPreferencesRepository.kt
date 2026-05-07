package com.gallery.data.prefs

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
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

    val preferences: Flow<UserPreferences> = dataStore.data.map { prefs ->
        UserPreferences(
            themeMode = ThemeMode.valueOf(prefs[Keys.THEME_MODE] ?: ThemeMode.SYSTEM.name),
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: true,
            gridSize = GridSize.valueOf(prefs[Keys.GRID_SIZE] ?: GridSize.NORMAL.name),
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
