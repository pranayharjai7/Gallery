package com.pranayharjai7.gallery

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pranayharjai7.gallery.data.prefs.ThemeMode
import com.pranayharjai7.gallery.data.prefs.UserPreferences
import com.pranayharjai7.gallery.data.prefs.UserPreferencesRepository
import com.pranayharjai7.gallery.ui.GalleryScaffold
import com.pranayharjai7.gallery.ui.common.MediaPermissionsWrapper
import com.pranayharjai7.gallery.ui.theme.GalleryTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : FragmentActivity() {

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
                MediaPermissionsWrapper {
                    GalleryScaffold()
                }
            }
        }
    }
}
