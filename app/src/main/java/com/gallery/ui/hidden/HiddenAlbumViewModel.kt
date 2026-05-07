package com.gallery.ui.hidden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.domain.model.MediaItem
import com.gallery.domain.usecase.GetHiddenMediaUseCase
import com.gallery.domain.usecase.RemoveFromHiddenUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class HiddenAuthState {
    object Locked : HiddenAuthState()
    object Authenticating : HiddenAuthState()
    object Unlocked : HiddenAuthState()
    data class Error(val message: String) : HiddenAuthState()
    object HardwareUnavailable : HiddenAuthState()
}

data class HiddenAlbumUiState(
    val authState: HiddenAuthState = HiddenAuthState.Locked,
    val items: List<MediaItem> = emptyList(),
    val isLoading: Boolean = false,
    val selectedIds: Set<Long> = emptySet()
)

@HiltViewModel
class HiddenAlbumViewModel @Inject constructor(
    private val getHiddenMedia: GetHiddenMediaUseCase,
    private val removeFromHidden: RemoveFromHiddenUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HiddenAlbumUiState())
    val uiState: StateFlow<HiddenAlbumUiState> = _uiState.asStateFlow()

    fun onAuthSuccess() {
        _uiState.update { it.copy(authState = HiddenAuthState.Unlocked, isLoading = true) }
        viewModelScope.launch {
            getHiddenMedia()
                .catch { _ -> _uiState.update { it.copy(isLoading = false) } }
                .collect { items ->
                    _uiState.update { it.copy(items = items, isLoading = false) }
                }
        }
    }

    fun onAuthError(message: String) {
        _uiState.update { it.copy(authState = HiddenAuthState.Error(message)) }
    }

    fun onHardwareUnavailable() {
        _uiState.update { it.copy(authState = HiddenAuthState.HardwareUnavailable) }
    }

    fun retryAuth() {
        _uiState.update { it.copy(authState = HiddenAuthState.Locked) }
    }

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val newSelected = if (id in state.selectedIds) {
                state.selectedIds - id
            } else {
                state.selectedIds + id
            }
            state.copy(selectedIds = newSelected)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedIds = emptySet()) }
    }

    fun unhideSelected() {
        val ids = _uiState.value.selectedIds.toSet()
        _uiState.update { it.copy(selectedIds = emptySet()) }
        viewModelScope.launch {
            ids.forEach { id -> removeFromHidden(id) }
        }
    }

    fun removeFromHiddenItem(id: Long) {
        viewModelScope.launch {
            removeFromHidden(id)
        }
    }
}
