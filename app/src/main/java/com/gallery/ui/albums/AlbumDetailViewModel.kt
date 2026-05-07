package com.gallery.ui.albums

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.gallery.domain.model.MediaItem
import com.gallery.domain.usecase.GetAlbumMediaUseCase
import com.gallery.domain.usecase.MoveToTrashUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AlbumDetailUiState(
    val isLoading: Boolean = true,
    val albumName: String = "",
    val items: List<MediaItem> = emptyList(),
    val selectedIds: Set<Long> = emptySet()
)

@HiltViewModel
class AlbumDetailViewModel @Inject constructor(
    private val getAlbumMedia: GetAlbumMediaUseCase,
    private val moveToTrash: MoveToTrashUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AlbumDetailUiState())
    val uiState: StateFlow<AlbumDetailUiState> = _uiState.asStateFlow()

    fun loadAlbum(albumId: String) {
        viewModelScope.launch {
            getAlbumMedia(albumId.toLong()).collect { items ->
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
            snapshot.items
                .filter { it.id in ids }
                .forEach { moveToTrash(it) }
            clearSelection()
        }
    }
}
