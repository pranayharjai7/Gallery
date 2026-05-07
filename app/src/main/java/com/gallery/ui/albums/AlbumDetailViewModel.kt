package com.gallery.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.domain.model.MediaItem
import com.gallery.domain.usecase.AddToHiddenUseCase
import com.gallery.domain.usecase.GetAlbumMediaUseCase
import com.gallery.domain.usecase.MoveToTrashUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumDetailUiState(
    val isLoading: Boolean = true,
    val albumName: String = "",
    val items: List<MediaItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet(),
    val error: String? = null
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val getAlbumMedia: GetAlbumMediaUseCase,
    private val moveToTrash: MoveToTrashUseCase,
    private val addToHidden: AddToHiddenUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    private var loadJob: Job? = null

    fun loadAlbum(albumId: String) {
        loadJob?.cancel()
        val id = albumId.toLongOrNull() ?: run {
            _uiState.update { it.copy(isLoading = false) }
            return
        }
        loadJob = viewModelScope.launch {
            getAlbumMedia(id)
                .catch { e -> _uiState.update { it.copy(isLoading = false, error = e.message) } }
                .collect { items ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            albumName = items.firstOrNull()?.bucketName ?: "",
                            items = items
                        )
                    }
                }
        }
    }

    fun toggleSelection(id: Long) {
        _uiState.update { state ->
            val newSet = if (id in state.selectedIds) state.selectedIds - id else state.selectedIds + id
            state.copy(selectedIds = newSet)
        }
    }

    fun clearSelection() = _uiState.update { it.copy(selectedIds = emptySet()) }

    fun selectAll(ids: List<Long>) = _uiState.update { it.copy(selectedIds = ids.toSet()) }

    fun deleteSelected() {
        if (_uiState.value.selectedIds.isEmpty()) return
        viewModelScope.launch {
            val snapshot = _uiState.value
            val ids = snapshot.selectedIds
            try {
                snapshot.items
                    .filter { it.id in ids }
                    .forEach { moveToTrash(it) }
            } finally {
                clearSelection()
            }
        }
    }

    fun addSelectedToHidden() {
        val ids = _uiState.value.selectedIds.toSet()
        viewModelScope.launch {
            try {
                ids.forEach { addToHidden(it) }
            } finally {
                _uiState.update { it.copy(selectedIds = it.selectedIds - ids) }
            }
        }
    }
}
