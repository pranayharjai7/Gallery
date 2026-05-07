package com.gallery

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.gallery.data.prefs.ThemeMode
import com.gallery.data.prefs.UserPreferences
import com.gallery.data.prefs.UserPreferencesRepository
import com.gallery.ui.GalleryScaffold
import com.gallery.ui.theme.GalleryTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefsRepo: UserPreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val prefs by prefsRepo.preferences.collectAsStateWithLifecycle(initialValue = UserPreferences())
            GalleryTheme(
                darkTheme = when (prefs.themeMode) {
                    ThemeMode.DARK -> true
                    ThemeMode.LIGHT -> false
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                },
                dynamicColor = prefs.dynamicColor
            ) {
                GalleryScaffold()
            }
        }
    }
}
