package com.gallery.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.data.prefs.GridSize
import com.gallery.data.prefs.ThemeMode
import com.gallery.data.prefs.UserPreferences
import com.gallery.data.prefs.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val prefs: UserPreferences = UserPreferences(),
    val isLoading: Boolean = true
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val prefsRepo: UserPreferencesRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            prefsRepo.preferences.collect { prefs ->
                _uiState.update { it.copy(prefs = prefs, isLoading = false) }
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { prefsRepo.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { prefsRepo.setDynamicColor(enabled) }
    }

    fun setGridSize(size: GridSize) {
        viewModelScope.launch { prefsRepo.setGridSize(size) }
    }

    fun setSlideshowInterval(seconds: Int) {
        viewModelScope.launch { prefsRepo.setSlideshowInterval(seconds) }
    }
}
